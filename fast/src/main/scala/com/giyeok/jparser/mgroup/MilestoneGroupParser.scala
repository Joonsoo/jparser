package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.{Input, TermGroupDesc}
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.mgroup.MilestoneGroupParser.reconstructParseTree
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}

class MilestoneGroupParser(val parserData: MilestoneGroupParserData, val verbose: Boolean = false) {
  def parse(inputSeq: Seq[Inputs.Input]): Either[MilestoneGroupParserContext, ParsingError] = {
    if (verbose) {
      println("=== initial")
      ???
    }
    ???
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

case class MilestoneGroupParserContext(gen: Int) {
  def expectingTerminals(parserData: MilestoneGroupParserData): Set[TermGroupDesc] = ???
}
