package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup2.`MilestoneGroupParserDataProtobufConverter$`
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// ktparser(mgroup2 Kotlin 런타임)의 D1/D2/FB3 코너 케이스 회귀 테스트.
// Scala MilestoneGroupParserGen으로 parser data를 생성해 proto로 변환한 뒤
// ktparser 런타임으로 파싱한다. (Mgroup3CornerCaseTest와 같은 케이스들;
// MilestoneAcceptConditionKt.reify의 lookahead beginGen 수정 회귀 확인 포함)
class KtParserCornerCaseTest {
  private class TestParser(cdg: String, startName: String) {
    val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, startName).ngrammar()
    val parser: MilestoneGroupParserKt

    init {
      val scalaData = MilestoneGroupParserGen(grammar).parserData()
      val proto = `MilestoneGroupParserDataProtobufConverter$`.`MODULE$`.toProto(scalaData)
      parser = MilestoneGroupParserKt(proto)
    }

    fun accepts(text: String): Boolean {
      val ctx = try {
        parser.parse(text)
      } catch (e: Exception) {
        return false
      }
      val hist = parser.kernelsHistory(ctx)
      return hist.last().kernels.contains(Kernel(grammar.startSymbol(), 1, 0, text.length))
    }
  }

  private fun assertAccepts(parser: TestParser, text: String) {
    assertTrue(parser.accepts(text), "expected '$text' to be accepted")
  }

  private fun assertRejects(parser: TestParser, text: String) {
    assertFalse(parser.accepts(text), "expected '$text' to be rejected")
  }

  @Test
  fun testNullableSharedSequenceD1() {
    val parser = TestParser(
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
  fun testGen0NullableJoin() {
    val parser = TestParser(
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
    val parser = TestParser(
      """
      S = ^A 'x'
      A = # | 'a'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "x")
  }

  @Test
  fun testGen0NullableExcept() {
    val parser = TestParser(
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
    val parser = TestParser(
      """
      S = 'x' ^A
      A = # | 'a'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "x")
  }

  @Test
  fun testMidGenNullableExcept() {
    val parser = TestParser(
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
    val parser = TestParser(
      """
      S = ^P 'a'* B
      P = A 'c'
      A = 'a' A 'b' | 'a' 'b'
      B = 'b' B 'c' | 'b' 'c'
      """.trimIndent(), "S"
    )
    assertAccepts(parser, "abc")
    assertAccepts(parser, "aabbcc")
    assertRejects(parser, "abbcc")
    assertRejects(parser, "aabbc")
  }
}
