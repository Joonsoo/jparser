package com.giyeok.jparser.studio

import com.giyeok.jparser.examples.{AllExamples, AmbiguousExamples, GrammarWithExamples, StringExamples}
import com.giyeok.jparser.examples.metalang3.SimpleExamples

object ParserStudioMain {
  def main(args: Array[String]): Unit = {
    def grammar(name: String, test: GrammarWithExamples): GrammarExample = {
      val (correctSamples, incorrectSamples) = test match {
        case t: StringExamples =>
          (t.correctExamples.toSeq, t.incorrectExamples.toSeq)
        case _ => (Seq(), Seq())
      }
      val ambiguousSamples = test match {
        case t: AmbiguousExamples =>
          t.ambiguousExamples.toSeq
        case _ => Seq()
      }
      GrammarExample(name, test.grammar, correctSamples, incorrectSamples, ambiguousSamples)
    }

    val allExamples = AllExamples.grammarWithExamples // ++
    // AllExamples.metaLangsToGrammar(MetaGrammar.translateForce, MetaLanguage2.grammarSpecToGrammar(_, _).get)

    val initial = SimpleExamples.ex4a
    ParserStudio.start(initial.grammar, initial.correctExamples.head, allExamples map { g => grammar(g.grammar.name, g) })
  }
}
