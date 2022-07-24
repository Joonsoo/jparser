package com.giyeok.jparser.nparser2

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, Never}
import com.giyeok.jparser.nparser2.NaiveParser2.{AcceptConditionsTracker, ParsingHistoryContext}

class NaiveParser2(val grammar: NGrammar) {
  val parsingTaskImpl: ParsingTaskImpl = new ParsingTaskImpl(grammar)
  val startKernel: Kernel = Kernel(grammar.startSymbol, 0, 0, 0)

  val initialParsingContext: ParsingContext = {
    val startCtx = ParsingContext(KernelGraph(Set(startKernel), Set()), Map(startKernel -> Always))
    parsingTaskImpl.rec(0, List(DeriveTask(startKernel)), startCtx)
    // TODO initialContext도 conditions update, filtering, trimming이 필요한가?
  }

  val initialParsingHistoryContext: ParsingHistoryContext = {
    ParsingHistoryContext(0, initialParsingContext, List(), List(initialParsingContext), AcceptConditionsTracker(Map()))
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
      Right(parsingTaskImpl.rec(gen + 1, initialProgressTasks.toList, ctx))
    }
  }

  def updateAcceptConditions(nextGen: Int, ctx: ParsingContext, tracker: AcceptConditionsTracker): (ParsingContext, AcceptConditionsTracker) = {
    val acceptConditions = ctx.acceptConditions.view.mapValues(_.evolve(nextGen, ctx))
    val droppedKernels = acceptConditions.filter(_._2 == Never)
    val survivingAcceptConditions = acceptConditions.filter(_._2 != Never)

    val evolves = tracker.evolves.view.mapValues(_.evolve(nextGen, ctx))
    val newTracker = AcceptConditionsTracker(survivingAcceptConditions.values.map(c => c -> c).toMap ++ evolves)
    (ParsingContext(ctx.graph.removeNodes(droppedKernels.keys.toSet), survivingAcceptConditions.toMap), newTracker)
  }

  def trimParsingContext(nextGen: Int, ctx: ParsingContext): ParsingContext = {
    val destKernels = ctx.graph.nodes.filter { kernel =>
      grammar.symbolOf(kernel.symbolId).isInstanceOf[NTerminal] &&
        kernel.pointer == 0 &&
        kernel.beginGen == nextGen
    }

    def traverse(pointer: Kernel, path: List[Edge], cc: Set[Kernel]): Set[Kernel] = {
      if (destKernels.contains(pointer)) {
        cc ++ path.flatMap(e => List(e.start, e.end))
      } else {
        ctx.graph.edgesByStart(pointer).filterNot(path.contains(_)).flatMap { next =>
          traverse(next.end, next +: path, cc)
        }
      }
    }

    val reachableNodes = traverse(startKernel, List(), Set())
    val droppedNodes = ctx.graph.nodes -- reachableNodes
    ParsingContext(ctx.graph.removeNodes(droppedNodes), ctx.acceptConditions.filter(p => reachableNodes.contains(p._1)))
  }

  def parseStep(gen: Int, hctx: ParsingHistoryContext, input: Input): Either[ParsingError, ParsingHistoryContext] = {
    val initialsProgressed = progressTerminalsForInput(gen, hctx.parsingContext, input)

    initialsProgressed map { ctx =>
      val nextGen = gen + 1
      val (updated, newTracker) = updateAcceptConditions(nextGen, ctx, hctx.acceptConditionsTracker)
      val trimmed = trimParsingContext(nextGen, updated)
      ParsingHistoryContext(nextGen, trimmed, hctx.inputs :+ input, hctx.history :+ updated, newTracker)
    }
  }

  def parse(inputSeq: List[Input]): Either[ParsingError, ParsingHistoryContext] =
    inputSeq.zipWithIndex.foldLeft[Either[ParsingError, ParsingHistoryContext]](Right(initialParsingHistoryContext)) { (cc, i) =>
      cc flatMap (parseStep(i._2, _, i._1))
    }
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
