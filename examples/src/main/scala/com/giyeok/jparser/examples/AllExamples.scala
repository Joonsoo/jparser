//package com.giyeok.jparser.examples
//
//import com.giyeok.jparser.Grammar
//import com.giyeok.jparser.examples.GrammarWithExamples
//import com.giyeok.jparser.examples.MetaLangExample
//import com.giyeok.jparser.examples.basics.BackupGrammars
//import com.giyeok.jparser.examples.basics.GrammarWithExcept
//import com.giyeok.jparser.examples.basics.JoinGrammars
//import com.giyeok.jparser.examples.basics.PaperTests
//import com.giyeok.jparser.examples.basics.ParsingTechniquesTests
//import com.giyeok.jparser.examples.basics.LongestMatchGrammars
//import com.giyeok.jparser.examples.basics.GrammarWithLookaheadExcept
//import com.giyeok.jparser.examples.basics.GrammarWithLookaheadIs
//import com.giyeok.jparser.examples.basics.RecursiveGrammarSet1
//import com.giyeok.jparser.examples.basics.SimpleGrammarSet1
//import com.giyeok.jparser.examples.basics.SimpleGrammarSet2
//import com.giyeok.jparser.examples.basics.SimpleGrammarSet3
//import com.giyeok.jparser.examples.basics.BackupGrammars
//import com.giyeok.jparser.examples.javascript.JavaScriptVarDecTestSuite1
//import com.giyeok.jparser.examples.javascript.JavaScriptGrammarExamples1
//import com.giyeok.jparser.examples.metalang.ExpressionGrammars
//import com.giyeok.jparser.examples.metalang.LexicalGrammars
//import com.giyeok.jparser.examples.metalang.MetaLangGrammar
//import com.giyeok.jparser.examples.metalang.SimpleGrammars
//import com.giyeok.jparser.examples.metalang2.ExpressionGrammarsMetaLang2
//import com.giyeok.jparser.examples.metalang2.MetaLang2GrammarExamples
//
//object AllExamples {
//    val grammarWithExamples: List[GrammarWithExamples] = List(
//        BackupGrammars.tests,
//        GrammarWithExcept.tests,
//        JoinGrammars.tests,
//        PaperTests.tests,
//        ParsingTechniquesTests.tests,
//        LongestMatchGrammars.tests,
//        GrammarWithLookaheadExcept.tests,
//        GrammarWithLookaheadIs.tests,
//        RecursiveGrammarSet1.tests,
//        SimpleGrammarSet1.tests,
//        SimpleGrammarSet2.tests,
//        SimpleGrammarSet3.tests,
//        JavaScriptVarDecTestSuite1.tests
//    ).flatten ++ List(
//        JavaScriptGrammarExamples1
//    )
//
//    val metalangExamples: List[MetaLangExample] = List(
//        ExpressionGrammars.examples,
//        LexicalGrammars.examples,
//        MetaLangGrammar.examples,
//        SimpleGrammars.examples,
//        ExpressionGrammarsMetaLang2.examples,
//        MetaLang2GrammarExamples.examples
//    ).flatten
//
//    // TODO add examples from metalang3
//
//    def metaLangsToGrammar(v1Translate: (String, String) => Grammar, v2Translate: (String, String) => Grammar): List[GrammarWithExamples] = {
//        metalangExamples map { e =>
//            val g = e match {
//                case MetaLang1Example(name, grammar, _, _, _) => v1Translate(name, grammar)
//                case MetaLang2Example(name, grammar, _, _, _) => v2Translate(name, grammar)
//            }
//            new GrammarWithExamples with StringExamples with AmbiguousExamples {
//                val grammar: Grammar = g
//                val correctExamples: Set[String] = e.correctExamples.toSet
//                val incorrectExamples: Set[String] = e.incorrectExamples.toSet
//                val ambiguousExamples: Set[String] = e.ambiguousExamples.toSet
//            }
//        }
//    }
//}
