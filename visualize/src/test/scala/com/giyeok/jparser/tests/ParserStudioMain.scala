package com.giyeok.jparser.tests

import com.giyeok.jparser.studio.GrammarExample
import com.giyeok.jparser.studio.ParserStudio

object ParserStudioMain {
    def main(args: Array[String]): Unit = {
        val examples = AllTestGrammars.allTestGrammars.toSeq map { test =>
            val (correctSamples, incorrectSamples) = test match {
                case t: StringSamples =>
                    (t.correctSamples.toSeq, t.incorrectSamples.toSeq)
                case _ => (Seq(), Seq())
            }
            val ambiguousSamples = test match {
                case t: AmbiguousSamples =>
                    t.ambiguousSamples.toSeq
                case _ => Seq()
            }
            GrammarExample(test.grammar, correctSamples, incorrectSamples, ambiguousSamples)
        }
        ParserStudio.start(examples)
    }
}
