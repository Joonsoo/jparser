package com.giyeok.jparser.milestone

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3.MetaLanguage3

object TestExprGrammar {
  def main(args: Array[String]): Unit = {
    println("TestExprGrammar.main")
    val grammar =
      """Expr = Term WS '+' WS Expr
        |     | Term
        |Term = Factor WS '*' WS Term
        |     | Factor
        |Factor = '0-9'
        |     | '(' WS Expr WS ')'
        |WS = ' '*
        |""".stripMargin
    val gram = MetaLanguage3.analyzeGrammar(grammar)
    val data = MilestoneParserGen.generateMilestoneParserData(gram.ngrammar)
    val input = Inputs.fromString("1+1")
    val parser = new MilestoneParser(data, true)
    println("Starting..")
    val finalCtx = parser.parse(input)
  }
}
