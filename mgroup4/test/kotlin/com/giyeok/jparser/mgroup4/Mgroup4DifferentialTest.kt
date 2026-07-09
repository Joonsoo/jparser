package com.giyeok.jparser.mgroup4

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.Mgroup3Parser
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * mgroup4 차등 오라클 (상위 세션 수정 지시 4 — 별도 모듈 재구성판).
 *
 * mgroup4 가 별도 모듈이 되면서 게이트는 **두 독립 파서 클래스의 차등**으로 강화된다:
 *
 *  - G1 — mgroup4(n=1) ≡ mgroup3: 같은 parserdata·입력에서 [Mgroup4Parser] (n=1) 와
 *    [Mgroup3Parser] 의 kernelsHistory·accept 를 직접 비교. 두 파서는 서로 다른
 *    패키지의 독립 클래스이므로, in-place 수정 때의 "n=1 이 현행 경로" 비트동일보다
 *    강한 검증이다 (fork 가 로직을 바꾸지 않았음을 증명).
 *  - G2 — mgroup4(n∈{2,4,6}) ≡ mgroup4(n=1): interior group 병합이 내부 표현일
 *    뿐 출력 불변임을 검증 (기존 A/B 로직 유지).
 *
 * kernelsHistory 는 양쪽 다 List<ktlib.KernelSet> — ktlib 공유 타입이라 직접 비교 가능.
 *
 * 커버리지:
 *  - 인프로세스 생성 문법 셋 (structuralCorpus) — ParserFixtureGenTest 의 inline corpus
 *    에서 구조 다양성 기준으로 선정한 대표 문법 + 각 문법의 accept/reject 입력.
 *  - asdl 문법 (4입력) — 조건/longest-match/중첩 규칙 (Mgroup2VsMgroup3HistoryTest 재사용).
 *  - mulang chain_boundaries.mu (MG4_DIFF=1, parserdata = mulang fixture data.pb).
 *  - es5 json2.js (MG4_DIFF=1, parserdata = scratchpad/mgroup4-phase0/es5-mg3.pb).
 *
 * 인프로세스 문법 케이스는 grammar 를 프로세스 내 생성하므로 항상 실행. mulang/es5 는
 * 대형 parserdata 가 필요해 env var MG4_DIFF=1 게이트 (기본 스위트에서는 인프로세스만).
 *
 * MG4_SHAPE_STATS=1 로 실행하면 병합률/거부사유 카운터도 출력 (A1 측정 보고용).
 */
class Mgroup4DifferentialTest {
  private val ns = listOf(2, 4, 6)

  // 한 parserdata·입력에서 G1(mgroup4 n=1 ≡ mgroup3) 과 G2(mgroup4 n∈ns ≡ n=1) 를
  // 모두 검증. accept 결과와 kernelsHistory 가 전부 동일해야 통과.
  private fun assertHistoryInvariant(label: String, data: Mgroup3ParserData, input: String) {
    // === G1: mgroup4(n=1) vs mgroup3 (두 독립 클래스) ===
    val mg3 = Mgroup3Parser(data)
    val mg3Ctx = mg3.parse(input)
    val mg3Accepted = mg3.isAccepted(mg3Ctx)
    val mg3Hist = mg3.kernelsHistory(mg3Ctx)

    val base = Mgroup4Parser(data, interiorGroupMaxDepth = 1)
    val baseCtx = base.parse(input)
    val baseAccepted = base.isAccepted(baseCtx)
    val baseHist = base.kernelsHistory(baseCtx)

    assertEquals(mg3Accepted, baseAccepted) { "$label G1: mgroup4(n=1) accept differs from mgroup3 (mg3=$mg3Accepted)" }
    assertEquals(mg3Hist.size, baseHist.size) { "$label G1: mgroup4(n=1) history size differs from mgroup3" }
    run {
      var firstDiff = -1
      for (g in mg3Hist.indices) {
        if (mg3Hist[g].kernels != baseHist[g].kernels) { firstDiff = g; break }
      }
      if (firstDiff >= 0) {
        val onlyMg3 = (mg3Hist[firstDiff].kernels - baseHist[firstDiff].kernels).take(8)
        val onlyMg4 = (baseHist[firstDiff].kernels - mg3Hist[firstDiff].kernels).take(8)
        System.err.println("$label G1 first history diff at gen $firstDiff")
        System.err.println("  only mgroup3: $onlyMg3")
        System.err.println("  only mgroup4(n=1): $onlyMg4")
      }
      assertEquals(-1, firstDiff) { "$label G1: mgroup4(n=1) kernelsHistory diverges from mgroup3 at gen $firstDiff" }
    }

    // === G2: mgroup4(n∈ns) vs mgroup4(n=1) ===
    for (n in ns) {
      val p = Mgroup4Parser(data, interiorGroupMaxDepth = n)
      if (mg4ShapeStatsEnabled) p.resetMg4Stats()
      val ctx = p.parse(input)
      val accepted = p.isAccepted(ctx)
      val hist = p.kernelsHistory(ctx)
      assertEquals(baseAccepted, accepted) { "$label G2 n=$n: accept differs (base=$baseAccepted)" }
      assertEquals(baseHist.size, hist.size) { "$label G2 n=$n: history size differs" }
      var firstDiff = -1
      for (g in baseHist.indices) {
        if (baseHist[g].kernels != hist[g].kernels) { firstDiff = g; break }
      }
      if (firstDiff >= 0) {
        val onlyBase = (baseHist[firstDiff].kernels - hist[firstDiff].kernels).take(8)
        val onlyN = (hist[firstDiff].kernels - baseHist[firstDiff].kernels).take(8)
        System.err.println("$label G2 n=$n first history diff at gen $firstDiff")
        System.err.println("  only n=1: $onlyBase")
        System.err.println("  only n=$n: $onlyN")
      }
      assertEquals(-1, firstDiff) { "$label G2 n=$n: kernelsHistory diverges at gen $firstDiff" }
      if (mg4ShapeStatsEnabled) println("[MG4-STATS] $label ${p.reportMg4Stats()}")
    }
    println("[MG4-DIFF] $label OK — G1(mgroup4 n=1 ≡ mgroup3) + G2(n=1 vs ${ns}) byte-identical (${input.length} chars, accepted=$baseAccepted)")
  }

