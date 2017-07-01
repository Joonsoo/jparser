package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.StringSamples

object MetaGrammarTests extends GrammarTestCases with StringSamples {
    val grammar = MetaGrammar

    val metaGrammarText1: String =
        """Grammar = ws* Rules ws*
          |Rules = Rules <(ws-'\n')*> '\n' ws* Rule
          |    | Rule
          |Rule = Nonterminal ws* '=' ws* RHSs
          |RHSs = RHSs ws* '|' ws* Sequence
          |    | Sequence
          |EmptySequence = '#' | 'ε'
          |Sequence = EmptySequence
          |    | Symbol
          |    | SymbolSeq
          |SymbolSeq = SymbolSeq ws+ Symbol
          |    | Symbol ws+ Symbol
          |Symbol = Exclusion-Symbol4
          |    | Symbol4
          |Exclusion = Symbol4
          |    | Exclusion ws* '-' ws* Symbol4
          |Symbol4 = Intersection-Symbol3
          |    | Symbol3
          |Intersection = Symbol3
          |    | Intersection ws* '&' ws* Symbol3
          |Symbol3 = Repeat0
          |    | Repeat1
          |    | Optional
          |    | Symbol2
          |Repeat0 = Symbol3 ws* '*'
          |Repeat1 = Symbol3 ws* '+'
          |Optional = Symbol3 ws* '?'
          |Symbol2 = FollowedBy
          |    | NotFollowedBy
          |    | Symbol1
          |FollowedBy = '$' ws* Symbol2
          |NotFollowedBy = '!' ws* Symbol2
          |Symbol1 = Terminal
          |    | String
          |    | Nonterminal
          |    | Proxy
          |    | Longest
          |    | '(' ws* Symbol ws* ')'
          |    | '(' ws* Either ws* ')'
          |Terminal = anychar
          |    | '\'' char '\''
          |    | TerminalCharSet
          |TerminalCharSet = '{' TerminalCharRange+ '}'
          |TerminalCharRange = charSetChar
          |    | charSetChar '-' charSetChar
          |String = '"' stringChar* '"'
          |Nonterminal = PlainNonterminalName
          |    | QuoteNonterminalName
          |PlainNonterminalName = {0-9A-Z_a-z}+
          |QuoteNonterminalName = '`' nontermNameChar* '`'
          |Proxy = '[' ws* Sequence ws* ']'
          |Either = Symbol ws* '|' ws* Symbol
          |    | Either ws* '|' ws* Symbol
          |Longest = '<' ws* Symbol ws* '>'
          |anychar = '.'
          |char = .-'\\'
          |    | '\\' {"'\\bnrt}
          |    | unicodeChar
          |charSetChar = .-{\-\\\}}
          |    | '\\' {"'\-\\bnrt\}}
          |    | unicodeChar
          |stringChar = .-{"\\}
          |    | '\\' {"'\\bnrt}
          |    | unicodeChar
          |nontermNameChar = .-{\\`}
          |    | '\\' {\\`bnrt}
          |    | unicodeChar
          |unicodeChar = '\\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}
          |ws = {\t\n\r }""".stripMargin('|')
    val metaGrammarText2: String =
        """Grammar = ws* Rules ws*
          |Rules = Rules <(ws-'\n')*> '\n' ws* Rule | Rule
          |Rule = Nonterminal ws* '=' ws* RHSs
          |RHSs = RHSs ws* '|' ws* Sequence | Sequence
          |EmptySequence = ε | {#ε}
          |Sequence = EmptySequence | Symbol | SymbolSeq
          |SymbolSeq = SymbolSeq ws+ Symbol | Symbol ws+ Symbol
          |Symbol = Exclusion-Symbol4 | Symbol4
          |Exclusion = Symbol4 | Exclusion ws* '-' ws* Symbol4
          |Symbol4 = Intersection-Symbol3 | Symbol3
          |Intersection = Symbol3 | Intersection ws* '&' ws* Symbol3
          |Symbol3 = Repeat0 | Repeat1 | Optional | Symbol2
          |Repeat0 = Symbol3 ws* '*'
          |Repeat1 = Symbol3 ws* '+'
          |Optional = Symbol3 ws* '?'
          |Symbol2 = FollowedBy | NotFollowedBy | Symbol1
          |FollowedBy = '$' ws* Symbol2
          |NotFollowedBy = '!' ws* Symbol2
          |Symbol1 = Terminal | String | Nonterminal | Proxy | Longest
          |    | '(' ws* Symbol ws* ')' | '(' ws* Either ws* ')'
          |Terminal = anychar | '\'' char '\'' | TerminalCharSet
          |TerminalCharSet = '{' TerminalCharRange+ '}'
          |TerminalCharRange = charSetChar | charSetChar '-' charSetChar
          |String = '"' stringChar* '"'
          |Nonterminal = PlainNonterminalName | QuoteNonterminalName
          |PlainNonterminalName = {0-9A-Z_a-z}+
          |QuoteNonterminalName = '`' nontermNameChar* '`'
          |Proxy = '[' ws* Sequence ws* ']'
          |Either = Symbol ws* '|' ws* Symbol | Either ws* '|' ws* Symbol
          |Longest = '<' ws* Symbol ws* '>'
          |anychar = '.'
          |char = .-'\\' | '\\' {"'\\bnrt} | unicodeChar
          |charSetChar = .-{\-\\\}} | '\\' {"'\-\\bnrt\}} | unicodeChar
          |stringChar = .-{"\\} | '\\' {"'\\bnrt} | unicodeChar
          |nontermNameChar = .-{\\`} | '\\' {\\`bnrt} | unicodeChar
          |unicodeChar = '\\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}
          |ws = {\t\n\r }""".stripMargin('|')
    val correctSamples = Set(
        "A=B-'\n'*",
        "S = 'a'? 'b'+ <'c'*>",
        "S = (abc|def)",
        "A = ε | B | C",
        "S = a&b&c&d&e-f-g-h&i&j",
        "S = !!!$$$$'a'***???+++&'b'&'c'-'d'&'e'&'f'-{agsdf}",
        """S = "asdf"
          |  | A
          |A = ASDF
          |  | "qwer"
          |ASDF = 'c'?
        """.stripMargin,
        "S = [ε]",
        metaGrammarText1,
        metaGrammarText2,
        ExpressionGrammar1Tests.expressionGrammarText,
        ExpressionGrammar0Tests.expressionGrammar0Text,
        LexicalGrammar0Tests.lexicalGrammar0Text,
        LexicalGrammar1Tests.lexicalGrammar1Text
    )
    val incorrectSamples = Set(
        "S = ()"
    )

