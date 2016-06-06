package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer
import com.giyeok.jparser.tests.GrammarTestCases

object Visualization extends Viewer {
    val allTests: Set[GrammarTestCases] = Set(
        JavaScriptTestSuite1.tests,
        JavaScriptVarDecTestSuite1.tests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
