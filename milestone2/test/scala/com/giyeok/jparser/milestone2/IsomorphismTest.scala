package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.nparser2.{ParsingContext => NaiveParsingContext}
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.milestone2.{ParsingContext => MilestoneParsingContext}
import com.giyeok.jparser.proto.MilestoneParserDataProto.Milestone2ParserData
import org.scalatest.flatspec.AnyFlatSpec

class IsomorphismTest extends AnyFlatSpec {
  def checkIsomorphic(naive: NaiveParsingContext, milestone: MilestoneParsingContext): Unit = {
    println(s"paths: ${milestone.paths.size} ${milestone.paths.count(_.first == Milestone(1, 0, 0))}")
  }

  it should "isomorphic" in {
    val parserData = MilestoneParser2ProtobufConverter.fromProto(
      Milestone2ParserData.parseFrom(getClass.getResourceAsStream("/bibix2-parserdata.pb")))

    val nparser = new NaiveParser2(parserData.grammar)
    val mparser = new MilestoneParser(parserData) //.setVerbose()

    var nctx = nparser.initialParsingHistoryContext
    var mctx = mparser.initialCtx

    checkIsomorphic(nctx.parsingContext, mctx)
    val inputs = Inputs.fromString(
      """//    schema = protobuf.schema(
        |//      srcs = [
        |//        "base/proto/GrammarProto.proto",
        |//        "base/proto/TermGroupProto.proto",
        |//      ]
        |//    )
        |a = "hello"
        |""".stripMargin)

    inputs.zipWithIndex.foreach { case (input, index) =>
      //      nctx = nparser.parseStep(nctx, input).getOrElse(throw new IllegalStateException("Failed"))
      println(s"$index $input")
      mctx = mparser.parseStep(mctx, input).getOrElse(throw new IllegalStateException("Failed"))
      checkIsomorphic(nctx.parsingContext, mctx)
    }
  }
}
