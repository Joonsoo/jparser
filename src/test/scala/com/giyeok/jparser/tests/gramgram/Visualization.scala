package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.javascript.JavaScriptGrammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer
import com.giyeok.jparser.tests.GrammarTestCases

object Visualization extends Viewer {
    val allTests: Set[GrammarTestCases] = Set(
        GrammarGrammarTests.tests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
