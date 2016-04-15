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
        "iff=1;",
        //"a=b\nc=d;",
        "")
    val incorrectSamples = Set[String]()
}

object JavaScriptTest1_1 extends Grammar with StringSamples {
    val name = JavaScriptGrammar.name + " (1_1)"
    val rules = JavaScriptGrammar.rules
    val startSymbol = JavaScriptGrammar.startSymbol

    val correctSamples = Set[String](
        "abc=  function(a){return a+1;}(1);",
        "c.log(function(a){return a+1;}(1));")
    val incorrectSamples = Set[String]()
}

object JavaScriptTestSuite1 {
    val grammars: Set[Grammar with Samples] = Set(
        JavaScriptTest1_1,
        JavaScriptTest1)
}

class JavaScriptTestSuite1 extends BasicParseTest(JavaScriptTestSuite1.grammars)
