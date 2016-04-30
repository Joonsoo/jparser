package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        JavaScriptTestSuite1.grammars,
        JavaScriptVarDecTestSuite1.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
