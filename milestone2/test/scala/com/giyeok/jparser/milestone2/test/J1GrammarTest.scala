package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.{Inputs, ParseForestFunc}
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParser2ProtobufConverter}
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Using

class J1GrammarTest extends AnyFlatSpec {
  "j1 grammar load" should "work" in {
    val parserData = Using(getClass.getResourceAsStream("/j1-parserdata.pb")) { inputStream =>
      MilestoneParser2ProtobufConverter.fromProto(
        MilestoneParserDataProto.Milestone2ParserData.parseFrom(inputStream)
      )
    }.get

    val parser = new MilestoneParser(parserData).setVerbose()
    val input = Inputs.fromString("class A {\n  String xx = \"\\22\";\n}")
    val result = parser.parseOrThrow(input)
    val history = parser.kernelsHistory(result)
    println(history(32))

    val parseForest = new ParseTreeConstructor2(ParseForestFunc)(parserData.grammar)(input, history.map(Kernels)).reconstruct().get
    assert(parseForest.trees.size == 1)
    println(parseForest.trees.head)
  }
}
