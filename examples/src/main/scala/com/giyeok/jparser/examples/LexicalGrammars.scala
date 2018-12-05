package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.gramgram.MetaGrammar

class LexicalGrammars {
    val keywordAndIf: Grammar = MetaGrammar.translateForce("Kw/if",
        """S = T*
          |T = Kw | Id | Ws
          |Kw = N & "if"
          |Id = N - Kw
          |N = <{a-z}+>
          |Ws = <' '+>
          |""".stripMargin)

    val keywordAndIfab: Grammar = MetaGrammar.translateForce("Kw/if-ifab",
        """S = T*
          |T = Kw | Id | Ws
          |Kw = N & ("if"|"ifab")
          |Id = N - Kw
          |N = <{a-z}+>
          |Ws = <' '+>
          |""".stripMargin)
}
