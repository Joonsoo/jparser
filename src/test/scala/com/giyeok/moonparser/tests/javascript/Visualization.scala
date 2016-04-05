package com.giyeok.moonparser.tests.javascript

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import com.giyeok.moonparser.tests.Viewer

object Visualization extends Viewer {
    val jsGrammar: Grammar with Samples = new Grammar with StringSamples {
        val name = JavaScriptGrammar.name
        val rules = JavaScriptGrammar.rules
        val startSymbol = JavaScriptGrammar.startSymbol

        val correctSamples = Set("var x = 1;", "iff=1;")
        val incorrectSamples = Set("sasd")
    }

    val allTests: Set[Grammar with Samples] = Set(
        Set(jsGrammar),
        JavaScriptVarDecTestSuite1.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