  // 인프로세스 생성 문법 케이스 — grammar + startName + accept 입력들.
  private data class GrammarCase(val label: String, val grammar: String, val start: String, val inputs: List<String>)

  // ParserFixtureGenTest 의 inline corpus 에서 구조 다양성 기준으로 선정한 대표 문법.
  // 선정 기준: term/range, sequence(짧/긴), choice(2/다), repeat(0/1), optional,
  // sequence+repeat, nested choice, nested repeat — 파서 fork/reduce 구조가 서로 다른
  // 대표 12개. accept 입력만 (kernelsHistory 는 accept 경로에서 가장 풍부; reject 도
  // 넣되 accept 위주로 다양성 확보). asdl 은 별도 (조건/중첩 규칙 커버).
  private val structuralCorpus: List<GrammarCase> = listOf(
    GrammarCase("single_char", "Grammar = 'a'", "Grammar", listOf("a")),
    GrammarCase("char_range", "Grammar = '0-9'", "Grammar", listOf("0", "5", "9")),
    GrammarCase("simple_sequence", "Grammar = 'a' 'b' 'c'", "Grammar", listOf("abc")),
    GrammarCase("longer_sequence", "Grammar = 'a' 'b' 'c' 'd' 'e'", "Grammar", listOf("abcde")),
    GrammarCase("choice", "Grammar = 'a' | 'b'", "Grammar", listOf("a", "b")),
    GrammarCase("multi_choice", "Grammar = 'a' | 'b' | 'c' | 'd'", "Grammar", listOf("a", "b", "c", "d")),
    GrammarCase("repeat0", "Grammar = 'a'*", "Grammar", listOf("", "a", "aa", "aaaaa")),
    GrammarCase("repeat1", "Grammar = 'a'+", "Grammar", listOf("a", "aa", "aaaaa")),
    GrammarCase("optional", "Grammar = 'a'?", "Grammar", listOf("", "a")),
    GrammarCase("sequence_with_repeat", "Grammar = 'a' 'b'* 'c'", "Grammar", listOf("ac", "abc", "abbbc")),
    GrammarCase("nested_choice", "Grammar = ('a' | 'b') ('c' | 'd')", "Grammar", listOf("ac", "ad", "bc", "bd")),
    GrammarCase("nested_repeat", "Grammar = ('a' 'b')+", "Grammar", listOf("ab", "abab", "ababab")),
  )

  @Test
  fun structuralCorpusDifferential() {
    for (case in structuralCorpus) {
      val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(case.grammar, case.start)
      val data = Mgroup3ParserGenerator(analysis.ngrammar()).generate()
      for (inp in case.inputs) {
        assertHistoryInvariant("${case.label}[\"${inp}\"]", data, inp)
      }
    }
  }

