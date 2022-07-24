package com.giyeok.jparser.nparser2

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NExcept, NJoin, NLongest, NLookaheadExcept, NLookaheadIs, NLookaheadSymbol, NSequence, NSimpleDerive, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, Exists, Never, NotExists, OnlyIf, Unless, disjunct}

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

    def addNode(ctx: ParsingContext, newNode: Kernel): ParsingContext =
      ParsingContext(ctx.graph.addNode(newNode), ctx.acceptConditions + (newNode -> Always))

    def derive0(cc: (ParsingContext, List[ParsingTask]), symbolId: Int): (ParsingContext, List[ParsingTask]) = {
      val newNode = Kernel(symbolId, 0, nextGen, nextGen)
      // 그래프에 newNode가 이미 포함된 경우에도, 새로운 엣지가 추가되기 때문에 FinishTask와 DeriveTask를 다시 수행한다
      // 이전 버젼에서는 updatedNodesMap을 써서 중복된 태스크를 돌리지 않게 했었는데, 코드가 복잡해져서 이런식으로 바꿈
      val newCtx = addNode(cc._1, newNode)
      val newEdge = Edge(task.kernel, newNode)
      // 새로 추가하려던 엣지도 이미 그래프에 있으면 아무것도 하지 않음
      if (!(newCtx.graph.edges contains newEdge)) {
        val newTask = if (isFinal(newNode)) FinishTask(newNode) else DeriveTask(newNode)
        (newCtx.copy(graph = newCtx.graph.addEdge(newEdge)), newTask +: cc._2)
      } else {
        (newCtx, cc._2)
      }
    }

    def addNode0(cc: (ParsingContext, List[ParsingTask]), symbolId: Int): (ParsingContext, List[ParsingTask]) =
      (addNode(cc._1, Kernel(symbolId, 0, nextGen, nextGen)), cc._2)

    grammar.symbolOf(task.kernel.symbolId) match {
      case _: NTerminal => (ctx, List()) // do nothing
      case derives: NSimpleDerive =>
        derives.produces.foldLeft((ctx, List[ParsingTask]())) { (cc, symbolId) => derive0(cc, symbolId) }
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

    val incomingCondition = grammar.symbolOf(task.kernel.symbolId) match {
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
    val newCondition = disjunct(ctx.acceptConditions.getOrElse(newKernel, Never), incomingCondition)

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
