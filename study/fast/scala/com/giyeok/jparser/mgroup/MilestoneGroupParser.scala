package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.mgroup.MilestoneGroupParser.reconstructParseTree
import com.giyeok.jparser.mgroup.MilestoneGroupParserData._
import com.giyeok.jparser.nparser.AcceptCondition
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}

class MilestoneGroupParser(val parserData: MilestoneGroupParserData, val verbose: Boolean = false) {
  def initialCtx: MilestoneGroupParserContext = MilestoneGroupParserContext(0,
    List(MilestoneGroupPath(None, parserData.startMilestoneGroup, 0, List(Always))), List())

  class Proceeder(ctx: MilestoneGroupParserContext) {
    private val currGen = ctx.gen
    private val nextGen = currGen + 1

    def applyEdgeAction(path: MilestoneGroupPath): List[MilestoneGroupPath] = path.parent match {
      case Some(parent) =>
        val edgeAction = parserData.edgeActions(parent.milestoneGroup -> path.milestoneGroup)
        val appending = edgeAction.appendingAction.map { appending =>
          val newParent = replaceTip(parent, appending.replacement)
          MilestoneGroupPath(Some(newParent), appending.appendingMGroup, nextGen,
            succeedEdgeActionACs(path, path.acceptConditionSlots, appending.acceptConditions))
        }
        val chaining = edgeAction.progress.toList.flatMap { parentProgress =>
          val parentAcceptConditions = parentProgress.tipReplacement.succeedingAcceptConditionSlots.map(parent.acceptConditionSlots)
          val succeededAcceptConditions =
            succeedEdgeActionACs(parent, path.acceptConditionSlots, parentProgress.acceptConditions)
          assert(parentAcceptConditions.size == succeededAcceptConditions.size)
          assert(parserData.milestoneGroups(parentProgress.tipReplacement.mgroup).acceptConditionSlots == succeededAcceptConditions.size)
          val newParent = MilestoneGroupPath(parent = parent.parent,
            milestoneGroup = parentProgress.tipReplacement.mgroup, gen = parent.gen,
            acceptConditionSlots = parentAcceptConditions.zip(succeededAcceptConditions).map(p => conjunct(p._1, p._2)))
          applyEdgeAction(newParent)
        }
        appending.toList ++ chaining
      case None => List()
    }

    def replaceTip(path: MilestoneGroupPath, replacementOpt: Option[StepReplacement]): MilestoneGroupPath = replacementOpt match {
      case Some(replacement) => replaceTip(path, replacement)
      case None => path
    }

    def replaceTip(path: MilestoneGroupPath, replacement: StepReplacement): MilestoneGroupPath =
      MilestoneGroupPath(parent = path.parent, milestoneGroup = replacement.mgroup,
        gen = path.gen, acceptConditionSlots = replacement.succeedingAcceptConditionSlots.map(path.acceptConditionSlots))

    def materializeEdgeActionAC(acceptCondition: AcceptCondition, path: MilestoneGroupPath): AcceptCondition = {
      //      new MilestoneGroupParserPrinter(parserData).printMilestoneGroupPath(path)
      //      println(acceptCondition)

      def mapGen(gen: Int) = gen match {
        case EDGE_PARENT_GEN => ???
        case EDGE_START_GEN => if (path.parent.isEmpty) -1 else path.parent.get.gen
        case EDGE_END_GEN | TERM_END_GEN => path.gen
        case EDGE_CURRENT_GEN | TERM_CURRENT_GEN => currGen
        case EDGE_NEXT_GEN | TERM_NEXT_GEN => nextGen
        case EDGE_NEXT_PLUS_GEN | TERM_NEXT_PLUS_GEN => nextGen + 1
      }

      def materialize(cond: AcceptCondition): AcceptCondition =
        cond match {
          case Always => Always
          case Never => Never
          case And(conditions) => conjunct(conditions.toSeq.map(materialize): _*)
          case Or(conditions) => disjunct(conditions.toSeq.map(materialize): _*)
          case NotExists(beginGen, endGen, symbolId) =>
            NotExists(mapGen(beginGen), mapGen(endGen), symbolId)
          case Exists(beginGen, endGen, symbolId) =>
            Exists(mapGen(beginGen), mapGen(endGen), symbolId)
          case Unless(beginGen, endGen, symbolId) =>
            Unless(mapGen(beginGen), mapGen(endGen), symbolId)
          case OnlyIf(beginGen, endGen, symbolId) =>
            OnlyIf(mapGen(beginGen), mapGen(endGen), symbolId)
          case AcceptCondition.AcceptConditionSlot(slotIdx) => ???
          case AcceptCondition.AcceptConditionSlotNeg(slotIdx) => ???
        }

      materialize(acceptCondition)
    }

