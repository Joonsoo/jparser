package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[GrammarTestCases] = Set(
        JavaScriptTestSuite1.tests,
        JavaScriptVarDecTestSuite1.tests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
