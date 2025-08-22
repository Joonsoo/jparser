package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.GenNode
import com.giyeok.jparser.mgroup3.gen.GenParsingTaskRunner
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Test

class ParserGenTest {
  @Test
  fun testPlainGrammar() {
    val cdg = """
      Expr = Term WS '+' WS Expr
           | Term
      Term = Factor WS '*' WS Term
           | Factor
      Factor = '0-9'
           | '(' WS Expr WS ')'
      WS = ' '*
    """.trimIndent()

    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()

    val gen = GenParsingTaskRunner(grammar)
    val graph = gen.derivedFrom(GenNode(grammar.startSymbol(), 0))
    println(graph)
  }

  @Test
  fun testCmakeDebug() {
    val cdg = """
      TODO
    """.trimIndent()
  }
}
