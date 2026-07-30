package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.nparser2.NaiveParser2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

// ES5.1 Annex A + 7.9 자동 세미콜론 삽입(ASI) 직역 문법
// (examples/metalang3/resources/es5-asi/grammar.cdg) 의 수용 배터리.
// 각 케이스는 7.9.1 의 삽입 규칙 하나 또는 7.9.2 의 예제 하나를 겨냥한다.
// 판정은 mgroup3, 문장 개수(=어디에 세미콜론이 들어갔는지)는 naive2 파스트리로.
class Es5AsiCdgTest {
  companion object {
    private const val GRAMMAR = "examples/metalang3/resources/es5-asi/grammar.cdg"
    private const val BASE_GRAMMAR = "examples/metalang3/resources/es5/grammar.cdg"

    private var cached: Triple<NGrammar, Mgroup3Parser, NaiveParser2>? = null

    fun parser(): Triple<NGrammar, Mgroup3Parser, NaiveParser2> {
      cached?.let { return it }
      val cdg = Path.of(GRAMMAR).readText()
      val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Program").ngrammar()
      val gen = Mgroup3ParserGenerator(grammar)
      val t = Triple(grammar, Mgroup3Parser(gen.generate()), NaiveParser2(grammar))
      cached = t
      return t
    }
  }

  private fun m3Accepts(src: String): Boolean {
    val (_, parser, _) = parser()
    return try {
      parser.isAccepted(parser.parse(src))
    } catch (e: ParsingError) {
      false
    }
  }

  /** naive2 (검증된 reference 파서) 로 파스 포레스트를 재구성한다. reject 면 null. */
  private fun forestOf(src: String): ParseForest? {
    val (_, _, naive) = parser()
    val r = naive.parse(Inputs.fromString(src))
    if (!r.isRight) return null
    val hctx = (r as scala.util.Right<*, *>).value() as NaiveParser2.ParsingHistoryContext
    val forest = naive.parseTreeReconstructor2<ParseForest>(
      com.giyeok.jparser.`ParseForestFunc$`.`MODULE$`, hctx
    ).reconstruct()
    if (forest.isEmpty) return null
    val f = forest.get() as ParseForest
    return if (f.trees().nonEmpty()) f else null
  }

  private fun naiveAccepts(src: String): Boolean = forestOf(src) != null

  private fun accept(src: String, alsoNaive: Boolean = false) {
    assertTrue(m3Accepts(src)) { "expected ACCEPT: $src" }
    if (alsoNaive) assertTrue(naiveAccepts(src)) { "naive2 disagrees (expected ACCEPT): $src" }
  }

  private fun reject(src: String, alsoNaive: Boolean = false) {
    assertTrue(!m3Accepts(src)) { "expected REJECT: $src" }
    if (alsoNaive) assertTrue(!naiveAccepts(src)) { "naive2 disagrees (expected REJECT): $src" }
  }

  // ---- 파스트리에서 문장 개수 세기 -------------------------------------------------
  // ASI 케이스는 "받아들여지는가"보다 "세미콜론이 어디에 들어갔는가"가 본질이라서,
  // top-level SourceElement / Block 안 Statement 의 개수를 직접 확인한다.

  private fun isNt(sym: Any?, name: String): Boolean =
    sym is NGrammar.NNonterminal && sym.symbol().name() == name

  /** name 으로 바인딩된 노드를 세되, 그 안으로는 내려가지 않는다 (= 최상위 개수). */
  private fun countTop(node: ParseResultTree.Node, name: String): Int = when (node) {
    is ParseResultTree.BindNode ->
      if (isNt(node.symbol(), name)) 1 else countTop(node.body(), name)

    is ParseResultTree.JoinNode -> countTop(node.body(), name)
    is ParseResultTree.SequenceNode -> {
      val cs = node.children()
      var sum = 0
      var i = 0
      while (i < cs.size()) {
        sum += countTop(cs.apply(i), name); i++
      }
      sum
    }

    else -> 0
  }

  private fun findFirst(node: ParseResultTree.Node, name: String): ParseResultTree.Node? =
    when (node) {
      is ParseResultTree.BindNode ->
        if (isNt(node.symbol(), name)) node else findFirst(node.body(), name)

      is ParseResultTree.JoinNode -> findFirst(node.body(), name)
      is ParseResultTree.SequenceNode -> {
        val cs = node.children()
        var found: ParseResultTree.Node? = null
        var i = 0
        while (i < cs.size() && found == null) {
          found = findFirst(cs.apply(i), name); i++
        }
        found
      }

      else -> null
    }

