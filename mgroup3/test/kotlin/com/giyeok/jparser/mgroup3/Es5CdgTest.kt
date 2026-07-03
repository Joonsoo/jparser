package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.nparser2.NaiveParser2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

// ES5.1 Annex A 직역 문법 (examples/metalang3/resources/es5/grammar.cdg) 의 수용 배터리.
// 각 케이스는 명세의 disambiguation 장치 하나를 겨냥한다 (논문 §9 exhibit 의 검증).
// mgroup3 로 판정하고, 표시된 케이스는 naive2 (검증된 reference) 와 교차 확인.
class Es5CdgTest {
  companion object {
    private var cached: Pair<NGrammar, Mgroup3Parser>? = null
    fun parser(): Pair<NGrammar, Mgroup3Parser> {
      cached?.let { return it }
      val cdg = Path.of("examples/metalang3/resources/es5/grammar.cdg").readText()
      val grammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Program").ngrammar()
      val gen = Mgroup3ParserGenerator(grammar)
      val p = Pair(grammar, Mgroup3Parser(gen.generate()))
      cached = p
      return p
    }
  }

  private fun m3Accepts(src: String): Boolean {
    val (_, parser) = parser()
    return try {
      parser.isAccepted(parser.parse(src))
    } catch (e: ParsingError) {
      false
    }
  }

  private fun naiveAccepts(src: String): Boolean {
    val (grammar, _) = parser()
    val naive = NaiveParser2(grammar)
    val r = naive.parse(Inputs.fromString(src))
    if (!r.isRight) return false
    val hctx = (r as scala.util.Right<*, *>).value() as NaiveParser2.ParsingHistoryContext
    val forest = naive.parseTreeReconstructor2<com.giyeok.jparser.ParseForest>(
      com.giyeok.jparser.`ParseForestFunc$`.`MODULE$`, hctx
    ).reconstruct()
    return !forest.isEmpty && (forest.get() as com.giyeok.jparser.ParseForest).trees().nonEmpty()
  }

  private fun accept(src: String, alsoNaive: Boolean = false) {
    assertTrue(m3Accepts(src)) { "expected ACCEPT: $src" }
    if (alsoNaive) assertTrue(naiveAccepts(src)) { "naive2 disagrees (expected ACCEPT): $src" }
  }

  private fun reject(src: String, alsoNaive: Boolean = false) {
    assertTrue(!m3Accepts(src)) { "expected REJECT: $src" }
    if (alsoNaive) assertTrue(!naiveAccepts(src)) { "naive2 disagrees (expected REJECT): $src" }
  }

  @Test
  fun basics() {
    accept("var x = 1;", alsoNaive = true)
    accept("var x = 1, y = z + 2;")
    accept("")
    accept("  // just a comment\n")
    accept("x;")
    accept("varx;", alsoNaive = true)      // keyword boundary: identifier, not var decl
    reject("var;", alsoNaive = true)       // Identifier = IdentifierName - ReservedWord
    reject("var var = 1;")
    accept("a.if;")                        // 11.2: MemberExpression . IdentifierName — reserved word legal
    accept("o = {if: 1, function: 2};")    // 11.1.5: PropertyName is IdentifierName
  }

  @Test
  fun expressionStatementLookahead() {
    // 12.4 [lookahead not-in {"{", "function"}]
    accept("{}", alsoNaive = true)                  // Block, not object literal
    accept("{};", alsoNaive = true)                 // Block + EmptyStatement
    accept("({});", alsoNaive = true)               // parenthesized object literal
    accept("{ x = 1; }")
    accept("function f() {}", alsoNaive = true)     // FunctionDeclaration
    accept("(function () {});")                     // FunctionExpression statement
    accept("functionX();")                          // identifier starting with 'function'
  }

  @Test
  fun restrictedProductions() {
    // 7.9.1 [no LineTerminator here] — ASI 미인코딩이므로 개행 개입 시 reject
    accept("function f() { return x; }", alsoNaive = true)
    accept("function f() { return; }")
    reject("function f() { return\nx; }", alsoNaive = true)
    reject("function f() { return /* \n */ x; }")   // 7.4: 개행 포함 블록주석 = LineTerminator
    accept("function f() { return /* c */ x; }")    // 개행 없는 블록주석은 무해
    accept("a++;", alsoNaive = true)
    reject("a\n++;")                                // postfix 는 [no LT here]
    accept("L: while (x) { break L; continue L; }")
    accept("throw e;")
    reject("function f() { throw\ne; }")
  }

