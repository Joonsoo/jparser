package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class MulangCdgTest {
  // mulang.cdg 위치는 mulang 프로젝트의 grammar/mulang.cdg.
  // jparser의 root 기준 상대경로로 접근.
  private fun findMulangCdg(): String? {
    val candidates = listOf(
      "../mulang/grammar/mulang.cdg",
      "../../mulang/grammar/mulang.cdg",
      "mulang.cdg",
    )
    for (c in candidates) {
      if (Path(c).exists()) return c
    }
    return null
  }

  // 데이터를 한 번 생성한 후 캐시에 저장. 다른 테스트에서 재사용.
  companion object {
    private var cachedData: Mgroup3ParserData? = null
    fun loadOrGenerate(cdgPath: String): Mgroup3ParserData {
      cachedData?.let { return it }
      val cdg = Path(cdgPath).readText()
      val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "CompileUnit")
      val grammar = grammarAnalysis.ngrammar()
      val gen = Mgroup3ParserGenerator(grammar)
      val data = gen.generate()
      cachedData = data
      return data
    }
  }

  private fun parseSimple(text: String): ParsingCtx {
    val cdgPath = findMulangCdg() ?: error("mulang.cdg not found")
    val data = loadOrGenerate(cdgPath)
    val parser = Mgroup3Parser(data)
    return parser.parse(text)
  }

  @org.junit.jupiter.api.Disabled
  @Test
  fun testTimingPerStep() {
    val cdgPath = findMulangCdg() ?: return
    val genStart = System.currentTimeMillis()
    val data = loadOrGenerate(cdgPath)
    val genEnd = System.currentTimeMillis()
    println("Generation/load time: ${genEnd - genStart} ms")
    val parser = Mgroup3Parser(data)
    val text = "def hello(): string {}"

    // 첫 번째 parse - cold start 측정
    val cold1 = System.currentTimeMillis()
    parser.parse(text)
    val cold2 = System.currentTimeMillis()
    println("Cold parse 1: ${cold2 - cold1} ms")

    // 두 번째 parse - warmed up
    val warm1 = System.currentTimeMillis()
    parser.parse(text)
    val warm2 = System.currentTimeMillis()
    println("Warm parse 2: ${warm2 - warm1} ms")

    // 세 번째 parse
    val warm3 = System.currentTimeMillis()
    parser.parse(text)
    val warm4 = System.currentTimeMillis()
    println("Warm parse 3: ${warm4 - warm3} ms")

    // 더 큰 input
    val src = """
      def x() {
        let a = 1
        let b = 2
        return a + b
      }
    """.trimIndent()
    val big1 = System.currentTimeMillis()
    parser.parse(src)
    val big2 = System.currentTimeMillis()
    println("Larger ${src.length} chars: ${big2 - big1} ms")
  }

  @Test
  fun testParseSimple1() {
    val ctx = parseSimple("def hello(): string {}")
    println("after parse: gen=${ctx.gen}, mainPaths=${ctx.mainPaths.size}, condPaths=${ctx.condPaths.size}")
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("isAccepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testStringLiteralInSrc() {
    // mulang 안에서 `"abc"` 같은 string literal을 포함
    val src = """
      def f() {
        let x = "abc"
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testStringLiteralInSrc: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testStringInPrintln() {
    val src = """
      def f() {
        println("hello")
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testStringInPrintln: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testMatchSimple() {
    // match.mu 패턴 단순화. col 9 = case 의 첫 char 다음에 string literal이 시작되는 위치
    val src = """
      def f() {
        match abc {
          case "Hello!" -> println("it is Hello!")
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testMatchSimple: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testMatchCase123() {
    val src = """
      def f() {
        match x {
          case 123 -> println("it is 123")
          case "abc" -> println("it is abc")
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testMatchCase123: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoCase1() {
    val src = """
      def f() {
        match x {
          case 1 -> a
          case "x" -> b
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoCase1: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoCaseStringFirst() {
    val src = """
      def f() {
        match x {
          case "x" -> a
          case 1 -> b
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoCaseStringFirst: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoCaseFnCallString() {
    // RHS에 function call + string. testMatchCase123 단순화.
    val src = """
      def f() {
        match x {
          case 1 -> p("a")
          case "b" -> q
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoCaseFnCallString: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang grammar 는 top-level 이 def/class/etc 만 가능. `match x { ... }` 는 invalid input.
  @org.junit.jupiter.api.Disabled("invalid input — match expression cannot be top-level")
  @Test
  fun testTwoCaseStringInRHSMinimal() {
    // 가능한 단순화. match 안에서 string RHS 후 stmt sep
    val src = "match x { case 1 -> \"a\"\ncase 2 -> b }"
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoCaseStringInRHSMinimal: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // micro grammar reproducer: NLongest + NRepeat 포함 string literal 두 개
  @Test
  fun testTwoStringMicro() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Str
      Str = '"' <(.-'"')+> '"'
      NL = '\n'
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("\"a\"\n\"b\"")
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringMicro: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang의 StringElem 처럼 NLongest 안에 NRepeat이 있는 경우. + 외부에 backslash escape 같은 alternative가 있어 NLongest는 NRepeat의 OneOf body
  @Test
  fun testTwoStringComplexElem() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      NL = '\n'
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("\"a\"\n\"b\"")
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringComplexElem: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // 두 string stmt fail. 각 step별 path 상태를 출력
  @Test
  fun testDebugTwoStringInBlock() {
    val cdgPath = findMulangCdg() ?: return
    val data = loadOrGenerate(cdgPath)
    val parser = Mgroup3Parser(data)
    val src = "def f() {\n  \"a\"\n  \"b\"\n}"
    var ctx = parser.initCtx()
    for ((idx, c) in src.withIndex()) {
      try {
        ctx = parser.parseStep(ctx, c, idx == src.length - 1)
        val charDisplay = if (c == '\n') "\\n" else c.toString()
        println("[$idx] '$charDisplay' -> mainPaths=${ctx.mainPaths.size}, condPaths=${ctx.condPaths.size}")
      } catch (e: ParsingError) {
        val charDisplay = if (c == '\n') "\\n" else c.toString()
        println("[$idx] '$charDisplay' -> FAIL")
        // fail 직전 main path 상태 출력
        for ((i, mp) in ctx.mainPaths.withIndex()) {
          println("  before path[$i]: tipGroupId=${mp.tipGroupId}, acc=${mp.acceptCondition}")
        }
        // fail 직전 condPaths 상태도 출력
        println("  --- condPaths ---")
        for ((root, paths) in ctx.condPaths) {
          println("  cond root=${root}: ${paths.size} paths")
        }
        // last history entry의 condPathFinishes
        println("  --- last history condPathFinishes ---")
        for ((root, cond) in ctx.history.last().condPathFinishes) {
          println("  fin ${root}: ${cond}")
        }
        // 문제의 1212, 1210 symbol은 무엇?
        println("  --- 1210/1212 type ---")
        // grammar 정보
        val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(Path(cdgPath).readText(), "CompileUnit")
        val grammar = grammarAnalysis.ngrammar()
        for (sid in listOf(300, 303, 439, 441, 1210, 1212)) {
          val sym = grammar.nsymbols().get(sid)
          println("  symbol $sid: $sym")
        }
        throw e
      }
    }
    println("Final: accepted=${parser.isAccepted(ctx)}")
  }

  // 외부 grammar 파일 + 입력으로 시험. grammar narrow 위해.
  // /tmp/mulang_test.cdg 가 있으면 그 grammar로 두 string statement 테스트.
  @Test
  fun testExternalGrammar() {
    val gpath = Path("/tmp/mulang_test.cdg")
    if (!gpath.exists()) {
      println("/tmp/mulang_test.cdg not found, skipping")
      return
    }
    val cdg = gpath.readText()
    println("Grammar size: ${cdg.length} chars")
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "CompileUnit")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val genStart = System.currentTimeMillis()
    val data = gen.generate()
    val genEnd = System.currentTimeMillis()
    println("Generation: ${genEnd - genStart} ms")
    val parser = Mgroup3Parser(data)
    val src = "def f() {\n  \"a\"\n  \"b\"\n}"
    try {
      val ctx = parser.parse(src)
      val accepted = parser.isAccepted(ctx)
      println("testExternalGrammar: accepted=$accepted, ${src.length} chars")
      org.junit.jupiter.api.Assertions.assertTrue(accepted)
    } catch (e: ParsingError) {
      println("testExternalGrammar FAIL: $e")
      throw e
    }
  }

  // 가장 작은 reproducer 디버깅
  @Test
  fun testDebugNLongestRepeat() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Expr
      Expr = <Atom (S Op S Atom)*>
      Atom = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"')+>
      Op = '+'
      S = ' '*
      NL = '\n' ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "\"a\"\n\"b\""
    var ctx = parser.initCtx()
    for ((idx, c) in src.withIndex()) {
      try {
        ctx = parser.parseStep(ctx, c, idx == src.length - 1)
        val charDisplay = if (c == '\n') "\\n" else c.toString()
        println("[$idx] '$charDisplay' -> mainPaths=${ctx.mainPaths.size}, condPaths=${ctx.condPaths.size}")
        for ((root, paths) in ctx.condPaths) {
          println("  cond ${root}: ${paths.size} paths")
        }
      } catch (e: ParsingError) {
        val charDisplay = if (c == '\n') "\\n" else c.toString()
        println("[$idx] '$charDisplay' -> FAIL: $e")
        for ((i, mp) in ctx.mainPaths.withIndex()) {
          println("  before path[$i]: tip=${mp.tipGroupId}, acc=${mp.acceptCondition}")
        }
        for ((root, paths) in ctx.condPaths) {
          println("  cond ${root}: ${paths.size} paths")
        }
        println("  history.last condPathFinishes: ${ctx.history.last().condPathFinishes}")
        // 1212에 해당하는 sym 찾기
        for (sid in 0 until grammar.nsymbols().size()) {
          val sym = grammar.nsymbols().get(sid)
          val symStr = sym.toString()
          if (symStr.contains("OneOf") && symStr.contains("Atom")) {
            println("  symbol $sid: $symStr")
          }
          if (sid > 30) break  // 출력 너무 많아짐 방지
        }
        throw e
      }
    }
  }

  // verbose debug로 testTwoStringMicro 분석
  @org.junit.jupiter.api.Disabled
  @Test
  fun testDebugTwoStringMicro() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Str
      Str = '"' <(.-'"')+> '"'
      NL = '\n'
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    parser.setVerbose()
    val src = "\"a\"\n\"b\""
    var ctx = parser.initCtx()
    for ((idx, c) in src.withIndex()) {
      try {
        ctx = parser.parseStep(ctx, c, idx == src.length - 1)
      } catch (e: ParsingError) {
        println("FAIL at $idx '$c'")
        throw e
      }
    }
    println("accepted=${parser.isAccepted(ctx)}")
  }

  @Test
  fun testOrExpression() {
    // rbln_examples.mu 의 fail 케이스 단순화
    val src = """
      def f() {
        if a || b {}
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testOrExpression: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testIfWithoutOr() {
    // testOrExpression 확인용 — || 없이는?
    val src = """
      def f() {
        if a {}
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testIfWithoutOr: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testOrSimpleStmt() {
    // if 없이 그냥 expression 으로
    val src = """
      def f() {
        let x = a || b
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testOrSimpleStmt: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testAndStmt() {
    val src = """
      def f() {
        let x = a && b
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testAndStmt: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testEqStmt() {
    val src = """
      def f() {
        let x = a == b
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testEqStmt: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // micro grammar: 직접 left recursion
  @Test
  fun testLeftRecursionMicro() {
    val cdg = """
      Grammar = E
      E = N | E WS '+' WS N
      N = '0-9'+
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("1 + 2")
    val accepted = parser.isAccepted(ctx)
    println("testLeftRecursionMicro: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testLeftRecursionMultiChar() {
    val cdg = """
      Grammar = E
      E = N | E WS "||" WS N
      N = '0-9'+
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("1 || 2")
    val accepted = parser.isAccepted(ctx)
    println("testLeftRecursionMultiChar: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testLeftRecursionWithOpTk() {
    // mulang 의 "||"&OpTk 패턴 mimic
    val cdg = """
      Grammar = E
      E = N | E WS "||"&OpTk WS N
      N = '0-9'+
      OpTk = <Op>
      Op = ('+' | '-' | "||")+
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("1 || 2")
    val accepted = parser.isAccepted(ctx)
    println("testLeftRecursionWithOpTk: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testDebugLeftRecJoin() {
    val cdg = """
      Grammar = E
      E = N | E WS "||"&OpTk WS N
      N = '0-9'+
      OpTk = <Op>
      Op = ('+' | '-' | "||")+
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "1 || 2"
    var ctx = parser.initCtx()
    println("== init: main=${ctx.mainPaths.size}, cond=${ctx.condPaths.size}")
    for ((root, ps) in ctx.condPaths) println("  cond ${root}: ${ps.size}")
    for ((idx, c) in src.withIndex()) {
      val display = if (c == ' ') "_" else c.toString()
      try {
        ctx = parser.parseStep(ctx, c, idx == src.length - 1)
        println("[$idx] '$display' -> main=${ctx.mainPaths.size} cond=${ctx.condPaths.size}")
        for ((i, p) in ctx.mainPaths.withIndex()) {
          println("  main[$i] tip=${p.tipGroupId} acc=${p.acceptCondition}")
        }
        for ((root, ps) in ctx.condPaths) println("  cond ${root}: ${ps.size}")
      } catch (e: ParsingError) {
        println("[$idx] '$display' -> FAIL: $e")
        // cond path (19, 3) 의 termActions 살펴보기
        val sym19Root = PathRoot(19, 3)
        val condPaths = ctx.condPaths[sym19Root]
        if (condPaths != null) {
          for ((j, cp) in condPaths.withIndex()) {
            println("  cond (19,3) cp[$j] tip=${cp.tipGroupId} acc=${cp.acceptCondition}")
            val termActions = data.termActionsMap[cp.tipGroupId]
            if (termActions != null) {
              for (ta in termActions.actionsList) {
                val termGroupChars = ta.termGroup.toString().replace("\n", " ")
                println("    termGroup: $termGroupChars")
              }
            }
          }
        }
        throw e
      }
    }
  }

  @Test
  fun testAddStmt() {
    val src = """
      def f() {
        let x = a + b
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testAddStmt: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testOrWithMethodCall() {
    val src = """
      def f() {
        if a.b || c.d {}
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testOrWithMethodCall: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // dead path 추적: step별 main path 의 milestone path + finishedKernels 자세히 출력
  @Test
  fun testTraceMainPathLifetime() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Expr
      Expr = <Atom (S Op S Atom)*>
      Atom = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"')+>
      Op = '+'
      S = ' '*
      NL = '\n' ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "\"a\"\n\"b\""
    var ctx = parser.initCtx()
    println("=== initCtx ===")
    println("  mainPaths=${ctx.mainPaths.size}")
    for ((i, p) in ctx.mainPaths.withIndex()) {
      println("  main[$i] tip=${p.tipGroupId}")
      var mp = p.milestonePath
      while (mp != null) {
        println("    mile gen=${mp.gen} kernel=(${mp.milestone.symbolId}+${mp.milestone.pointer}@${mp.milestone.gen}) obs=${mp.observingCondSymbolIds}")
        mp = mp.parent
      }
    }
    for ((idx, c) in src.withIndex()) {
      val charDisplay = if (c == '\n') "\\n" else c.toString()
      try {
        ctx = parser.parseStep(ctx, c, idx == src.length - 1)
        println("=== step $idx '$charDisplay' -> gen=${ctx.gen} ===")
        println("  mainPaths=${ctx.mainPaths.size}")
        for ((i, p) in ctx.mainPaths.withIndex()) {
          println("  main[$i] tip=${p.tipGroupId} acc=${p.acceptCondition}")
          var mp = p.milestonePath
          while (mp != null) {
            println("    mile gen=${mp.gen} kernel=(${mp.milestone.symbolId}+${mp.milestone.pointer}@${mp.milestone.gen}) obs=${mp.observingCondSymbolIds}")
            mp = mp.parent
          }
        }
        // sym 7 finish만 출력
        val entry = ctx.history.last()
        val sym7Finishes = entry.finishedKernels.filter { it.kernel.symbolId == 7 }
        if (sym7Finishes.isNotEmpty()) {
          println("  sym7 finishes:")
          for (f in sym7Finishes) {
            println("    (${f.kernel.symbolId}+${f.kernel.pointer}@${f.kernel.gen}) cond=${f.condition}")
          }
        }
        // condPaths 의 (7, 1) starter path 들
        val sym7Root = PathRoot(7, 1)
        if (sym7Root in ctx.condPaths) {
          println("  cond (7, 1) paths: ${ctx.condPaths[sym7Root]?.size}")
          for ((j, cp) in (ctx.condPaths[sym7Root] ?: emptyList()).withIndex()) {
            println("    cp[$j] tip=${cp.tipGroupId} acc=${cp.acceptCondition}")
            var mp = cp.milestonePath
            while (mp != null) {
              println("      mile gen=${mp.gen} kernel=(${mp.milestone.symbolId}+${mp.milestone.pointer}@${mp.milestone.gen}) obs=${mp.observingCondSymbolIds}")
              mp = mp.parent
            }
          }
        }
      } catch (e: ParsingError) {
        println("[$idx] '$charDisplay' -> FAIL: $e")
        // 마지막 main path 의 termAction 결과 살펴보기
        for ((i, p) in ctx.mainPaths.withIndex()) {
          println("  before-fail main[$i] tip=${p.tipGroupId} acc=${p.acceptCondition}")
          val ta = parser.findApplicableAction(p, c)
          if (ta != null) {
            println("    termAction matches!")
            for (rea in ta.replaceAndAppendsList) {
              println("    rea: replace=(${rea.replace.symbolId}+${rea.replace.pointer}) appendMgroup=${rea.append.milestoneGroupId} cond=${rea.append.acceptCondition}")
            }
            for (rap in ta.replaceAndProgressesList) {
              println("    rap: replaceMgroup=${rap.replaceMilestoneGroupId} cond=${rap.acceptCondition}")
            }
            if (ta.hasParsingActions()) {
              for (f in ta.parsingActions.finishedList) {
                println("    finish: (${f.symbolId}+${f.pointer}@${f.startGen}) cond=${f.finishCondition}")
              }
              for (pr in ta.parsingActions.progressedList) {
                println("    progress: (${pr.symbolId}+${pr.pointer}@${pr.startGen}+${pr.midGen})")
              }
            }
          }
        }
        throw e
      }
    }
  }

  // 가장 단순한 reproducer 시도. NLongest body의 NRepeat
  @org.junit.jupiter.api.Disabled
  @Test
  fun testNLongestRepeatTwoStrings() {
    // NLongest body가 NRepeat. WS에 newline. 두 string stmt
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Expr
      Expr = <Atom (S Op S Atom)*>
      Atom = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"')+>
      Op = '+'
      S = ' '*
      NL = '\n' ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "\"a\"\n\"b\""
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testNLongestRepeatTwoStrings: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // 더 narrow: WS에 newline 없으면? newline 만나도 Stmt 끝났다고 알 수 있나
  @Test
  fun testAddExprNoNewlineInWS() {
    val cdg = """
      Grammar = '{' WS Stmt (StmtDelim Stmt)* WS '}'
      Stmt = Expr
      Expr = <Atom (WSNN '+' WSNN Atom)*>
      Atom = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      WS = ' \t\n\r'*
      WSNN = ' \t'*
      WS_NO_NL = ' \t'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "{ \"a\"\n  \"b\" }"
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testAddExprNoNewlineInWS: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // 더 작은: AddExpr 안 NLongest body가 단순한 형태
  @Test
  fun testAddExprNLongestMinimal() {
    val cdg = """
      Grammar = '{' WS Stmt (StmtDelim Stmt)* WS '}'
      Stmt = Expr
      Expr = <Atom (WS '+' WS Atom)*>
      Atom = Str
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      WS = ' \t\n\r'*
      WS_NO_NL = ' \t'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "{ \"a\"\n  \"b\" }"
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testAddExprNLongestMinimal: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // 더 작은: MulExpr 단순화 (string 만)
  @Test
  fun testAddExprNLongestSimpler() {
    val cdg = """
      Grammar = '{' (WS Stmt (StmtDelim Stmt)*)? WS '}'
      Stmt = AddExpr
      AddExpr = <Str | Str (WS Op WS Str)+>
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      Op = '+' | '-'
      WS = ' \t\n\r'*
      WS_NO_NL = ' \t'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "{ \"a\"\n  \"b\" }"
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testAddExprNLongestSimpler: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang의 AddExpr 구조 simpler 흉내
  @Test
  fun testAddExprNLongest() {
    // AddExpr = <MulExpr | MulExpr (WS Op MulExpr)+> 형태
    val cdg = """
      Grammar = '{' (WS Stmt (StmtDelim Stmt)*)? WS '}'
      Stmt = AddExpr
      AddExpr = <MulExpr | MulExpr (WS Op WS MulExpr)+>
      MulExpr = Str | Idnt
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      Idnt = 'a-zA-Z' '0-9a-zA-Z_'*
      Op = '+' | '-' | '*' | '/'
      WS = ' \t\n\r'*
      WS_NO_NL = ' \t'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "{ \"a\"\n  \"b\" }"
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testAddExprNLongest: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoStringWithCallExpr() {
    // BaseCallee와 CallChainArgs lookahead-not 패턴
    val cdg = """
      Grammar = '{' (WS Stmts)? WS '}'
      Stmts = Stmt (StmtDelim Stmt)*
      Stmt = CallExpr
      CallExpr = BaseCallee CallChain*
      BaseCallee = StringExpr | Idnt
      CallChain = WS_NO_NL CallChainArgs
      CallChainArgs = (CallArgs WS_NO_NL)? TrailingLambda
        | CallArgs !(WS_NO_NL TrailingLambda)
      CallArgs = '(' WS ')' | '(' WS Idnt WS ')'
      TrailingLambda = '{' WS '}'
      StringExpr = '"' StringElem* '"'
      StringElem = <(.-'"\\')+>
      Idnt = 'a-zA-Z' '0-9a-zA-Z_'*
      WS = ' \t\n\r'*
      WS_NO_NL = ' \t'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val src = "{ \"a\"\n\"b\" }"
    val ctx = parser.parse(src)
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringWithCallExpr: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoStringStmtAlts() {
    // Stmt가 Expr | Let 같은 alternatives. Expr는 String | Idnt
    val cdg = """
      Grammar = '{' WS Stmt (StmtDelim Stmt)* WS '}'
      Stmt = Expr | Let
      Expr = Str | Idnt
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
      Idnt = 'a-zA-Z' '0-9a-zA-Z_'*
      Let = "let"&Tk WS Idnt
      Tk = <Idnt>
      WS = ' \t\n\r'*
      StmtDelim = WS_NO_NL '\n' WS | WS ';' WS
      WS_NO_NL = ' \t'*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("{ \"a\"\n\"b\" }")
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringStmtAlts: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang의 StringExpr와 가까운 구조: Elem이 OneOf의 NLongest
  @Test
  fun testTwoStringWithEscapeAlts() {
    val cdg = """
      Grammar = Stmt (NL Stmt)*
      Stmt = Str | Idnt
      Str = '"' Elem* '"'
      Elem = <(.-'"\\')+>
        | '\\' '\\nbrt'
      Idnt = 'a-zA-Z' '0-9a-zA-Z_'*
      NL = '\n'
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("\"a\"\n\"b\"")
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringWithEscapeAlts: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoStringInBlock() {
    // block 안에 두 string statement
    val src = "def f() {\n  \"a\"\n  \"b\"\n}"
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoStringInBlock: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testCaseStringNoSecondCase() {
    val src = "def f() {\n  match x {\n    case 1 -> \"a\"\n  }\n}"
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testCaseStringNoSecondCase: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testCaseStringPlusIdentifier() {
    // 첫 case가 string RHS, 다음 case는 identifier RHS
    val src = "def f() {\n  match x {\n    case 1 -> \"a\"\n    case 2 -> b\n  }\n}"
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testCaseStringPlusIdentifier: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testStringInBlock() {
    // block 안에 두 개의 statement이고 첫 stmt 가 string
    val src = "def f() {\n  \"a\"\n  b\n}"
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testStringInBlock: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testTwoCaseStringInRHS() {
    // RHS에 단순 string literal만.
    val src = """
      def f() {
        match x {
          case 1 -> "a"
          case "b" -> "c"
        }
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testTwoCaseStringInRHS: accepted=$accepted, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang ParseTest의 testParseGeneric 같은 케이스
  @Test
  fun testParseSimpleGeneric() {
    val src = """
      def viaFor(): i32 {
        let count: i32 = 0
        for x in empty<i32>() {
          count = count + 1
        }
        return count
      }
    """.trimIndent()
    val ctx = parseSimple(src)
    val cdgPath = findMulangCdg()!!
    val parser = Mgroup3Parser(loadOrGenerate(cdgPath))
    val accepted = parser.isAccepted(ctx)
    println("testParseSimpleGeneric: isAccepted=$accepted, mainPaths=${ctx.mainPaths.size}, ${src.length} chars")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang examples 작은 케이스
  @Test
  fun testSingleJoin() {
    // 단일 NJoin: "mut"&Tk
    val cdg = """
      Grammar = "mut"&Tk
      Tk = <Word>
      Word = 'a-zA-Z' '0-9a-zA-Z_'*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("mut")
    val accepted = parser.isAccepted(ctx)
    println("testSingleJoin: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @Test
  fun testJoinedKeywordSeq() {
    // "mut" "def" 이렇게 NJoin이 두 번 등장
    val cdg = """
      Grammar = "mut"&Tk WS "def"&Tk
      Tk = <Word>
      Word = 'a-zA-Z' '0-9a-zA-Z_'*
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("mut def")
    val accepted = parser.isAccepted(ctx)
    println("testJoinedKeywordSeq: accepted=$accepted")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  // mulang의 mut def 패턴 단순화. "mut"&Tk 라는 NJoin이 핵심
  @Test
  fun testJoinedKeyword() {
    // 첫 단계 - Tk는 NLongest만 (Word까지만)
    val cdg = """
      Grammar = ("mut"&Tk WS)? "def"&Tk
      Tk = <Word>
      Word = 'a-zA-Z' '0-9a-zA-Z_'*
      WS = ' '*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    val parser = Mgroup3Parser(data)
    val ctx = parser.parse("mut def")
    val accepted = parser.isAccepted(ctx)
    println("testJoinedKeyword: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
    org.junit.jupiter.api.Assertions.assertTrue(accepted)
  }

  @org.junit.jupiter.api.Disabled
  @Test
  fun testParseMutDef() {
    val cdgPath = findMulangCdg() ?: return
    val data = loadOrGenerate(cdgPath)
    val parser = Mgroup3Parser(data)
    // 더 단순한 케이스: 그냥 class 안에 mut def
    val src = """
      class A {
        mut def f() {}
      }
    """.trimIndent()
    try {
      val ctx = parser.parse(src)
      val accepted = parser.isAccepted(ctx)
      println("testParseMutDef: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
      org.junit.jupiter.api.Assertions.assertTrue(accepted)
    } catch (e: ParsingError) {
      println("testParseMutDef error at $e")
      throw e
    }
  }

  @Test
  fun testParseClassMinimal() {
    val cdgPath = findMulangCdg() ?: return
    val data = loadOrGenerate(cdgPath)
    val parser = Mgroup3Parser(data)
    // class.mu 의 line 21 부근까지의 minimal 한 portion으로 만들어보기
    val src = """
      class List<T> {
        private elements: array<T> = []
        private capacity: u32 = 0
        private length: u32 = 0

        mut def append(elem: T) {
        }
      }
    """.trimIndent()
    try {
      val ctx = parser.parse(src)
      val accepted = parser.isAccepted(ctx)
      println("testParseClassMinimal: accepted=$accepted, mainPaths=${ctx.mainPaths.size}")
      org.junit.jupiter.api.Assertions.assertTrue(accepted)
    } catch (e: ParsingError) {
      println("testParseClassMinimal: $e")
      throw e
    }
  }

  // 동시 테스트는 generator 시간이 느려서 느림. 한꺼번에 모든 example 처리하면서
  // 어떤게 통과/실패인지 빠르게 파악
  @Test
  fun testAllMulangExamples() {
    val cdgPath = findMulangCdg() ?: return
    val data = loadOrGenerate(cdgPath)
    val parser = Mgroup3Parser(data)

    // 가장 작은 example부터 ascending order
    val examples = listOf(
      "../mulang/examples/string_lit.mu",
      "../mulang/examples/if_let.mu",
      "../mulang/examples/exprs.mu",
      "../mulang/examples/oneof.mu",
      "../mulang/examples/match.mu",
      "../mulang/examples/class.mu",
      "../mulang/examples/class_init.mu",
      "../mulang/examples/try_let.mu",
      "../mulang/examples/def_sigs.mu",
      "../mulang/examples/cpp_ast.mu",
      "../mulang/examples/muast.mu",
      "../mulang/examples/annotation_defs.mu",
      "../mulang/examples/annotation_processor.mu",
      "../mulang/examples/rbln_examples.mu",
      "../mulang/examples/ccgen.mu",
    ).sortedBy { Path(it).takeIf { p -> p.exists() }?.let { p -> p.toFile().length() } ?: Long.MAX_VALUE }
    for (path in examples) {
      val srcPath = Path(path)
      if (!srcPath.exists()) {
        println("$path: NOT FOUND")
        continue
      }
      val src = srcPath.readText()
      try {
        val start = System.currentTimeMillis()
        val ctx = parser.parse(src)
        val end = System.currentTimeMillis()
        val accepted = parser.isAccepted(ctx)
        println("$path: ${src.length} chars, ${end - start}ms, accepted=$accepted")
      } catch (e: ParsingError) {
        println("$path: error $e")
      } catch (e: OutOfMemoryError) {
        println("$path: OOM at parsing")
        return
      }
    }
  }

  @org.junit.jupiter.api.Disabled
  @Test
  fun testGenerateMulang() {
    val cdgPath = findMulangCdg()
    if (cdgPath == null) {
      println("mulang.cdg not found, skipping")
      return
    }
    val cdg = Path(cdgPath).readText()
    println("Loading mulang.cdg from: $cdgPath (${cdg.length} chars)")
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "CompileUnit")
    val grammar = grammarAnalysis.ngrammar()
    println("nsymbols=${grammar.nsymbols().size()}, nsequences=${grammar.nsequences().size()}, startSymbol=${grammar.startSymbol()}")
    val gen = Mgroup3ParserGenerator(grammar)
    val start = System.currentTimeMillis()
    val data = gen.generate()
    val end = System.currentTimeMillis()
    println("generation time: ${end - start} ms")
    println("milestoneGroups=${data.milestoneGroupsCount}")
    println("pathRoots=${data.pathRootsCount}")
    println("termActions=${data.termActionsCount}")
    println("tipEdgeActions=${data.tipEdgeActionsCount}")
    println("midEdgeActions=${data.midEdgeActionsCount}")
  }
}
