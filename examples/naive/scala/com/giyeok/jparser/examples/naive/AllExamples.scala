package com.giyeok.jparser.examples.naive

import com.giyeok.jparser.examples.naive.basics._
import com.giyeok.jparser.examples.naive.javascript.{JavaScriptGrammarExamples1, JavaScriptVarDecTestSuite1}

object AllExamples {
  val grammarWithExamples: List[GrammarWithExamples] = List(
    BackupGrammars.tests,
    GrammarWithExcept.tests,
    JoinGrammars.tests,
    PaperTests.tests,
    ParsingTechniquesTests.tests,
    LongestMatchGrammars.tests,
    GrammarWithLookaheadExcept.tests,
    GrammarWithLookaheadIs.tests,
    RecursiveGrammarSet1.tests,
    SimpleGrammarSet1.tests,
    SimpleGrammarSet2.tests,
    SimpleGrammarSet3.tests,
    JavaScriptVarDecTestSuite1.tests
  ).flatten ++ List(
    JavaScriptGrammarExamples1
  )
}