  private val asdlGrammar = """
    |Defs = WS ModuleDef WS ${'$'}1
    |ModuleDef = "module"&Tk WS Name WS '{' WS SuperClassDef (WS SuperClassDef)* WS '}' {ModuleDef(name=${'$'}2, defs=[${'$'}6] + ${'$'}7)}
    |SuperClassDef = Name WS '=' WS SuperClassDefBody (WS AttributesDef)?
    |                {SuperClassDef(name=${'$'}0, body=${'$'}4, attrs=${'$'}5)}
    |SuperClassDefBody: SuperClassDefBody = SubClassDef (WS '|' WS SubClassDef)* {SealedClassDefs(subs=[${'$'}0] + ${'$'}1)}
    |  | Params {TupleDef(body=${'$'}0)}
    |AttributesDef = "attributes"&Tk WS Params {Attributes(attrs=${'$'}2)}
    |SubClassDef = Name (WS Params)? {SubClassDef(name=${'$'}0, params=${'$'}1)}
    |Params = '(' WS Param (WS ',' WS Param)* WS ')' {[${'$'}2] + ${'$'}3}
    |Param = Name (WS ('*' {%REPEATED} | '?' {%OPTIONAL}))? WS Name
    |        {Param(typeName=${'$'}0, typeAttr: %TypeAttr = ${'$'}1 ?: %PLAIN, name=${'$'}3)}
    |Name = <'a-zA-Z_'+ {str(${'$'}0)}>
    |Tk = <'a-zA-Z_'+>
    |WS = (' \t' | NEWLINE | LineComment)*
    |LineComment = "--" (.-'\n')* (EOF | '\n')
    |NEWLINE = '\n'
    |EOF = !.
  """.trimMargin()

