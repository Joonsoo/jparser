package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.examples.GrammarWithExamples
import com.giyeok.jparser.examples.basics.{BackupGrammars, GrammarWithExcept, GrammarWithLookaheadExcept, GrammarWithLookaheadIs, JoinGrammars, LongestMatchGrammars, PaperTests, ParsingTechniquesTests, RecursiveGrammarSet1, SimpleGrammarSet1, SimpleGrammarSet2, SimpleGrammarSet3}
import com.giyeok.jparser.tests.TrickyTestSuite1
import com.giyeok.jparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[GrammarWithExamples] = Set(
        SimpleGrammarSet1.tests,
        SimpleGrammarSet2.tests,
        SimpleGrammarSet3.tests,
        RecursiveGrammarSet1.tests,
        GrammarWithExcept.tests,
        GrammarWithLookaheadExcept.tests,
        GrammarWithLookaheadIs.tests,
        BackupGrammars.tests,
        JoinGrammars.tests,
        LongestMatchGrammars.tests,
        ParsingTechniquesTests.tests,
        PaperTests.tests,
        TrickyTestSuite1.tests
    ).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
