package com.giyeok.jparser.examples.metagram

object LexicalGrammars extends MetaGramExamples {
    val basic0: MetaGram1Example = MetaGram1Example("Lexical Grammar 0",
        """S = token*
          |token = <(keyword | operator | identifier | number | whitespace)>
          |keyword = ("if" | "else" | "true" | "false") & name
          |operator = <('+' | '-' | "++" | "--" | '=' | "+=" | "-=")>
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |number = <('0' | [{1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |whitespace = { \t\n\r}+
          |""".stripMargin('|'))
        .example("if true 1+2++3 else 4-5--6")
        .example("ifx true 1+2+=3 else 4-5-=6")
        .example("ifx truex 1+2+=3 elsex 4-5-=6")

    val basic1: MetaGram1Example = MetaGram1Example("Lexical Grammar 1",
        """S = token*
          |token = <(keyword | operator | paren | identifier | number | whitespace)>
          |keyword = ("if" | "else" | "true" | "false") & name
          |operator = <('+' | '-' | '*' | '/' | "++" | "--" | "**" | '=' | "==" | "+=" | "-=" | "*=" | "/=" | '<' | '>' | "<=" | ">=")> & opchars
          |opchar = {+\-*/=<>}
          |opchars = <opchar+>
          |paren = '(' | ')'
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |number = <('0' | [{1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |whitespace = <{ \t\n\r}+> | comment
          |comment = "//" (.-'\n')* !'\n'
          |        | "/*" [<(.-'*')*> <'*'+>]+ '/'
          |""".stripMargin('|'))
        .example("1e+1+1")
        .example("e+e")
        .example("1234e+1234++1234")
        .example("1234e+1234++1234*abcdef-e")
        .example("1+2++3*4**5")
        .example("if (x == 1+2) 1 else 2 + 4")
        .example("a /= 123")
        .example("/**/")
        .example("/* abc */")
        .example("/* abc * def */")
        .example(
            """// asdf
              |asdf
              |++
              |1234
              |**
              |789""".stripMargin('|'))
        .example("if (x /* x is not a blah blah //// **** * / */) y+=1")

    val basic2: MetaGram1Example = MetaGram1Example("Lexical Grammar 2",
        """S = token*
          |token = keyword | operator | identifier
          |keyword = ("if" | "else") & name
          |operator = <op>
          |op = '+' | "++"
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |""".stripMargin('|'))
        .example("ifx")

    val keywordAndIf: MetaGram1Example = MetaGram1Example("Kw/if",
        """S = T*
          |T = Kw | Id | Ws
          |Kw = N & "if"
          |Id = N - Kw
          |N = <{a-z}+>
          |Ws = <' '+>
          |""".stripMargin)

    val keywordAndIfab: MetaGram1Example = MetaGram1Example("Kw/if-ifab",
        """S = T*
          |T = Kw | Id | Ws
          |Kw = N & ("if"|"ifab")
          |Id = N - Kw
          |N = <{a-z}+>
          |Ws = <' '+>
          |""".stripMargin)

    val examples: List[MetaGramExample] = List(basic0, basic1, keywordAndIf, keywordAndIfab)
}
