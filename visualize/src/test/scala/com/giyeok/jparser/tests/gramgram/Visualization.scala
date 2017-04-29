package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[GrammarTestCases] = Set(
        GrammarGrammarTests.tests
    ).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
