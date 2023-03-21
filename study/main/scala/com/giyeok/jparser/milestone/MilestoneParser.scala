package com.giyeok.jparser.milestone

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.fast.{KernelTemplate, TasksSummary}
import com.giyeok.jparser.milestone.MilestoneParser.{AcceptConditionEvaluator, reconstructParseTree, transformEdgeActionCondition, transformTermActionCondition}
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.{Kernel, ParseTreeConstructor2}
import com.giyeok.jparser.utils.Memoize
import com.giyeok.jparser.{Inputs, ParseForest, ParseForestFunc, ParsingErrors}

import scala.annotation.tailrec

object MilestoneParser {
  def getKernelsHistory(parserData: MilestoneParserData, finalCtx: MilestoneParserContext): List[List[Kernel]] = {
    val finalAcceptancesMemo = Memoize[AcceptCondition, Boolean]()

    def isFinallyAccepted(gen: Int, acceptCondition: AcceptCondition): Boolean = acceptCondition match {
      case Always => true
      case Never => false
      case _ => finalAcceptancesMemo(acceptCondition) {
        if (gen < finalCtx.genProgressHistory.length) {
          val genProgress = finalCtx.genProgressHistory(gen)
          val evolvedAcceptCondition = new AcceptConditionEvaluator(parserData, genProgress.untrimmedMilestonePaths, gen, genProgress.genActions).evolveAcceptCondition(acceptCondition)
          isFinallyAccepted(gen + 1, evolvedAcceptCondition)
        } else {
          acceptCondition match {
            case _: NotExists => true
            case _: Exists => false
            case And(conditions) => conditions.forall(isFinallyAccepted(gen, _))
            case Or(conditions) => conditions.exists(isFinallyAccepted(gen, _))
            case _: Unless | _: OnlyIf =>
              // cannot happen
              ???
          }
        }
      }
    }

    finalCtx.genProgressHistory.map(gen => gen.genActions.flatMap {
      case TermAction(beginGen, midGen, endGen, summary, condition) =>
        if (isFinallyAccepted(endGen, condition)) {
          def genOf(gen: Int) = gen match {
            case 0 => beginGen
            case 1 => midGen
            case 2 => endGen
          }

          (summary.finishedKernels ++ summary.progressedKernels.map(_._1)).filter { node =>
            isFinallyAccepted(endGen, transformTermActionCondition(node.condition, beginGen, midGen, endGen))
          }.map(_.kernel).map { kernel =>
            Kernel(kernel.symbolId, kernel.pointer, genOf(kernel.beginGen), genOf(kernel.endGen))
          }
        } else List()
      case EdgeAction(parentBeginGen, beginGen, midGen, endGen, summary, condition) =>
        if (isFinallyAccepted(endGen, condition)) {
          def genOf(gen: Int) = gen match {
            case 0 => parentBeginGen
            case 1 => beginGen
            case 2 => midGen
            case 3 => endGen
          }

          (summary.finishedKernels ++ summary.progressedKernels.map(_._1)).filter { node =>
            isFinallyAccepted(endGen, transformEdgeActionCondition(node.condition, parentBeginGen, beginGen, midGen, endGen))
          }.map(_.kernel).map { kernel =>
            Kernel(kernel.symbolId, kernel.pointer, genOf(kernel.beginGen), genOf(kernel.endGen))
          }
        } else List()
    })
  }

  def reconstructParseTree(parserData: MilestoneParserData, finalCtx: MilestoneParserContext, input: Seq[Input]): Option[ParseForest] = {
    val startTime = System.currentTimeMillis()
    val kernels = getKernelsHistory(parserData, finalCtx).map(ks => Kernels(ks.toSet))
    println(s"Filtering kernels: ${System.currentTimeMillis() - startTime} ms")
    new ParseTreeConstructor2(ParseForestFunc)(parserData.grammar)(input, kernels).reconstruct()
  }

