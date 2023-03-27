package com.giyeok.jparser.ktglue.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktglue.Milestone2ParserLoader
import com.giyeok.jparser.ktglue.toKtKernelSet
import com.giyeok.jparser.ktglue.toKtList
import com.giyeok.jparser.ktlib.test.BibixAst
import org.junit.jupiter.api.Test

object BibixAstTest {
  @Test
  fun testNew() {
    val parser = Milestone2ParserLoader.loadGzippedParserFromResource("/bibix2-m2-parserdata.pb.gz")
      .setVerbose()

//    val inputText = File("build.bbx").readText()
    val inputText = """
      aaa = "hello" + " world!"
      bbb = ["hello", "world"] + ["everyone"]
      ccc = ["hello"] + "world"
      ddd = [] + "hello"
      eee = [("hello", "world")] + ["good"]
      fff = ["hello", "world"] as set<string> + ["everyone"]
      ggg = ["hello", "world"] + (["everyone"] as set<string>)
    """.trimIndent()
    val startTime = System.currentTimeMillis()
    val inputs = Inputs.fromString(inputText)
    val parseResult = parser.parseOrThrow(inputs)
    println("Parsing: ${System.currentTimeMillis() - startTime}")
    val startTime2 = System.currentTimeMillis()
    val history = parser.kernelsHistory(parseResult).toKtList().map {
      it.toKtKernelSet()
    }
    println("History: ${System.currentTimeMillis() - startTime2}")
    val startTime3 = System.currentTimeMillis()
    val buildScript = BibixAst(inputText, history).matchStart()
    // println(buildScript)
    println("Astifier: ${System.currentTimeMillis() - startTime3}")
  }

//  fun testOld() {
//    val parserDataProto = this::class.java.getResourceAsStream("/bibix2-oldparserdata.pb").use {
//      MilestoneParserDataProto.MilestoneParserData.parseFrom(it)
//    }
//    val parserData =
//      MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(parserDataProto)
//
//    val parser = com.giyeok.jparser.milestone.MilestoneParser(parserData, false)
//    val inputText = File("build.bbx").readText()
//
//    val inputs = Inputs.fromString(inputText)
//    parser.parseAndReconstructToForest(inputs)
//  }
}

// gen 18의 314, 4, 19에서 'i'에 대한 term action이 실행될 때 pended path로 (69, 0) -> (77, 1)이 들어가야 하는데 왜 빠져있을까?
