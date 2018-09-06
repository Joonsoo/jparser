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
}