    def materializeTermActionAC(acceptCondition: AcceptCondition, path: MilestoneGroupPath): AcceptCondition = {
      def mapGen(gen: Int) = gen match {
        case TERM_START_GEN => ???
        case TERM_END_GEN => path.gen
        case TERM_CURRENT_GEN => currGen
        case TERM_NEXT_GEN => nextGen
        case TERM_NEXT_PLUS_GEN => nextGen + 1
      }

      def materialize(cond: AcceptCondition): AcceptCondition =
        cond match {
          case Always => Always
          case Never => Never
          case And(conditions) => conjunct(conditions.toSeq.map(materialize): _*)
          case Or(conditions) => disjunct(conditions.toSeq.map(materialize): _*)
          case NotExists(beginGen, endGen, symbolId) =>
            NotExists(mapGen(beginGen), mapGen(endGen), symbolId)
          case Exists(beginGen, endGen, symbolId) =>
            Exists(mapGen(beginGen), mapGen(endGen), symbolId)
          case Unless(beginGen, endGen, symbolId) =>
            Unless(mapGen(beginGen), mapGen(endGen), symbolId)
          case OnlyIf(beginGen, endGen, symbolId) =>
            OnlyIf(mapGen(beginGen), mapGen(endGen), symbolId)
          case AcceptCondition.AcceptConditionSlot(slotIdx) => ???
          case AcceptCondition.AcceptConditionSlotNeg(slotIdx) => ???
        }

      materialize(acceptCondition)
    }

    def succeedEdgeActionACs(path: MilestoneGroupPath, prevSlots: List[AcceptCondition], successions: List[AcceptCondition]): List[AcceptCondition] = {
      def traverseSuccessionCondition(cond: AcceptCondition): AcceptCondition = cond match {
        case Always | Never => cond
        case AcceptCondition.AcceptConditionSlot(slotIdx) => prevSlots(slotIdx)
        case AcceptCondition.AcceptConditionSlotNeg(slotIdx) => prevSlots(slotIdx).neg
        case And(conditions) => conjunct(conditions.toSeq.map(traverseSuccessionCondition): _*)
        case Or(conditions) => disjunct(conditions.toSeq.map(traverseSuccessionCondition): _*)
        case _: NotExists | _: Exists | _: Unless | _: OnlyIf =>
          materializeEdgeActionAC(cond, path)
      }

      val result = successions.map(traverseSuccessionCondition)
      result
      //        disjunct(prevSlots(succ.succeedingSlot),
      //          materializeEdgeActionAcceptCondition(succ.newCondition, ???, ???, ???, ???))
    }

    def succeedTermActionACs(path: MilestoneGroupPath, prevSlots: List[AcceptCondition], successions: List[AcceptCondition]): List[AcceptCondition] = {
      def traverseSuccessionCondition(cond: AcceptCondition): AcceptCondition = cond match {
        case Always | Never => cond
        case AcceptCondition.AcceptConditionSlot(slotIdx) => prevSlots(slotIdx)
        case AcceptCondition.AcceptConditionSlotNeg(slotIdx) => prevSlots(slotIdx).neg
        case And(conditions) => conjunct(conditions.toSeq.map(traverseSuccessionCondition): _*)
        case Or(conditions) => disjunct(conditions.toSeq.map(traverseSuccessionCondition): _*)
        case _: NotExists | _: Exists | _: Unless | _: OnlyIf =>
          materializeTermActionAC(cond, path)
      }

      val result = successions.map(traverseSuccessionCondition)
      result
    }

    def applyTipProgress(path: MilestoneGroupPath, tipProgress: StepProgress): MilestoneGroupPath = {
      val replacedTipAcceptConditions = tipProgress.tipReplacement.succeedingAcceptConditionSlots.map(path.acceptConditionSlots)
      assert(replacedTipAcceptConditions.size == tipProgress.acceptConditions.size)
      val succeededAcceptConditions = succeedEdgeActionACs(path, path.acceptConditionSlots, tipProgress.acceptConditions)
      assert(replacedTipAcceptConditions.size == succeededAcceptConditions.size)
      MilestoneGroupPath(parent = path.parent,
        milestoneGroup = tipProgress.tipReplacement.mgroup, gen = path.gen,
        // materialize accept condition
        acceptConditionSlots = replacedTipAcceptConditions.zip(succeededAcceptConditions).map(p => conjunct(p._1, p._2)))
    }

    def proceed(input: Inputs.Input): Either[MilestoneGroupParserContext, ParsingError] = {
      val newPaths = ctx.paths.flatMap { path =>
        // TODO path에 drop action 적용(parserData.milestoneDropActions(path.milestoneGroup))
        val termActions = parserData.termActions(path.milestoneGroup)
        termActions.find(_._1.contains(input)) match {
          case Some((_, termAction)) =>
            val appended = termAction.appendingAction.map { appending =>
              // appending이 있으면
              val newTip = replaceTip(path, appending.replacement)
              // appending.acceptConditions는 모두 새로 생긴 AC들이므로 AcceptConditionSlot이 없음
              MilestoneGroupPath(Some(newTip), appending.appendingMGroup, nextGen,
                succeedTermActionACs(path, path.acceptConditionSlots, appending.acceptConditions))
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