  /**
   * [src] 를 파스했을 때 (a) 파스트리가 정확히 하나이고 (b) [under] (null 이면 Program 전체)
   * 바로 아래의 [name] 개수가 [expected] 인지 확인한다. 트리가 하나뿐이라는 것 자체가
   * SemiC/SemiA 의 세 대안이 서로소라는 -- 즉 ASI 인코딩이 모호성을 만들지 않는다는 -- 증거다.
   */
  private fun assertCount(src: String, expected: Int, name: String, under: String? = null) {
    val forest = forestOf(src) ?: throw AssertionError("expected ACCEPT: $src")
    val trees = forest.trees()
    assertEquals(1, trees.size()) { "ambiguous parse (${trees.size()} trees): $src" }
    val root = trees.apply(0)
    val base = if (under == null) root else findFirst(root, under)
      ?: throw AssertionError("no $under node in: $src")
    val body = if (base is ParseResultTree.BindNode) base.body() else base
    assertEquals(expected, countTop(body, name)) { "$name count under ${under ?: "Program"}: $src" }
  }

  private fun assertTopLevelStatements(src: String, expected: Int) =
    assertCount(src, expected, "SourceElement")

  // ================================================================================
  // 7.9.1 규칙 1: offending token 앞에 LineTerminator 가 있는 경우
  // ================================================================================
  @Test
  fun rule1LineTerminator() {
    accept("a = 1\nb = 2")                      // 규칙 1 + 규칙 2(EOF)
    accept("var x\nvar y = 2")                  // 12.2 VariableStatement
    accept("debugger\nx = 1")                   // 12.15 DebuggerStatement (SemiA)
    accept("throw new Error(\"x\")\nf()")       // 12.13, 피연산자가 있는 throw 는 SemiC
    accept("do f(); while (false)\ng()")        // 12.6.1 do-while 의 ')' (SemiA)
    accept("s = \"a\"\ns = s + \"b\"")

    // 7.9.2 예제: x \n ++ \n y 는 "x; ++y;" 다. ++ 는 restricted production 이므로
    // ContTok 의 '+' 는 '+' !'+' 로 막혀 있고, 그래서 개행 뒤 ++ 앞에서 삽입이 일어난다.
    accept("x\n++\ny")
    accept("a = b\n++c")

    // 단항 '!' 는 EqualityExpression 을 잇지 못한다 (ContTok 은 "!=" 만 갖는다).
    accept("a = b\n!c")
    // '.' 뒤에 숫자가 오면 member access 가 아니라 NumericLiteral ".5" 다 (7.8.3).
    accept("a = b\n.5.toFixed(2)")
    // 7.4: 개행을 포함한 블록 주석은 LineTerminator 로 친다.
    accept("a = b /* \n */ c()")
    // 개행 없는 블록 주석은 LineTerminator 가 아니므로 삽입되지 않는다.
    reject("a = b /* c */ c()")

    // 7.9.2: { 1 \n 2 } 3 은 { 1; 2; } 3; 이다.
    accept("{ 1\n2 } 3")
  }

  // ================================================================================
  // 7.9.1 규칙 1b ('}' 가 offending token) 과 규칙 2 (입력의 끝)
  // ================================================================================
  @Test
  fun rule1bCloseBraceAndRule2Eof() {
    accept("a = 1")                             // 규칙 2: 개행조차 없이 EOF
    accept("a = 1\n")
    accept("while (true) { break }")            // 규칙 1b
    accept("if (x) { a = 1 }")
    accept("function f() { return x }")
    accept("L: while (true) { continue L\n}")
    accept("while (true) { continue\n}")
    accept("do f(); while (false)")             // ')' 뒤 바로 EOF
    accept("{ a = 1 }")
  }

