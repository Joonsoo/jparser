package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.study.ContextFreeGrammar
import com.giyeok.jparser.study.parsing.DirectionalBottomUpParsing.EarleyParser
import com.giyeok.jparser.tests.basics.Fig7_8

object DirectionalBottomUpParsingTest {
    def main(args: Array[String]): Unit = {
        val grammar = ContextFreeGrammar.convertFrom(Fig7_8)
        val parser = new EarleyParser(grammar)
        val inputs = Inputs.fromString("a-a+a")
        parser.initial.itemset.print()
        val result = inputs.foldLeft(parser.initial) { (ctx, i) =>
            val nextctx = parser.proceed(ctx, i)
            nextctx.get.itemset.print()
            nextctx.get
        }

        result.history.horizontalLines() foreach println
    }
}
