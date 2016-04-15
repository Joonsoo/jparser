package com.giyeok.moonparser.tests.javascript

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import com.giyeok.moonparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        JavaScriptTestSuite1.grammars,
        JavaScriptVarDecTestSuite1.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
