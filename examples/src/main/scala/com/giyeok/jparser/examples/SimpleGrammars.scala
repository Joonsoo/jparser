package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.gramgram.MetaGrammar

object SimpleGrammars {
    val arrayGrammar: Grammar = MetaGrammar.translateForce("SimpleArrayGrammar",
        """S = '[' WS elems WS ']'
          |elems = elem | elem WS ',' WS elems
          |elem = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val earley1970ae: Grammar = MetaGrammar.translateForce("Earley 1970 AE",
        """E = T | E '+' T
          |T = P | T '*' P
          |P = 'a'
        """.stripMargin)

    val knuth1965_24: Grammar = MetaGrammar.translateForce("Knuth 1965 Grammar 24",
        """S = # | 'a' A 'b' S | 'b' B 'a' S
          |A = # | 'a' A 'b' A
          |B = # | 'b' B 'a' B
        """.stripMargin)
}
