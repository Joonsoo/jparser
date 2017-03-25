package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.study.ContextFreeGrammar
import com.giyeok.jparser.tests.basics.Fig6_6

object DirectionalTopDownParsingTest {
    def main(args: Array[String]): Unit = {
        val grammar = ContextFreeGrammar.convertFrom(Fig6_6)
        val (init, parser) = DirectionalTopDownParsing.breadthFirst(grammar, Inputs.fromString("aabb").toList)
        parser.iterate(init, 5)
    }
}
