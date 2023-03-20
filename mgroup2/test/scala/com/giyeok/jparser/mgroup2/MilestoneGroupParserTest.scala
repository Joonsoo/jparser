package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.{Inputs, ParseForestFunc}
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import org.scalatest.flatspec.AnyFlatSpec

class MilestoneGroupParserTest extends AnyFlatSpec {
  it should "work" in {
    val grammar =
      """A = B+
        |B: string? = <'a'+ {str($0)} | ' '+ {null}>
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    val parserGen = new MilestoneGroupParserGen(analysis.ngrammar)

    val parserData = parserGen.parserData()

    val parser = new MilestoneGroupParser(parserData).setVerbose()
    val inputs = Inputs.fromString("aaa")
    val result = parser.parseOrThrow(inputs)

    println(result)

    val history = parser.kernelsHistory(result)
    history.foreach { kernels =>
      println(kernels.toList.sorted)
    }

    val parseForest = new ParseTreeConstructor2(ParseForestFunc)(analysis.ngrammar)(inputs, history.map(Kernels))
      .reconstruct().get
    println(parseForest.trees.size)

    val valuefy = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
    println(valuefy.valuefyStart(parseForest.trees.head))
  }
}