  @Test
  fun numericBoundaryAndMaximalMunch() {
    // 7.8.3 boundary + ch.7 longest match
    accept("x = 3 in y;")
    reject("x = 3in y;", alsoNaive = true)          // NumericLiteral ! IdentifierStart
    reject("x = 3.toString();")                     // 렉서는 "3."을 greedy 하게 — SyntaxError
    accept("x = 3..toString();")
    accept("x = 3 .toString();")
    accept("x = .5 + 0x1F + 1e-3 + 1.e3;")
    reject("x = 0x;")
    accept("a+++b;")                                // (a++)+b — greedy ++
    reject("a++++b;")
    accept("a+ ++b;")
    accept("x = -a - -b - --c;")
    accept("x = a---b - -c;;")                      // greedy: (a--)-b, 그리고 빈 문장
    reject("x = a----b;")                           // greedy: (a--)-- 는 LHS 가 아님 — 명세와 동일한 SyntaxError
  }

  @Test
  fun stringsAndEscapes() {
    accept("s = \"abc\";", alsoNaive = true)
    accept("s = 'a\\n\\t\\'b';")
    accept("s = \"quote \\\" inside\";")
    accept("s = \"nul \\0 ok\";")
    reject("s = \"\\01\";", alsoNaive = true)       // 7.8.4: 0 [lookahead not-in DecimalDigit]
    accept("s = \"hex \\x41 uni \\u0041\";")
    reject("s = \"broken\nstring\";")               // LineTerminator 는 문자열에 못 들어감
    accept("s = \"line\\\ncontinuation\";")         // 7.8.4 LineContinuation
  }

  @Test
  fun regexVsDivision() {
    // 7.8.5 — 두 렉서 goal 이 문법 문맥으로 해소되는지
    accept("r = /ab+c/g;", alsoNaive = true)
    accept("x = a / b / c;", alsoNaive = true)
    accept("r = /=start/;")                          // '=' 는 regex 첫 문자로 허용
    accept("x /= 2;")
    accept("r = /[a/b]/;")                           // 클래스 안의 '/' 는 종결자가 아님
    accept("r = /a\\/b/;")
    accept("if (a) /re/.test(b);")
    accept("x = /a/ / /b/;")                         // regex 나누기 regex
    reject("x = a / ;")
  }

