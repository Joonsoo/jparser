package com.giyeok.jparser.mgroup4

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * mgroup4 Phase G0 — suffix-set 상태 공간 동적 하한 프로브 (phase_g_plan.md §2 G0).
 *
 * 각 대상 (문법·입력) 을 n∈{2,3,4} 로 파싱하면서 [G0SuffixSetStats] 로 매 gen main root
 * live shape 를 캐노니컬라이즈해 distinct suffix-set 상태 수를 잰다 (조건 무시/포함 두 모드).
 * 이 프로브가 Phase G 착수/중단(킬 게이트 1)을 가른다.
 *
 * 게이트: env MG4_G0_STATS=1 로만 실행 (기본 스위트 무영향). runMgroup4DiffTest 는 이
 * 훅이 출력에 영향 없음을 별도로 확인 (mg4G0StatsEnabled 미설정 시 hook 미호출).
 *
 * 파스는 병합(runtime packing) 켜진 상태(interiorGroupMaxDepth=n) 로 돌리되, G0SuffixSetStats
 * 가 group 을 멤버로 펼쳐 반영하므로 관찰이 왜곡되지 않는다.
 */
class G0StaticProbe {
  // n=1 은 방법론 검증용 (suffix = tipGroupId 만 = 현행 milestone group 동형 → 대비
   //  배수가 ~1x 근방이어야 캐노니컬라이제이션이 올바름). MG4_G0_N1=1 일 때만 포함.
  private val nsToProbe: List<Int> =
    if (System.getenv("MG4_G0_N1") == "1") listOf(1, 2, 3, 4) else listOf(2, 3, 4)

  private fun probeOne(label: String, data: Mgroup3ParserData, input: String) {
    for (n in nsToProbe) {
      val stats = G0SuffixSetStats(n, expectedTotalGens = input.length)
      val parser = Mgroup4Parser(data, interiorGroupMaxDepth = n).setG0Stats(stats)
      val ctx = parser.parse(input)
      val accepted = parser.isAccepted(ctx)
      println(stats.report("$label (accepted=$accepted, ${input.length} chars)", parser.milestoneGroupCount))
    }
  }

  // 캐노니컬라이제이션 유닛 검증 (합성 chain) — 실코퍼스에서 B==A 관찰이 수집기 버그가
  // 아님을 못박는다: A 는 gen 무시, B 는 gen 동등/순서 패턴을 실제로 구분해야 한다.
  //  - obs1/obs2: 같은 구조, 절대 gen 만 평행이동 (동등/순서 패턴 동일) → A 1개, B 1개.
  //  - obs3: 같은 구조, fork 두 tuple 의 부착 gen 이 같아짐 (패턴 상이) → A 그대로 1개,
  //    B 는 2개로 갈라져야 한다.
  @Test
  fun canonicalizationUnitCheck() {
    val obs = emptyList<Int>()
    // n=3: window = tip-most 2 milestone 노드. prefix = 그 아래.
    fun shape(prefixGen: Int, k2: Kernel, g2: Int, k1: Kernel, g1: Int, tip: Int): PathShape {
      val prefix = MilestonePath(gen = prefixGen, milestone = Kernel(10, 1, prefixGen - 1), parent = null,
        observingCondSymbolIds = obs, reportGen = prefixGen, milestoneReportGen = prefixGen)
      val n2 = MilestonePath(gen = g2, milestone = k2, parent = prefix,
        observingCondSymbolIds = obs, reportGen = g2, milestoneReportGen = g2)
      val n1 = MilestonePath(gen = g1, milestone = k1, parent = n2,
        observingCondSymbolIds = obs, reportGen = g1, milestoneReportGen = g1)
      return PathShape(n1, tip)
    }
    val stats = G0SuffixSetStats(3)
    // obs1: fork 형제 a/c 가 gen 2 에 부착, 공통 b 가 gen 3 에 부착.
    stats.observe(linkedMapOf(
      shape(1, Kernel(20, 1, 2), 2, Kernel(30, 1, 3), 3, 9) to Always,
      shape(1, Kernel(21, 1, 2), 2, Kernel(30, 1, 3), 3, 9) to Always,
    ), 3)
    org.junit.jupiter.api.Assertions.assertEquals(1, stats.distinctStatesANoCond)
    org.junit.jupiter.api.Assertions.assertEquals(1, stats.distinctStatesBNoCond)
    // obs2: 같은 구조를 절대 gen +10 평행이동 — A/B 모두 같은 상태로 접혀야 (gen-무관).
    stats.observe(linkedMapOf(
      shape(11, Kernel(20, 1, 12), 12, Kernel(30, 1, 13), 13, 9) to Always,
      shape(11, Kernel(21, 1, 12), 12, Kernel(30, 1, 13), 13, 9) to Always,
    ), 13)
    org.junit.jupiter.api.Assertions.assertEquals(1, stats.distinctStatesANoCond) { "A must be gen-invariant" }
    org.junit.jupiter.api.Assertions.assertEquals(1, stats.distinctStatesBNoCond) { "B must be invariant to order-preserving gen shift" }
    // obs3: 같은 구조인데 b 의 부착 gen 이 a/c 와 같음 (동등 패턴 상이: {2,2,3,3} 이 아니라
    // 전부 같은 gen) → A 는 여전히 1개 (gen 무시), B 는 새 상태.
    stats.observe(linkedMapOf(
      shape(1, Kernel(20, 1, 2), 2, Kernel(30, 1, 2), 2, 9) to Always,
      shape(1, Kernel(21, 1, 2), 2, Kernel(30, 1, 2), 2, 9) to Always,
    ), 3)
    org.junit.jupiter.api.Assertions.assertEquals(1, stats.distinctStatesANoCond) { "A must ignore gen pattern" }
    org.junit.jupiter.api.Assertions.assertEquals(2, stats.distinctStatesBNoCond) { "B must distinguish gen-equality patterns" }
    println("[G0-STATS] canonicalizationUnitCheck OK — A gen-invariant, B distinguishes gen patterns")
  }

