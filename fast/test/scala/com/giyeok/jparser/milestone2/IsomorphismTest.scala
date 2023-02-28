package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor2}
import com.giyeok.jparser.proto.{MilestoneParserDataProto, MilestoneParserProtobufConverter}
import org.scalatest.flatspec.AnyFlatSpec

class IsomorphismTest extends AnyFlatSpec {
  it should "nogentest" in {
    val parserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
      MilestoneParserDataProto.MilestoneParserData.parseFrom(getClass.getResourceAsStream("/bibix2-parserdata.pb")))

    println("Testing...")
    val nparser = new NaiveParser(parserData.grammar)
    val mparser = new MilestoneParser(parserData, verbose = true)

    val exampleCode = "str = \"$fghasdf\""
    //    val exampleCode = "a = \"abc\""
    println(exampleCode)
    val example = Inputs.fromString(exampleCode)

    val nparseResult = nparser.parse(example).left.get
    val constructor = ParseTreeConstructor2.forestConstructor(parserData.grammar)(example, nparseResult.history, nparseResult.conditionFinal)
    val nparseForest = constructor.reconstruct().get
    println(nparseForest.trees.size)

    val mparseResult = mparser.parse(example).left.get
    val mparseForest = mparser.parseAndReconstructToForest(example).left.get
    println(mparseForest.trees.size)

    // nparseForst.size는 1이고 mparseForest.size는 2고.. milestone parser에 버그가 있음
  }

  it should "gentest" in {
    println("Analyzing...")
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/simple-bibixlike.cdg").readAllBytes()))
    println("ParserGen...")
    val parserData = MilestoneParserGen.generateMilestoneParserData(analysis.ngrammar)

    println("Testing...")
    val nparser = new NaiveParser(analysis.ngrammar)
    val mparser = new MilestoneParser(parserData, verbose = true)

    // a = "abc" 가 실패하는 이유: accept condition 처리할 때 symbolFinishConditions 에서 finishedKernels 를 제대로 못 찾아서
    //    val exampleCode = "a = \"abc\""
    val exampleCode = "str = \"hello $fgh\""
    val example = Inputs.fromString(exampleCode)

    val nparseResult = nparser.parse(example).left.get
    val constructor = ParseTreeConstructor2.forestConstructor(analysis.ngrammar)(example, nparseResult.history, nparseResult.conditionFinal)
    val nparseForest = constructor.reconstruct().get
    println(s"naive: ${nparseForest.trees.size}")

    val mparseResult = mparser.parse(example) match {
      case Right(error) => throw new Exception(error.toString)
      case Left(value) => value
    }
    val kernels = MilestoneParser.getKernelsHistory(parserData, mparseResult)
    val mparseForest = mparser.parseAndReconstructToForest(example).left.get
    println(s"milestone: ${mparseForest.trees.size}")

    // nparseForst.size는 1이고 mparseForest.size는 2고.. milestone parser에 버그가 있음
  }
}