    def main(): Unit = {
        println("===== generated =====")
        println(MetaGrammar.reverse(MetaGrammar))

        val metaGrammar1 = MetaGrammar.translate("Grammar", metaGrammarText1).left.get
        println("===== translated =====")
        println(MetaGrammar.reverse(metaGrammar1))
        println("Meta=meta1", MetaGrammar.rules.toSet == metaGrammar1.rules.toSet)

        val metaGrammar2 = MetaGrammar.translate("Grammar", metaGrammarText2).left.get
        println("===== translated0 =====")
        println(MetaGrammar.reverse(metaGrammar2))
        println("Meta=meta2", MetaGrammar.rules.toSet == metaGrammar2.rules.toSet)

        println("meta1=meta2", metaGrammar1.rules.toSet == metaGrammar2.rules.toSet)

        println("========= parsing metaGrammar1 from metaGrammar1 ========")
        val metaGrammarParser1 = new NaiveParser(NGrammar.fromGrammar(metaGrammar1))
        val metaGrammarParser2 = new NaiveParser(NGrammar.fromGrammar(metaGrammar2))

        def parse(parser: NaiveParser, text: String): Grammar = {
            parser.parse(text) match {
                case Left(ctx) =>
                    new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct() match {
                        case Some(forest) if forest.trees.size == 1 =>
                            println("successful")
                            MetaGrammar.translate("Grammar", forest.trees.head)
                        case forestOpt =>
                            println(forestOpt)
                            println("???")
                            ???
                    }
                case Right(error) =>
                    println(error)
                    ???
            }
        }

        val meta1FromMeta1 = parse(metaGrammarParser1, metaGrammarText1)
        val meta1FromMeta2 = parse(metaGrammarParser1, metaGrammarText2)
        val meta2FromMeta1 = parse(metaGrammarParser2, metaGrammarText1)
        val meta2FromMeta2 = parse(metaGrammarParser2, metaGrammarText2)

        def test(name: String, grammar: Grammar, base: Grammar): Unit = {
            println(s"===== $name =====")
            // println(MetaGrammar.reverse(grammar))
            println(base.rules == grammar.rules)
        }

        test("meta1FromMeta1", meta1FromMeta1, metaGrammar1)
        test("meta1FromMeta2", meta1FromMeta2, metaGrammar1)
        test("meta2FromMeta1", meta2FromMeta1, metaGrammar1)
        test("meta2FromMeta2", meta2FromMeta2, metaGrammar1)
        // println(MetaGrammar.reverse(grammar.get) == metaGrammar1)
    }

}

