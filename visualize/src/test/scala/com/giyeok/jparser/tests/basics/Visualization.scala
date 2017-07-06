package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.TrickyTestSuite1
import com.giyeok.jparser.tests.Viewer

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
        LongestMatchGrammars.tests,
        ParsingTechniquesTests.tests,
        PaperTests.tests
    ).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
