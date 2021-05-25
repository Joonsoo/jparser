package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.mgroup.MilestoneGroupParser.reconstructParseTree
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, And, Exists, Never, NotExists, OnlyIf, Or, Unless, conjunct, disjunct}
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}

class MilestoneGroupParser(val parserData: MilestoneGroupParserData, val verbose: Boolean = false) {
  val startMilestoneGroupPath: MilestoneGroupPath = MilestoneGroupPath(MilestoneGroupStep(None, parserData.startMilestoneGroup, 0), Always)

  def parse(inputSeq: Seq[Inputs.Input]): Either[MilestoneGroupParserContext, ParsingError] = {
    if (verbose) {
      println("=== initial")
      ???
    }
    ???
  }

  def parse(input: String): Either[MilestoneGroupParserContext, ParsingError] = parse(Inputs.fromString(input))

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

  private class ProceedProcessor() {
    def proceed(ctx: MilestoneGroupParserContext, gen: Int, input: Inputs.Input): List[MilestoneGroupPath] = ctx.paths.flatMap { path =>
      val tip = path.tip
      val parentGen = tip.parent.map(_.gen).getOrElse(0)
      val termActions = parserData.termActions(tip.milestoneGroup)
      termActions.find(_._1.contains(input)) match {
        case Some((_, action)) =>
          val appended = action.appendingMilestoneGroups.map { appending =>
            MilestoneGroupPath(MilestoneGroupStep(Some(tip), appending.appendingMilestoneGroup, gen),
              Always)
          }
          val reduced = progressTip(tip, gen, List())
          appended ++ reduced
        case None => List()
      }
    }

    def progressTip(tip: MilestoneGroupStep, gen: Int, acceptConditions: List[AcceptCondition]): List[MilestoneGroupPath] = {
      acceptConditions flatMap { condition =>
        tip.parent match {
          case Some(parent) =>
            val parentBeginGen = parent.parent.map(_.gen).getOrElse(0)
            val edgeAction = parserData.edgeProgressActions((parent.milestoneGroup, tip.milestoneGroup))
            val appended = edgeAction.appendingMilestoneGroups.map { appending =>
              MilestoneGroupPath(MilestoneGroupStep(Some(parent), appending.appendingMilestoneGroup, gen),
                conjunct(condition, appending.acceptCondition))
            }
            val transformedConditions = edgeAction.startNodeProgressConditions.values.map(
              transformEdgeActionCondition(_, -1, parent.gen, tip.gen, gen)
            ).toList
            val propagated = progressTip(parent, gen, transformedConditions)
            appended ++ propagated
          case None =>
            // 파싱 종료
            // TODO
            List()
        }
      }
    }
  }

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

case class MilestoneGroupParserContext(gen: Int, paths: List[MilestoneGroupPath], genProgressHistory: List[GenProgress]) {
  def expectingTerminals(parserData: MilestoneGroupParserData): Set[TermGroupDesc] = ???
}

case class MilestoneGroupPath(tip: MilestoneGroupStep, acceptCondition: AcceptCondition)

case class MilestoneGroupStep(parent: Option[MilestoneGroupStep], milestoneGroup: Int, gen: Int) {
  private def myself: String = s"($milestoneGroup $gen)"

  def prettyString: String = parent match {
    case Some(value) => s"${value.prettyString} $myself"
    case None => myself
  }
}

case class GenProgress()
