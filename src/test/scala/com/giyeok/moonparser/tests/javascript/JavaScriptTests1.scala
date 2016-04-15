package com.giyeok.moonparser.tests.javascript

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.tests.StringSamples

object JavaScriptTest1 extends Grammar with StringSamples {
    val name = JavaScriptGrammar.name + " (1)"
    val rules = JavaScriptGrammar.rules
    val startSymbol = JavaScriptGrammar.startSymbol

    val correctSamples = Set[String](
        "var x = 1;",
        "varx = 1;",
        "iff=1;")
    val incorrectSamples = Set[String]()
}

object JavaScriptTestSuite1 {
    val grammars: Set[Grammar with Samples] = Set(
        JavaScriptTest1)
}

class JavaScriptTestSuite1 extends BasicParseTest(JavaScriptTestSuite1.grammars)