  // --- asdl (인프로세스 생성, 4 입력) — 항상 실행 (대형 pb 불필요) ---
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

  @EnabledIfEnvironmentVariable(named = "MG4_G0_STATS", matches = "1")
  @Test
  fun asdlG0Probe() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val data = Mgroup3ParserGenerator(analysis.ngrammar()).generate()
    val inputs = listOf(
      "module M { foo = Bar(int x) | Baz }",
      "module M { a = (x y) }",
      "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
      "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
    )
    for ((i, inp) in inputs.withIndex()) probeOne("asdl[$i]", data, inp)
  }

  // --- mulang (chain_boundaries.mu + ccgen.mu), es5 (json2.js + jquery) — 대형 pb ---
  private fun mulangData(): Mgroup3ParserData {
    val pb = File("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    check(pb.exists()) { "mulang parserdata not found: ${pb.absolutePath}" }
    return Mgroup3ParserData.parseFrom(pb.readBytes())
  }

  private fun firstExisting(vararg paths: String): String? = paths.firstOrNull { Path(it).exists() }

  @EnabledIfEnvironmentVariable(named = "MG4_G0_STATS", matches = "1")
  @Test
  fun mulangG0Probe() {
    val data = mulangData()
    val chain = firstExisting(
      "../mulang/examples/chain_boundaries.mu",
      "../../mulang/examples/chain_boundaries.mu",
      "/Users/joonsoo/Documents/workspace/mulang/examples/chain_boundaries.mu",
    )
    check(chain != null) { "chain_boundaries.mu not found" }
    probeOne("mulang/chain_boundaries", data, Path(chain).readText())

    val ccgen = firstExisting(
      "../mulang/examples/ccgen.mu",
      "../../mulang/examples/ccgen.mu",
      "/Users/joonsoo/Documents/workspace/mulang/examples/ccgen.mu",
    )
    check(ccgen != null) { "ccgen.mu not found" }
    probeOne("mulang/ccgen", data, Path(ccgen).readText())
  }

  private fun es5Data(): Mgroup3ParserData {
    val pb = File(
      "/private/tmp/claude-501/-Users-joonsoo-Documents-workspace-jparser/" +
        "24b37725-2f65-49f2-9454-d72679e67d3f/scratchpad/mgroup4-phase0/es5-mg3.pb"
    )
    check(pb.exists()) { "es5 parserdata not found: ${pb.absolutePath}" }
    return Mgroup3ParserData.parseFrom(pb.readBytes())
  }

  @EnabledIfEnvironmentVariable(named = "MG4_G0_STATS", matches = "1")
  @Test
  fun es5Json2G0Probe() {
    val data = es5Data()
    val json2 = File("es5-corpus/json2.js")
    check(json2.exists()) { "json2.js not found: ${json2.absolutePath}" }
    probeOne("es5/json2", data, json2.readText())
  }

  // jquery 는 메모리 부하가 커 별도 메서드 (json2 결과가 jquery OOM 에 안 묻히게).
  @EnabledIfEnvironmentVariable(named = "MG4_G0_STATS", matches = "1")
  @Test
  fun es5JqueryG0Probe() {
    val data = es5Data()
    val jquery = File("es5-corpus/jquery-1.12.4.js")
    check(jquery.exists()) { "jquery.js not found: ${jquery.absolutePath}" }
    probeOne("es5/jquery", data, jquery.readText())
  }
}
