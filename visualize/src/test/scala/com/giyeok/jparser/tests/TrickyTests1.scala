package com.giyeok.jparser.tests

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.gramgram.MetaGrammar

object TrickyTests1 extends GrammarTestCases with StringSamples {
    val grammar: Grammar =
        MetaGrammar.parseTextDefinition("Tricky Grammar 1", """S = 'x' $$"abc" "abc"""").left.get
    val correctSamples: Set[String] = Set("xabc")
    val incorrectSamples: Set[String] = Set()
}

object TrickyTests2 extends GrammarTestCases with StringSamples {
    val grammar: Grammar =
        MetaGrammar.parseTextDefinition("Tricky Grammar 2", """S = 'x' $!"abc" "abc"""").left.get
    val correctSamples: Set[String] = Set()
    val incorrectSamples: Set[String] = Set("xabc")
}

class TrickyTestSuite1 extends BasicParseTest(Seq(TrickyTests1, TrickyTests2))

object TrickyTestSuite1 {
    val tests = Seq(
        TrickyTests1,
        TrickyTests2
    )
    def main(args: Array[String]): Unit = {
        new TrickyTestSuite1().parse(TrickyTests1, Inputs.fromString("xabc"))
    }
}
