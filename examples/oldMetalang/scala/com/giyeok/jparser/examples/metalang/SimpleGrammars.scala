package com.giyeok.jparser.examples.metalang

import com.giyeok.jparser.examples.{MetaLang1Example, MetaLangExample, MetaLangExamples}

object SimpleGrammars extends MetaLangExamples {
    val arrayGrammar: MetaLang1Example = MetaLang1Example("SimpleArrayGrammar",
        """S = '[' WS elems WS ']'
          |elems = elem | elem WS ',' WS elems
          |elem = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val array0Grammar: MetaLang1Example = MetaLang1Example("SimpleArray0Grammar",
        """S = '[' [WS E [WS ',' WS E]*]? WS ']'
          |E = 'a'
          |WS = ' '*
        """.stripMargin)

    val array1Grammar: MetaLang1Example = MetaLang1Example("ExprArrayGrammar",
        """S = '[' [WS E [WS ',' WS E]*]? WS ']'
          |E = T | E WS '+' WS T
          |T = F | T WS '*' WS F
          |F = N | '(' WS E WS ')'
          |N = 'a'
          |WS = ' ' *
        """.stripMargin)

    val arrayRGrammar: MetaLang1Example = MetaLang1Example("SimpleArrayRGrammar",
        """S = '[' [WS E Emores]? WS ']'
          |Emore = WS ',' WS E
          |Emores = # | Emore Emores
          |E = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val arrayOrObjectGrammar: MetaLang1Example = MetaLang1Example("SimpleArrayOrObjectGrammar",
        """S = '[' WS elems WS ']' | '{' WS elems WS '}'
          |elems = elem | elem WS ',' WS elems
          |elem = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val earley1970ae: MetaLang1Example = MetaLang1Example("Earley 1970 AE",
        """E = T | E '+' T
          |T = P | T '*' P
          |P = 'a'
        """.stripMargin)

    val knuth1965_24: MetaLang1Example = MetaLang1Example("Knuth 1965 Grammar 24",
        """S = # | 'a' A 'b' S | 'b' B 'a' S
          |A = # | 'a' A 'b' A
          |B = # | 'b' B 'a' B
        """.stripMargin)

    val lexer1: MetaLang1Example = MetaLang1Example("SimpleLexerGrammar1",
        """S = T*
          |T = Kw | Id | P
          |Kw = "if"&W
          |Id = W-Kw
          |W = <{a-z}+>
          |P = ' '
        """.stripMargin)

    val lexer2: MetaLang1Example = MetaLang1Example("SimpleLexerGrammar2",
        """S = T*
          |T = Kw | Id | P
          |Kw = "xyz"&W
          |Id = W-Kw
          |W = <{a-z}+>
          |P = ' '
        """.stripMargin)

    val lexer2_1: MetaLang1Example = MetaLang1Example("SimpleLexerGrammar2_1",
        """S = T*
          |T = Kw | Id | P
          |Kw = "xyz"&W
          |Id = W-Kw
          |W = <({a-w} | 'x' | 'y' | 'z')+>
          |P = ' '
        """.stripMargin)

    val weird: MetaLang1Example = MetaLang1Example("WeirdGrammar1",
        """S = A B C | D E F
          |A = 'a'
          |D = 'a'
          |B = 'x' 'y'
          |E = 'x' 'y' 'z'
          |C = 'z'
          |F = 'q'
        """.stripMargin)

    val examples: List[MetaLangExample] = List(arrayGrammar, array0Grammar, array1Grammar, arrayRGrammar,
        arrayOrObjectGrammar, earley1970ae, knuth1965_24, lexer1, lexer2, lexer2_1, weird)
}