  class AcceptConditionEvaluator(parserData: MilestoneParserData, milestonePaths: List[MilestonePath], gen: Int, genActions: List[GenAction]) {
    def evolveAcceptCondition(acceptCondition: AcceptCondition): AcceptCondition = {
      def symbolFinishConditions(beginGen: Int, endGen: Int, symbolId: Int): Seq[AcceptCondition] =
        genActions.filter(_.endGen >= endGen).flatMap {
          case termAction: TermAction =>
            if (termAction.beginGen == beginGen || termAction.midGen == beginGen) {
              val metaKernel = if (termAction.beginGen == beginGen) Kernel(symbolId, 1, 0, 2) else Kernel(symbolId, 1, 1, 2)
              // 여기서 symbol은 항상 atomic symbol이므로 progress되면 바로 finish되기 때문에 progressed는 고려할 필요 없을듯.
              termAction.summary.finishedKernels.filter(_.kernel == metaKernel).map(_.condition)
                .map(transformTermActionCondition(_, termAction.beginGen, termAction.midGen, termAction.endGen))
            } else List()
          case edgeAction: EdgeAction =>
            if (edgeAction.beginGen == beginGen || edgeAction.midGen == beginGen) {
              val metaKernel = if (edgeAction.beginGen == beginGen) Kernel(symbolId, 1, 1, 3) else Kernel(symbolId, 1, 2, 3)
              // 여기서도 마찬가지로 symbol은 항상 atomic이므로 finish만 고려하면 됨
              edgeAction.summary.finishedKernels.filter(_.kernel == metaKernel).map(_.condition)
                .map(transformEdgeActionCondition(_, edgeAction.parentBeginGen, edgeAction.beginGen, edgeAction.midGen, edgeAction.endGen))
            } else List()
        }.distinct

      def symbolStillPossible(symbolId: Int, beginGen: Int): Boolean = {
        def needToPreserve(milestone: Milestone): Boolean = {
          if (milestone.gen == beginGen) {
            parserData.derivedGraph(milestone.kernelTemplate).nodes
              .exists(_.kernel == Kernel(symbolId, 0, 0, 0))
          } else if (milestone.gen > beginGen) {
            @tailrec def findParent(child: Milestone): Boolean =
              child.parent match {
                case Some(parent) =>
                  if (parent.gen == beginGen)
                    parserData.edgeProgressActions(parent.kernelTemplate -> child.kernelTemplate).graphBetween.nodes
                      .exists(_.kernel == Kernel(symbolId, 0, 0, 0))
                  else if (parent.gen < beginGen) false
                  else findParent(parent)
                case None => false
              }

            findParent(milestone)
          } else false
        }

        milestonePaths.exists(path => needToPreserve(path.tip))
      }

      acceptCondition match {
        case Always => Always
        case Never => Never
        case And(conditions) =>
          val evaluated = conditions.map(evolveAcceptCondition)
          conjunct(evaluated.toSeq: _*)
        case Or(conditions) =>
          disjunct(conditions.map(evolveAcceptCondition).toSeq: _*)
        case NotExists(_, endGen, _) if gen < endGen => acceptCondition
        case NotExists(beginGen, endGen, symbolId) =>
          // genAction을 통해서 symbolId가 (beginGen..endGen+) 에서 match될 수 있으면 매치되는 조건을,
          // genAction을 통해서는 이 accept condition을 확인할 수 없으면 그대로 반환
          // TODO genAction을 통해서는 이 accept condition을 확인할 수 있는 경우는 전체 milestone들을 확인해야 알 수 있음..
          // -> milestone들 중에 milestone.gen이 beginGen과 같고, 해당 milestone에서 derive돼서 이 symbolId가 나올 수 있으면 아직 미확정
          val metaConditions0 = symbolFinishConditions(beginGen, endGen, symbolId).map { c => evolveAcceptCondition(c.neg) }
          val metaConditions = if (symbolStillPossible(symbolId, beginGen)) acceptCondition +: metaConditions0 else metaConditions0
          conjunct(metaConditions: _*)
        case Exists(_, endGen, _) if gen < endGen => acceptCondition
        case Exists(beginGen, endGen, symbolId) =>
          val metaConditions0 = symbolFinishConditions(beginGen, endGen, symbolId).map(evolveAcceptCondition)
          val metaConditions = if (symbolStillPossible(symbolId, beginGen)) acceptCondition +: metaConditions0 else metaConditions0
          disjunct(metaConditions: _*)
        case Unless(beginGen, endGen, symbolId) =>
          if (gen > endGen) Always else {
            assert(gen == endGen)
            val conditions0 = symbolFinishConditions(beginGen, endGen, symbolId)
            evolveAcceptCondition(disjunct(conditions0: _*).neg)
          }
        case OnlyIf(beginGen, endGen, symbolId) =>
          if (gen > endGen) Never else {
            assert(gen == endGen)
            val conditions0 = symbolFinishConditions(beginGen, endGen, symbolId)
            evolveAcceptCondition(disjunct(conditions0: _*))
          }
      }
    }
  }

