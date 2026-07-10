package com.giyeok.jparser.mgroup4

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * mgroup4 Phase G-b0 — State 캐노니컬라이즈 → 정확 키 id 브리지 검증
 * (mgroup4/docs/phase_g_design.md §4 G-b0).
 *
 * 검증 3종:
 *  (i)  같은 구조가 gen 무관하게 같은 id (canonicalizationUnitCheck 의 정확-키 판 —
 *       조건 템플릿까지 gen 평행이동 불변).
 *  (ii) n=1 에서 State ↔ tipGroupId 함수성 (각 State 가 정확히 하나의 tipGroup 에서).
 *  (iii) 관찰 distinct State 수가 G0 v2 수치와 정합 (실코퍼스 — 항상 실행하는 asdl 로
 *       G0SuffixSetStats(withCond) 와 나란히 재고 비율 보고; 대형은 MG4_G0_STATS 게이트).
 *
 * (i)/(ii) 는 항상 실행 (합성 + asdl 인프로세스). (iii) 대형 코퍼스는 MG4_G0_STATS=1.
 */
class G0StateBridgeTest {
  // 합성 chain 을 만든다 (G0StaticProbe.canonicalizationUnitCheck 와 같은 shape 구성).
  // n=3: window = tip-most 2 milestone 노드. prefix = 그 아래.
  private val obs = emptyList<Int>()
  private fun shape(prefixGen: Int, k2: Kernel, g2: Int, k1: Kernel, g1: Int, tip: Int): PathShape {
    val prefix = MilestonePath(gen = prefixGen, milestone = Kernel(10, 1, prefixGen - 1), parent = null,
      observingCondSymbolIds = obs, reportGen = prefixGen, milestoneReportGen = prefixGen)
    val n2 = MilestonePath(gen = g2, milestone = k2, parent = prefix,
      observingCondSymbolIds = obs, reportGen = g2, milestoneReportGen = g2)
    val n1 = MilestonePath(gen = g1, milestone = k1, parent = n2,
      observingCondSymbolIds = obs, reportGen = g1, milestoneReportGen = g1)
    return PathShape(n1, tip)
  }

  // (i) — 정확 키의 gen 불변성 (구조·조건 공히). CanonKey 는 지문이 아닌 정확 키이므로
  // 같은 구조는 gen 평행이동해도 같은 id 여야 하고, 다른 구조는 반드시 다른 id 여야 한다.
  @Test
  fun canonKeyGenInvariance() {
    val bridge = G0StateBridge(3)
    // obs1: fork 형제 a/c 가 gen 2 에 부착, 공통 b 가 gen 3 에 부착 (curGen=3).
    bridge.observe(linkedMapOf(
      shape(1, Kernel(20, 1, 2), 2, Kernel(30, 1, 3), 3, 9) to Always,
      shape(1, Kernel(21, 1, 2), 2, Kernel(30, 1, 3), 3, 9) to Always,
    ), 3)
    assertEquals(1, bridge.distinctStates) { "single bucket → single State" }
    // obs2: 같은 구조를 절대 gen +10 평행이동, curGen 도 +10 — 같은 State 로 접혀야 (gen-무관).
    bridge.observe(linkedMapOf(
      shape(11, Kernel(20, 1, 12), 12, Kernel(30, 1, 13), 13, 9) to Always,
      shape(11, Kernel(21, 1, 12), 12, Kernel(30, 1, 13), 13, 9) to Always,
    ), 13)
    assertEquals(1, bridge.distinctStates) { "State must be gen-invariant (parallel shift)" }
    // obs3: window 튜플이 다른 구조 (다른 tip milestone) — 새 State.
    bridge.observe(linkedMapOf(
      shape(1, Kernel(20, 1, 2), 2, Kernel(31, 1, 3), 3, 9) to Always,
      shape(1, Kernel(21, 1, 2), 2, Kernel(31, 1, 3), 3, 9) to Always,
    ), 3)
    assertEquals(2, bridge.distinctStates) { "different window tuple → new State" }
    println("[G-b0-BRIDGE] canonKeyGenInvariance OK — exact key is gen-invariant, structure-sensitive")
  }

  // (i-cond) — 조건 템플릿의 gen 불변성 (R4). 같은 조건 클래스를 gen 평행이동하면 같은
  // State, 다른 조건 클래스는 다른 State. 조건 전체 템플릿이 절대 gen 이 아니라 curGen 기준
  // 상대 오프셋임을 강제 (H5 "동일 condition 병합" 동형).
  @Test
  fun condTemplateGenInvariance() {
    val bridge = G0StateBridge(1) // n=1: State 정체성 = (tipGroupId, 조건 템플릿).
    // n=1 shape: window 노드 없음, tipGroupId 만. root 직속 tip.
    fun tipShape(tip: Int): PathShape = PathShape(null, tip)
    // c1: Unless(sym=5, start=2, end=4) @ curGen=4 → 오프셋 (start off=2, end off=0).
    bridge.observe(linkedMapOf(tipShape(9) to Unless(5, 2, 4)), 4)
    assertEquals(1, bridge.distinctStates)
    // c2: 같은 조건 span 을 gen +10 평행이동, curGen +10 → 같은 오프셋 → 같은 State.
    bridge.observe(linkedMapOf(tipShape(9) to Unless(5, 12, 14)), 14)
    assertEquals(1, bridge.distinctStates) { "cond template must be gen-invariant" }
    // c3: 다른 조건 span 오프셋 (start off=3 대신 2) — 다른 State.
    bridge.observe(linkedMapOf(tipShape(9) to Unless(5, 1, 4)), 4)
    assertEquals(2, bridge.distinctStates) { "different relative span → new State" }
    // c4: 다른 조건 종류 (OnlyIf 대신 Unless) — 다른 State.
    bridge.observe(linkedMapOf(tipShape(9) to OnlyIf(5, 2, 4)), 4)
    assertEquals(3, bridge.distinctStates) { "different cond kind → new State" }
    println("[G-b0-BRIDGE] condTemplateGenInvariance OK — R4 full cond template, gen-invariant")
  }

