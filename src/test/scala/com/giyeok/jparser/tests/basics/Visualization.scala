package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.javascript.JavaScriptGrammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        SimpleGrammarSet1.grammars,
        SimpleGrammarSet2.grammars,
        SimpleGrammarSet3.grammars,
        RecursiveGrammarSet1.grammars,
        GrammarWithExcept.grammars,
        GrammarWithLookaheadExcept.grammars,
        GrammarWithLookaheadIs.grammars,
        BackupGrammars.grammars,
        JoinGrammars.grammars,
        LongestMatchGrammars.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
