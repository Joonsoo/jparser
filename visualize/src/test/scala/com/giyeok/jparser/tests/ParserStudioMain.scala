package com.giyeok.jparser.tests

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples1.{FollowedByGrammar3, JoinGrammar3_1, StringSamples}
import com.giyeok.jparser.examples2.{ExpressionGrammar1, LexicalGrammar2, LexicalGrammar3, MetaGrammarSamples}
import com.giyeok.jparser.metagrammar.MetaGrammar
import com.giyeok.jparser.studio.{GrammarExample, ParserStudio}

object ParserStudioMain {
    def main(args: Array[String]): Unit = {
        def grammar(name: String, grammar: Grammar, samples: StringSamples): GrammarExample = {
            val (correctSamples, incorrectSamples, ambiguousSamples) = samples match {
                case t: StringSamples =>
                    (t.validInputs.toSeq, t.invalidInputs.toSeq, t.ambiguousInputs.toSeq)
                case _ => (Seq(), Seq(), Seq())
            }
            GrammarExample(name, grammar, correctSamples, incorrectSamples, ambiguousSamples)
        }

        val examples = AllTestGrammars.allTestGrammars.toSeq map { pair => grammar(pair grammar.name, test) } sortBy {_.name}
        val specials = Seq(
            grammar("(Fig 1) Expression Grammar", ExpressionGrammar1.grammar, ExpressionGrammar1),
            grammar("(Fig 2) Lexical Grammar", LexicalGrammar3.grammar, LexicalGrammar3),
            grammar("(Fig 3) CDG Grammar", MetaGrammar, MetaGrammarSamples),
            grammar("(Fig 7) a^n b^n c^n Grammar", JoinGrammar3_1, JoinGrammar3_1),
            grammar("(Fig 8) a^n b^n c^n Grammar", FollowedByGrammar3, FollowedByGrammar3)
        )
        ParserStudio.start(ExpressionGrammar0Tests.expressionGrammar0Text, ExpressionGrammar0Tests.correctSamples.head, specials ++ examples)
    }
}
