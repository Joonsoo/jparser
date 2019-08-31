package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.examples.metagram.MetaGramInMetaGram
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Grammar, NGrammar, ParseForestFunc}

object MetaGrammarTests extends GrammarWithExamples with StringExamples {
    val grammar = MetaGrammar

    val metaGrammarText1: String = MetaGramInMetaGram.metaGrammarText1
    val metaGrammarText2: String = MetaGramInMetaGram.metaGrammarText2
    val correctExamples = MetaGramInMetaGram.correctExamples.toSet
    val incorrectExamples = MetaGramInMetaGram.incorrectExamples.toSet

    def main(): Unit = {
        println("===== generated =====")
        println(MetaGrammar.stringify(MetaGrammar))

        val metaGrammar1 = MetaGrammar.translate("Grammar", metaGrammarText1).left.get
        println("===== translated =====")
        println(MetaGrammar.stringify(metaGrammar1))
        println("Meta=meta1", MetaGrammar.rules.toSet == metaGrammar1.rules.toSet)

        val metaGrammar2 = MetaGrammar.translate("Grammar", metaGrammarText2).left.get
        println("===== translated0 =====")
        println(MetaGrammar.stringify(metaGrammar2))
        println("Meta=meta2", MetaGrammar.rules.toSet == metaGrammar2.rules.toSet)

        println("meta1=meta2", metaGrammar1.rules.toSet == metaGrammar2.rules.toSet)

        println("========= parsing metaGrammar1 from metaGrammar1 ========")
        val metaGrammarParser1 = new NaiveParser(NGrammar.fromGrammar(metaGrammar1))
        val metaGrammarParser2 = new NaiveParser(NGrammar.fromGrammar(metaGrammar2))

        def parse(parser: NaiveParser, text: String): Grammar = {
            parser.parse(text) match {
                case Left(ctx) =>
                    new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct() match {
                        case Some(forest) if forest.trees.size == 1 =>
                            println("successful")
                            MetaGrammar.translate("Grammar", forest.trees.head)
                        case forestOpt =>
                            println(forestOpt)
                            println("???")
                            ???
                    }
                case Right(error) =>
                    println(error)
                    ???
            }
        }

        val meta1FromMeta1 = parse(metaGrammarParser1, metaGrammarText1)
        val meta1FromMeta2 = parse(metaGrammarParser1, metaGrammarText2)
        val meta2FromMeta1 = parse(metaGrammarParser2, metaGrammarText1)
        val meta2FromMeta2 = parse(metaGrammarParser2, metaGrammarText2)

        def test(name: String, grammar: Grammar, base: Grammar): Unit = {
            println(s"===== $name =====")
            // println(MetaGrammar.reverse(grammar))
            println(base.rules == grammar.rules)
        }

        test("meta1FromMeta1", meta1FromMeta1, metaGrammar1)
        test("meta1FromMeta2", meta1FromMeta2, metaGrammar1)
        test("meta2FromMeta1", meta2FromMeta1, metaGrammar1)
        test("meta2FromMeta2", meta2FromMeta2, metaGrammar1)
        // println(MetaGrammar.reverse(grammar.get) == metaGrammar1)
    }

}
