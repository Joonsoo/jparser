package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.GenNode
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.Curr
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.Next
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.path.Path
import kotlin.io.path.readText

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

    val gen = Mgroup3ParserGenerator(grammar)
    val generated = gen.generate()
    println(generated)
  }

  @Test
  fun testCmakeDebug() {
    val cdg = Path("examples/metalang3/resources/cmake/cmake_debug.cdg").readText()

    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, "Grammar")
    val grammar = grammarAnalysis.ngrammar()

    val gen = Mgroup3ParserGenerator(grammar)
    val generated = gen.generate()
    println(generated)
  }
}
