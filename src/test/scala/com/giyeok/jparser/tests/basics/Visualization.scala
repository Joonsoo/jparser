package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.javascript.JavaScriptGrammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer
import com.giyeok.jparser.tests.GrammarTestCases

object Visualization extends Viewer {
    val allTests: Set[GrammarTestCases] = Set(
        SimpleGrammarSet1.tests,
        SimpleGrammarSet2.tests,
        SimpleGrammarSet3.tests,
        RecursiveGrammarSet1.tests,
        GrammarWithExcept.tests,
        GrammarWithLookaheadExcept.tests,
        GrammarWithLookaheadIs.tests,
        BackupGrammars.tests,
        JoinGrammars.tests,
        LongestMatchGrammars.tests).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
