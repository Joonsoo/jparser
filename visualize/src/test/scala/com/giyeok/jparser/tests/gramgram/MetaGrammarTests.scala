package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.StringSamples

object MetaGrammarTests extends GrammarTestCases with StringSamples {
    val grammar = MetaGrammar

    val metaGrammar: String =
        """Grammar = `ws*` Rules `ws*`
          |Rules = Rules <(ws-'\n')*> '\n' `ws*` Rule
          |    | Rule
          |Rule = Nonterminal `ws*` '=' `ws*` RHSs
          |RHSs = RHSs `ws*` '|' `ws*` Sequence
          |    | Sequence
          |EmptySequence = ε
          |    | {#ε}
          |Sequence = EmptySequence
          |    | Symbol
          |    | SymbolSeq
          |SymbolSeq = SymbolSeq `ws+` Symbol
          |    | Symbol `ws+` Symbol
          |Symbol = Exclusion-Symbol4
          |    | Symbol4
          |Exclusion = Symbol4
          |    | Exclusion `ws*` '-' `ws*` Symbol4
          |Symbol4 = Intersection-Symbol3
          |    | Symbol3
          |Intersection = Symbol3
          |    | Intersection `ws*` '&' `ws*` Symbol3
          |Symbol3 = Repeat0
          |    | Repeat1
          |    | Optional
          |    | Symbol2
          |Repeat0 = Symbol3 `ws*` '*'
          |Repeat1 = Symbol3 `ws*` '+'
          |Optional = Symbol3 `ws*` '?'
          |Symbol2 = FollowedBy
          |    | NotFollowedBy
          |    | Symbol1
          |FollowedBy = '$' `ws*` Symbol2
          |NotFollowedBy = '!' `ws*` Symbol2
          |Symbol1 = Terminal
          |    | String
          |    | Nonterminal
          |    | Proxy
          |    | Longest
          |    | '(' `ws*` Either `ws*` ')'
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
          |Proxy = '[' `ws*` Sequence `ws*` ']'
          |Either = Either `ws*` '|' `ws*` Symbol
          |    | Symbol
          |Longest = '<' `ws*` Symbol `ws*` '>'
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
          |ws = {\t\n\r }
          |`ws*` = <ws*>
          |`ws+` = <ws+>""".stripMargin('|')
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
        metaGrammar
    )
    val incorrectSamples = Set[String]()

    def main(args: Array[String]): Unit = {
        println("===== generated =====")
        println(MetaGrammar.reverse(MetaGrammar))

        val translated = MetaGrammar.translate(metaGrammar).get
        println("===== translated =====")
        println(MetaGrammar.reverse(translated))

        println("========= parsing metaGrammar from metaGrammar ========")
        val parser = new NaiveParser(NGrammar.fromGrammar(translated))
        val translatedFromTranslated = parser.parse(metaGrammar) match {
            case Left(ctx) =>
                new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct() match {
                    case Some(forest) if forest.trees.size == 1 =>
                        println("successful")
                        MetaGrammar.translate(forest.trees.head)
                    case _ =>
                        println("???")
                        ???
                }
            case Right(error) =>
                println(error)
                ???
        }

        println("===== translated from translated =====")
        println(MetaGrammar.reverse(translatedFromTranslated))
        println(translated.rules == translatedFromTranslated.rules)
        // println(MetaGrammar.reverse(grammar.get) == metaGrammar1)
    }
}
