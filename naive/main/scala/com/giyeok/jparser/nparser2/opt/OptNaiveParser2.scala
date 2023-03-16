package com.giyeok.jparser.nparser2.opt

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NExcept, NJoin, NLongest, NLookaheadExcept, NLookaheadIs, NLookaheadSymbol, NSequence, NSimpleDerive, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.{Always, Exists, Never, NotExists, OnlyIf, Unless, conjunct, disjunct}
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.NaiveParser2.{AcceptConditionsTracker, ParsingHistoryContext}
import com.giyeok.jparser.nparser2.{DeriveTask, Edge, FinishTask, KernelGraph, ParsingContext, ParsingTask, ProgressTask}

import scala.annotation.tailrec

// mutable data structure를 사용해서 조금 더 빠른 naive parser
class OptNaiveParser2(val grammar: NGrammar) {
  val startKernel: Kernel = Kernel(grammar.startSymbol, 0, 0, 0)

  val initialParsingContext: ParsingContext = {
    val startCtx = ParsingContext(KernelGraph(Set(startKernel), Set()), Map(startKernel -> Always))
    runTasks(0, List(DeriveTask(startKernel)), startCtx)
    // TODO initialContext도 conditions update, filtering, trimming이 필요한가?
  }

  val initialParsingHistoryContext: ParsingHistoryContext = {
    ParsingHistoryContext(0, initialParsingContext, List(), List(initialParsingContext), AcceptConditionsTracker(Map()))
  }

  private def isFinal(kernel: Kernel): Boolean = {
    grammar.symbolOf(kernel.symbolId) match {
      case _: NGrammar.NAtomicSymbol => kernel.pointer == 1
      case NGrammar.NSequence(_, _, sequence) => kernel.pointer == sequence.length
    }
  }

  def deriveTask(nextGen: Int, task: DeriveTask, mutCtx: MutableParsingContext): List[ParsingTask] = {
    assert(mutCtx.graph.containsNode(task.kernel))
    assert(!isFinal(task.kernel))
    assert(task.kernel.endGen == nextGen)

    def derive0(symbolId: Int): List[ParsingTask] = {
      val newNode = Kernel(symbolId, 0, nextGen, nextGen)
      if (!mutCtx.graph.containsNode(newNode)) {
        mutCtx.graph.addNode(newNode)
        mutCtx.graph.addEdge(Edge(task.kernel, newNode))
        mutCtx.acceptConditions += (newNode -> Always)
        val newTask = if (isFinal(newNode)) FinishTask(newNode) else DeriveTask(newNode)
        List(newTask)
      } else {
        // newNode가 그래프에 이미 들어있는 경우

        // 1. task.kernel -> newNode 엣지를 추가해야 하고,
        // 2. newNode가 Progress된 경우 task.kernel -> newNode의 다음 kernel로 가는 엣지도 추가해야 하고,
        // 3. 그렇게 추가한 노드들 중 (task.kernel에서부터) 종료된 노드로 가는 엣지가 생기면 task.kernel에 대한 ProgressTask 추가
        // -> 기존의 updatedNodesMap을 대체

        @tailrec
        def addNext(newNode: Kernel): List[ParsingTask] = {
          mutCtx.graph.addEdge(Edge(task.kernel, newNode))
          if (isFinal(newNode)) {
            List(ProgressTask(task.kernel, mutCtx.acceptConditions(newNode)))
          } else {
            val nextNode = newNode.copy(pointer = newNode.pointer + 1)
            if (mutCtx.graph.containsNode(nextNode)) {
              addNext(nextNode)
            } else {
              List()
            }
          }
        }

        addNext(newNode)
      }
    }

    def addNode0(symbolId: Int): List[ParsingTask] = {
      val newNode = Kernel(symbolId, 0, nextGen, nextGen)
      mutCtx.graph.addNode(newNode)
      mutCtx.acceptConditions += newNode -> Always
      val newTask = if (isFinal(newNode)) FinishTask(newNode) else DeriveTask(newNode)
      List(newTask)
    }

    grammar.symbolOf(task.kernel.symbolId) match {
      case _: NTerminal => List()
      case derives: NSimpleDerive =>
        derives.produces.toList.flatMap(derive0)
      case NExcept(_, _, body, except) =>
        derive0(body) ++ addNode0(except)
      case NJoin(_, _, body, join) =>
        derive0(body) ++ addNode0(join)
      case NLongest(_, _, body) =>
        derive0(body)
      case lookahead: NLookaheadSymbol =>
        derive0(lookahead.emptySeqId) ++ addNode0(lookahead.lookahead)
      case NSequence(_, _, sequence) =>
        derive0(sequence(task.kernel.pointer))
    }
  }

  def finishTask(nextGen: Int, task: FinishTask, mutCtx: MutableParsingContext): List[ParsingTask] = {
    assert(mutCtx.graph.containsNode(task.kernel))
    assert(isFinal(task.kernel))
    assert(task.kernel.endGen == nextGen)

    val incomingEdges = mutCtx.graph.edgesByEnd(task.kernel)
    val chainTasks = incomingEdges map { edge =>
      ProgressTask(edge.start, mutCtx.acceptConditions(task.kernel))
    }
    chainTasks.toList
  }

  def progressTask(nextGen: Int, task: ProgressTask, mutCtx: MutableParsingContext): List[ParsingTask] = {
    assert(mutCtx.graph.containsNode(task.kernel))
    assert(!isFinal(task.kernel))

    val newKernel = Kernel(task.kernel.symbolId, task.kernel.pointer + 1, task.kernel.beginGen, nextGen)
    val existingKernel = mutCtx.graph.containsNode(newKernel)

    val incomingEdges = mutCtx.graph.edgesByEnd(task.kernel)
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
      mutCtx.acceptConditions.getOrElse(newKernel, Never),
      conjunct(
        mutCtx.acceptConditions.getOrElse(task.kernel, Never),
        task.condition,
        addingCondition))

    // ctx.graph에 newKernel, newEdges를 모두 추가하고, newKernel의 조건으로 newCondition을 disjunct로 추가한다.
    mutCtx.graph.addNode(newKernel)
    newEdges.foreach(mutCtx.graph.addEdge)
    mutCtx.acceptConditions += newKernel -> newCondition

    val newTasks = if (!existingKernel) {
      if (isFinal(newKernel)) List(FinishTask(newKernel)) else List(DeriveTask(newKernel))
    } else List()

    newTasks
  }

  def process(nextGen: Int, task: ParsingTask, mutCtx: MutableParsingContext): List[ParsingTask] = task match {
    case task: DeriveTask => deriveTask(nextGen, task, mutCtx)
    case task: ProgressTask => progressTask(nextGen, task, mutCtx)
    case task: FinishTask => finishTask(nextGen, task, mutCtx)
  }

  def runTasks(nextGen: Int, tasks: List[ParsingTask], ctx: ParsingContext): ParsingContext = {
    val mutCtx = MutableParsingContext(ctx)

    def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case task +: rest =>
          val newTasks = process(nextGen, task, mutCtx)
          recursion(newTasks ++: rest)
        case List() =>
      }

    recursion(tasks)
    mutCtx.toParsingContext
  }
}