  @Test
  fun asdlDifferential() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val grammar = analysis.ngrammar()
    val data = Mgroup3ParserGenerator(grammar).generate()
    val inputs = listOf(
      "module M { foo = Bar(int x) | Baz }",
      "module M { a = (x y) }",
      "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
      "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
    )
    for (inp in inputs) assertHistoryInvariant("asdl[${inp.take(20)}]", data, inp)
  }

  // d>=3 merge/explodeShapeFully 유닛 검증 — 실코퍼스도 A2/A3 에서 d>=3 을 발화하지만
  // (json2 n=6 mergesByDepth d3..d6), 재구성의 byte-exact 를 결정론적으로 못박기 위해
  // 합성 chain 으로 직접 확인한다. 두 shape 가 depth 3 에서만 상이하도록 만들고 (같은 tip
  // milestone·같은 prefix), 병합이 그 위치를 group 으로 접고 explodeShapeFully 가 원본
  // 2개를 (reportGen 까지) 정확히 되돌리는지 검사.
  @Test
  fun depth3MergeAndExplodeRoundTrip() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val parser = Mgroup4Parser(Mgroup3ParserGenerator(analysis.ngrammar()).generate(), interiorGroupMaxDepth = 4)
    val obs = emptyList<Int>()
    // 공유 root-side prefix (depth 4): root -> P
    val prefix = MilestonePath(gen = 1, milestone = Kernel(10, 0, 0), parent = null, observingCondSymbolIds = obs, reportGen = 1, milestoneReportGen = 1)
    // depth 3 노드 (상이 위치): 두 멤버 A / B (같은 gen, 같은 observing).
    fun shapeWith(depth3: Kernel, mrep3: Int): PathShape {
      val n3 = MilestonePath(gen = 2, milestone = depth3, parent = prefix, observingCondSymbolIds = obs, reportGen = 2, milestoneReportGen = mrep3)
      // depth 2 노드 (tip milestone) — 두 shape 동일.
      val n2 = MilestonePath(gen = 3, milestone = Kernel(30, 1, 2), parent = n3, observingCondSymbolIds = obs, reportGen = 3, milestoneReportGen = 3)
      return PathShape(n2, tipGroupId = 99)
    }
    val sA = shapeWith(Kernel(20, 1, 1), 11)
    val sB = shapeWith(Kernel(21, 1, 1), 12)
    val input = linkedMapOf<PathShape, AcceptCondition>(sA to Always, sB to Always)

    val merged = parser.mergeInteriorGroups(input, 4)
    // 정확히 하나의 group shape 로 접혀야 (2 -> 1).
    assertEquals(1, merged.size) { "expected single merged group shape, got ${merged.size}" }
    val groupShape = merged.keys.single()
    // group 노드는 depth 3 (tip 에서 2번째 milestone). members = {A,B}.
    val groupNode = groupShape.milestonePath!!.parent!!
    assertEquals(2, groupNode.groupMembers?.size) { "group node must hold 2 members" }
    // explode 가 원본 2개 shape 를 정확히 복원 (equals + reportGen byte-exact).
    val exploded = parser.explodeShapeFully(groupShape).toSet()
    assertEquals(setOf(sA, sB), exploded) { "explode must restore original member shapes" }
    // reportGen 도 byte-exact (equals 는 무시하므로 별도 확인).
    for (es in exploded) {
      val orig = if (es == sA) sA else sB
      val eMid = es.milestonePath!!.parent!!
      val oMid = orig.milestonePath!!.parent!!
      assertEquals(oMid.milestoneReportGen, eMid.milestoneReportGen) { "depth3 milestoneReportGen must round-trip" }
    }
    println("[MG4-DIFF] depth3MergeAndExplodeRoundTrip OK")
  }

  @EnabledIfEnvironmentVariable(named = "MG4_DIFF", matches = "1")
  @Test
  fun mulangChainBoundariesDifferential() {
    val pb = File("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    check(pb.exists()) { "mulang parserdata not found: ${pb.absolutePath}" }
    val data = Mgroup3ParserData.parseFrom(pb.readBytes())
    val inputCandidates = listOf(
      "../mulang/examples/chain_boundaries.mu",
      "../../mulang/examples/chain_boundaries.mu",
    )
    val inputPath = inputCandidates.firstOrNull { Path(it).exists() }
    check(inputPath != null) { "chain_boundaries.mu not found in $inputCandidates" }
    assertHistoryInvariant("mulang/chain_boundaries", data, Path(inputPath).readText())
  }

  @EnabledIfEnvironmentVariable(named = "MG4_DIFF", matches = "1")
  @Test
  fun es5Json2Differential() {
    val pb = File(
      "/private/tmp/claude-501/-Users-joonsoo-Documents-workspace-jparser/" +
        "24b37725-2f65-49f2-9454-d72679e67d3f/scratchpad/mgroup4-phase0/es5-mg3.pb"
    )
    check(pb.exists()) { "es5 parserdata not found: ${pb.absolutePath}" }
    val data = Mgroup3ParserData.parseFrom(pb.readBytes())
    val input = File("es5-corpus/json2.js")
    check(input.exists()) { "json2.js not found: ${input.absolutePath}" }
    assertHistoryInvariant("es5/json2", data, input.readText())
  }

  // A2/A3 거친 wall-clock 신호 (정식 A4 측정 전 신호용) — MG4_WALLCLOCK=1 게이트.
  // json2 파스 시간 n=1 vs n=6, 웜업 후 3회 중앙값. 파스 상태 접힘의 실제 시간 영향
  // 관찰 (정확도보다 신호). A4 정식 측정 주의사항은 최종 보고 참조.
  @EnabledIfEnvironmentVariable(named = "MG4_WALLCLOCK", matches = "1")
  @Test
  fun es5Json2WallClockSignal() {
    val pb = File(
      "/private/tmp/claude-501/-Users-joonsoo-Documents-workspace-jparser/" +
        "24b37725-2f65-49f2-9454-d72679e67d3f/scratchpad/mgroup4-phase0/es5-mg3.pb"
    )
    check(pb.exists()) { "es5 parserdata not found: ${pb.absolutePath}" }
    val data = Mgroup3ParserData.parseFrom(pb.readBytes())
    val src = File("es5-corpus/json2.js").readText()

    fun median(xs: List<Double>): Double = xs.sorted()[xs.size / 2]
    fun timeParse(n: Int): Double {
      val p = Mgroup4Parser(data, interiorGroupMaxDepth = n)
      // 웜업 2회.
      repeat(2) { check(p.isAccepted(p.parse(src))) }
      val runs = (1..3).map {
        val t0 = System.nanoTime()
        val ctx = p.parse(src)
        val ok = p.isAccepted(ctx)
        val ms = (System.nanoTime() - t0) / 1e6
        check(ok)
        ms
      }
      return median(runs)
    }

    // 별도 JVM 웜업 순서 편향을 줄이려 n=1, n=6 을 두 번 번갈아 재고 각자 중앙값.
    val n1a = timeParse(1); val n6a = timeParse(6)
    val n1b = timeParse(1); val n6b = timeParse(6)
    val n1 = minOf(n1a, n1b); val n6 = minOf(n6a, n6b)
    println("[MG4-WALLCLOCK] json2 (${src.length} chars): n=1 median≈%.1fms, n=6 median≈%.1fms, n6/n1=%.3f"
      .format(n1, n6, n6 / n1))
  }
}
