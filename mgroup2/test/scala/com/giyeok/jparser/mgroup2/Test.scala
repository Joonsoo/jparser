package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3.MetaLanguage3
import org.scalatest.flatspec.AnyFlatSpec

class Test extends AnyFlatSpec {
  it should "work" in {
    val grammar =
      """A = B+
        |B = <'a'+ | ' '+>
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    val parserGen = new MilestoneGroupParserGen(analysis.ngrammar)

    val parserData = parserGen.parserData()

    val parser = new MilestoneGroupParser(parserData).setVerbose()
    val inputs = Inputs.fromString("aaa")
    val result = parser.parse(inputs)
    println(result)
  }
}
