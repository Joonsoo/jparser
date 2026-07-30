package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// mgroup3 generator/parser 에서 한때 깨져 있던 케이스들의 회귀 가드.
// 원래는 미수정 영역 (@Disabled) 모음이었고, 고쳐질 때마다 @Disabled 를 떼서
// 가드로 승격시켜 왔다. 현재는 전부 통과한다 (@Disabled 없음):
//  - Recursive grammar 에서 reduce chain 이 한 input step 에 다 일어나야 하는 경우
//  - Nullable nonterm 이 sequence 중간에 있는 경우 (예: `A WS B` 에서 WS 가 ' '*)
//  - Lookahead/except/join 등 cond path 를 통한 reject 처리
//  - except watcher 가 첫 글자에서 즉사하는 경우 (2026-07-02 span-정규화)
//  - 부정 lookahead 가 가려진 본문보다 짧은 경우 = bug B (2026-07-30 lookahead
//    span-정규화 + 누적 finish 기록; mgroup3/docs/watcher_anchor_dedup.md §9)
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

  // ================================================================================
  // "bug B" 회귀 가드: 가려진 본문이 lookahead 본문보다 길 때의 부정 lookahead
  // ================================================================================
  // 2026-07-30 발견/수정. 최소 재현:
  //     Program = !"fn" Expression ';'
  //     Expression = "fn" "()"
  // 입력 "fn();" -> naive2 REJECT (정답). 수정 전에는 milestone2 / mgroup2(Scala) /
  // mgroup2(Kotlin) / mgroup3(Kotlin) / mgroup3-native(Rust) 전부 ACCEPT.
  // `Expression = "fn"` (본문이 lookahead 본문과 같은 길이) 이면 수정 전에도 REJECT.
  // 실문법 영향: examples/metalang3/resources/es5/grammar.cdg 의
  // `ExpressionStatement = !('{' | "function"&Tk) Expression WS ';'` 가 강제되지 않아
  // `{ function f() {}; }` 가 오수락됐다 (지금은 Es5CdgTest 가 직접 거부를 단언한다).
  //
  // 테이블은 원래도 정상이었다 — mid edge ({1,ptr0} -> {12,ptr1}) 의 append 조건이
  // lookahead_notfound{symbol_id:7} 을 담고 있었다. 문제는 *런타임 방출 시점* 이다:
  // 조건부 interior kernel ({3,ptr1}) 은 milestone 이 아니라 그룹 closure 안에 접혀
  // 있어서, 조건은 seq 의 dot 이 그 kernel 을 통과하는 step (= Expression 이 끝나는
  // gen 4) 에 비로소 물질화된다. 그런데 감시 대상 watcher 는 gen 2 에 완성되고
  // 소멸했고, evolveAcceptCondition 의 NotExists 분기는 per-step 채널
  // (condPathFins / lateCondPathFins) 과 activeCondPaths 만 봤으므로 그 leaf 는
  // "이번 step 의 finish 없음 + root 비활성" -> Always 로 오해소됐다. isAccepted 의
  // RecordConditionEvaluator 도 흡수 창을 record 생성 gen 이후로 잘라 같은 오해소를 했다.
  //
  // 수정 (except 항목의 (a)(b)(c) 레시피를 lookahead 계열까지 확장):
  //   (a) remapEdgeCondGens 가 NotExists/Exists 의 startGen 도 Grand(=dot) 로 리맵 —
  //       이전에는 lookahead 만 구 규약이라 같은 태그 MID 가 term frame 에서는 span,
  //       edge frame 에서는 span+1 로 resolve 되어 한 key 가 두 span 을 뜻했다,
  //   (b) emitCondRootStarters 가 lookahead 도 bounded 와 같은 span-정규화 key 로 emit,
  //   (c) 런타임 3곳: step3 시동 flavor, starterDied 의 fresh fallback 제거,
  //       step6 의 lookahead 예외 anchor(tip/parent) 제거,
  //   (d) 누적 finish 기록 seenCondPathFins — watcher 사망 이후 물질화되는 leaf 가
  //       과거 관찰을 본다 (RecordConditionEvaluator 는 root 전 생애를 스캔),
  //   (e) step3 에서 cond root 의 내부 cond symbol 을 부모와 같은 span key 로 시동
  //       (`"fn"&Tk` 의 Tk 등 — 없으면 부모 watcher 의 finish 조건 OnlyIf(Tk@span,…) 이
  //       빈 key 를 보고 Never 로 무너진다: 블록 안 문장에서 강제 실패의 원인).
  // 상세: mgroup3/docs/watcher_anchor_dedup.md §9.
  @Test
  fun testNegativeLookaheadDroppedWhenBodyOutrunsLookahead() {
    // 본문이 lookahead 본문보다 긴 경우.
    val outrun = makeParser(
      """
        Program = !"fn" Expression ';'
        Expression = "fn" "()"
      """.trimIndent(),
      startName = "Program",
    )
    assertRejects(outrun, "fn();")

    // 같은 모양의 positive control — 본문이 lookahead 본문과 다른 문자열이면 통과.
    val outrunOk = makeParser(
      """
        Program = !"fn" Expression ';'
        Expression = "gn" "()"
      """.trimIndent(),
      startName = "Program",
    )
    assertAccepts(outrunOk, "gn();")

    // 뒤에 ';' 가 없어도 (조건이 최종 progress 조건에 실려 isAccepted 에서 평가되는 경로).
    val outrunNoSemi = makeParser(
      """
        Program = !"fn" Expression
        Expression = "fn" "()"
      """.trimIndent(),
      startName = "Program",
    )
    assertRejects(outrunNoSemi, "fn()")

    // lookahead 본문 심볼이 본문과 공유되지 않는 경우에도 동일.
    val outrunDistinct = makeParser(
      """
        Program = !"fn" Expression ';'
        Expression = "fnx"
      """.trimIndent(),
      startName = "Program",
    )
    assertRejects(outrunDistinct, "fnx;")

    // 중첩 블록 안의 문장에서도 (es5 `{ function f() {}; }` 대응).
    val nested = makeParser(
      """
        S = Stmts
        Stmts = Stmt*
        Stmt = Block | ExprStmt
        Block = '{' Stmts '}'
        ExprStmt = !('{' | "fn") Expr ';'
        Expr = "fn" "()" | 'x'
      """.trimIndent(),
      startName = "S",
    )
    assertRejects(nested, "fn();")
    assertRejects(nested, "{fn();}")

    // lookahead 본문에 join 이 있는 경우 (ES5 의 `"function"&Tk` 형태) — watcher 의
    // finish 조건이 Always 가 아니라 OnlyIf(Tk@span, …) 이므로 (e) 가 필요하다.
    val joinInLookahead = makeParser(
      """
        S = Stmts
        Stmts = Stmt*
        Stmt = Block | ExprStmt
        Block = '{' Stmts '}'
        ExprStmt = !('{' | "fn"&Tk) Expr ';'
        Tk = <Word>
        Word = 'a-z' 'a-z'*
        Expr = Call | 'x'
        Call = Word '(' ')'
      """.trimIndent(),
      startName = "S",
    )
    assertRejects(joinInLookahead, "fn();")
    assertRejects(joinInLookahead, "{fn();}")
    assertRejects(joinInLookahead, "{{fn();}}")
    assertAccepts(joinInLookahead, "x;")
    assertAccepts(joinInLookahead, "{x;}")
    assertAccepts(joinInLookahead, "fna();")   // "fna" 는 Tk 경계가 달라 lookahead 불통과
    assertAccepts(joinInLookahead, "{fna();}")

    // 다세대 lookahead-is (Exists 쪽 — 같은 key 규약 문제의 반대 방향 오거부였다).
    val lookaheadIs = makeParser("S = ^\"ab\" 'a-b'*", startName = "S")
    assertAccepts(lookaheadIs, "abab")
  }

  // bug B 를 "관찰된 finish 를 누적해 나중에 태어난 leaf 도 본다" 로 고치려는 시도가
  // 깨뜨리는 케이스들 — 같은 key 의 다른 span 매치를 흡수해 오거부가 된다.
  // 현재는 전부 통과하므로 회귀 가드로 남긴다 (bug B 수정 시 반드시 함께 통과해야 함).
  @Test
  fun testLookaheadSpanKeyGuards() {
    // 같은 key (sym,1) 를 span 0 (테이블 스타터) 와 span 1 (fresh 재시동) 이 공유:
    // '{' 는 position 1 에 있으므로 position 0 의 `!'{'` 는 통과해야 한다.
    val drift = makeParser(
      """
        S = Stmt
        Stmt = !'{' Expr ';'
        Expr = '(' Obj ')'
        Obj = '{' '}'
      """.trimIndent(),
      startName = "S",
    )
    assertAccepts(drift, "({});")

    // 바깥 문장의 watcher (span 0) 가 key 를 점유한 채 매치되고, 안쪽 문장의 leaf 는
    // span 1 을 뜻한다 — 누적 기록을 span 구분 없이 쓰면 `{x;}` 가 오거부된다.
    val nested = makeParser(
      """
        S = Stmts
        Stmts = Stmt*
        Stmt = Block | ExprStmt
        Block = '{' Stmts '}'
        ExprStmt = !('{' | "fn") Expr ';'
        Expr = "fn" "()" | 'x'
      """.trimIndent(),
      startName = "S",
    )
    assertAccepts(nested, "x;")
    assertAccepts(nested, "{x;}")
    assertAccepts(nested, "{{x;}}")
    assertAccepts(nested, "{x;x;}")

    // per-char 부정 lookahead repeat (구 규약이 의존하는 fresh fallback 경로).
    val perChar = makeParser("S = (!'b' 'a-c')*", startName = "S")
    assertAccepts(perChar, "")
    assertAccepts(perChar, "ac")
    assertAccepts(perChar, "ca")
    assertRejects(perChar, "b")
    assertRejects(perChar, "ab")
  }

  // ================================================================================
  // "bug A" 회귀 가드: 감시 대상 (except 의 피제외항 / join 의 상대항) 이 nullable 일 때
  // ================================================================================
  // 2026-07-30 발견/수정. 최소 재현:
  //     G = 'a' X 'b'
  //     X = '\n' - ' '*
  // 입력 "a\nb" -> naive2 / milestone2 / mgroup2(Scala) / mgroup2(Kotlin) 는 모두
  // ACCEPT (정답: ' '* 는 span (1,2) 를 매치하지 않는다). 수정 전 mgroup3 만 REJECT.
  // `X = '\n' - ' '+` (피제외항 non-nullable) 이면 수정 전에도 전원 일치 — nullable
  // 전용 결함이다. `^X`/`!X` 아래에서는 같은 결함이 양방향으로 뒤집혔다.
  //
  // 근본 원인은 *빈 매치 (zero-width finish) 의 end gen 오인* 두 곳이다. per-step
  // finish 채널의 end 규약은 eager(condPathFinishes) = gen, late(lateCondPathFinishes)
  // = gen-1 로 고정되어 있고, bounded 조건 (Unless/OnlyIf) 은 정확히 그 span 의 finish
  // 만 discharge 한다:
  //  (1) cond root 의 selfFinishAcceptCondition (빈 span (startGen,startGen) 매치) 을
  //      startGen 과 무관하게 eager 로 기록했다. 빈 매치로 완성·소멸한 watcher 가
  //      다음 gen 에 조건 참조로 재물질화되면 (step 3), 그 빈 매치가 "한 글자 매치" 로
  //      오인되어 Unless(sym, g, g+1) 을 죽였다 → `X = '\n' - ' '*` 오거부.
  //      수정: recordZeroWidthSelfFinish — startGen==gen 은 eager, startGen==gen-1 은
  //      late, 그보다 과거는 기록하지 않음.
  //  (2) late 채널 finish 는 end 가 gen-1 인데 관찰은 gen 에서 일어나므로, 그 조건 안의
  //      bounded leaf (endGen == gen-1) 가 소비자에게 "창을 지난 leaf" 로 보여
  //      default (Unless→Always) 로 되살아났다 → `^(WS - WSNoNL)` 가 빈 매치를
  //      오인해 "ab" 오수락. 수정: settleLateFin — 저장 전에 직전 gen 기준으로 한 step
  //      먼저 evolve (endOfInputLateFins 의 가상 late step 도 동일).
  //
  // 테이블은 원래도 정상이었다 (a1 의 term action 조건은 except{symbol_id:8,
  // start_gen:MID, end_gen:NEXT} = Unless(8,1,2) — 정확히 body 의 span). 순수 런타임 결함.
  @Test
  fun testNullableExcludedOperandOfExcept() {
    // (1) 평문 nullable except.
    val star = makeParser(
      """
        G = 'a' X 'b'
        X = '\n' - ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(star, "a\nb")
    assertRejects(star, "ab")
    assertRejects(star, "a b")

    // nullable 을 ? 로 표현해도 동일.
    val opt = makeParser(
      """
        G = 'a' X 'b'
        X = '\n' - ('x')?
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(opt, "a\nb")
    assertRejects(opt, "ab")
    assertRejects(opt, "axb")

    // control: 피제외항이 non-nullable 이면 수정 전에도 정상이었다.
    val nonNullable = makeParser(
      """
        G = 'a' X 'b'
        X = '\n' - ' '+
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(nonNullable, "a\nb")
    assertRejects(nonNullable, "ab")
    assertRejects(nonNullable, "a b")

    // control: 본문이 nullable, 피제외항이 non-nullable (반대 방향).
    val nullableBody = makeParser(
      """
        G = 'a' X 'b'
        X = ' '* - '\n'
      """.trimIndent(),
      startName = "G",
    )
    assertRejects(nullableBody, "a\nb")
    assertAccepts(nullableBody, "ab")
    assertAccepts(nullableBody, "a  b")

    // 양쪽 nullable — 같은 심볼이면 아무것도 매치하지 않는다.
    val bothSame = makeParser(
      """
        G = 'a' X 'b'
        X = ' '* - ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertRejects(bothSame, "ab")
    assertRejects(bothSame, "a b")

    // 양쪽 nullable, 서로 다름 — 빈 매치만 죽고 나머지는 살아야 한다.
    val bothDiff = makeParser(
      """
        G = 'a' X 'b'
        X = ' '* - '\n'*
      """.trimIndent(),
      startName = "G",
    )
    assertRejects(bothDiff, "ab")
    assertAccepts(bothDiff, "a b")
    assertAccepts(bothDiff, "a  b")

    // watcher 가 여러 gen 살아야 하는 경우 — 긴 span 의 매치도 정확히 관찰돼야 한다.
    val multigen = makeParser(
      """
        G = 'a' X 'b'
        X = Y - Z
        Y = ' \n'+
        Z = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertRejects(multigen, "a b")
    assertRejects(multigen, "a   b")
    assertAccepts(multigen, "a \nb")
    assertAccepts(multigen, "a\n b")
    assertAccepts(multigen, "a\n\nb")

    // nonterminal chain 을 거쳐도 (피제외항이 여러 단계 아래).
    val chain = makeParser(
      """
        G = 'a' X 'b'
        X = NL - Sp
        NL = '\n'
        Sp = SpInner
        SpInner = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(chain, "a\nb")
    assertRejects(chain, "ab")

    // repeat 안에서 / 같은 심볼이 여러 span 에서 감시될 때.
    val inRepeat = makeParser("G = X*\nX = 'a-c' - 'b'*", startName = "G")
    assertAccepts(inRepeat, "")
    assertAccepts(inRepeat, "a")
    assertRejects(inRepeat, "b")
    assertAccepts(inRepeat, "ac")
    assertRejects(inRepeat, "abc")

    // 입력 끝에서 조건이 평가되는 경로 (endOfInputLateFins 의 가상 late step).
    val atEof = makeParser(
      """
        G = 'a' X
        X = '\n' - ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(atEof, "a\n")
    assertRejects(atEof, "a")
  }

  // (2) 의 direction-flip: nullable except 가 lookahead 본문일 때. 수정 전 mgroup3 는
  // "a\nb" 를 오거부하고 "ab" 를 오수락했다 (^ 아래), ! 아래에서는 그 반대.
  @Test
  fun testNullableExcludedOperandUnderLookahead() {
    val posLookahead = makeParser(
      """
        G = 'a' ^X WS 'b'
        X = WS - WSNoNL
        WS = ' \n'*
        WSNoNL = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(posLookahead, "a\nb")
    assertRejects(posLookahead, "ab")
    assertRejects(posLookahead, "a b")
    assertAccepts(posLookahead, "a \nb")
    assertAccepts(posLookahead, "a\n b")

    val negLookahead = makeParser(
      """
        G = 'a' !X WS 'b'
        X = WS - WSNoNL
        WS = ' \n'*
        WSNoNL = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertRejects(negLookahead, "a\nb")
    assertAccepts(negLookahead, "ab")
    assertAccepts(negLookahead, "a b")
    assertRejects(negLookahead, "a \nb")
    assertRejects(negLookahead, "a\n b")

    // 입력 끝에서 (가상 late step) 도 동일.
    val posAtEof = makeParser(
      """
        G = 'a' ^X WS
        X = WS - WSNoNL
        WS = ' \n'*
        WSNoNL = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(posAtEof, "a\n")
    assertRejects(posAtEof, "a")
    assertRejects(posAtEof, "a ")
    assertAccepts(posAtEof, "a \n")
  }

  // 같은 근본 원인의 join (`&`) 쪽 — 상대항이 nullable 이면 빈 매치가 OnlyIf 를
  // 부당하게 만족시켜 오수락됐다.
  @Test
  fun testNullableOperandOfJoin() {
    val join = makeParser(
      """
        G = 'a' X 'b'
        X = Y & Z
        Y = ' \n'*
        Z = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(join, "ab")
    assertAccepts(join, "a b")
    assertAccepts(join, "a  b")
    assertRejects(join, "a\nb")
    assertRejects(join, "a \nb")

    val joinAtEof = makeParser(
      """
        G = 'a' X
        X = Y & Z
        Y = ' \n'*
        Z = ' '*
      """.trimIndent(),
      startName = "G",
    )
    assertAccepts(joinAtEof, "a")
    assertAccepts(joinAtEof, "a ")
    assertRejects(joinAtEof, "a\n")
  }
}
