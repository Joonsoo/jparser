package com.giyeok.jparser.examples2

import com.giyeok.jparser.examples1.ExampleGrammarSet

object LexicalGrammar1 extends MetaGrammarWithStringSamples("Lexical Grammar 1") {
    val grammarText: String =
        """S = token*
          |token = <(keyword | operator | identifier | number | whitespace)>
          |keyword = ("if" | "else" | "true" | "false") & name
          |operator = <('+' | '-' | "++" | "--" | '=' | "+=" | "-=")>
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |number = <('0' | [{1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |whitespace = { \t\n\r}+
          |""".stripMargin('|')

    val validInputs = Set(
        "if true 1+2++3 else 4-5--6",
        "ifx true 1+2+=3 else 4-5-=6",
        "ifx truex 1+2+=3 elsex 4-5-=6"
    )
    val invalidInputs = Set()
}

object LexicalGrammar2 extends MetaGrammarWithStringSamples("Lexical Grammar 2") {
    val grammarText: String =
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
          |""".stripMargin('|')

    val validInputs = Set(
        "1e+1+1",
        "e+e",
        "1234e+1234++1234",
        "1234e+1234++1234*abcdef-e",
        "1+2++3*4**5",
        "if (x == 1+2) 1 else 2 + 4",
        "a /= 123",
        "/**/",
        "/* abc */",
        "/* abc * def */",
        """// asdf
          |asdf
          |++
          |1234
          |**
          |789""".stripMargin('|'),
        "if (x /* x is not a blah blah //// **** * / */) y+=1"
    )
    val invalidInputs = Set()
}

object LexicalGrammar3 extends MetaGrammarWithStringSamples("Lexical Grammar 3") {
    val grammarText: String =
        """S = token*
          |token = keyword | operator | identifier
          |keyword = ("if" | "else") & name
          |operator = <op>
          |op = '+' | "++"
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |""".stripMargin('|')

    val validInputs = Set(
        "ifx"
    )
    val invalidInputs = Set()
}

object LexicalGrammars extends ExampleGrammarSet {
    val examples = Set(
        LexicalGrammar1.toPair,
        LexicalGrammar2.toPair,
        LexicalGrammar3.toPair
    )
}