  private def transformTermActionCondition(condition: AcceptCondition, parentGen: Int, beginGen: Int, endGen: Int): AcceptCondition = {
    def genOf(gen: Int) = gen match {
      case 0 => parentGen
      case 1 => beginGen
      case 2 => endGen
      case 3 => endGen + 1
    }

    condition match {
      case Always | Never => condition
      case And(conditions) => conjunct(conditions.map(transformTermActionCondition(_, parentGen, beginGen, endGen)).toSeq: _*)
      case Or(conditions) => disjunct(conditions.map(transformTermActionCondition(_, parentGen, beginGen, endGen)).toSeq: _*)
      case NotExists(beginGen, endGen, symbolId) => NotExists(genOf(beginGen), genOf(endGen), symbolId)
      case Exists(beginGen, endGen, symbolId) => Exists(genOf(beginGen), genOf(endGen), symbolId)
      case Unless(beginGen, endGen, symbolId) => Unless(genOf(beginGen), genOf(endGen), symbolId)
      case OnlyIf(beginGen, endGen, symbolId) =>
        OnlyIf(genOf(beginGen), genOf(endGen), symbolId)
    }
  }

  private def transformEdgeActionCondition(condition: AcceptCondition, parentBeginGen: Int, parentGen: Int, beginGen: Int, endGen: Int): AcceptCondition = {
    def genOf(gen: Int) = gen match {
      case 0 => parentBeginGen
      case 1 => parentGen
      case 2 => beginGen
      case 3 => endGen
      case 4 => endGen + 1
    }

    condition match {
      case Always | Never => condition
      case And(conditions) => conjunct(conditions.map(transformEdgeActionCondition(_, parentBeginGen, parentGen, beginGen, endGen)).toSeq: _*)
      case Or(conditions) => disjunct(conditions.map(transformEdgeActionCondition(_, parentBeginGen, parentGen, beginGen, endGen)).toSeq: _*)
      case NotExists(beginGen, endGen, symbolId) => NotExists(genOf(beginGen), genOf(endGen), symbolId)
      case Exists(beginGen, endGen, symbolId) => Exists(genOf(beginGen), genOf(endGen), symbolId)
      case Unless(beginGen, endGen, symbolId) => Unless(genOf(beginGen), genOf(endGen), symbolId)
      case OnlyIf(beginGen, endGen, symbolId) =>
        OnlyIf(genOf(beginGen), genOf(endGen), symbolId)
    }
  }
}

class MilestoneParser(val parserData: MilestoneParserData, val verbose: Boolean = false) {
  val startMilestonePath: MilestonePath = MilestonePath(Milestone(None, parserData.grammar.startSymbol, 0, 0), Always, None, Set())

  def initialCtx: MilestoneParserContext = MilestoneParserContext(0, List(startMilestonePath),
    List(GenProgress(List(startMilestonePath), List(TermAction(0, 0, 0, parserData.byStart, Always)))))

  def parse(inputSeq: Seq[Inputs.Input]): Either[MilestoneParserContext, ParsingError] = {
    if (verbose) {
      println("=== initial")
      initialCtx.paths.foreach(t => println(t.prettyString))
    }
    inputSeq.foldLeft[Either[MilestoneParserContext, ParsingError]](Left(initialCtx)) { (m, nextInput) =>
      m match {
        case Left(currCtx) =>
          if (verbose) {
            println(s"=== ${currCtx.gen} $nextInput ${currCtx.paths.size}")
          }
          proceed(currCtx, nextInput)
        case error => error
      }
    }
  }

  def parse(input: String): Either[MilestoneParserContext, ParsingError] = parse(Inputs.fromString(input))

  def parseAndReconstructToForest(inputSeq: Seq[Inputs.Input]): Either[ParseForest, ParsingError] = {
    val startTime = System.currentTimeMillis()
    parse(inputSeq) match {
      case Left(finalCtx) =>
        val afterParse = System.currentTimeMillis()
        println(s"Parsing: ${afterParse - startTime} ms")
        reconstructParseTree(parserData, finalCtx, inputSeq) match {
          case Some(forest) =>
            val afterReconst = System.currentTimeMillis()
            println(s"Parse tree reconstruction: ${afterReconst - afterParse} ms")
            Left(forest)
          case None => Right(ParsingErrors.UnexpectedEOFByTermGroups(finalCtx.expectingTerminals(parserData), finalCtx.gen))
        }
      case Right(error) => Right(error)
    }
  }

