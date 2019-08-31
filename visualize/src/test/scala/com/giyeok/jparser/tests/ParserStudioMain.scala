package com.giyeok.jparser.tests

import com.giyeok.jparser.examples.basics.{FollowedByGrammar3, JoinGrammar3_1}
import com.giyeok.jparser.examples.{AmbiguousExamples, GrammarWithExamples, StringExamples}
import com.giyeok.jparser.studio.{GrammarExample, ParserStudio}
import com.giyeok.jparser.tests.gramgram.MetaGrammarTests

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

        val examples = AllTestGrammars.allTestGrammars.toSeq map { test => grammar(test.grammar.name, test) } sortBy (_.name)
        val specials = Seq(
            grammar("(Fig 3) CDG Grammar", MetaGrammarTests),
            grammar("(Fig 7) a^n b^n c^n Grammar", JoinGrammar3_1),
            grammar("(Fig 8) a^n b^n c^n Grammar", FollowedByGrammar3)
        )
        ParserStudio.start("", "", specials ++ examples)
    }
}
