package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import com.giyeok.jparser.milestone2.MilestoneParser
import com.giyeok.jparser.milestone2.MilestoneParser2ProtobufConverter
import com.giyeok.jparser.milestone2.MilestoneParserData
import com.giyeok.jparser.milestone2.MilestoneParserGen
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.proto.GrammarProtobufConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import scala.jdk.javaapi.CollectionConverters
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.readText

// Ablation 하네스: Naive ACP (nparser2) vs Milestone ACP (milestone2) vs MGACP (mgroup3)
// 를 같은 문법/같은 입력으로 측정. 논문 §7 의 ablation (claim (b) 의 직접 증거) 데이터 소스.
// 구 /tmp/divcheck/AblationBench.scala 의 재구축 (2026-07-03; 원본은 커밋된 적 없이 소실).
//
// 방법론:
//  - 생성 시간: 파서 데이터 생성을 1회 측정 (mulang milestone2 는 파일 캐시,
//    mulang mgroup3 는 fixture data.pb 로드 — 그 경우 "(cached)"/"(fixture)" 표기).
//  - 파스 시간: warmup 1회 + 3회 측정의 median. warmup 이 CAP_MS(기본 5000ms) 를
//    넘으면 그 1회 시간만 기록하고 (capped) 같은 파서의 더 큰 입력은 스킵.
//  - liveMax: 비계측 step 구동 1회로 gen 별 live 상태 크기의 최대값.
//      naive2      = trimmed kernel graph 의 node 수
//      milestone2  = ctx.paths 수 (병합 없는 milestone 체인 수)
//      mgroup3     = 전체 path shape 수 (main + watcher; main 만은 별도 열)
//    파스가 cap 에 걸린 크기에서는 liveMax 도 스킵.
//
// 실행: bibix4 runAblationBenchmark (json+proto3) / runAblationBenchmarkMulang /
//       runAblationBenchmarkAll. 결과 로그는 jparser-paper/notes 에 보관.
@EnabledIfEnvironmentVariable(named = "ABLATION_BENCH", matches = "1")
class AblationBenchmarkTest {
  private val capMs: Double = (System.getenv("ABLATION_CAP_MS")?.toLongOrNull() ?: 5000L).toDouble()

