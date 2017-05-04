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
          |EmptySequence = ε
          |    | {#ε}
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
        "A = | B | C",
        "S = a&b&c&d&e-f-g-h&i&j",
        "S = !!!~~~~'a'***???+++&'b'&'c'-'d'&'e'&'f'-{agsdf}",
        """S = "asdf"
          |  | A
          |A = ASDF
          |  | "qwer"
          |ASDF = 'c'?
        """.stripMargin,
        "S = []",
        metaGrammarText1,
        metaGrammarText2,
        ExpressionGrammarTests.expressionGrammarText,
        LexicalGrammarTests.lexicalGrammarText
    )
    val incorrectSamples = Set(
        "S = ()"
    )

    def main(): Unit = {
        println("===== generated =====")
        println(MetaGrammar.reverse(MetaGrammar))

        val metaGrammar1 = MetaGrammar.translate("Grammar", metaGrammarText1).get
        println("===== translated =====")
        println(MetaGrammar.reverse(metaGrammar1))
        println("Meta=meta1", MetaGrammar.rules.toSet == metaGrammar1.rules.toSet)

        val metaGrammar2 = MetaGrammar.translate("Grammar", metaGrammarText2).get
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

object ExpressionGrammarTests extends GrammarTestCases with StringSamples {
    val expressionGrammarText: String =
        """expression = term | expression {+\-} term
          |term = factor | term {*/} factor
          |factor = number | variable | '(' expression ')'
          |number = <('0' | [{+\-}? {1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |variable = <{a-zA-Z}+>""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Expression Grammar", expressionGrammarText).get

    override val correctSamples: Set[String] = Set(
        "1e+1+1",
        "e+e",
        "1234e+1234++1234",
        "1234e+1234++1234*abcdef-e"
    )
    override val incorrectSamples: Set[String] = Set()
}

object LexicalGrammarTests extends GrammarTestCases with StringSamples {
    val lexicalGrammarText: String =
        """S = token*
          |token = <(keyword | operator | identifier | number | whitespace)>
          |keyword = name & ("if" | "for")
          |operator = '+' | '-' | '*' | '/'
          |identifier = name - keyword
          |name = <[{a-zA-Z} {a-zA-Z0-9}*]>
          |number = <('0' | [{+\-}? {1-9} {0-9}* [{eE} {+\-}? {0-9}+]?])>
          |whitespace = { \t\n\r}+""".stripMargin('|')

    val grammar: Grammar = MetaGrammar.translate("Lexical Grammar", lexicalGrammarText).get

    override val correctSamples: Set[String] = Set(
        "1e+1+1",
        "e+e",
        "1234e+1234++1234",
        "1234e+1234++1234*abcdef-e"
    )
    override val incorrectSamples: Set[String] = Set()
}
