package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.GrammarTestCases

object JavaScriptTest1 extends GrammarTestCases with StringSamples {
    val grammar = JavaScriptGrammar

    val correctSamples = Set[String](
        "",
        "var x = 1;",
        "varx = 1;",
        "iff=1;",
        "a=b\nc=d;",
        "abc=  function(a){return a+1;}(1);",
        "console.log(function(a){return a+1;}(1));",
        "function x(a) { return a + 1; }",
        "{return a}",
        "var vara = function ifx(a){return(function(y){return (y+1);})(a)};")
    val incorrectSamples = Set[String]()
}

object JavaScriptTestSuite1 {
    val tests: Set[GrammarTestCases] = Set(
        JavaScriptTest1)
}

class JavaScriptTestSuite1 extends BasicParseTest(JavaScriptTestSuite1.tests)
