package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples.metalang.{ExpressionGrammars, LexicalGrammars, MetaLangGrammar, SimpleGrammars}
import com.giyeok.jparser.examples.metalang2.{ExpressionGrammarsMetaLang2, MetaLang2GrammarExamples}
import com.giyeok.jparser.examples.naive.{AmbiguousExamples, GrammarWithExamples, StringExamples}

object Catalogue {
  val metalangExamples: List[MetaLangExample] = List(
    ExpressionGrammars.examples,
    LexicalGrammars.examples,
    MetaLangGrammar.examples,
    SimpleGrammars.examples,
    ExpressionGrammarsMetaLang2.examples,
    MetaLang2GrammarExamples.examples
  ).flatten

  def metaLangsToGrammar(v1Translate: (String, String) => Grammar, v2Translate: (String, String) => Grammar): List[GrammarWithExamples] = {
    metalangExamples map { e =>
      val g = e match {
        case MetaLang1Example(name, grammar, _, _, _) => v1Translate(name, grammar)
        case MetaLang2Example(name, grammar, _, _, _) => v2Translate(name, grammar)
      }
      new GrammarWithExamples with StringExamples with AmbiguousExamples {
        val grammar: Grammar = g
        val correctExamples: Set[String] = e.correctExamples.toSet
        val incorrectExamples: Set[String] = e.incorrectExamples.toSet
        val ambiguousExamples: Set[String] = e.ambiguousExamples.toSet
      }
    }
  }
}
