package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// mgroup3 generator/parser의 고급 케이스 테스트
// (recursive, nullable, lookahead/except/join, deep reduce chain 등)
class Mgroup3ParserAdvancedTest {
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
      "expected '$text' to be accepted, mainPaths=${ctx.mainPaths.size}, condPaths=${ctx.condPaths.size}",
    )
  }

  private fun assertRejects(parser: Mgroup3Parser, text: String) {
    try {
      val ctx = parser.parse(text)
      assertFalse(parser.isAccepted(ctx), "expected '$text' to be rejected")
    } catch (e: ParsingError) {
      // OK
    }
  }

  // === Recursive ===

  @Test
  fun testRightRecursive() {
    val parser = makeParser(
      """
        Grammar = 'a' Grammar
                | 'b'
      """.trimIndent()
    )
    assertAccepts(parser, "b")
    assertAccepts(parser, "ab")
    assertAccepts(parser, "aab")
    assertAccepts(parser, "aaaaaab")
    assertAccepts(parser, "aaaaaaaaaaaaaab")
    assertRejects(parser, "")
    assertRejects(parser, "a")
    assertRejects(parser, "ba")
    assertRejects(parser, "abb")
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
    assertAccepts(parser, "(((((1)))))")
    assertAccepts(parser, "(((((9)))))")
    assertRejects(parser, "(1")
    assertRejects(parser, "1)")
    assertRejects(parser, "()")
    assertRejects(parser, "")
    assertRejects(parser, "((1)")
    assertRejects(parser, "(1))")
  }

  @Test
  fun testParensWithOps() {
    val parser = makeParser(
      """
        Expr = '(' Expr Op Expr ')' | '0-9'
        Op = '+' | '*'
      """.trimIndent(),
      startName = "Expr",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "(1+2)")
    assertAccepts(parser, "(1*2)")
    assertAccepts(parser, "(1*(2+3))")
    assertAccepts(parser, "((1+2)*(3+4))")
    assertAccepts(parser, "(((1+2)+(3+4))*((5+6)+(7+8)))")
    assertRejects(parser, "(1+2")
    assertRejects(parser, "(1)")
    assertRejects(parser, "(+)")
  }

  @Test
  fun testDeepRightRecursive() {
    val parser = makeParser(
      """
        L = 'a' L | E
        E = 'b'
      """.trimIndent(),
      startName = "L",
    )
    assertAccepts(parser, "b")
    assertAccepts(parser, "aaaaaaaaaaaaaaaab")
    assertRejects(parser, "")
  }

  // === Plain arith (no whitespace) ===

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
    assertAccepts(parser, "1*2")
    assertAccepts(parser, "1+2+3")
    assertAccepts(parser, "1*2*3")
    assertAccepts(parser, "1+2*3")
    assertAccepts(parser, "1*2+3")
    assertAccepts(parser, "1+2+3+4+5+6")
    assertAccepts(parser, "1*2+3*4+5*6")
    assertRejects(parser, "")
    assertRejects(parser, "+")
    assertRejects(parser, "1+")
    assertRejects(parser, "+1")
    assertRejects(parser, "1++2")
    assertRejects(parser, "*")
  }

  @Test
  fun testArithWithWS() {
    // WS = ' '* 같은 nullable nonterm이 sequence 중간에 있는 경우
    val parser = makeParser(
      """
        Expr = Term WS '+' WS Expr
             | Term
        Term = Factor WS '*' WS Term
             | Factor
        Factor = '0-9'
              | '(' WS Expr WS ')'
        WS = ' '*
      """.trimIndent(),
      startName = "Expr",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "1+2")
    assertAccepts(parser, "1 + 2")
    assertAccepts(parser, "1  +  2")
    assertAccepts(parser, "1+2*3")
    assertAccepts(parser, "1 + 2 * 3")
    assertAccepts(parser, "(1+2)")
    assertAccepts(parser, "( 1 + 2 )")
    assertAccepts(parser, "( 1 + 2 ) * 3")
    assertAccepts(parser, "((1 + 2) * (3 + 4))")
    assertRejects(parser, "1 +")
    assertRejects(parser, "+ 1")
  }

  // === Join (&) ===

  // NSequence 안에 NRepeat 있는 케이스. 단독 (no join) 테스트.
  @Test
  fun testSeqWithRepeat() {
    val parser = makeParser(
      """
        Grammar = 'a-zA-Z' '0-9a-zA-Z_'*
      """.trimIndent()
    )
    assertAccepts(parser, "a")
    assertAccepts(parser, "ab")
    assertAccepts(parser, "abc")
    assertAccepts(parser, "abc123")
    assertRejects(parser, "")
    assertRejects(parser, "1")
  }

  @Test
  fun testJoin() {
    // body가 join과 동시에 매치되어야
    val parser = makeParser(
      """
        Grammar = "mut"&Word
        Word = 'a-zA-Z'+
      """.trimIndent()
    )
    assertAccepts(parser, "mut")
    assertRejects(parser, "mu")
    assertRejects(parser, "muta")
    assertRejects(parser, "")
  }

  @Test
  fun testJoinWithProxyDebug() {
    val cdg = """
        Grammar = "mut"&Tk
        Tk = <Word>
        Word = 'a-zA-Z' '0-9a-zA-Z_'*
    """.trimIndent()
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    val data = gen.generate()
    println("startSymbolId=${data.startSymbolId}")
    val nsymbols = scala.jdk.javaapi.CollectionConverters.asJava(grammar.nsymbols())
    nsymbols.entries.sortedBy { it.key as Int }.forEach { (id, sym) -> println("  N $id -> $sym") }
    val nseqs = scala.jdk.javaapi.CollectionConverters.asJava(grammar.nsequences())
    nseqs.entries.sortedBy { it.key as Int }.forEach { (id, seq) -> println("  Seq $id -> $seq") }
    data.milestoneGroupsMap.entries.sortedBy { it.key }.forEach { (id, mg) ->
      println("  mgroup $id -> ${mg.kernelsList.map { "(${it.symbolId},${it.pointer})" }}")
    }
    data.pathRootsMap.entries.sortedBy { it.key }.forEach { (id, info) ->
      println("  root $id -> mgroup=${info.milestoneGroupId} hasSelf=${info.hasSelfFinishAcceptCondition()} initialConds=${info.initialCondSymbolIdsList}")
    }
    data.termActionsMap.entries.sortedBy { it.key }.forEach { (id, actions) ->
      println("  termActions[$id]:")
      actions.actionsList.forEach { action ->
        val tg = action.termGroup.charsGroup.chars
        println("    on '$tg':")
        action.termAction.replaceAndAppendsList.forEach { rea ->
          println("      replaceAndAppend: replace=(${rea.replace.symbolId},${rea.replace.pointer}) append=mg${rea.append.milestoneGroupId} acc=${rea.append.acceptCondition.toString().replace("\n", " ")}")
        }
        action.termAction.replaceAndProgressesList.forEach { rap ->
          println("      replaceAndProgress: mg${rap.replaceMilestoneGroupId} acc=${rap.acceptCondition.toString().replace("\n", " ")}")
        }
      }
    }
    data.tipEdgeActionsList.forEach { tea ->
      println("  tipEdge (${tea.parent.symbolId},${tea.parent.pointer})->mg${tea.tipGroupId}")
      tea.edgeAction.appendMilestoneGroupsList.forEach { append ->
        println("    append: mg${append.milestoneGroupId} acc=${append.acceptCondition.toString().replace("\n", " ")}")
      }
      if (tea.edgeAction.hasStartNodeProgress()) {
        println("    startNodeProgress: ${tea.edgeAction.startNodeProgress.toString().replace("\n", " ")}")
      }
    }

    val parser = Mgroup3Parser(data)
    var ctx = parser.initCtx()
    println("init: mainPaths=${ctx.mainPaths}, condPaths=${ctx.condPaths}")
    val text = "mut"
    text.forEachIndexed { idx, c ->
      try {
        ctx = parser.parseStep(ctx, c, idx == text.length - 1)
        println("after '$c' (gen=${ctx.gen}):")
        ctx.mainPaths.forEach { p ->
          val mp = p.milestonePath
          val parts = mutableListOf<String>()
          var cur = mp
          while (cur != null) {
            parts.add("(${cur.milestone.symbolId},${cur.milestone.pointer},gen=${cur.gen})")
            cur = cur.parent
          }
          println("  main: tip=mg${p.tipGroupId} mp=[${parts.joinToString(" -> ")}] cond=${p.acceptCondition}")
        }
        ctx.condPaths.forEach { (root, paths) ->
          println("  cond[$root]:")
          paths.forEach { p ->
            val mp = p.milestonePath
            val parts = mutableListOf<String>()
            var cur = mp
            while (cur != null) {
              parts.add("(${cur.milestone.symbolId},${cur.milestone.pointer},gen=${cur.gen})")
              cur = cur.parent
            }
            println("    tip=mg${p.tipGroupId} mp=[${parts.joinToString(" -> ")}] cond=${p.acceptCondition}")
          }
        }
        println("  history.last: condPathFinishes=${ctx.history.last().condPathFinishes}")
        println("  history.last finishedKernels: ${ctx.history.last().finishedKernels}")
      } catch (e: Exception) {
        println("error: $e"); return
      }
    }
    println("isAccepted=${parser.isAccepted(ctx)}")
  }

  @Test
  fun testJoinWithProxy() {
    // join이 NProxy를 통하는 경우 (Tk = <Word>)
    val parser = makeParser(
      """
        Grammar = "mut"&Tk
        Tk = <Word>
        Word = 'a-zA-Z' '0-9a-zA-Z_'*
      """.trimIndent()
    )
    assertAccepts(parser, "mut")
    assertRejects(parser, "mu")
    assertRejects(parser, "muta")
  }

  @Test
  fun testJoinSeq() {
    val parser = makeParser(
      """
        Grammar = "mut"&Word WS "def"&Word
        Word = 'a-zA-Z'+
        WS = ' '*
      """.trimIndent()
    )
    assertAccepts(parser, "mut def")
    assertAccepts(parser, "mut  def")
    assertRejects(parser, "mut")
    assertRejects(parser, "muta def")
  }

  // === Lookahead ===

  @Test
  fun testLookaheadExcept() {
    val parser = makeParser(
      """
        Grammar = 'a' !'b' Tail
        Tail = '0-9'
      """.trimIndent()
    )
    assertAccepts(parser, "a1")
    assertAccepts(parser, "a9")
    assertRejects(parser, "ab")
    assertRejects(parser, "a")
  }

  // TODO: lookaheadIs (& syntax) 정확한 metalang3 문법 확인 필요
  // @Test fun testLookaheadIs() { ... }

  // === Except (-) ===

  @Test
  fun testExcept() {
    val parser = makeParser("Grammar = 'a-z' - 'a'")
    assertAccepts(parser, "b")
    assertAccepts(parser, "z")
    assertAccepts(parser, "m")
    assertRejects(parser, "a")
    assertRejects(parser, "")
    assertRejects(parser, "ab")
  }

  @Test
  fun testExceptMultiple() {
    val parser = makeParser(
      """
        Grammar = 'a-z' - Vowel
        Vowel = 'a' | 'e' | 'i' | 'o' | 'u'
      """.trimIndent()
    )
    assertAccepts(parser, "b")
    assertAccepts(parser, "z")
    assertRejects(parser, "a")
    assertRejects(parser, "e")
    assertRejects(parser, "i")
    assertRejects(parser, "o")
    assertRejects(parser, "u")
  }

  // === Repeat with non-trivial inner ===

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
    assertAccepts(parser, "ababab")
    assertAccepts(parser, "ababababab")
    assertRejects(parser, "a")
    assertRejects(parser, "aba")
    assertRejects(parser, "ba")
    assertRejects(parser, "abc")
  }

  @Test
  fun testRepeat1NonterminalInner() {
    val parser = makeParser(
      """
        Grammar = AB+
        AB = 'a' 'b'
      """.trimIndent()
    )
    assertRejects(parser, "")
    assertAccepts(parser, "ab")
    assertAccepts(parser, "abab")
    assertAccepts(parser, "ababab")
    assertRejects(parser, "a")
    assertRejects(parser, "aba")
  }

  @Test
  fun testRepeatChoiceLong() {
    val parser = makeParser("Grammar = ('a' | 'b' | 'c')*")
    assertAccepts(parser, "")
    assertAccepts(parser, "abcabcabc")
    assertAccepts(parser, "aaabbbccc")
    assertRejects(parser, "abd")
  }

  // === 복잡 시퀀스 ===

  @Test
  fun testIdentifier() {
    val parser = makeParser(
      """
        Ident = Letter LetterOrDigit*
        Letter = 'a-zA-Z'
        LetterOrDigit = 'a-zA-Z0-9'
      """.trimIndent(),
      startName = "Ident",
    )
    assertAccepts(parser, "a")
    assertAccepts(parser, "abc")
    assertAccepts(parser, "abc123")
    assertAccepts(parser, "Hello")
    assertAccepts(parser, "x1y2z3")
    assertRejects(parser, "")
    assertRejects(parser, "1abc")
    assertRejects(parser, "abc!")
  }

  @Test
  fun testStringLiteral() {
    // simplified string literal: "..." with non-quote chars (escape 처리는 향후)
    val parser = makeParser(
      """
        Str = '"' Char* '"'
        Char = 'a-zA-Z0-9 '
      """.trimIndent(),
      startName = "Str",
    )
    assertAccepts(parser, "\"\"")
    assertAccepts(parser, "\"abc\"")
    assertAccepts(parser, "\"hello world\"")
    assertAccepts(parser, "\"123\"")
    assertRejects(parser, "\"")
    assertRejects(parser, "\"abc")
    assertRejects(parser, "abc\"")
  }

  // === Recursive list ===

  @Test
  fun testCommaList() {
    val parser = makeParser(
      """
        List = Item ',' List
             | Item
        Item = '0-9'
      """.trimIndent(),
      startName = "List",
    )
    assertAccepts(parser, "1")
    assertAccepts(parser, "1,2")
    assertAccepts(parser, "1,2,3")
    assertAccepts(parser, "1,2,3,4,5,6,7,8,9,0")
    assertRejects(parser, "")
    assertRejects(parser, ",")
    assertRejects(parser, "1,")
    assertRejects(parser, ",1")
    assertRejects(parser, "1,,2")
  }

  @Test
  fun testEmptyOrList() {
    // empty alternative을 'a'? 같은 형태로 단순 표현
    val parser = makeParser(
      """
        List = Items*
        Items = Item ',' Items
              | Item
        Item = '0-9'
      """.trimIndent(),
      startName = "List",
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "1")
    assertAccepts(parser, "1,2,3")
  }

  // === Mixed conditions (lookahead + repeat) ===

  @Test
  fun testKeywordExcludingIdent() {
    // 'a-z' 한 글자만 매치하고 그게 'i'면 안 됨
    val parser = makeParser(
      """
        Ident = Letter - Keyword
        Letter = 'a-z'
        Keyword = 'i'
      """.trimIndent(),
      startName = "Ident",
    )
    assertAccepts(parser, "a")
    assertAccepts(parser, "b")
    assertAccepts(parser, "z")
    assertRejects(parser, "i")
  }

  // === Cond path 누적 ===

  @Test
  fun testNestedExcept() {
    val parser = makeParser(
      """
        Grammar = ('a-z' - 'a')*
      """.trimIndent()
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "b")
    assertAccepts(parser, "bcd")
    assertAccepts(parser, "xyz")
    assertAccepts(parser, "bcdefghijklmnopqrstuvwxyz")
    assertRejects(parser, "a")
    assertRejects(parser, "abc")
    assertRejects(parser, "bca")
    assertRejects(parser, "xyza")
    assertRejects(parser, "ab")
    assertRejects(parser, "ba")
  }

  @Test
  fun testNestedExceptMultipleSymbols() {
    val parser = makeParser(
      """
        Grammar = ('a-z' - Forbidden)*
        Forbidden = 'a' | 'e' | 'i' | 'o' | 'u'
      """.trimIndent()
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "b")
    assertAccepts(parser, "bcd")
    assertAccepts(parser, "xyz")
    assertRejects(parser, "a")
    assertRejects(parser, "e")
    assertRejects(parser, "i")
    assertRejects(parser, "o")
    assertRejects(parser, "u")
    assertRejects(parser, "bca")
    assertRejects(parser, "xyzeabc")
  }

  // === 다양한 grammar ===

  // === Multiple lookaheadExcept ===

  @Test
  fun testLookaheadExceptInRepeat() {
    val parser = makeParser(
      """
        Grammar = (!'b' 'a-z')*
      """.trimIndent()
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "a")
    assertAccepts(parser, "ac")
    assertAccepts(parser, "acdef")
    assertAccepts(parser, "acdefghijklmnopqrstuvwxyz")
    assertRejects(parser, "b")
    assertRejects(parser, "ab")
    assertRejects(parser, "acb")
    assertRejects(parser, "abc")
  }

  // cmake-style: bracket open ([[) shouldn't match unquoted_argument
  @Test
  fun testCmakeStyleBracketGuard() {
    // body starts with non-'[' chars after which any chars except '[[' allowed
    // simplified: word = (char - '[')+ but '[' followed by '[' shouldn't match
    val parser = makeParser(
      """
        Word = (Char - BracketOpen)+
        Char = 'a-z[]'
        BracketOpen = '[' '['
      """.trimIndent(),
      startName = "Word",
    )
    // 'abc' OK, '[' single OK, '[a' OK, '[[' should be rejected
    assertAccepts(parser, "abc")
    assertAccepts(parser, "a")
    assertAccepts(parser, "abcdef")
  }

  @Test
  fun testLeftBracketKeyword() {
    // simulates `[[` -> bracket open keyword vs `[` followed by something
    val parser = makeParser(
      """
        Grammar = (Char - '[')*
        Char = 'a-zA-Z[]'
      """.trimIndent()
    )
    assertAccepts(parser, "")
    assertAccepts(parser, "abc")
    assertAccepts(parser, "abc]")
    assertAccepts(parser, "abc]def")
  }

  @Test
  fun testIfElse() {
    val parser = makeParser(
      """
        Stmt = 'i' 'f' Cond 't' 'h' 'e' 'n' Stmt 'e' 'l' 's' 'e' Stmt
             | 'i' 'f' Cond 't' 'h' 'e' 'n' Stmt
             | 's'
        Cond = '0-9'
      """.trimIndent(),
      startName = "Stmt",
    )
    assertAccepts(parser, "s")
    assertAccepts(parser, "if1thens")
    assertAccepts(parser, "if1thenselses")
    assertAccepts(parser, "if1thenif2thenselses")
    assertRejects(parser, "")
    assertRejects(parser, "if")
  }
}
