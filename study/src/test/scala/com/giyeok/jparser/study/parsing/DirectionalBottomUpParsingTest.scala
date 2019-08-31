package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.examples.basics.Fig7_8
import com.giyeok.jparser.study.ContextFreeGrammar
import com.giyeok.jparser.study.parsing.DirectionalBottomUpParsing.EarleyParserNoEmptyRules

object DirectionalBottomUpParsingTest {
    def test(grammar: ContextFreeGrammar, inputs: Seq[Inputs.Input]): Unit = {
        val parser = new EarleyParserNoEmptyRules(grammar)
        parser.initial.itemset.print()
        val result = inputs.foldLeft(parser.initial) { (ctx, i) =>
            val nextctx = parser.proceed(ctx, i)
            nextctx.get
        }

        result.history.horizontalLines(inputs) foreach println
    }

    def main(args: Array[String]): Unit = {
        test(ContextFreeGrammar.convertFrom(Fig7_8), Inputs.fromString("a-a+a"))
    }
}
