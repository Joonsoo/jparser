package com.giyeok.jparser.nparser2

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NExcept, NJoin, NLongest, NLookaheadExcept, NLookaheadIs, NLookaheadSymbol, NSequence, NSimpleDerive, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, Exists, Never, NotExists, OnlyIf, Unless, conjunct, disjunct}

import scala.annotation.tailrec

sealed trait ParsingTask

case class DeriveTask(kernel: Kernel) extends ParsingTask

case class FinishTask(kernel: Kernel) extends ParsingTask

case class ProgressTask(kernel: Kernel, condition: AcceptCondition) extends ParsingTask

class ParsingTaskImpl(val grammar: NGrammar) {
  private def isFinal(kernel: Kernel): Boolean = {
    grammar.symbolOf(kernel.symbolId) match {
      case _: NGrammar.NAtomicSymbol => kernel.pointer == 1
      case NGrammar.NSequence(_, _, sequence) => kernel.pointer == sequence.length
    }
  }

  def deriveTask(nextGen: Int, task: DeriveTask, ctx: ParsingContext): (ParsingContext, List[ParsingTask]) = {
    assert(ctx.graph.nodes contains task.kernel)
    assert(!isFinal(task.kernel))
    assert(task.kernel.endGen == nextGen)

    def derive0(cc: (ParsingContext, List[ParsingTask]), symbolId: Int): (ParsingContext, List[ParsingTask]) = {
      val newNode = Kernel(symbolId, 0, nextGen, nextGen)
      if (!(cc._1.graph.nodes contains newNode)) {
        val newCtx = ParsingContext(
          cc._1.graph.addNode(newNode).addEdge(Edge(task.kernel, newNode)),
          cc._1.acceptConditions + (newNode -> Always))
        val newTask = if (isFinal(newNode)) FinishTask(newNode) else DeriveTask(newNode)
        (newCtx, newTask +: cc._2)
      } else {
        // newNode가 그래프에 이미 들어있는 경우

        // 1. task.kernel -> newNode 엣지를 추가해야 하고,
        // 2. newNode가 Progress된 경우 task.kernel -> newNode의 다음 kernel로 가는 엣지도 추가해야 하고,
        // 3. 그렇게 추가한 노드들 중 (task.kernel에서부터) 종료된 노드로 가는 엣지가 생기면 task.kernel에 대한 ProgressTask 추가
        // -> 기존의 updatedNodesMap을 대체

        @tailrec
        def addNext(cc: (ParsingContext, List[ParsingTask]), newNode: Kernel): (ParsingContext, List[ParsingTask]) = {
          val newCtx = ParsingContext(cc._1.graph.addEdge(Edge(task.kernel, newNode)), cc._1.acceptConditions)
          if (isFinal(newNode)) {
            val newTask = ProgressTask(task.kernel, cc._1.acceptConditions(newNode))
            (newCtx, newTask +: cc._2)
          } else {
            val nextNode = newNode.copy(pointer = newNode.pointer + 1)
            if (cc._1.graph.nodes contains nextNode) {
              addNext((newCtx, cc._2), nextNode)
            } else {
              (newCtx, cc._2)
            }
          }
        }

        addNext(cc, newNode)
      }
    }

    def addNode0(cc: (ParsingContext, List[ParsingTask]), symbolId: Int): (ParsingContext, List[ParsingTask]) = {
      val newNode = Kernel(symbolId, 0, nextGen, nextGen)
      val newTask = if (isFinal(newNode)) FinishTask(newNode) else DeriveTask(newNode)
      (ParsingContext(cc._1.graph.addNode(newNode), cc._1.acceptConditions + (newNode -> Always)), newTask +: cc._2)
    }

    grammar.symbolOf(task.kernel.symbolId) match {
      case _: NTerminal => (ctx, List()) // do nothing
      case derives: NSimpleDerive =>
        val result = derives.produces.foldLeft((ctx, List[ParsingTask]())) { (cc, symbolId) => derive0(cc, symbolId) }
        result
      case NExcept(_, _, body, except) =>
        val cc1 = derive0((ctx, List()), body)
        addNode0(cc1, except)
      case NJoin(_, _, body, join) =>
        val cc1 = derive0((ctx, List()), body)
        addNode0(cc1, join)
      case NLongest(_, _, body) =>
        derive0((ctx, List()), body)
      case lookahead: NLookaheadSymbol =>
        addNode0(derive0((ctx, List()), lookahead.emptySeqId), lookahead.lookahead)
      case NSequence(_, _, sequence) =>
        derive0((ctx, List()), sequence(task.kernel.pointer))
    }
  }

