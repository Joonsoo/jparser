package com.giyeok.jparser.ktlib.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.milestone2.MilestoneParser
import com.giyeok.jparser.proto.MilestoneParser2ProtobufConverter
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter
import java.io.File

object BibixAstTest {
  fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
    List<T>(this.size()) { idx -> this.apply(idx) }

  fun <T> scala.collection.immutable.Seq<T>.toKtList(): List<T> =
    List<T>(this.size()) { idx -> this.apply(idx) }

  fun <T> scala.collection.immutable.Set<T>.toKtSet(): Set<T> =
    this.toList().toKtList().toSet()

  fun testNew() {
    val parserDataProto = this::class.java.getResourceAsStream("/bibix2-parserdata.pb").use {
      MilestoneParserDataProto.Milestone2ParserData.parseFrom(it)
    }
    val parserData = MilestoneParser2ProtobufConverter.fromProto(parserDataProto)

    val parser = MilestoneParser(parserData).setVerbose()
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
      KernelSet(it.toKtSet())
    }
    println("History: ${System.currentTimeMillis() - startTime2}")
    val startTime3 = System.currentTimeMillis()
    val buildScript = BibixAst(inputs.toKtList(), history).matchStart()
    // println(buildScript)
    println("Astifier: ${System.currentTimeMillis() - startTime3}")
  }

  fun testOld() {
    val parserDataProto = this::class.java.getResourceAsStream("/bibix2-oldparserdata.pb").use {
      MilestoneParserDataProto.MilestoneParserData.parseFrom(it)
    }
    val parserData =
      MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(parserDataProto)

    val parser = com.giyeok.jparser.milestone.MilestoneParser(parserData, false)
    val inputText = File("build.bbx").readText()

    val inputs = Inputs.fromString(inputText)
    parser.parseAndReconstructToForest(inputs)
  }

  @JvmStatic
  fun main(args: Array<String>) {
    println("=== new ===")
    testNew()
//    println("=== old ===")
//    testOld()
  }
}

// gen 18의 314, 4, 19에서 'i'에 대한 term action이 실행될 때 pended path로 (69, 0) -> (77, 1)이 들어가야 하는데 왜 빠져있을까?