  // blowup 점화 성분 격리용: 합성 주석 패턴별 shapes 관찰
  @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "ES5_DEBUG", matches = "1")
  @Test
  fun debugCommentShapes() {
    val (_, parser) = parser()
    fun shapesAfter(src: String): Pair<Int, Int> {
      var ctx = parser.initCtx()
      var peak = 0
      for ((idx, c) in src.withIndex()) {
        ctx = parser.parseStep(ctx, c, idx + 1 == src.length)
        val sh = ctx.paths.values.sumOf { it.size }
        if (sh > peak) peak = sh
        if (sh > 20000) return Pair(-idx, peak)  // 조기 중단: 음수 = 폭발 gen
      }
      return Pair(ctx.paths.values.sumOf { it.size }, peak)
    }
    val cases = listOf(
      "A-prose" to "// hello world foo bar baz\n".repeat(10),
      "B-call" to "//      JSON.stringify(value, replacer, space)\n".repeat(6),
      "C-head" to "//  json2.js\n//  2023-05-10\n".repeat(5),
      "D-punct" to "//  value: any JavaScript value, usually an object?\n".repeat(6),
      "E-mixed" to "// a b c\n//    x(y, z)\n".repeat(6),
      "F-code-then-comment" to "var x = 1;\n" + "// c d e f\n".repeat(10),
    )
    for ((name, src) in cases) {
      val (fin, peak) = shapesAfter(src)
      println("  $name (${src.length} chars): final=$fin peak=$peak")
    }
  }

  // 실코퍼스 검증: es5-corpus/*.js (repo 루트 기준; untracked) 전부 파싱.
  // bibix4 runEs5CorpusTest 로 실행.
  @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "ES5_CORPUS", matches = "1")
  @Test
  fun corpus() {
    val dir = Path.of("es5-corpus")
    val files = java.nio.file.Files.list(dir).filter { it.toString().endsWith(".js") }.sorted().toList()
    check(files.isNotEmpty()) { "no .js files in ${dir.toAbsolutePath()}" }
    val (_, parser) = parser()
    var failures = 0
    for (f in files) {
      val src = f.readText()
      val t0 = System.nanoTime()
      try {
        // step 구동 + 주기적 진행 출력: 선형 감속인지 상태 폭발인지 구분용.
        // shapes 가 임계를 넘으면 위치를 덤프하고 중단 (GC 폭사 방지).
        var ctx = parser.initCtx()
        var peak = 0
        for ((idx, c) in src.withIndex()) {
          ctx = parser.parseStep(ctx, c, idx + 1 == src.length)
          val shapes = ctx.paths.values.sumOf { it.size }
          if (shapes > peak) peak = shapes
          if ((idx + 1) % 1000 == 0) {
            println("  ${f.fileName} gen ${idx + 1}/${src.length}: shapes=$shapes (roots=${ctx.paths.size}) peak=$peak elapsed=%.0fs"
              .format((System.nanoTime() - t0) / 1e9))
          }
          if (shapes > 50000) {
            val (grammar, _) = parser()
            fun symName(id: Int): String = try {
              "sym$id(" + grammar.symbolOf(id).symbol().toShortString().take(80) + ")"
            } catch (e: Throwable) { "sym$id" }
            // root 별 shape 수
            ctx.paths.entries.sortedByDescending { it.value.size }.take(8).forEach { (root, pm) ->
              println("  ROOT ${symName(root.symbolId)}@${root.startGen}: ${pm.size} shapes")
            }
            // gen 을 지운 체인 시그니처 히스토그램 — 구조 증식인지 gen-분할 증식인지 판별
            val sig = HashMap<String, Int>()
            for (pm in ctx.paths.values) for (shape in pm.keys) {
              val chain = generateSequence(shape.milestonePath) { it.parent }
                .map { "${it.milestone.symbolId}:${it.milestone.pointer}" }.toList().reversed()
                .joinToString(">") + "|tip${shape.tipGroupId}"
              sig.merge(chain, 1) { a, b -> a + b }
            }
            println("  distinct chain signatures: ${sig.size}")
            sig.entries.sortedByDescending { it.value }.take(6).forEach { (k, v) ->
              val named = k.split(">").joinToString(">") { part ->
                val m = Regex("(\\d+):(\\d+)").find(part)
                if (m != null) "${symName(m.groupValues[1].toInt())}:${m.groupValues[2]}" else part
              }
              println("  x$v  $named")
            }
            // 최다 시그니처의 샘플 체인 3개를 gen 포함으로 — 어느 gen 이 흩어지는지 확인
            val topSig = sig.entries.maxByOrNull { it.value }!!.key
            var printed = 0
            outer@ for (pm in ctx.paths.values) for (shape in pm.keys) {
              val chain = generateSequence(shape.milestonePath) { it.parent }
                .map { "${it.milestone.symbolId}:${it.milestone.pointer}" }.toList().reversed()
                .joinToString(">") + "|tip${shape.tipGroupId}"
              if (chain == topSig) {
                val withGens = generateSequence(shape.milestonePath) { it.parent }
                  .map { "${symName(it.milestone.symbolId)}:${it.milestone.pointer}@${it.milestone.gen}" }
                  .toList().reversed().joinToString(" > ")
                println("  SAMPLE $withGens")
                if (++printed >= 3) break@outer
              }
            }
            val s = maxOf(0, idx - 150)
            error("state blowup at gen ${idx + 1} (shapes=$shapes)\nsource: ...${src.substring(s, idx + 1)}⟨HERE⟩")
          }
        }
        val ms = (System.nanoTime() - t0) / 1e6
        val ok = parser.isAccepted(ctx)
        println("${f.fileName}: ${src.length} chars, %.0fms, accepted=$ok, peakShapes=$peak".format(ms))
        if (!ok) failures++
      } catch (e: ParsingError) {
        failures++
        val pos = Regex("at (\\d+)").find(e.toString())?.groupValues?.get(1)?.toIntOrNull()
        val around = pos?.let {
          val s = maxOf(0, it - 60); val t = minOf(src.length, it + 20)
          val line = src.substring(0, minOf(it, src.length)).count { c -> c == '\n' } + 1
          "line $line: ...${src.substring(s, minOf(it, src.length))}⟨HERE⟩${src.substring(minOf(it, src.length), t)}..."
        } ?: ""
        println("${f.fileName}: PARSE ERROR $e\n  $around")
      }
    }
    assertTrue(failures == 0) { "$failures corpus file(s) failed" }
  }

  @Test
  fun statementsAndStructures() {
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
    reject("{ function f() {} }")                    // ES5: 블록 안 FunctionDeclaration 없음
  }
}