object ExpressionGrammar0Tests extends GrammarTestCases with StringSamples {
    val expressionGrammar0Text: String =
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Expression Grammar 0", expressionGrammar0Text).left.get

    override val correctSamples: Set[String] = Set(
        "1+2",
        "a+b",
        "1234+1234*4321"
    )
    override val incorrectSamples: Set[String] = Set()
}

object ExpressionGrammar1Tests extends GrammarTestCases with StringSamples {
    val expressionGrammarText: String =
        """expression = term | expression {+\-} term
          |term = factor | term {*/} factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | [{+\-}? {1-9} {0-9}* [{eE} {+\-}? {0-9}+]?]
          |variable = {A-Za-z}+""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Expression Grammar 1", expressionGrammarText).left.get

    override val correctSamples: Set[String] = Set(
        "1e+1+1",
        "e+e",
        "1234e+1234++1234",
        "1234e+1234++1234*abcdef-e"
    )
    override val incorrectSamples: Set[String] = Set()
}

object LexicalGrammar0Tests extends GrammarTestCases with StringSamples {
    val lexicalGrammar0Text: String =
        """S = token*
          |token = <(keyword | operator | identifier | number | whitespace)>
          |keyword = ("if" | "else" | "true" | "false") & name
          |operator = <('+' | '-' | "++" | "--" | '=' | "+=" | "-=")>
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |number = <('0' | [{1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |whitespace = { \t\n\r}+
          |""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Lexical Grammar 0", lexicalGrammar0Text).left.get

    override val correctSamples: Set[String] = Set(
        "if true 1+2++3 else 4-5--6",
        "ifx true 1+2+=3 else 4-5-=6",
        "ifx truex 1+2+=3 elsex 4-5-=6"
    )
    override val incorrectSamples: Set[String] = Set()
}

object LexicalGrammar1Tests extends GrammarTestCases with StringSamples {
    val lexicalGrammar1Text: String =
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

    val grammar: Grammar = MetaGrammar.translate("Lexical Grammar 1", lexicalGrammar1Text).left.get

    override val correctSamples: Set[String] = Set(
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
    override val incorrectSamples: Set[String] = Set()
}

object LexicalGrammar2Tests extends GrammarTestCases with StringSamples {
    val lexicalGrammar2Text: String =
        """S = token*
          |token = keyword | operator | identifier
          |keyword = ("if" | "else" | "true" | "false") & name
          |operator = <op>
          |op = '+' | '-' | "++" | "--" | '=' | "+=" | "-="
          |identifier = name - keyword
          |name = <[{A-Za-z} {0-9A-Za-z}*]>
          |""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Lexical Grammar 2", lexicalGrammar2Text).left.get

    override val correctSamples: Set[String] = Set(
        "ifx"
    )
    override val incorrectSamples: Set[String] = Set()
}