  private fun analyze(cdgText: String, start: String): NGrammar =
    `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdgText, start).ngrammar()

  private inline fun timeMs(block: () -> Unit): Double {
    val t0 = System.nanoTime()
    block()
    return (System.nanoTime() - t0) / 1e6
  }

  private fun fmtMs(ms: Double): String =
    if (ms >= 1000) "%.1fs".format(ms / 1000) else "%.1fms".format(ms)

  // ===== 파서 어댑터 =====

  private interface ParserAdapter {
    val name: String

    // 한 번의 전체 파스 (계측 대상)
    fun parseOnce(input: String)

    // 수용 여부 (비계측, 크기별 1회 sanity check)
    fun accepts(input: String): Boolean

    // step 구동하며 (liveMax, auxMax) 측정. aux 는 파서별 보조 지표
    // (mgroup3 = main root 만의 shape 수; 나머지는 0).
    fun liveProfile(input: String): Pair<Int, Int>
  }

  private class NaiveAdapter(val grammar: NGrammar) : ParserAdapter {
    override val name = "naive2"
    val parser = NaiveParser2(grammar)

    override fun parseOnce(input: String) {
      val r = parser.parse(Inputs.fromString(input))
      check(r.isRight) { "naive2 parse error" }
    }

    override fun accepts(input: String): Boolean {
      val r = parser.parse(Inputs.fromString(input))
      if (!r.isRight) return false
      val hctx = (r as scala.util.Right<*, *>).value() as NaiveParser2.ParsingHistoryContext
      // 최종 trimmed 컨텍스트는 완료된 start kernel 을 유지하지 않으므로 (미래 진행에
      // 기여하지 않는 kernel 은 trim), 판정은 history 기반 트리 재구성으로 —
      // NaiveParser2CornerCaseTests 의 naive2Accepts 와 동일한 규약.
      val forest = parser.parseTreeReconstructor2<com.giyeok.jparser.ParseForest>(
        com.giyeok.jparser.`ParseForestFunc$`.`MODULE$`, hctx
      ).reconstruct()
      return !forest.isEmpty && (forest.get() as com.giyeok.jparser.ParseForest).trees().nonEmpty()
    }

    override fun liveProfile(input: String): Pair<Int, Int> {
      var hctx = parser.initialParsingHistoryContext()
      var maxNodes = hctx.parsingContext().graph().nodes().size()
      for (c in input) {
        val r = parser.parseStep(hctx, Inputs.Character(c))
        check(r.isRight) { "naive2 parse error during live profile" }
        hctx = (r as scala.util.Right<*, *>).value() as NaiveParser2.ParsingHistoryContext
        val n = hctx.parsingContext().graph().nodes().size()
        if (n > maxNodes) maxNodes = n
      }
      return Pair(maxNodes, 0)
    }
  }

  private class MilestoneAdapter(val grammar: NGrammar, val data: MilestoneParserData) : ParserAdapter {
    override val name = "milestone2"
    val parser = MilestoneParser(data)

    override fun parseOnce(input: String) {
      val r = parser.parse(Inputs.fromString(input))
      check(r.isRight) { "milestone2 parse error" }
    }

    override fun accepts(input: String): Boolean {
      val r = parser.parse(Inputs.fromString(input))
      if (!r.isRight) return false
      val ctx = (r as scala.util.Right<*, *>).value() as com.giyeok.jparser.milestone2.ParsingContext
      val lastKernels = CollectionConverters.asJava(parser.kernelsHistory(ctx).last())
      val startKernel = Kernel(grammar.startSymbol(), 1, 0, input.length)
      return lastKernels.contains(startKernel)
    }

    override fun liveProfile(input: String): Pair<Int, Int> {
      var ctx = parser.initialCtx()
      var maxPaths = ctx.paths().size()
      for (c in input) {
        val r = parser.parseStep(ctx, Inputs.Character(c))
        check(r.isRight) { "milestone2 parse error during live profile" }
        ctx = (r as scala.util.Right<*, *>).value() as com.giyeok.jparser.milestone2.ParsingContext
        val n = ctx.paths().size()
        if (n > maxPaths) maxPaths = n
      }
      return Pair(maxPaths, 0)
    }
  }

  private class Mgroup3Adapter(val data: Mgroup3ParserData) : ParserAdapter {
    override val name = "mgroup3"
    val parser = Mgroup3Parser(data)

    override fun parseOnce(input: String) {
      parser.parse(input)
    }

    override fun accepts(input: String): Boolean =
      try {
        parser.isAccepted(parser.parse(input))
      } catch (e: ParsingError) {
        false
      }

    override fun liveProfile(input: String): Pair<Int, Int> {
      var ctx = parser.initCtx()
      var maxShapes = ctx.paths.values.sumOf { it.size }
      var maxMain = ctx.paths[ctx.mainRoot]?.size ?: 0
      for ((idx, c) in input.withIndex()) {
        ctx = parser.parseStep(ctx, c, idx + 1 == input.length)
        val shapes = ctx.paths.values.sumOf { it.size }
        val main = ctx.paths[ctx.mainRoot]?.size ?: 0
        if (shapes > maxShapes) maxShapes = shapes
        if (main > maxMain) maxMain = main
      }
      return Pair(maxShapes, maxMain)
    }
  }

  // ===== milestone2 파서 데이터 파일 캐시 (milestone2-test-cache 규약과 동일) =====

  private fun milestoneDataCached(grammarName: String, grammar: NGrammar): Pair<MilestoneParserData, String> {
    val cacheRoot = Path.of("milestone2-test-cache")
    if (!Files.exists(cacheRoot)) Files.createDirectory(cacheRoot)
    val hash = MessageDigest.getInstance("SHA-1")
      .digest(GrammarProtobufConverter.convertNGrammarToProto(grammar).toByteArray())
      .joinToString("") { "%02X".format(it) }
    val path = cacheRoot.resolve("$grammarName-$hash.pb")
    return if (Files.exists(path)) {
      val proto = MilestoneParserDataProto.Milestone2ParserData.parseFrom(Files.readAllBytes(path))
      Pair(MilestoneParser2ProtobufConverter.fromProto(proto), "(cached)")
    } else {
      var data: MilestoneParserData? = null
      val genMs = timeMs { data = MilestoneParserGen(grammar).parserData() }
      Files.write(path, MilestoneParser2ProtobufConverter.toProto(data!!).toByteArray())
      Pair(data!!, fmtMs(genMs))
    }
  }

  // ===== 측정 루프 =====

  private data class Row(
    val parser: String, val label: String, val chars: Int,
    val medianMs: Double, val capped: Boolean, val liveMax: Int?, val auxMax: Int?,
  )

  private fun runSuite(
    suite: String,
    adapters: List<ParserAdapter>,
    inputs: List<Pair<String, String>>, // (label, input)
  ): List<Row> {
    val rows = mutableListOf<Row>()
    for (adapter in adapters) {
      var skipRest = false
      for ((label, input) in inputs) {
        if (skipRest) {
          println("[$suite] ${adapter.name} $label: SKIPPED (previous size capped)")
          continue
        }
        val warmup = timeMs { adapter.parseOnce(input) }
        val row = if (warmup > capMs) {
          // capped 크기는 acceptance 검증 생략 (전체 파스 1회 추가 비용) —
          // 파서 정합성은 카탈로그/parity 테스트가 담당.
          skipRest = true
          Row(adapter.name, label, input.length, warmup, capped = true, liveMax = null, auxMax = null)
        } else {
          val ok = adapter.accepts(input)
          check(ok) { "[$suite] ${adapter.name} did not accept input $label (${input.length} chars)" }
          val runs = (1..3).map { timeMs { adapter.parseOnce(input) } }.sorted()
          val (liveMax, auxMax) = adapter.liveProfile(input)
          Row(adapter.name, label, input.length, runs[1], capped = false, liveMax = liveMax, auxMax = auxMax)
        }
        rows += row
        val liveStr = row.liveMax?.let { "liveMax=$it" + (row.auxMax?.takeIf { a -> a > 0 }?.let { a -> " (main=$a)" } ?: "") } ?: ""
        println(
          "[$suite] ${adapter.name} $label (${input.length} chars): ${fmtMs(row.medianMs)}" +
            (if (row.capped) " (capped, single run)" else " (median of 3)") + " $liveStr"
        )
      }
    }
    return rows
  }

  private fun printCsv(suite: String, rows: List<Row>) {
    println("CSV suite,parser,label,chars,medianMs,capped,liveMax,auxMax")
    rows.forEach { r ->
      println("CSV $suite,${r.parser},${r.label},${r.chars},%.2f,${r.capped},${r.liveMax ?: ""},${r.auxMax ?: ""}".format(r.medianMs))
    }
  }

  // ===== 입력 생성기 =====

  private fun jsonInput(n: Int): String = buildString {
    append('[')
    for (i in 0 until n) {
      if (i > 0) append(',')
      append("""{"id":$i,"name":"item$i","ok":true,"score":${i % 100}.5}""")
    }
    append(']')
  }

  private fun proto3Input(n: Int): String = buildString {
    append("syntax = \"proto3\";\n")
    for (i in 0 until n) {
      append("message M$i { int32 a$i = 1; string b$i = 2; repeated M${maxOf(0, i - 1)} c$i = 3; }\n")
    }
  }

  // ===== 스위트 =====

  @Test
  fun ablation() {
    val suites = (System.getenv("ABLATION_SUITES") ?: "json,proto3").split(",").map { it.trim() }
    println("=== Ablation benchmark: naive2 vs milestone2 vs mgroup3 ===")
    println("cap=${fmtMs(capMs)}, suites=$suites")

    if ("json" in suites) runGrammarSuite(
      suite = "json",
      cdgPath = Path.of("examples/metalang3/resources/json/grammar.cdg"),
      start = "json",
      inputs = listOf(3, 30, 300, 3000).map { n -> Pair("n$n", jsonInput(n)) },
    )

    if ("proto3" in suites) runGrammarSuite(
      suite = "proto3",
      cdgPath = Path.of("examples/metalang3/resources/proto3/grammar.cdg"),
      start = "proto3",
      inputs = listOf(1, 10, 100, 600).map { n -> Pair("n$n", proto3Input(n)) },
    )

    if ("mulang" in suites) runMulangSuite()
  }

  private fun runGrammarSuite(suite: String, cdgPath: Path, start: String, inputs: List<Pair<String, String>>) {
    println()
    println("--- suite: $suite ---")
    val grammar = analyze(cdgPath.readText(), start)

    var m2Data: MilestoneParserData? = null
    val m2GenMs = timeMs { m2Data = MilestoneParserGen(grammar).parserData() }
    var m3Data: Mgroup3ParserData? = null
    val m3GenMs = timeMs { m3Data = Mgroup3ParserGenerator(grammar).generate() }
    println("[$suite] gen: milestone2 ${fmtMs(m2GenMs)}, mgroup3 ${fmtMs(m3GenMs)} (table ${m3Data!!.serializedSize} bytes)")

    // mgroup3 → milestone2 → naive2 순: 판정이 검증된 파서가 먼저 입력 유효성을 확인.
    val rows = runSuite(
      suite,
      listOf(Mgroup3Adapter(m3Data!!), MilestoneAdapter(grammar, m2Data!!), NaiveAdapter(grammar)),
      inputs,
    )
    printCsv(suite, rows)
  }

  private fun runMulangSuite() {
    println()
    println("--- suite: mulang ---")
    // 논문 재현용 override: MULANG_PINNED_CDG 가 있으면 그 문법으로 측정한다
    // (GroupInventoryStatsTest / GrammarStatsTest 와 같은 규약). 미설정 시 기존
    // 동작 유지 — 툴체인 체크아웃의 현재 문법. 논문 §7.4 의 Mulang 수치는 pinned
    // 문법 기준이므로, 문법이 진화한 뒤에는 override 없이는 재현되지 않는다.
    val cdgPath = Path.of(System.getenv("MULANG_PINNED_CDG") ?: "../mulang/grammar/mulang.cdg")
    if (!cdgPath.exists()) {
      println("[mulang] SKIPPED: ${cdgPath.toAbsolutePath()} not found")
      return
    }
    println("[mulang] grammar: ${cdgPath.toAbsolutePath()}")
    val inputs = listOf("class.mu", "ccgen.mu").mapNotNull { name ->
      val p = Path.of("../mulang/examples/$name")
      if (p.exists()) Pair(name, p.readText()) else {
        println("[mulang] input $name not found, skipping"); null
      }
    }
    val grammar = analyze(cdgPath.readText(), "CompileUnit")

    // milestone2: 파일 캐시 (fresh 생성은 수 분 소요 — 최초 1회만)
    val (m2Data, m2GenNote) = milestoneDataCached("ablation-mulang", grammar)
    println("[mulang] milestone2 gen: $m2GenNote")

    // mgroup3: fixture data.pb 재사용 (runMgroup3FixtureGen 산출물); 없으면 fresh 생성.
    val fixturePath = Path.of("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    val m3Data: Mgroup3ParserData
    if (fixturePath.exists()) {
      m3Data = BufferedInputStream(FileInputStream(fixturePath.toFile())).use { Mgroup3ParserData.parseFrom(it) }
      println("[mulang] mgroup3 data: (fixture ${Files.size(fixturePath)} bytes)")
    } else {
      var d: Mgroup3ParserData? = null
      val ms = timeMs { d = Mgroup3ParserGenerator(grammar).generate() }
      m3Data = d!!
      println("[mulang] mgroup3 gen: ${fmtMs(ms)}")
    }

    val adapters = mutableListOf<ParserAdapter>()
    if (System.getenv("ABLATION_NAIVE_MULANG") == "1") adapters += NaiveAdapter(grammar)
    adapters += MilestoneAdapter(grammar, m2Data)
    adapters += Mgroup3Adapter(m3Data)

    val rows = runSuite("mulang", adapters, inputs)
    printCsv("mulang", rows)
  }
}
