package com.giyeok.jparser.metalang3.symbols

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast

object Escapes {

    implicit class NonterminalName(val nonterminal: MetaGrammar3Ast.NonterminalName) {
        def stringName: String = nonterminal.name.sourceText // backtick escape
    }

}
