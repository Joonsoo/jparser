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

// ktparser가 mgroup2 파서와 동일하게 동작하는지 테스트
class EqualityWithMgroup2Tests {
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
    scalaParserData: MilestoneGroupParserData,
    kotlinParserData: MilestoneGroupParserDataKt,
    example: String
  ) {
    println(":: ${example.substringBefore('\n')} (len=${example.length})")
    val scalaParser = MilestoneGroupParser(scalaParserData)
    // .setVerbose()
    val ktParser = MilestoneGroupParserKt(kotlinParserData)
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

    val scalaParseResult = measureAndPrintTime(" scala parse") {
      scalaParser.parseOrThrow(Inputs.fromString(example))
    }
    val ktParseResult = measureAndPrintTime("kotlin parse") {
      ktParser.parse(example)
    }

    val scalaKernels = measureAndPrintTime(" scala history") {
      scalaParser.kernelsHistory(scalaParseResult)
    }
    val ktKernels = measureAndPrintTime("kotlin history") {
      ktParser.kernelsHistory(ktParseResult)
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
  ): Pair<MilestoneGroupParserData, MilestoneGroupParserDataKt> {

    val proto: MilestoneGroupParserDataProto.MilestoneGroupParserData =
      measureAndPrintTime("proto load") {
        MilestoneGroupParserLoader.loadParserDataFromGzippedResource(resourceName)
      }

    val scalaParserData = measureAndPrintTime("scala conversion") {
      MilestoneGroupParserDataProtobufConverter.fromProto(proto)
    }

    val kotlinParserData = measureAndPrintTime("kotlin conversion") {
      MilestoneGroupParserDataKt(proto)
    }

    return Pair(scalaParserData, kotlinParserData)
  }

  @Test
  fun testMetalang3() {
    val (scalaParserData, kotlinParserData) = loadParserData("/cdglang3-mg2-parserdata.pb.gz")
    testEquality(scalaParserData, kotlinParserData, "A = 'a'+")
    testEquality(scalaParserData, kotlinParserData, "Abc = 'a-z'+ {Hello(a=\"world\")}")
  }

  @Test
  fun testJ1() {
    val (scalaParserData, kotlinParserData) = loadParserData("/j1-mg2-parserdata.pb.gz")

    Catalog.j1.examples.forEach { example ->
      println("==== name: ${example.name}")
      testEquality(scalaParserData, kotlinParserData, example.example)
    }
  }

  @Test
  fun testBibix() {
    val (scalaParserData, kotlinParserData) = loadParserData("/bibix2-mg2-parserdata.pb.gz")

    Catalog.bibix2.examples.forEach { example ->
      println("==== name: ${example.name}")
      testEquality(scalaParserData, kotlinParserData, example.example)
    }
  }

  @Test
  fun testBibixTrimmed() {
    val (scalaParserData, kotlinParserData) = loadParserData("/bibix2-mg2-parserdata-trimmed.pb.gz")

    Catalog.bibix2.examples.forEach { example ->
      println("==== name: ${example.name}")
      testEquality(scalaParserData, kotlinParserData, example.example)
    }
  }
}
