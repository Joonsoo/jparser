package com.giyeok.jparser.examples.metagram

object SimpleGrammars extends MetaGramExamples {
    val arrayGrammar: MetaGram1Example = MetaGram1Example("SimpleArrayGrammar",
        """S = '[' WS elems WS ']'
          |elems = elem | elem WS ',' WS elems
          |elem = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val array0Grammar: MetaGram1Example = MetaGram1Example("SimpleArray0Grammar",
        """S = '[' [WS E [WS ',' WS E]*]? WS ']'
          |E = 'a'
          |WS = ' '*
        """.stripMargin)

    val array1Grammar: MetaGram1Example = MetaGram1Example("ExprArrayGrammar",
        """S = '[' [WS E [WS ',' WS E]*]? WS ']'
          |E = T | E WS '+' WS T
          |T = F | T WS '*' WS F
          |F = N | '(' WS E WS ')'
          |N = 'a'
          |WS = ' ' *
        """.stripMargin)

    val arrayRGrammar: MetaGram1Example = MetaGram1Example("SimpleArrayRGrammar",
        """S = '[' [WS E Emores]? WS ']'
          |Emore = WS ',' WS E
          |Emores = # | Emore Emores
          |E = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val arrayOrObjectGrammar: MetaGram1Example = MetaGram1Example("SimpleArrayOrObjectGrammar",
        """S = '[' WS elems WS ']' | '{' WS elems WS '}'
          |elems = elem | elem WS ',' WS elems
          |elem = 'a'
          |WS = # | ' ' WS
        """.stripMargin)

    val earley1970ae: MetaGram1Example = MetaGram1Example("Earley 1970 AE",
        """E = T | E '+' T
          |T = P | T '*' P
          |P = 'a'
        """.stripMargin)

    val knuth1965_24: MetaGram1Example = MetaGram1Example("Knuth 1965 Grammar 24",
        """S = # | 'a' A 'b' S | 'b' B 'a' S
          |A = # | 'a' A 'b' A
          |B = # | 'b' B 'a' B
        """.stripMargin)

    val lexer1: MetaGram1Example = MetaGram1Example("SimpleLexerGrammar1",
        """S = T*
          |T = Kw | Id | P
          |Kw = "if"&W
          |Id = W-Kw
          |W = <{a-z}+>
          |P = ' '
        """.stripMargin)

    val lexer2: MetaGram1Example = MetaGram1Example("SimpleLexerGrammar2",
        """S = T*
          |T = Kw | Id | P
          |Kw = "xyz"&W
          |Id = W-Kw
          |W = <{a-z}+>
          |P = ' '
        """.stripMargin)

    val lexer2_1: MetaGram1Example = MetaGram1Example("SimpleLexerGrammar2_1",
        """S = T*
          |T = Kw | Id | P
          |Kw = "xyz"&W
          |Id = W-Kw
          |W = <({a-w} | 'x' | 'y' | 'z')+>
          |P = ' '
        """.stripMargin)

    val weird: MetaGram1Example = MetaGram1Example("WeirdGrammar1",
        """S = A B C | D E F
          |A = 'a'
          |D = 'a'
          |B = 'x' 'y'
          |E = 'x' 'y' 'z'
          |C = 'z'
          |F = 'q'
        """.stripMargin)

    val examples: List[MetaGramExample] = List(arrayGrammar, array0Grammar, array1Grammar, arrayRGrammar,
        arrayOrObjectGrammar, earley1970ae, knuth1965_24, lexer1, lexer2, lexer2_1, weird)
}
