package com.giyeok.jparser.examples

import com.giyeok.jparser.gramgram.MetaGrammar

class LexicalGrammars {
    val keywordAndIf = MetaGrammar.translate("Kw/if",
        """S = T*
          |T = Kw | Id
          |Kw = N & "if"
          |Id = N - Kw
          |N = <{a-z}+>
          |""".stripMargin)

    val keywordAndIfab = MetaGrammar.translate("Kw/if-ifab",
        """S = T*
          |T = Kw | Id
          |Kw = N & ("if"|"ifab")
          |Id = N - Kw
          |N = <{a-z}+>
          |""".stripMargin)
}
