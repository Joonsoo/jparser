package com.giyeok.jparser.tests

import com.giyeok.jparser.{Grammar, Inputs}
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}
import com.giyeok.jparser.gramgram.MetaGrammar

object TrickyTests1 extends GrammarWithExamples with StringExamples {
    val grammar: Grammar =
        MetaGrammar.translate("Tricky Grammar 1", """S = 'x' $$"abc" "abc"""").left.get
    val correctExamples: Set[String] = Set("xabc")
    val incorrectExamples: Set[String] = Set()
}

object TrickyTests2 extends GrammarWithExamples with StringExamples {
    val grammar: Grammar =
        MetaGrammar.translate("Tricky Grammar 2", """S = 'x' $!"abc" "abc"""").left.get
    val correctExamples: Set[String] = Set()
    val incorrectExamples: Set[String] = Set("xabc")
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