  def finishTask(nextGen: Int, task: FinishTask, ctx: ParsingContext): (ParsingContext, List[ParsingTask]) = {
    assert(ctx.graph.nodes contains task.kernel)
    assert(isFinal(task.kernel))
    assert(task.kernel.endGen == nextGen)

    val incomingEdges = ctx.graph.edgesByEnd(task.kernel)
    val chainTasks = incomingEdges map { edge =>
      ProgressTask(edge.start, ctx.acceptConditions(task.kernel))
    }
    (ctx, chainTasks.toList)
  }

  def progressTask(nextGen: Int, task: ProgressTask, ctx: ParsingContext): (ParsingContext, List[ParsingTask]) = {
    assert(ctx.graph.nodes contains task.kernel)
    assert(!isFinal(task.kernel))

    val newKernel = Kernel(task.kernel.symbolId, task.kernel.pointer + 1, task.kernel.beginGen, nextGen)

    val incomingEdges = ctx.graph.edgesByEnd(task.kernel)
    val newEdges = incomingEdges map { edge => Edge(edge.start, newKernel) }

    val addingCondition = grammar.symbolOf(task.kernel.symbolId) match {
      case NLongest(_, _, longest) =>
        NotExists(task.kernel.beginGen, nextGen + 1, longest)
      case NExcept(_, _, _, except) =>
        Unless(task.kernel.beginGen, nextGen, except)
      case NJoin(_, _, _, join) =>
        OnlyIf(task.kernel.beginGen, nextGen, join)
      case NLookaheadIs(_, _, _, lookahead) =>
        Exists(nextGen, nextGen, lookahead)
      case NLookaheadExcept(_, _, _, lookahead) =>
        NotExists(nextGen, nextGen, lookahead)
      case _ => Always
    }
    val newCondition = disjunct(
      ctx.acceptConditions.getOrElse(newKernel, Never),
      conjunct(task.condition, addingCondition))

    // ctx.graph에 newKernel, newEdges를 모두 추가하고, newKernel의 조건으로 newCondition을 disjunct로 추가한다.
    val newCtx = ParsingContext(
      ctx.graph.addNode(newKernel).addAllEdges(newEdges),
      ctx.acceptConditions + (newKernel -> newCondition)
    )

    val newTasks = if (!(ctx.graph.nodes contains newKernel)) {
      if (isFinal(newKernel)) List(FinishTask(newKernel)) else List(DeriveTask(newKernel))
    } else List()

    (newCtx, newTasks)
  }

  def process(nextGen: Int, task: ParsingTask, ctx: ParsingContext): (ParsingContext, List[ParsingTask]) = task match {
    case task: DeriveTask => deriveTask(nextGen, task, ctx)
    case task: ProgressTask => progressTask(nextGen, task, ctx)
    case task: FinishTask => finishTask(nextGen, task, ctx)
  }

  @tailrec final def rec(nextGen: Int, tasks: List[ParsingTask], ctx: ParsingContext): ParsingContext = tasks match {
    case task +: rest =>
      val (ncc, newTasks) = process(nextGen, task, ctx)
      rec(nextGen, newTasks ++: rest, ncc)
    case List() => ctx
  }
}
