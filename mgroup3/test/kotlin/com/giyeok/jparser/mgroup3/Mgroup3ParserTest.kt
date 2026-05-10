package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// mgroup3 parser/generator의 단위 테스트.
// 각 grammar 케이스마다 accept/reject를 확인한다.
class Mgroup3ParserTest {
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

  // === 단순 케이스 ===

  @Test
  fun testSingleChar() {
    val parser = makeParser("Grammar = 'a'")
    assertAccepts(parser, "a")
    assertRejects(parser, "b")
    assertRejects(parser, "")
    assertRejects(parser, "aa")
  }

  @Test
  fun testCharRange() {
    val parser = makeParser("Grammar = '0-9'")
    assertAccepts(parser, "0")
    assertAccepts(parser, "5")
    assertAccepts(parser, "9")
    assertRejects(parser, "a")
    assertRejects(parser, "")
  }

  @Test
  fun testEscapedChar() {
    val parser = makeParser("Grammar = '+\\-'")
    assertAccepts(parser, "+")
    assertAccepts(parser, "-")
    assertRejects(parser, "*")
  }

  @Test
  fun testSimpleSequence() {
    val parser = makeParser("Grammar = 'a' 'b' 'c'")
    assertAccepts(parser, "abc")
    assertRejects(parser, "ab")
    assertRejects(parser, "abcd")
    assertRejects(parser, "a")
    assertRejects(parser, "")
  }

  @Test
  fun testLongerSequence() {
    val parser = makeParser("Grammar = 'a' 'b' 'c' 'd' 'e'")
    assertAccepts(parser, "abcde")
    assertRejects(parser, "abcd")
    assertRejects(parser, "abcdef")
  }

  @Test
  fun testChoice() {
    val parser = makeParser("Grammar = 'a' | 'b'")
    assertAccepts(parser, "a")
    assertAccepts(parser, "b")
    assertRejects(parser, "c")
    assertRejects(parser, "ab")
  }

  @Test
  fun testMultiChoice() {
    val parser = makeParser("Grammar = 'a' | 'b' | 'c' | 'd'")
    assertAccepts(parser, "a")
    assertAccepts(parser, "b")
    assertAccepts(parser, "c")
    assertAccepts(parser, "d")
    assertRejects(parser, "e")
  }

  // === Repeat ===

  @Test
  fun testRepeat0() {
    val parser = makeParser("Grammar = 'a'*")
    assertAccepts(parser, "")
    assertAccepts(parser, "a")
    assertAccepts(parser, "aa")
    assertAccepts(parser, "aaaaa")
    assertAccepts(parser, "aaaaaaaaaaaa")
    assertRejects(parser, "ab")
    assertRejects(parser, "b")
  }

  @Test
  fun testRepeat1() {
    val parser = makeParser("Grammar = 'a'+")
    assertRejects(parser, "")
    assertAccepts(parser, "a")
    assertAccepts(parser, "aa")
    assertAccepts(parser, "aaaaa")
    assertRejects(parser, "ab")
  }

  @Test
  fun testRepeatRange() {
    val parser = makeParser("Grammar = '0-9'+")
    assertRejects(parser, "")
    assertAccepts(parser, "0")
    assertAccepts(parser, "1234567890")
    assertRejects(parser, "1a")
  }

  @Test
  fun testRepeatChoiceInner() {
    val parser = makeParser("Grammar = ('a' | 'b')*")
    assertAccepts(parser, "")
    assertAccepts(parser, "a")
    assertAccepts(parser, "b")
    assertAccepts(parser, "ababab")
    assertAccepts(parser, "aaabbb")
    assertRejects(parser, "abc")
  }

  // === Nonterminal ===

  @Test
  fun testNestedNonterminals() {
    val parser = makeParser(
      """
        Grammar = A B
        A = 'a'
        B = 'b'
      """.trimIndent()
    )
    assertAccepts(parser, "ab")
    assertRejects(parser, "ba")
    assertRejects(parser, "a")
    assertRejects(parser, "abc")
  }

  @Test
  fun testNonterminalChain() {
    val parser = makeParser(
      """
        Grammar = A
        A = B
        B = C
        C = 'x'
      """.trimIndent()
    )
    assertAccepts(parser, "x")
    assertRejects(parser, "")
    assertRejects(parser, "y")
  }

  @Test
  fun testNonterminalReuse() {
    val parser = makeParser(
      """
        Grammar = A A A
        A = 'a' | 'b'
      """.trimIndent()
    )
    assertAccepts(parser, "aaa")
    assertAccepts(parser, "abb")
    assertAccepts(parser, "bba")
    assertAccepts(parser, "aab")
    assertRejects(parser, "aa")
    assertRejects(parser, "abc")
  }

  // === Edge cases ===

  @Test
  fun testEmptyMatch() {
    // empty가 가능한 grammar
    val parser = makeParser("Grammar = 'a'*")
    assertAccepts(parser, "")
  }

  @Test
  fun testSingletonAlt() {
    val parser = makeParser(
      """
        Grammar = 'a' 'b' 'c' | 'x' 'y' 'z'
      """.trimIndent()
    )
    assertAccepts(parser, "abc")
    assertAccepts(parser, "xyz")
    assertRejects(parser, "abz")
    assertRejects(parser, "xyc")
  }

  // === Longest ===

  @Test
  fun testLongest() {
    // metalang3에서 longest는 alternation 분기에서 자연스럽게 처리되는 듯.
    // 일단 단순 grammar
    val parser = makeParser(
      """
        Grammar = A 'b'
        A = 'a' | 'a' 'a'
      """.trimIndent()
    )
    assertAccepts(parser, "ab")
    assertAccepts(parser, "aab")
    assertRejects(parser, "b")
  }
}
