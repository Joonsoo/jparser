package com.giyeok.moonparser.tests.javascript

import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples

object VarDec1Test1 extends VarDecGrammar1 with StringSamples {
    override val name = "JS VarDec Test 1"
    val correctSamples = Set(
        "var abc = 123;\n\nvar xyz = 321; var if = 154;")
    val incorrectSamples = Set(
        "")
}

object VarDec3Test1 extends VarDecGrammar3 with StringSamples {
    override val name = "JS VarDec with Semicolon Backup Test 1"
    val correctSamples = Set(
        "var abc = 123\n\nvar xyz = 321; var if = 154")
    val incorrectSamples = Set(
        "")
}

object JavaScriptVarDecTestSuite1 {
    val grammars: Set[Grammar with Samples] = Set(
        VarDec1Test1,
        VarDec3Test1)
}

class JavaScriptVarDecTestSuite1 extends BasicParseTest(JavaScriptVarDecTestSuite1.grammars)