  // ================================================================================
  // 7.9.1 restricted production (7.9.1 의 세 번째 조건) — 피연산자 없는 형태로 갈라진다
  // ================================================================================
  @Test
  fun restrictedProductions() {
    // 12.9: `return \n a + b` 는 `return; a + b;`. bare return 이 SemiA 를 쓰기 때문에
    // 가능하다 -- SemiC 였다면 ContTok 이 다음 토큰을 continuation 으로 보고 삽입을 막고,
    // WSNoNL 은 a+b 를 return 에 붙이지 못해서 교착 상태가 된다.
    accept("function f() { return\na + b }")
    assertCount("function f() { return\na + b }", 2, "SourceElement", "FunctionBody")
    // `return \n (x);` 는 ContTok 이 '(' 를 갖고 있어도 삽입되어야 한다 (SemiA 의 존재 이유).
    accept("function f() { return\n(x); }")
    assertCount("function f() { return\n(x); }", 2, "SourceElement", "FunctionBody")

    // 12.7/12.8: break/continue 도 마찬가지.
    accept("L: while (true) { continue\nL; }")
    accept("L: while (true) { break\nL; }")

    // 12.13: throw 에는 피연산자 없는 형태가 없다 -> 개행은 SyntaxError 로 남는다.
    reject("throw\nnew Error()", alsoNaive = true)
    reject("function f() { throw\ne; }")

    // 11.3: postfix ++/-- 앞의 개행도 restricted -- 위 rule1LineTerminator 의 x\n++\ny 참고.
    accept("a++")
    reject("a\n++;")                            // `a; ++;` 는 ++ 의 피연산자가 없다
  }

  // ================================================================================
  // 7.9.1 의 부정 조건: 다음 토큰이 문장을 이을 수 있으면 삽입하지 않는다 (7.9.2 예제들)
  // ================================================================================
  @Test
  fun noInsertionWhenTokenContinues() {
    // 7.9.2 의 대표 예제: 두 번째 줄의 괄호는 함수 호출의 인자 목록으로 읽힌다.
    accept("a = b + c\n(d + e).f()")
    assertTopLevelStatements("a = b + c\n(d + e).f()", 1)
    // '[' 도 마찬가지 (member access).
    accept("a = b\n[1, 2].concat(c)")
    assertTopLevelStatements("a = b\n[1, 2].concat(c)", 1)

    // 7.9.2 division entanglement. '/' 는 continuation token 이므로 삽입되지 않고,
    // 프로그램은 `a = b / hi / g.exec(c)` 로 읽힌다 -- 정규식 리터럴이 아니라 나눗셈이다.
    // (V8 도 이 소스를 문법적으로는 받아들인다; 실패는 런타임에서 난다.)
    accept("a = b\n/hi/g.exec(c)")
    assertTopLevelStatements("a = b\n/hi/g.exec(c)", 1)
    // 나눗셈으로 읽으면 문법 오류인 형태를 주면 규칙 1 의 충실도가 드러난다:
    // 정규식으로 읽으면 멀쩡하지만 '/' 앞에서는 삽입이 금지되므로 SyntaxError 다.
    reject("a = b\n/hi there/g.exec(c)", alsoNaive = true)

    // 개행이 없으면 규칙 1 자체가 발동하지 않는다 (7.9.2).
    reject("{ 1 2 } 3", alsoNaive = true)
    reject("a = 1 b = 2")
    // ';' 는 offending token 이 아니다 -> `a = b \n ;` 는 문장 하나다.
    assertTopLevelStatements("a = b\n;", 1)
  }

  // ================================================================================
  // 7.9.1 이 절대 삽입하지 않는 자리
  // ================================================================================
  @Test
  fun neverInserted() {
    // 7.9.1: for 헤더의 ';' 는 절대 삽입되지 않는다.
    reject("for (a; b\n) f()", alsoNaive = true)
    reject("for (a\nb; c) f()")
    accept("for (a; b\n; c) f()")                 // 진짜 ';' 는 물론 허용

    // 7.9.1: 삽입된 세미콜론이 EmptyStatement 가 되는 경우도 없다. 이 인코딩에서는
    // SemiC/SemiA 가 terminator 라서 Statement 를 만들 수 없으므로 구조적으로 불가능하다.
    reject("if (a > b)\nelse c = d", alsoNaive = true)

    // 7.9.2: do-while 의 ')' 뒤에 개행이 없으면 ES5.1 문자 그대로는 SyntaxError.
    // (ES2015 부터는 엔진이 받아들인다 -- 명세가 바뀐 것이지 이 문법이 틀린 게 아니다.)
    reject("do f(); while (false) g()", alsoNaive = true)

    // 7.8.3 숫자 리터럴 경계는 그대로 (ASI 와 무관).
    reject("3in x", alsoNaive = true)
  }

