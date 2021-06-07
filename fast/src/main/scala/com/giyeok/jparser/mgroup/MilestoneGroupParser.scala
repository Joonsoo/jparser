package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.mgroup.MilestoneGroupParser.reconstructParseTree
import com.giyeok.jparser.nparser.AcceptCondition
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, And, Exists, Never, NotExists, OnlyIf, Or, Unless, conjunct, disjunct}
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}

class MilestoneGroupParser(val parserData: MilestoneGroupParserData, val verbose: Boolean = false) {
  def initialCtx: MilestoneGroupParserContext = MilestoneGroupParserContext(0,
    List(MilestoneGroupPath(None, parserData.startMilestoneGroup, 0, List(Always))), List())

  def materializeEdgeActionAcceptCondition(acceptCondition: AcceptCondition,
                                           grandParentGen: Int, parentGen: Int, tipGen: Int, nextGen: Int): AcceptCondition = acceptCondition

  class Proceeder(ctx: MilestoneGroupParserContext) {
    private val nextGen = ctx.gen + 1

    def applyEdgeAction(path: MilestoneGroupPath): List[MilestoneGroupPath] = path.parent match {
      case Some(parent) =>
        val edgeAction = parserData.edgeActions(parent.milestoneGroup -> path.milestoneGroup)
        val appending = edgeAction.appendingAction.map { appending =>
          val newParent = replaceTip(parent, appending.replacement)
          MilestoneGroupPath(Some(newParent), appending.appendingMGroup, nextGen,
            succeedAcceptConditions(path.acceptConditionSlots, appending.acceptConditions))
        }
        val chaining = edgeAction.progress.toList.flatMap { parentProgress =>
          applyEdgeAction(applyTipProgress(parent, parentProgress))
        }
        appending.toList ++ chaining
      case None => List()
    }

    def replaceTip(path: MilestoneGroupPath, replacementOpt: Option[StepReplacement]): MilestoneGroupPath = replacementOpt match {
      case Some(replacement) => MilestoneGroupPath(parent = path.parent, milestoneGroup = replacement.mgroup,
        gen = path.gen, acceptConditionSlots = replacement.succeedingAcceptConditionSlots.map(path.acceptConditionSlots))
      case None => path
    }

    // successions에는 SlotCondition이 들어가 있을 수 있음
    def succeedAcceptConditions(prevSlots: List[AcceptCondition], successions: List[AcceptCondition]): List[AcceptCondition] = {
      def traverseSuccessionCondition(cond: AcceptCondition): AcceptCondition = cond match {
        case AcceptCondition.AcceptConditionSlot(slotIdx) => prevSlots(slotIdx)
        case _ => ???
      }

      val result = successions.map(traverseSuccessionCondition)
      result
      //        disjunct(prevSlots(succ.succeedingSlot),
      //          materializeEdgeActionAcceptCondition(succ.newCondition, ???, ???, ???, ???))
    }

    def applyTipProgress(path: MilestoneGroupPath, tipProgress: StepProgress): MilestoneGroupPath =
      MilestoneGroupPath(parent = path.parent,
        milestoneGroup = tipProgress.tipReplacement, gen = path.gen,
        // materialize accept condition
        acceptConditionSlots = tipProgress.acceptConditions)

    def proceed(input: Inputs.Input): Either[MilestoneGroupParserContext, ParsingError] = {
      val newPaths = ctx.paths.flatMap { path =>
        // TODO path에 drop action 적용(parserData.milestoneDropActions(path.milestoneGroup))
        val termActions = parserData.termActions(path.milestoneGroup)
        termActions.find(_._1.contains(input)) match {
          case Some((_, termAction)) =>
            val appended = termAction.appendingAction.map { appending =>
              // appending이 있으면
              val newTip = replaceTip(path, appending.replacement)
              MilestoneGroupPath(Some(newTip), appending.appendingMGroup, nextGen, appending.acceptConditions)
            }
            // (path.parent->newTip) 엣지에 대해서 edgeAction 적용 시작
            val progressed = termAction.progress.toList.flatMap { tipProgress =>
              applyEdgeAction(applyTipProgress(path, tipProgress))
            }
            appended.toList ++ progressed
          case None => List()
        }
      }
      Left(MilestoneGroupParserContext(nextGen, newPaths, List()))
    }
  }

  def proceed(ctx: MilestoneGroupParserContext, input: Inputs.Input): Either[MilestoneGroupParserContext, ParsingError] =
    new Proceeder(ctx).proceed(input)

  def parse(inputSeq: Seq[Inputs.Input]): Either[MilestoneGroupParserContext, ParsingError] = {
    if (verbose) {
      println("=== initial")
      new MilestoneGroupParserPrinter(parserData).printMilestoneGroupPaths(initialCtx.paths)
    }
    inputSeq.foldLeft[Either[MilestoneGroupParserContext, ParsingError]](Left(initialCtx)) { (m, nextInput) =>
      m match {
        case Left(currCtx) if currCtx.paths.isEmpty =>
          Right(ParsingErrors.UnexpectedInput(nextInput, Set(), currCtx.gen))
        case Left(currCtx) =>
          if (verbose) {
            println(s"=== ${currCtx.gen} -> ${currCtx.gen + 1} $nextInput")
          }
          val result = proceed(currCtx, nextInput)
          if (verbose) {
            result match {
              case Left(nextCtx) =>
                new MilestoneGroupParserPrinter(parserData).printMilestoneGroupPaths(nextCtx.paths)
              case Right(value) =>
                println(s"Error: $value")
            }
          }
          result
        case error => error
      }
    }
  }

  def parse(input: String): Either[MilestoneGroupParserContext, ParsingError] = parse(Inputs.fromString(input))

  def parseAndReconstructToForest(inputSeq: Seq[Inputs.Input]): Either[ParseForest, ParsingError] = {
    parse(inputSeq) match {
      case Left(finalCtx) =>
        reconstructParseTree(parserData, finalCtx, inputSeq) match {
          case Some(forest) => Left(forest)
          case None => Right(ParsingErrors.UnexpectedEOFByTermGroups(finalCtx.expectingTerminals(parserData), finalCtx.gen))
        }
      case Right(error) => Right(error)
    }
  }

  def parseAndReconstructToForest(input: String): Either[ParseForest, ParsingError] =
    parseAndReconstructToForest(Inputs.fromString(input))
}

object MilestoneGroupParser {
  def reconstructParseTree(parserData: MilestoneGroupParserData, finalCtx: MilestoneGroupParserContext, input: Seq[Input]): Option[ParseForest] = {
    ???
  }
}

// Main path와 dependent path로 나눠야 할듯?
case class MilestoneGroupParserContext(gen: Int, paths: List[MilestoneGroupPath], genProgressHistory: List[GenProgress]) {
  def expectingTerminals(parserData: MilestoneGroupParserData): Set[TermGroupDesc] =
    parserData.termActions(paths.head.milestoneGroup).map(_._1).toSet
}

case class MilestoneGroupPath(parent: Option[MilestoneGroupPath], milestoneGroup: Int, gen: Int, acceptConditionSlots: List[AcceptCondition]) {
  private def myself: String = s"($milestoneGroup $gen)"

  def prettyString: String = parent match {
    case Some(value) => s"${value.prettyString} $myself"
    case None => myself
  }
}

case class GenProgress()
