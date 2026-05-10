package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// 현재 mgroup3 generator/parser에서 아직 정상 동작하지 않는 케이스들.
// 다음과 같은 미완성 영역에 해당:
//  - Recursive grammar에서 reduce chain이 한 input step에서 다 일어나야 하는 경우
//  - Nullable nonterm이 sequence 중간에 있는 경우 (예: `A WS B` 에서 WS가 ' '*)
//  - Lookahead/except/join 등 cond path를 통한 reject 처리
//
// 추후 generator/parser 개선 후 @Disabled 제거.
class Mgroup3ParserKnownIssuesTest {
  private fun makeParser(cdg: String, startName: String = "Grammar"): Mgroup3Parser {
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
    assertTrue(parser.isAccepted(ctx), "expected '$text' to be accepted")
  }

  private fun assertRejects(parser: Mgroup3Parser, text: String) {
    try {
      val ctx = parser.parse(text)
      assertFalse(parser.isAccepted(ctx), "expected '$text' to be rejected")
    } catch (e: ParsingError) {
      // OK
    }
  }

  @Test
  fun testRecursive() {
    val parser = makeParser(
      """
        Grammar = 'a' Grammar
                | 'b'
      """.trimIndent()
    )
    assertAccepts(parser, "b")
    assertAccepts(parser, "ab")
    assertAccepts(parser, "aab")
  }

  @Test
  fun testParens() {
    val parser = makeParser(
      """
        Expr = '(' Expr ')'
             | '0-9'
      """.trimIndent(),
      startName = "Expr",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "(1)")
    assertAccepts(parser, "((1))")
  }

  @Test
  fun testPlainArith() {
    val parser = makeParser(
      """
        Expr = Term '+' Expr
             | Term
        Term = Factor '*' Term
             | Factor
        Factor = '0-9'
      """.trimIndent(),
      startName = "Expr",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "1+2")
    assertAccepts(parser, "1+2+3")
  }

  @Test
  fun testWithWS() {
    // WS = ' '* 같은 nullable nonterm이 sequence 중간에 있는 경우
    val parser = makeParser(
      """
        Expr = Term WS '+' WS Expr
             | Term
        Term = Factor WS '*' WS Term
             | Factor
        Factor = '0-9'
        WS = ' '*
      """.trimIndent(),
      startName = "Expr",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "1+2")
    assertAccepts(parser, "1 + 2")
  }

  @Test
  fun testLookaheadExcept() {
    // 'a' followed by anything except 'b'
    val parser = makeParser(
      """
        Grammar = 'a' !'b' Tail
        Tail = '0-9'
      """.trimIndent()
    )
    assertAccepts(parser, "a1")
    assertRejects(parser, "ab")
  }

  @Test
  fun testExcept() {
    val parser = makeParser("Grammar = 'a-z' - 'a'")
    assertAccepts(parser, "b")
    assertRejects(parser, "a")
  }

  @Test
  fun testRepeatNonterminalInner() {
    val parser = makeParser(
      """
        Grammar = AB*
        AB = 'a' 'b'
      """.trimIndent()
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "ab")
    assertAccepts(parser, "abab")
  }
}
