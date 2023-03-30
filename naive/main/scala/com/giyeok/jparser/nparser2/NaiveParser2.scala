package com.giyeok.jparser.nparser2

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput}
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel, ParseTreeConstructor2}
import com.giyeok.jparser.nparser2.NaiveParser2.{AcceptConditionsTracker, ParsingHistoryContext}
import com.giyeok.jparser.{NGrammar, ParseResult, ParseResultFunc}

import scala.annotation.tailrec

class NaiveParser2(val grammar: NGrammar) {
  val startKernel: Kernel = Kernel(grammar.startSymbol, 0, 0, 0)

  val initialParsingContext: ParsingContext = {
    val startCtx = ParsingContext(KernelGraph(Set(startKernel), Set()), Map(startKernel -> Always))
    runTasks(0, List(DeriveTask(startKernel)), startCtx)
    // TODO initialContext도 conditions update, filtering, trimming이 필요한가?
  }

  val initialParsingHistoryContext: ParsingHistoryContext = ParsingHistoryContext(
    0,
    initialParsingContext,
    List(),
    List(initialParsingContext),
    AcceptConditionsTracker(initialParsingContext.acceptConditions.values.map(cond => cond -> cond).toMap))

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
      conjunct(
        ctx.acceptConditions(task.kernel),
        task.condition,
        addingCondition))

    // ctx.graph에 newKernel, newEdges를 모두 추가하고, newKernel의 조건으로 newCondition을 disjunct로 추가한다.
    val newCtx = ParsingContext(
      ctx.graph.addNode(newKernel).addAllEdges(newEdges),
      ctx.acceptConditions + (newKernel -> newCondition)
    )

    // 새로운 커널이거나 accept condition이 업데이트 되었으면 추가 작업 실행. 바뀐게 없으면 추가 작업 없음
    val newTasks =
      if (!(ctx.graph.nodes contains newKernel) || ctx.acceptConditions.get(newKernel) != Some(newCondition)) {
        if (isFinal(newKernel)) List(FinishTask(newKernel)) else List(DeriveTask(newKernel))
      } else List()

    (newCtx, newTasks)
  }

  def process(nextGen: Int, task: ParsingTask, ctx: ParsingContext): (ParsingContext, List[ParsingTask]) = task match {
    case task: DeriveTask => deriveTask(nextGen, task, ctx)
    case task: ProgressTask => progressTask(nextGen, task, ctx)
    case task: FinishTask => finishTask(nextGen, task, ctx)
  }

  @tailrec final def runTasks(nextGen: Int, tasks: List[ParsingTask], ctx: ParsingContext): ParsingContext = tasks match {
    case task +: rest =>
      val (ncc, newTasks) = process(nextGen, task, ctx)
      runTasks(nextGen, newTasks ++: rest, ncc)
    case List() => ctx
  }

  // Either가 이제 right-biased라고 하니.. error를 left로 반환
  def progressTerminalsForInput(gen: Int, ctx: ParsingContext, input: Input): Either[ParsingError, ParsingContext] = {
    val initialProgressTasks = ctx.graph.nodes.filter {
      case Kernel(symbolId, 0, `gen`, `gen`) =>
        grammar.symbolOf(symbolId) match {
          case NTerminal(_, terminal) => terminal.accept(input)
          case _ => false
        }
      case _ => false
    }.map(ProgressTask(_, Always))

    if (initialProgressTasks.isEmpty) {
      val eligibleTerminals = ctx.graph.nodes.flatMap {
        case Kernel(symbolId, 0, `gen`, `gen`) =>
          grammar.symbolOf(symbolId) match {
            case NTerminal(_, terminal) => Some(terminal)
            case _ => None
          }
        case _ => None
      }
      Left(UnexpectedInput(input, eligibleTerminals, gen))
    } else {
      Right(runTasks(gen + 1, initialProgressTasks.toList, ctx))
    }
  }

  def evolveAcceptCondition(acceptCondition: AcceptCondition, gen: Int, ctx: ParsingContext): AcceptCondition = {
    def initKernel(condition: SymbolCondition): Kernel =
      Kernel(condition.symbolId, 0, condition.beginGen, condition.beginGen)

    def finalKernel(condition: SymbolCondition): Kernel =
      Kernel(condition.symbolId, 1, condition.beginGen, gen)

    val res = acceptCondition match {
      case AcceptCondition.Always => AcceptCondition.Always
      case AcceptCondition.Never => AcceptCondition.Never
      case And(conditions) =>
        val evolved = conditions.map(evolveAcceptCondition(_, gen, ctx)).toArray
        conjunct(evolved: _*)
      case Or(conditions) =>
        val evolved = conditions.map(evolveAcceptCondition(_, gen, ctx)).toArray
        disjunct(evolved: _*)
      case acceptCondition: NotExists =>
        if (gen < acceptCondition.endGen) acceptCondition else {
          val moreTrackingNeeded = ctx.graph.nodes contains initKernel(acceptCondition)
          ctx.acceptConditions get finalKernel(acceptCondition) match {
            case Some(progressCondition) =>
              val evolvedCondition = evolveAcceptCondition(progressCondition.neg, gen, ctx)
              if (moreTrackingNeeded) conjunct(evolvedCondition, acceptCondition) else evolvedCondition
            case None =>
              if (moreTrackingNeeded) acceptCondition else Always
          }
        }
      case acceptCondition: Exists =>
        if (gen < acceptCondition.endGen) acceptCondition else {
          val moreTrackingNeeded = ctx.graph.nodes contains initKernel(acceptCondition)
          ctx.acceptConditions get finalKernel(acceptCondition) match {
            case Some(progressCondition) =>
              val evolvedCondition = evolveAcceptCondition(progressCondition, gen, ctx)
              if (moreTrackingNeeded) disjunct(evolvedCondition, acceptCondition) else evolvedCondition
            case None =>
              if (moreTrackingNeeded) acceptCondition else Never
          }
        }
      case acceptCondition: Unless =>
        if (gen > acceptCondition.endGen) {
          Always
        } else {
          assert(gen == acceptCondition.endGen)
          ctx.acceptConditions get finalKernel(acceptCondition) match {
            case Some(condition) => evolveAcceptCondition(condition.neg, gen, ctx)
            case None =>
              // 대상 심볼이 아예 매치되지 않았다는 의미
              Always
          }
        }
      case acceptCondition: OnlyIf =>
        if (gen > acceptCondition.endGen) {
          Never
        } else {
          assert(gen == acceptCondition.endGen)
          ctx.acceptConditions get finalKernel(acceptCondition) match {
            case Some(condition) => evolveAcceptCondition(condition, gen, ctx)
            case None =>
              // 대상 심볼이 아예 매치되지 않았다는 의미
              Never
          }
        }
    }
    assert(containsNoOnlyOfOrUnless(res))
    res
  }

  def evaluateAcceptCondition(acceptCondition: AcceptCondition, gen: Int, ctx: ParsingContext): Boolean = acceptCondition match {
    case AcceptCondition.Always => true
    case AcceptCondition.Never => false
    case And(conditions) => conditions.forall(evaluateAcceptCondition(_, gen, ctx))
    case Or(conditions) => conditions.exists(evaluateAcceptCondition(_, gen, ctx))
    case NotExists(beginGen, endGen, symbolId) =>
      if (gen < endGen) true else {
        ctx.acceptConditions get Kernel(symbolId, 1, beginGen, gen) match {
          case Some(condition) => !evaluateAcceptCondition(condition, gen, ctx)
          case None => true
        }
      }
    case Exists(beginGen, endGen, symbolId) =>
      if (gen < endGen) false else {
        ctx.acceptConditions get Kernel(symbolId, 1, beginGen, gen) match {
          case Some(condition) => evaluateAcceptCondition(condition, gen, ctx)
          case None => false
        }
      }
    case Unless(beginGen, endGen, symbolId) =>
      assert(gen >= endGen)
      if (gen != endGen) true else {
        ctx.acceptConditions get Kernel(symbolId, 1, beginGen, gen) match {
          case Some(condition) => !evaluateAcceptCondition(condition, gen, ctx)
          case None => true
        }
      }
    case OnlyIf(beginGen, endGen, symbolId) =>
      assert(gen >= endGen)
      if (gen != endGen) false else {
        ctx.acceptConditions get Kernel(symbolId, 1, beginGen, gen) match {
          case Some(condition) => evaluateAcceptCondition(condition, gen, ctx)
          case None => false
        }
      }
  }

  def containsNoOnlyOfOrUnless(condition: AcceptCondition.AcceptCondition): Boolean = condition match {
    case AcceptCondition.Always => true
    case AcceptCondition.Never => true
    case And(conditions) => conditions.forall(containsNoOnlyOfOrUnless)
    case Or(conditions) => conditions.forall(containsNoOnlyOfOrUnless)
    case _: NotExists => true
    case _: Exists => true
    case _: Unless => false
    case _: OnlyIf => false
  }

  def updateAcceptConditions(nextGen: Int, ctx: ParsingContext, tracker: AcceptConditionsTracker): (ParsingContext, AcceptConditionsTracker) = {
    // 1. tracker를 일단 생각하지 않는다면 해야 할 일은 간단
    val newAcceptConditions = ctx.acceptConditions.view.mapValues(evolveAcceptCondition(_, nextGen, ctx)).toMap
    val droppedKernels = newAcceptConditions.filter(_._2 == Never).keySet
    val newGraph = ctx.graph.removeNodes(droppedKernels)

    assert(newAcceptConditions.forall(c => containsNoOnlyOfOrUnless(c._2)))
    val newParsingContext = ParsingContext(newGraph, newAcceptConditions.filter(_._2 != Never))

    // 2. tracker에서는 기존에 추적중이던 accept condition들을 이번 generation에 맞춰 업데이트하고, ctx에서 새로 추가된 accept condition들을 추가해야 함
    val evolvedEvolves = tracker.evolves.view.mapValues(evolveAcceptCondition(_, nextGen, ctx)).toMap
    val newConditions = ctx.acceptConditions.values.toList.map { cond => cond -> evolveAcceptCondition(cond, nextGen, ctx) }.toMap
    val newUpdatedConditions = newAcceptConditions.values.map { cond => cond -> cond }.toMap

    (evolvedEvolves.keySet ++ newConditions.keySet ++ newUpdatedConditions.keySet).foreach { key =>
      val cond1 = evolvedEvolves.get(key)
      val cond2 = newConditions.get(key)
      val cond3 = newUpdatedConditions.get(key)
      assert((cond1.toList ++ cond2.toList ++ cond3.toList).distinct.size == 1)
    }

    (newParsingContext, AcceptConditionsTracker(evolvedEvolves ++ newConditions ++ newUpdatedConditions))


    //    assert(ctx.graph.nodes == ctx.acceptConditions.keySet)
    //    val intersect = tracker.evolves.keySet.intersect(ctx.acceptConditions.values.toSet)
    //    if (intersect.nonEmpty) {
    //      println(intersect)
    //    }
    //    val conditionsMap = tracker.evolves ++ ctx.acceptConditions.values.map(condition => condition -> condition).toMap
    //    val evolvedConditionsMap = conditionsMap.view.mapValues(evolveAcceptCondition(_, nextGen, ctx)).toMap
    //
    //    val newTracker = AcceptConditionsTracker(evolvedConditionsMap)
    //    val droppedKernels = ctx.acceptConditions.filter { case (_, condition) => evolvedConditionsMap(condition) == Never }.keySet
    //
    //    val newGraph = ctx.graph.removeNodes(droppedKernels)
    //    val evolvedAcceptConditions = (ctx.acceptConditions -- droppedKernels).view.mapValues(evolvedConditionsMap).toMap
    //
    //    // newTracker.evolves.key가 ctx.acceptConditions.values 와 evolvedAcceptConditions.values를 모두 커버해야 함
    //    if (!ctx.acceptConditions.values.toSet.subsetOf(newTracker.evolves.keySet)) {
    //      println("??")
    //    }
    //    assert(ctx.acceptConditions.values.toSet.subsetOf(newTracker.evolves.keySet))
    //    if (!evolvedAcceptConditions.values.toSet.subsetOf(newTracker.evolves.keySet)) {
    //      println("??")
    //    }
    //    assert(evolvedAcceptConditions.values.toSet.subsetOf(newTracker.evolves.keySet))

    //    def oldLogic(): Unit = {
    //      val acceptConditions = ctx.acceptConditions.view.mapValues(evolveAcceptCondition(_, nextGen, ctx)).toMap
    //      val droppedKernels = acceptConditions.filter(_._2 == Never)
    //      val survivingAcceptConditions = acceptConditions.filter(_._2 != Never)
    //
    //      val evolves = tracker.evolves.view.mapValues(evolveAcceptCondition(_, nextGen, ctx)).toMap
    //      val newTracker = AcceptConditionsTracker(survivingAcceptConditions.values.map(c => c -> c).toMap ++ evolves)
    //      println(evolves)
    //      println(newTracker)
    //    }

    //    (ParsingContext(newGraph, evolvedAcceptConditions), newTracker)
  }

  def trimParsingContext(start: Kernel, nextGen: Int, ctx: ParsingContext): ParsingContext = {
    trimParsingContext(Set(start), nextGen, ctx)
  }

  def trimParsingContext(starts: Set[Kernel], nextGen: Int, ctx: ParsingContext): ParsingContext = {
    val destKernels = ctx.graph.nodes.filter { kernel =>
      grammar.symbolOf(kernel.symbolId).isInstanceOf[NTerminal] &&
        kernel.pointer == 0 &&
        kernel.beginGen == nextGen
    }

    def reachableFrom(curr: Kernel, cc: Set[Kernel]): Set[Kernel] = {
      if (!ctx.graph.nodes.contains(curr) || ctx.acceptConditions(curr) == Never) {
        cc
      } else if (destKernels.contains(curr)) {
        cc + curr
      } else {
        val outEdges = ctx.graph.edgesByStart(curr)
        outEdges.foldLeft(cc + curr) { (cc1, outEdge) =>
          val outNode = outEdge.end
          if (cc1.contains(outNode)) {
            cc1
          } else {
            val sym = grammar.symbolOf(outNode.symbolId)
            sym match {
              case NExcept(_, _, body, except) =>
                val bodyKernel = Kernel(body, 0, outNode.beginGen, outNode.beginGen)
                val cc2 = reachableFrom(bodyKernel, cc1 + outNode)
                val exceptKernel = Kernel(except, 0, outNode.beginGen, outNode.beginGen)
                reachableFrom(exceptKernel, cc2)
              case NJoin(_, _, body, join) =>
                val bodyKernel = Kernel(body, 0, outNode.beginGen, outNode.beginGen)
                val cc2 = reachableFrom(bodyKernel, cc1 + outNode)
                val joinKernel = Kernel(join, 0, outNode.beginGen, outNode.beginGen)
                reachableFrom(joinKernel, cc2)
              case NLookaheadIs(_, _, emptySeqId, lookaheadId) =>
                val emptySeqKernel = Kernel(emptySeqId, 0, outNode.beginGen, outNode.beginGen)
                val cc2 = reachableFrom(emptySeqKernel, cc1 + outNode)
                val lookaheadKernel = Kernel(lookaheadId, 0, outNode.beginGen, outNode.beginGen)
                reachableFrom(lookaheadKernel, cc2)
              case NLookaheadExcept(_, _, emptySeqId, lookaheadId) =>
                val emptySeqKernel = Kernel(emptySeqId, 0, outNode.beginGen, outNode.beginGen)
                val cc2 = reachableFrom(emptySeqKernel, cc1 + outNode)
                val lookaheadKernel = Kernel(lookaheadId, 0, outNode.beginGen, outNode.beginGen)
                reachableFrom(lookaheadKernel, cc2)
              case _ => reachableFrom(outNode, cc1)
            }
          }
        }
      }
    }

    def reachableTo(curr: Kernel, cc: Set[Kernel]): Set[Kernel] = {
      if (cc.contains(curr)) {
        cc
      } else {
        ctx.graph.edgesByEnd.get(curr) match {
          case Some(inEdges) =>
            inEdges.foldLeft(cc + curr) { (cc1, inEdge) =>
              reachableTo(inEdge.start, cc1)
            }
          case None => cc
        }
      }
    }

    val reachableFromStart = starts.flatMap { start => reachableFrom(start, Set()) }
    val reachableToTerms = destKernels.flatMap(reachableTo(_, Set()))
    val reachableNodes = reachableFromStart.intersect(reachableToTerms)
    val droppedNodes = ctx.graph.nodes -- reachableNodes
    ParsingContext(ctx.graph.removeNodes(droppedNodes), ctx.acceptConditions.filter(p => reachableNodes.contains(p._1)))
  }

  def parseStep(hctx: ParsingHistoryContext, input: Input): Either[ParsingError, ParsingHistoryContext] = {
    val initialsProgressed = progressTerminalsForInput(hctx.gen, hctx.parsingContext, input)

    initialsProgressed map { ctx =>
      val nextGen = hctx.gen + 1
      val (updated, newTracker) = updateAcceptConditions(nextGen, ctx, hctx.acceptConditionsTracker)
      val trimmedCtx = trimParsingContext(startKernel, nextGen, updated)
      assert(trimmedCtx.acceptConditions.values.toSet.subsetOf(newTracker.evolves.keySet))
      ParsingHistoryContext(nextGen, trimmedCtx, hctx.inputs :+ input, hctx.history :+ updated, newTracker)
    }
  }

  def parse(inputSeq: List[Input]): Either[ParsingError, ParsingHistoryContext] =
    inputSeq.foldLeft[Either[ParsingError, ParsingHistoryContext]](Right(initialParsingHistoryContext)) { (cc, i) =>
      cc flatMap (parseStep(_, i))
    }

  def historyKernels(ctx: ParsingHistoryContext): Vector[Set[Kernel]] = {
    val conditionsFinal: Map[AcceptCondition, Boolean] = ctx.acceptConditionsTracker.evolves.map { p =>
      p._1 -> evaluateAcceptCondition(p._2, ctx.gen, ctx.parsingContext)
    }
    val historyKernels = ctx.history.map { historyCtx =>
      historyCtx.acceptConditions.filter { p => conditionsFinal(p._2) }.keys
    }.map(_.toSet)
    historyKernels.toVector
  }

  def parseTreeReconstructor2[R <: ParseResult](resultFunc: ParseResultFunc[R], ctx: ParsingHistoryContext): ParseTreeConstructor2[R] =
    new ParseTreeConstructor2[R](resultFunc)(grammar)(ctx.inputs, historyKernels(ctx).map { kernels =>
      Kernels(kernels.map { k => Kernel(k.symbolId, k.pointer, k.beginGen, k.endGen) })
    })
}

object NaiveParser2 {
  case class AcceptConditionsTracker(evolves: Map[AcceptCondition, AcceptCondition])

  case class ParsingHistoryContext(
    gen: Int,
    parsingContext: ParsingContext,
    inputs: List[Input],
    history: List[ParsingContext],
    acceptConditionsTracker: AcceptConditionsTracker)
}
