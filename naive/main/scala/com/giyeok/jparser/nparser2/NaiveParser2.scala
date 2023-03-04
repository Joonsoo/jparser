package com.giyeok.jparser.nparser2

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.NGrammar._
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput}
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.{Kernel, ParseTreeConstructor2}
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

  val initialParsingHistoryContext: ParsingHistoryContext = {
    ParsingHistoryContext(0, initialParsingContext, List(), List(initialParsingContext), AcceptConditionsTracker(Map()))
  }

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
        ctx.acceptConditions.getOrElse(task.kernel, Never),
        task.condition,
        addingCondition))

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

  def updateAcceptConditions(nextGen: Int, ctx: ParsingContext, tracker: AcceptConditionsTracker): (ParsingContext, AcceptConditionsTracker) = {
    // TODO 버그가 있음 - 무슨 버그지..?
    val acceptConditions = ctx.acceptConditions.view.mapValues(_.evolve(nextGen, ctx)).toMap
    val droppedKernels = acceptConditions.filter(_._2 == Never)
    val survivingAcceptConditions = acceptConditions.filter(_._2 != Never)

    val evolves = tracker.evolves.view.mapValues(_.evolve(nextGen, ctx)).toMap
    val newTracker = AcceptConditionsTracker(survivingAcceptConditions.values.map(c => c -> c).toMap ++ evolves)
    (ParsingContext(ctx.graph.removeNodes(droppedKernels.keys.toSet), survivingAcceptConditions.toMap), newTracker)
  }

  def trimParsingContext(start: Kernel, nextGen: Int, ctx: ParsingContext): ParsingContext = {
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

    val reachableFromStart = reachableFrom(start, Set())
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
      val trimmed = trimParsingContext(startKernel, nextGen, updated)
      ParsingHistoryContext(nextGen, trimmed, hctx.inputs :+ input, hctx.history :+ updated, newTracker)
    }
  }

  def parse(inputSeq: List[Input]): Either[ParsingError, ParsingHistoryContext] =
    inputSeq.foldLeft[Either[ParsingError, ParsingHistoryContext]](Right(initialParsingHistoryContext)) { (cc, i) =>
      cc flatMap (parseStep(_, i))
    }
}

object NaiveParser2 {
  case class AcceptConditionsTracker(evolves: Map[AcceptCondition, AcceptCondition])

  case class ParsingHistoryContext(
    gen: Int,
    parsingContext: ParsingContext,
    inputs: List[Input],
    history: List[ParsingContext],
    acceptConditionsTracker: AcceptConditionsTracker) {

    lazy val conditionsFinal: Map[AcceptCondition, Boolean] = acceptConditionsTracker.evolves.map { p =>
      p._1 -> p._2.accepted(gen, parsingContext)
    }

    lazy val historyKernels: List[Set[Kernel]] = {
      history.map { ctx =>
        ctx.acceptConditions.filter(p => conditionsFinal(p._2)).keys
      }.map(_.toSet)
    }

    def parseTreeReconstructor2[R <: ParseResult](resultFunc: ParseResultFunc[R], grammar: NGrammar): ParseTreeConstructor2[R] =
      new ParseTreeConstructor2[R](resultFunc)(grammar)(inputs, historyKernels.map { kernels =>
        Kernels(kernels.map { k => Kernel(k.symbolId, k.pointer, k.beginGen, k.endGen) })
      })
  }
}