  def parseAndReconstructToForest(input: String): Either[ParseForest, ParsingError] =
    parseAndReconstructToForest(Inputs.fromString(input))

  private var _nextTrackerId = 0

  private def nextTrackerId(): Int = {
    _nextTrackerId += 1
    _nextTrackerId
  }

  private class ProceedProcessor(var genActions: List[GenAction] = List()) {
    def proceed(ctx: MilestoneParserContext, gen: Int, input: Inputs.Input): List[MilestonePath] = ctx.paths.flatMap { path =>
      val tip = path.tip
      val parentGen = tip.parent.map(_.gen).getOrElse(0)
      val termActions = parserData.termActions(tip.kernelTemplate)
      termActions.find(_._1.contains(input)) match {
        case Some((_, action)) =>
          genActions +:= TermAction(parentGen, tip.gen, gen, action.tasksSummary, path.acceptCondition)
          // action.appendingMilestones를 뒤에 덧붙인다
          val appended = action.appendingMilestones.flatMap { appending =>
            val kernelTemplate = appending.milestone
            val acceptCondition = transformTermActionCondition(appending.acceptCondition, parentGen, tip.gen, gen)
            // TODO appending.dependents 처리
            val dependents = appending.dependents.map { dependent =>
              val parentMilestone = Milestone(None, dependent._1.symbolId, dependent._1.pointer, tip.gen)
              val tipMilestone = Milestone(Some(parentMilestone), dependent._2.symbolId, dependent._2.pointer, gen)
              val condition = transformTermActionCondition(dependent._3, parentGen, tip.gen, gen)
              val trackerId = nextTrackerId()
              // TODO 여기에는.. tracking 필요 없나? 필요 없을것 같긴 한데..
              MilestonePath(tipMilestone, condition, Some(trackerId), Set())
            }
            MilestonePath(Milestone(Some(tip), kernelTemplate.symbolId, kernelTemplate.pointer, gen),
              conjunct(path.acceptCondition, acceptCondition), path.trackerId,
              path.tracking ++ dependents.map(_.trackerId.get).toSet) +: dependents
          }
          // action.startNodeProgressConditions가 비어있지 않으면 tip을 progress 시킨다
          // val transformedConditions = transformStartProgressConditions(parentGen, tip.gen, gen, action.startNodeProgressConditions)
          val transformedConditions = action.startNodeProgressConditions.map(
            transformTermActionCondition(_, parentGen, tip.gen, gen))
          val reduced = progressTip(tip, gen, transformedConditions.map(conjunct(_, path.acceptCondition)), path.trackerId, path.tracking)
          appended ++ reduced
        case None => List()
      }
    }

    private def progressTip(tip: Milestone, gen: Int, acceptConditions: List[AcceptCondition], trackerId: Option[Int], tracking: Set[Int]): List[MilestonePath] =
      acceptConditions.flatMap { condition =>
        // (tip.parent-tip) 사이의 엣지에 대한 edge action 실행
        tip.parent match {
          case Some(parent) =>
            val parentBeginGen = parent.parent.map(_.gen).getOrElse(0)
            val edgeAction = parserData.edgeProgressActions((parent.kernelTemplate, tip.kernelTemplate))
            genActions +:= EdgeAction(parentBeginGen, parent.gen, tip.gen, gen, edgeAction.tasksSummary, condition)
            // TODO tip.acceptCondition은 이미 그 뒤에 붙었던 milestone에서 처리됐으므로 무시해도 될듯?
            // tip은 지워지고 tip.parent - edgeAction.appendingMilestones 가 추가됨
            val appended = edgeAction.appendingMilestones.flatMap { appending =>
              val appendingCondition = transformEdgeActionCondition(appending.acceptCondition, parentBeginGen, parent.gen, tip.gen, gen)
              // TODO appending.dependents 처리
              val dependents = appending.dependents.map { dependent =>
                val parentMilestone = Milestone(None, dependent._1.symbolId, dependent._1.pointer, tip.gen)
                val tipMilestone = Milestone(Some(parentMilestone), dependent._2.symbolId, dependent._2.pointer, gen)
                val condition = transformEdgeActionCondition(dependent._3, parentBeginGen, parent.gen, tip.gen, gen)
                val trackerId = nextTrackerId()
                // TODO 여기에는.. tracking 필요 없나? 필요 없을것 같긴 한데..
                MilestonePath(tipMilestone, condition, Some(trackerId), Set())
              }
              MilestonePath(Milestone(Some(parent), appending.milestone.symbolId, appending.milestone.pointer, gen),
                conjunct(condition, appendingCondition), trackerId, tracking ++ dependents.map(_.trackerId.get).toSet) +: dependents
            }
            // edgeAction.startNodeProgressConditions에 대해 위 과정 반복 수행
            // val transformedConditions = transformStartProgressConditions(parent.gen, tip.gen, gen, edgeAction.startNodeProgressConditions)
            val transformedConditions = edgeAction.startNodeProgressConditions.map(
              transformEdgeActionCondition(_, -1, parent.gen, tip.gen, gen))
            val propagated = progressTip(parent, gen, transformedConditions.map(conjunct(condition, _)), trackerId, tracking)
            appended ++ propagated
          case None =>
            // 파싱 종료
            // TODO 어떻게 처리하지?
            List()
        }
      }
  }