  // ================================================================================
  // 삽입 위치 확인: 문장 개수 (트리가 하나뿐인 것도 함께 확인 = 모호성 없음)
  // ================================================================================
  @Test
  fun statementCounts() {
    assertTopLevelStatements("a = 1\nb = 2", 2)
    assertTopLevelStatements("x\n++\ny", 2)                 // x; ++y;
    assertTopLevelStatements("a = b\n++c", 2)               // a = b; ++c;
    assertTopLevelStatements("var x\nvar y = 2", 2)
    assertTopLevelStatements("debugger\nx = 1", 2)
    assertTopLevelStatements("a = b\n!c", 2)
    assertTopLevelStatements("a = b\n.5.toFixed(2)", 2)
    assertTopLevelStatements("s = \"a\"\ns = s + \"b\"", 2)
    assertTopLevelStatements("a = b /* \n */ c()", 2)       // 7.4
    assertTopLevelStatements("throw new Error(\"x\")\nf()", 2)
    assertTopLevelStatements("do f(); while (false)\ng()", 2)

    // 7.9.2: { 1 \n 2 } 3  ->  { 1; 2; } 3;
    assertTopLevelStatements("{ 1\n2 } 3", 2)
    assertCount("{ 1\n2 } 3", 2, "Statement", "Block")
  }

  // ================================================================================
  // superset 확인: ASI 없는 원본 문법(Es5CdgTest)의 배터리를 그대로 돌린다.
  // 원본 ACCEPT 는 전부 그대로 ACCEPT 여야 하고, 원본 REJECT 중에서는 "세미콜론 생략"
  // 때문에 거절되던 것만 ACCEPT 로 뒤집혀야 한다.
  // ================================================================================
  @Test
  fun supersetOfNonAsiBattery() {
    // --- Es5CdgTest.basics ---
    accept("var x = 1;", alsoNaive = true)
    accept("var x = 1, y = z + 2;")
    accept("")
    accept("  // just a comment\n")
    accept("x;")
    accept("varx;", alsoNaive = true)
    reject("var;", alsoNaive = true)
    reject("var var = 1;")
    accept("a.if;")
    accept("o = {if: 1, function: 2};")

    // --- Es5CdgTest.expressionStatementLookahead ---
    accept("{}", alsoNaive = true)
    accept("{};", alsoNaive = true)
    accept("({});", alsoNaive = true)
    accept("{ x = 1; }")
    accept("function f() {}", alsoNaive = true)
    accept("(function () {});")
    accept("functionX();")

    // --- Es5CdgTest.restrictedProductions ---
    accept("function f() { return x; }", alsoNaive = true)
    accept("function f() { return; }")
    accept("function f() { return /* c */ x; }")
    accept("a++;", alsoNaive = true)
    reject("a\n++;")
    accept("L: while (x) { break L; continue L; }")
    accept("throw e;")
    reject("function f() { throw\ne; }")
    // FLIP 1 (7.9.1 restricted production): 원본은 REJECT, ASI 에서는 `return; x;`.
    accept("function f() { return\nx; }", alsoNaive = true)
    // FLIP 2 (7.4 + 7.9.1): 개행을 품은 블록 주석도 LineTerminator 다.
    accept("function f() { return /* \n */ x; }")

    // --- Es5CdgTest.numericBoundaryAndMaximalMunch ---
    accept("x = 3 in y;")
    reject("x = 3in y;", alsoNaive = true)
    reject("x = 3.toString();")
    accept("x = 3..toString();")
    accept("x = 3 .toString();")
    accept("x = .5 + 0x1F + 1e-3 + 1.e3;")
    reject("x = 0x;")
    accept("a+++b;")
    reject("a++++b;")
    accept("a+ ++b;")
    accept("x = -a - -b - --c;")
    accept("x = a---b - -c;;")
    reject("x = a----b;")

    // --- Es5CdgTest.stringsAndEscapes ---
    accept("s = \"abc\";", alsoNaive = true)
    accept("s = 'a\\n\\t\\'b';")
    accept("s = \"quote \\\" inside\";")
    accept("s = \"nul \\0 ok\";")
    reject("s = \"\\01\";", alsoNaive = true)
    accept("s = \"hex \\x41 uni \\u0041\";")
    reject("s = \"broken\nstring\";")
    accept("s = \"line\\\ncontinuation\";")

    // --- Es5CdgTest.regexVsDivision ---
    accept("r = /ab+c/g;", alsoNaive = true)
    accept("x = a / b / c;", alsoNaive = true)
    accept("r = /=start/;")
    accept("x /= 2;")
    accept("r = /[a/b]/;")
    accept("r = /a\\/b/;")
    accept("if (a) /re/.test(b);")
    accept("x = /a/ / /b/;")
    reject("x = a / ;")

    // --- Es5CdgTest.statementsAndStructures ---
    accept("for (var i = 0; i < 10; i++) f(i);")
    accept("for (k in o) f(k);")
    accept("for (var k in o) {}")
    accept("for (;;) break;")
    accept("if (a) b(); else c();")
    accept("do x--; while (x > 0);")
    accept("switch (x) { case 1: f(); break; default: g(); }")
    accept("try { f(); } catch (e) { g(e); } finally { h(); }")
    accept("with (o) { f(); }")
    accept("debugger;")
    accept("var o = {get x() { return 1; }, set x(v) { this._v = v; }, \"k\": 1, 3: 2,};")
    accept("var a = [1, , 2, ], b = [,,];")
    accept("new a.b(c)(d)[e].f;")
    accept("x = a ? b : c, y = (f, g);")
    accept("x = typeof a === \"string\" && !b || void 0;")
    accept("x = a << 2 >>> b >= c & d | e ^ f;")
    accept("x = a <<= 2;")

    // 12.4 `[lookahead not-in {"{", "function"}]` — ASI 문법에서는 SemiC 가 ';' 를
    // 공급하므로 base 문법의 `{ function f() {} }` 뿐 아니라 `{ function f() {}; }`
    // 도 같은 경로로 거부돼야 한다. 2026-07-30 의 bug B 수정 전에는 오수락됐다.
    reject("{ function f() {} }", alsoNaive = true)
    reject("{ function f() {}; }", alsoNaive = true)
  }

