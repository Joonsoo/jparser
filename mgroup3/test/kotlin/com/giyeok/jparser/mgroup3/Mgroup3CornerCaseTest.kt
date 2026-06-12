package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// naive2/milestone2/mgroup2(Scala)에서 발견되어 수정된 버그 계열의 mgroup3 회귀 테스트.
// (2026-06-12; 상세는 NaiveParser2CornerCaseTests, MilestoneAcceptCondition.reify 수정 참고)
//
// - D1: 공유 nullable sequence의 늦은 파생 — 파생 순서와 무관하게 수용해야 함
// - D2: gen 0에서 zero-width로 성립하는 nullable join/lookahead/except 조건
// - D2': 입력 중간 세대에서 zero-width로 성립하는 조건 (progress 단계)
// - FB3: 여러 세대에 걸친 lookahead 대상(^P)의 추적
class Mgroup3CornerCaseTest {
  private fun makeParser(cdg: String, startName: String): Mgroup3Parser {
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, startName)
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    return Mgroup3Parser(data)
  }

  private fun assertAccepts(parser: Mgroup3Parser, text: String) {
    val ctx = try {
      parser.parse(text)
    } catch (e: ParsingError) {
      throw AssertionError("expected '$text' to be accepted, but threw $e")
    }
    assertTrue(
      parser.isAccepted(ctx),
      "expected '$text' to be accepted, mainPaths=${ctx.mainPaths.size}, condPaths=${ctx.condPaths.size}"
    )
  }

  private fun assertRejects(parser: Mgroup3Parser, text: String) {
    try {
      val ctx = parser.parse(text)
      assertFalse(parser.isAccepted(ctx), "expected '$text' to be rejected, but it was accepted")
    } catch (e: ParsingError) {
      // 기대된 동작
    }
  }

  @Test
  fun testNullableSharedSequenceD1() {
    val parser = makeParser(
      """
      S = N1 'x' | N2
      N1 = A B
      N2 = A B
      A = # | 'a'
      B = # | 'b'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "x")
  }

  @Test
  fun testNullableSharedSequenceD1Reversed() {
    val parser = makeParser(
      """
      S = N2 | N1 'x'
      N1 = A B
      N2 = A B
      A = # | 'a'
      B = # | 'b'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "x")
  }

  @Test
  fun testGen0NullableJoin() {
    val parser = makeParser(
      """
      S = (A&B) 'x'
      A = # | 'a'
      B = # | 'b'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "x")
  }

  @Test
  fun testGen0NullableLookahead() {
    val parser = makeParser(
      """
      S = ^A 'x'
      A = # | 'a'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "x")
  }

  @Test
  fun testGen0NullableExcept() {
    // A-B에서 B가 ε을 매치하므로 (A-B)는 ε을 매치할 수 없다
    val parser = makeParser(
      """
      S = (A-B) 'x'
      A = # | 'a'
      B = #
      """.trimIndent(), "S"
    )
    assertRejects(parser, "x")
  }

  @Test
  fun testMidGenNullableLookahead() {
    val parser = makeParser(
      """
      S = 'x' ^A
      A = # | 'a'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "x")
  }

  @Test
  fun testMidGenNullableExcept() {
    val parser = makeParser(
      """
      S = 'x' (A-B)
      A = # | 'a'
      B = #
      """.trimIndent(), "S"
    )
    assertRejects(parser, "x")
  }

  @Test
  fun testMultiGenLookaheadTarget() {
    // a^n b^n c^n: ^P의 대상 P가 여러 세대에 걸쳐 매치됨
    val parser = makeParser(
      """
      S = ^P 'a'* B
      P = A 'c'
      A = 'a' A 'b' | 'a' 'b'
      B = 'b' B 'c' | 'b' 'c'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "abc")
    assertAccepts(parser, "aabbcc")
    assertAccepts(parser, "aaabbbccc")
    // main('a'* B)은 매치되지만 ^P가 실패하는 입력
    assertRejects(parser, "abbcc")
    assertRejects(parser, "aabbc")
  }
}
