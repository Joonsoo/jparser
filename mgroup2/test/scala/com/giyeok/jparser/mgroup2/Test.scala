package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3.MetaLanguage3
import org.scalatest.flatspec.AnyFlatSpec

class Test extends AnyFlatSpec {
  it should "work" in {
    val grammar =
      """Expr: Expr = Term WS '+' WS Expr {BinOp(op:%Op=%Add, lhs=$0, rhs=$4)}
        |           | Term
        |Term: Term = Factor WS '*' WS Term {BinOp(op=%Mul, lhs=$0, rhs=$4)}
        |           | Factor
        |Factor: Factor = '0-9' {Number(value=str($0))}
        |               | '(' WS Expr WS ')' {Paren(body=$2)}
        |WS = ' '*
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    val parserGen = new MilestoneGroupParserGen(analysis.ngrammar)

    val parserData = parserGen.parserData()

    val parser = new MilestoneGroupParser(parserData).setVerbose()
    val inputs = Inputs.fromString("1+(2*3)*4")
    val result = parser.parse(inputs)
    println(result)
  }
}