  // ================================================================================
  // 12.4 ExpressionStatement lookahead — ASI 문법과 base 문법 양쪽에서 직접 단언
  // ================================================================================
  // 이전에는 "알려진 파서 발산" 이었다: 12.4 의 `[lookahead not-in {"{", "function"}]`
  // 이 milestone/mgroup 계열에서 강제되지 않아 (bug B) reference 파서로만 올바른
  // 판정을 고정해 뒀다. 2026-07-30 수정 후 mgroup3 가 naive2 와 일치하므로 직접
  // 단언으로 바꾼다. 최소 재현/근본 원인:
  //   Mgroup3ParserKnownIssuesTest.testNegativeLookaheadDroppedWhenBodyOutrunsLookahead
  //   + mgroup3/docs/watcher_anchor_dedup.md §9.
  @Test
  fun expressionStatementLookaheadIsEnforced() {
    // ASI 문법 (이 테스트의 parser) — mgroup3 와 naive2 양쪽으로.
    reject("{ function f() {} }", alsoNaive = true)
    reject("{ function f() {}; }", alsoNaive = true)
    reject("{ function g() {}; };", alsoNaive = true)
    accept("function f() {};", alsoNaive = true)   // FunctionDeclaration + EmptyStatement

    // base (ASI 이전) 문법에서도 mgroup3 가 직접 거부하는지 확인.
    val baseCdg = Path.of(BASE_GRAMMAR).readText()
    val baseGrammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(baseCdg, "Program").ngrammar()
    val baseM3 = Mgroup3Parser(Mgroup3ParserGenerator(baseGrammar).generate())
    for (src in listOf("{ function f() {} }", "{ function f() {}; }", "{ function g() {}; };")) {
      val accepted = try {
        val ctx = baseM3.parse(src)
        baseM3.isAccepted(ctx)
      } catch (e: ParsingError) {
        false
      }
      assertTrue(!accepted) { "base es5 grammar must reject: $src" }
    }
    // naive2 로도 같은 판정 (base 문법).
    val baseNaive = NaiveParser2(baseGrammar)
    val r = baseNaive.parse(Inputs.fromString("{ function f() {}; }"))
    val baseNaiveAccepts = if (!r.isRight) false else {
      val hctx = (r as scala.util.Right<*, *>).value() as NaiveParser2.ParsingHistoryContext
      val forest = baseNaive.parseTreeReconstructor2<ParseForest>(
        com.giyeok.jparser.`ParseForestFunc$`.`MODULE$`, hctx
      ).reconstruct()
      !forest.isEmpty && (forest.get() as ParseForest).trees().nonEmpty()
    }
    assertTrue(!baseNaiveAccepts) { "base es5 grammar must also reject { function f() {}; }" }
  }
}
