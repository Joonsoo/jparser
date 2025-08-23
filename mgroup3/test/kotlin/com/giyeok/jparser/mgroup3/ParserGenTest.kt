package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.GenNode
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.Curr
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.Next
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.google.common.truth.Truth.assertThat
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

    val gen = Mgroup3ParserGenerator(grammar)

    val startNode = GenNode(grammar.startSymbol(), 0, Curr, Curr)
    val startMgroupId = gen.milestoneGroupIdOf(setOf(startNode))

    val graph = gen.tasks.derivedFrom(GenNode(grammar.startSymbol(), 0, Curr, Curr))
    println(graph)
    assertThat(gen.milestonesOf(graph)).isEmpty()

    val prog = gen.progressibleTermNodesOf(graph)
    println(prog)

    val termGroups = gen.progressibleTermGroupsOf(graph)
    println(termGroups)

    for ((tg, nodes) in termGroups) {
      val g2 = gen.tasks.progressedFrom(graph, nodes, Next)
      println(tg)
      println(g2)
      val milestones = gen.milestonesOf(g2)
      println(milestones)
    }
  }

  @Test
  fun testCmakeDebug() {
    val cdg = """
      TODO
    """.trimIndent()
  }
}