  def proceed(ctx: MilestoneParserContext, input: Inputs.Input): Either[MilestoneParserContext, ParsingError] = {
    val gen = ctx.gen + 1
    val processor = new ProceedProcessor()
    val milestones0 = processor.proceed(ctx, gen, input)
    if (processor.genActions.isEmpty) {
      if (verbose) {
        println("  ** no eligible actions")
      }
      Right(ParsingErrors.UnexpectedInputByTermGroups(input, ctx.expectingTerminals(parserData), gen))
    } else {
      if (verbose) {
        println("  ** before evaluating accept condition")
        milestones0.foreach(t => println(t.prettyString))
      }
      val acceptConditionEvaluator = new AcceptConditionEvaluator(parserData, milestones0, gen, processor.genActions)
      val milestones1 = milestones0.flatMap { milestone =>
        val newCond = acceptConditionEvaluator.evolveAcceptCondition(milestone.acceptCondition)
        if (newCond == Never) None else Some(milestone.copy(acceptCondition = newCond))
      }
      if (verbose) {
        println("  ** after evaluating accept condition")
        milestones1.foreach(t => println(t.prettyString))
      }
      // milestones에서 trackerId가 있는데 그 ID를 트래킹하는 path가 없어졌으면 제거
      // TODO tracking하는 ID의 path가 이미 사라졌으면 그 ID도 제거
      val pathsBeingTracked = milestones1.flatMap(_.tracking).toSet
      val milestones = milestones1.filter(_.trackerId match {
        case Some(trackerId) => pathsBeingTracked.contains(trackerId)
        case None => true
      })
      if (verbose) {
        println(s"  ** after removing milestones that are not tracked (tracked={${pathsBeingTracked.toList.sorted.mkString(", ")}})")
        milestones.foreach(t => println(t.prettyString))
      }
      Left(MilestoneParserContext(gen, milestones, ctx.genProgressHistory :+ GenProgress(milestones0, processor.genActions)))
    }
  }
}

case class MilestonePath(tip: Milestone, acceptCondition: AcceptCondition, trackerId: Option[Int], tracking: Set[Int]) {
  def prettyString: String = s"${tip.prettyString} $acceptCondition ($trackerId, tracking={${tracking.toList.sorted.mkString(", ")}})"
}

case class Milestone(parent: Option[Milestone], symbolId: Int, pointer: Int, gen: Int) {
  def kernelTemplate: KernelTemplate = KernelTemplate(symbolId, pointer)

  private def myself = s"($symbolId $pointer $gen)"

  def prettyString: String = parent match {
    case Some(value) => s"${value.prettyString} $myself"
    case None => myself
  }
}

sealed trait GenAction {
  val beginGen: Int
  val midGen: Int
  val endGen: Int
  val summary: TasksSummary
}

case class TermAction(beginGen: Int, midGen: Int, endGen: Int, summary: TasksSummary, condition: AcceptCondition) extends GenAction

case class EdgeAction(parentBeginGen: Int, beginGen: Int, midGen: Int, endGen: Int, summary: TasksSummary, condition: AcceptCondition) extends GenAction

case class MilestoneParserContext(gen: Int, paths: List[MilestonePath], genProgressHistory: List[GenProgress]) {
  assert(genProgressHistory.size == gen + 1)

  def expectingTerminals(parserData: MilestoneParserData): Set[TermGroupDesc] = paths.flatMap { path =>
    parserData.termActions(path.tip.kernelTemplate).map(_._1)
  }.toSet
}

case class GenProgress(untrimmedMilestonePaths: List[MilestonePath], genActions: List[GenAction])
