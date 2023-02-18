package com.giyeok.jparser.tests

import com.giyeok.jparser.examples.basics.{BackupGrammars, GrammarWithExcept, GrammarWithLookaheadExcept, GrammarWithLookaheadIs, JoinGrammars, LongestMatchGrammars, PaperTests, ParsingTechniquesTests, RecursiveGrammarSet1, SimpleGrammarSet1, SimpleGrammarSet2, SimpleGrammarSet3, TrickyLookaheadsTests1}
import com.giyeok.jparser.examples.javascript.{JavaScriptGrammarExamples1, JavaScriptVarDecTestSuite1}

class BackupGrammarTestSuite1 extends BasicParseTest(BackupGrammars.tests)

class ExceptGrammarTestSuite1 extends BasicParseTest(GrammarWithExcept.tests)

class JoinGrammarTestSuite1 extends BasicParseTest(JoinGrammars.tests)

class ParsingTechniquesTestSuite extends BasicParseTest(ParsingTechniquesTests.tests)

class LiteratureTestSuite1 extends BasicParseTest(PaperTests.tests)

class LiteratureTestSuite2 extends BasicParseTest(ParsingTechniquesTests.tests)

class LongestMatchGrammarTestSuite1 extends BasicParseTest(LongestMatchGrammars.tests)

class LookaheadExceptTestSuite1 extends BasicParseTest(GrammarWithLookaheadExcept.tests)

class LookaheadIsTestSuite1 extends BasicParseTest(GrammarWithLookaheadIs.tests)

class RecursiveGrammarTestSuite1 extends BasicParseTest(RecursiveGrammarSet1.tests)

class SimpleGrammarTestSuite1 extends BasicParseTest(SimpleGrammarSet1.tests)

class SimpleGrammarTestSuite2 extends BasicParseTest(SimpleGrammarSet2.tests)

class SimpleGrammarTestSuite3 extends BasicParseTest(SimpleGrammarSet3.tests)

class JavaScriptVarDecTestSuite1 extends BasicParseTest(JavaScriptVarDecTestSuite1.tests)

class JavaScriptTestSuite1 extends BasicParseTest(List(JavaScriptGrammarExamples1))

class TrickyLookaheadsTestSuite1 extends BasicParseTest(TrickyLookaheadsTests1.tests)
