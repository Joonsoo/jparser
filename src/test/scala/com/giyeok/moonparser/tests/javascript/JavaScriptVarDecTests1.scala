package com.giyeok.moonparser.tests.javascript

import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples

trait JavaScriptVarDecTest extends Grammar with StringSamples {
    val name: String
    val rules = JavaScriptVariableDeclarationGrammar.rules
    val startSymbol = JavaScriptVariableDeclarationGrammar.startSymbol
}

object JavaScriptVarDecTest1 extends JavaScriptVarDecTest {
    val name = "JS VarDec Test 1"
    val correctSamples = Set(
        "var abc = 123\n\nvar xyz = 321; var if = 154")
    val incorrectSamples = Set(
        "")
}

object JavaScriptVarDecTestSuite1 {
    val grammars: Set[Grammar with Samples] = Set(
        JavaScriptVarDecTest1)
}

class JavaScriptVarDecTestSuite1 extends BasicParseTest(JavaScriptVarDecTestSuite1.grammars)