  // asdl 문법 — 인프로세스 생성 (대형 pb 불필요). (ii) n=1 함수성 + (iii) G0 대비 정합.
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

  private val asdlInputs = listOf(
    "module M { foo = Bar(int x) | Baz }",
    "module M { a = (x y) }",
    "module Mod { foo = Bar(int x, str y) | Baz | Qux\n  attributes (int z) }",
    "module M {\n  -- comment here\n  foo = Bar(int x)\n}",
  )

  // (ii) — n=1 에서 State ↔ tipGroupId 함수성. 각 State 가 정확히 하나의 tipGroup 에서
  // 왔는지 (조건으로 1:다는 허용). n=1 이면 window 노드가 없어 State 정체성 = (tipGroupId,
  // 조건 템플릿) 이므로 한 State 는 한 tipGroupId 만 가진다.
  @Test
  fun n1StateToTipFunctional() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val data = Mgroup3ParserGenerator(analysis.ngrammar()).generate()
    for ((i, inp) in asdlInputs.withIndex()) {
      val bridge = G0StateBridge(1)
      val parser = Mgroup4Parser(data, interiorGroupMaxDepth = 1).setStateBridge(bridge)
      parser.parse(inp)
      val (violations, msg) = bridge.checkN1StateToTipFunctional()
      assertEquals(0, violations) { "asdl[$i] n=1 State->tipGroup not functional: $msg" }
      println("[G-b0-BRIDGE] asdl[$i] n=1 functional OK (states=${bridge.distinctStates})")
    }
  }

  // (iii) — 관찰 distinct State 수가 G0SuffixSetStats(withCond) 수치와 정합. G-b0 은 조건
  // 전체 템플릿(정밀)이라 G0 의 구조클래스 근사(withCond)보다 다소 클 수 있다 (설계 §4 (iii)).
  // asdl 로 브리지 수치를 보고 (게이트가 아니라 관찰 — G0 프로브 수치와 육안 대조).
  @Test
  fun bridgeStateCountReport() {
    val analysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(asdlGrammar, "Defs")
    val data = Mgroup3ParserGenerator(analysis.ngrammar()).generate()
    for (n in listOf(1, 2)) {
      for ((i, inp) in asdlInputs.withIndex()) {
        val bridge = G0StateBridge(n)
        val parser = Mgroup4Parser(data, interiorGroupMaxDepth = n).setStateBridge(bridge)
        parser.parse(inp)
        println("[G-b0-BRIDGE] ${bridge.report("asdl[$i]", parser.milestoneGroupCount).trim()}")
      }
    }
  }

  // --- 대형 코퍼스 (mulang/es5) — MG4_G0_STATS 게이트. 브리지 distinct State 수를
  // G0 프로브 수치 (별도 실행) 와 육안 대조하기 위한 보고. ---
  private fun mulangData(): Mgroup3ParserData {
    val pb = File("mgroup3-native/tests/fixtures/parser_generated/mulang/data.pb")
    check(pb.exists()) { "mulang parserdata not found: ${pb.absolutePath}" }
    return Mgroup3ParserData.parseFrom(pb.readBytes())
  }

  private fun firstExisting(vararg paths: String): String? = paths.firstOrNull { Path(it).exists() }

  @EnabledIfEnvironmentVariable(named = "MG4_G0_STATS", matches = "1")
  @Test
  fun es5Json2BridgeReport() {
    val pb = File(
      "/private/tmp/claude-501/-Users-joonsoo-Documents-workspace-jparser/" +
        "24b37725-2f65-49f2-9454-d72679e67d3f/scratchpad/mgroup4-phase0/es5-mg3.pb"
    )
    check(pb.exists()) { "es5 parserdata not found: ${pb.absolutePath}" }
    val data = Mgroup3ParserData.parseFrom(pb.readBytes())
    val json2 = File("es5-corpus/json2.js")
    check(json2.exists()) { "json2.js not found: ${json2.absolutePath}" }
    val src = json2.readText()
    for (n in listOf(1, 2)) {
      val bridge = G0StateBridge(n)
      val parser = Mgroup4Parser(data, interiorGroupMaxDepth = n).setStateBridge(bridge)
      parser.parse(src)
      println("[G-b0-BRIDGE] ${bridge.report("es5/json2", parser.milestoneGroupCount).trim()}")
      if (n == 1) {
        val (v, msg) = bridge.checkN1StateToTipFunctional()
        assertTrue(v == 0) { "es5/json2 n=1 State->tipGroup not functional: $msg" }
      }
    }
  }
}
