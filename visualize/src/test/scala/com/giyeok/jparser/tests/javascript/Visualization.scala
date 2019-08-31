package com.giyeok.jparser.tests.javascript

import com.giyeok.jparser.examples.GrammarWithExamples
import com.giyeok.jparser.examples.javascript.{JavaScriptGrammarExamples1, JavaScriptVarDecTestSuite1}
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[GrammarWithExamples] = Set(
        Set(JavaScriptGrammarExamples1),
        JavaScriptVarDecTestSuite1.tests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
