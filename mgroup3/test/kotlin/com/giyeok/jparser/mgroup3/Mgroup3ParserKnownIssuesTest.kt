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

  // ExceptGrammar4_1 계열 (catalog 차분 잔여 6건 중 하나; mulang 실코퍼스의 json.bbx
  // `jobject(...)` 오거부와 같은 근본 원인 — 2026-07-02 조사로 확정):
  //
  // 증상: except watcher 가 첫 글자에서 즉사하고 (첫 글자가 except body 시작 불가),
  // 입력의 suffix 가 except body 에 매치하면 오거부. 예: Word-Ks (Ks='a'+) 에서
  // "baaaa" — 'b' 로 watcher 즉사 후 fresh fallback 이 같은 key (Ks, 1) 로
  // position 1 부터 소비하는 zombie watcher 를 만들고, 그 "aaaa" finish (end=5) 를
  // same-input 규약 (span 0) 으로 anchoring 된 Unless(Ks, 1, 5) 가 흡수한다.
  // mulang: NameTok = Word-AllKeyword 에서 "jobject" — 'j' 는 키워드 시작 불가 문자,
  // "object" 는 키워드 → 같은 메커니즘으로 오거부. (첫 글자가 키워드 시작 가능하면
  // watcher 가 살아남아 정상 동작 — "aobject" 는 통과.)
  //
  // 근본 원인: cond root key (sym, gen) 의 span 규약이 이원화되어 있음.
  //  - NExcept-over-atomic (NLongest 포함): 스타터 same-input (key gen, span gen-1),
  //    Unless anchor 도 gen (= milestone gen, span+1) — 쌍이 맞음.
  //  - repeat frontier 의 per-char except: Unless anchor 는 span 시작 (MID=ctx.gen),
  //    스타터는 여전히 (sym, gen) = span gen-1 → 애초에 한 칸 어긋나 있고, 실제로는
  //    same-input 시동 실패 시의 fresh fallback (Mgroup3Parser step 3) 이 우연히
  //    올바른 watcher 를 만들어 동작한다 (('a-z'-'a')* 의 'bca' 거부가 이 경로).
  // fallback 을 막으면 후자가 깨지고, 두면 전자가 오염됨 — 국소 수정 불가.
  //
  // 수정 방향 (m2 정합): cond root key 를 span 시작으로 정규화 —
  //  (a) same-input 스타터 key 를 (sym, gen-1) 로 등록 (CondRootStarter 에 flavor
  //      구분 필요: lookahead 용 fresh 스타터는 (sym, gen) 유지),
  //  (b) 생성기의 Unless/OnlyIf/NoLongerMatch anchor 태그를 span 시작으로 — 현재
  //      Prev(CURR)=milestone gen 으로 resolve 되는 자리를 Grand(=milestone startGen)
  //      로 (GenNodeGeneration.Grand 가 정확히 이 용도로 준비되어 있으나 미사용),
  //  (c) Rust 미러 + fixture/golden 재생성.
  // (a) 만 하면 (12,1)-anchored 조건이 빈 key 를 봐서 "aaaaa" 류가 오수락되고,
  // (b) 없이 fallback 만 막으면 repeat except 가 깨짐 — 반드시 세트로.
  // 2026-07-02 cond root key span-정규화로 수정됨 — 회귀 가드로 유지.
  @Test
  fun testExceptWatcherDiesAtFirstChar() {
    val parser = makeParser(
      """
        Grammar = Word-Ks
        Word = <Word_>
        Word_ = 'a-z' 'a-z'*
        Ks = 'a' 'a'*
      """.trimIndent()
    )
    assertAccepts(parser, "abcd")
    assertAccepts(parser, "aaaab")
    assertAccepts(parser, "bbbbb")
    assertRejects(parser, "a")
    assertRejects(parser, "aaaaa")
    assertAccepts(parser, "baaaa") // 오거부되는 케이스
  }
}
