package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.examples.metalang3.Catalog
import com.giyeok.jparser.ktglue.toKtList
import com.giyeok.jparser.ktglue.toKtSet
import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.mgroup2.MilestoneGroupParser
import com.giyeok.jparser.mgroup2.MilestoneGroupParserData
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup2.ParsingContext
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.zip.GZIPInputStream

class MilestoneGroupParserKtTests {
  fun convertKernel(kernel: com.giyeok.jparser.nparser.Kernel): Kernel =
    Kernel(kernel.symbolId(), kernel.pointer(), kernel.beginGen(), kernel.endGen())

  fun assertEqualCtx(scalaCtx: ParsingContext, ktCtx: ParsingContextKt) {
    val gen = scalaCtx.gen()
    assertEquals(gen, ktCtx.gen)

//    println("::: $gen")

    val scalaPaths = scalaCtx.paths().toKtList().map { path -> path.prettyString() }
//    scalaPaths.forEach { println(it) }
//    println("-----")
    val ktPaths = ktCtx.paths.map { path -> path.prettyString() }
//    ktPaths.forEach { println(it) }

    assertEquals(scalaCtx.paths().size(), ktCtx.paths.size)
    assertEquals(scalaPaths.toSet(), ktPaths.toSet())
  }

  fun testEquality(
    parserData: MilestoneGroupParserDataProto.MilestoneGroupParserData,
    scalaParserData: MilestoneGroupParserData,
    example: String
  ) {
    println(":: $example")
    val scalaParser = MilestoneGroupParser(scalaParserData)
    // .setVerbose()
    val ktParser = MilestoneGroupParserKt(parserData)
    // .setVerbose()

    var scalaCtx = scalaParser.initialCtx()
    var ktCtx = ktParser.initialCtx
    assertEqualCtx(scalaCtx, ktCtx)
    example.forEach { input ->
      scalaCtx = scalaParser.parseStep(scalaCtx, Inputs.Character(input))
        .getOrElse { throw IllegalStateException() }
      ktCtx = ktParser.parseStep(ktCtx, input)
      assertEqualCtx(scalaCtx, ktCtx)
    }

    val ktParseResult = measureAndPrintTime("kotlin parse") {
      ktParser.parse(example)
    }
    val scalaParseResult = measureAndPrintTime(" scala parse") {
      scalaParser.parseOrThrow(Inputs.fromString(example))
    }

    val ktKernels = measureAndPrintTime("kotlin history") {
      ktParser.kernelsHistory(ktParseResult)
    }
    val scalaKernels = measureAndPrintTime(" scala history") {
      scalaParser.kernelsHistory(scalaParseResult)
    }

    assertEquals(ktKernels.size, scalaKernels.length())
    ktKernels.zip(scalaKernels.toKtList()).forEach { (kt, sc) ->
      assertEquals(kt.kernels, sc.toKtSet().map { convertKernel(it) }.toSet())
    }
  }

  fun <T> measureAndPrintTime(description: String, body: () -> T): T {
    val startTime = Instant.now()
    val result = body()
    val endTime = Instant.now()
    println("elapsed for $description: ${Duration.between(startTime, endTime)}")
    return result
  }

  fun loadParserData(
    resourceName: String
  ): Pair<MilestoneGroupParserDataProto.MilestoneGroupParserData, MilestoneGroupParserData> {

    val proto: MilestoneGroupParserDataProto.MilestoneGroupParserData =
      measureAndPrintTime("proto load") {
        MilestoneGroupParserLoader.loadParserFromGzippedResource(resourceName).parserData
      }

    val scalaParserData = measureAndPrintTime("scala conversion") {
      MilestoneGroupParserDataProtobufConverter.fromProto(proto)
    }

    return Pair(proto, scalaParserData)
  }

  @Test
  fun testMetalang3() {
    val (parserData, scalaParserData) = loadParserData("/cdglang3-mg2-parserdata.pb.gz")
    testEquality(parserData, scalaParserData, "A = 'a'+")
    testEquality(parserData, scalaParserData, "Abc = 'a-z'+ {Hello(a=\"world\")}")
  }

  @Test
  fun testJ1() {
    val (parserData, scalaParserData) = loadParserData("/j1-mg2-parserdata.pb.gz")
    testEquality(parserData, scalaParserData, "class Abc {}")
  }

  @Test
  fun testBibix() {
    val (parserData, scalaParserData) = loadParserData("/bibix2-mg2-parserdata.pb.gz")

    Catalog.bibix2.examples.forEach { example ->
      println("==== name: ${example.name}")
      testEquality(parserData, scalaParserData, example.example)
    }
  }
}
