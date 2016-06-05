package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.tests.StringSamples

object JavaScriptTest1 extends Grammar with StringSamples {
    val name = JavaScriptGrammar.name + " (1)"
    val rules = JavaScriptGrammar.rules
    val startSymbol = JavaScriptGrammar.startSymbol

    val correctSamples = Set[String](
        "var x = 1;",
        "varx = 1;",
        "iff=1;",
        "a=b\nc=d;",
        "")
    val incorrectSamples = Set[String]()
}

object JavaScriptTest1_1 extends Grammar with StringSamples {
    val name = JavaScriptGrammar.name + " (1_1)"
    val rules = JavaScriptGrammar.rules
    val startSymbol = JavaScriptGrammar.startSymbol

    val correctSamples = Set[String](
        "abc=  function(a){return a+1;}(1);",
        "console.log(function(a){return a+1;}(1));",
        "function x(a) { return a + 1; }",
        "{return a}",
        "var vara = function ifx(a){return(function(y){return (y+1);})(a)};")
    val incorrectSamples = Set[String]()
}

object JavaScriptTestSuite1 {
    val grammars: Set[Grammar with Samples] = Set(
        JavaScriptTest1_1,
        JavaScriptTest1)
}

class JavaScriptTestSuite1 extends BasicParseTest(JavaScriptTestSuite1.grammars)
