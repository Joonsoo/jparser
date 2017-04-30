package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.StringSamples

object MetaGrammarTests extends GrammarTestCases with StringSamples {
    val grammar = MetaGrammar

    val metaGrammar =
        """Grammar = `ws*` Rules `ws*`
          |Rules = Rules <(ws-'\n')*> '\n' `ws*` Rule
          |    | Rule
          |Rule = Nonterminal `ws*` '=' `ws*` RHSs
          |RHSs = RHSs `ws*` '|' `ws*` RHS
          |    | RHS
          |RHS = Symbol
          |    |
          |    | SymbolSeq
          |Sequence =
          |    | Symbol
          |    | SymbolSeq
          |SymbolSeq = SymbolSeq `ws+` Symbol
          |    | Symbol `ws+` Symbol
          |Symbol = Terminal
          |    | String
          |    | Nonterminal
          |    | Repeat0
          |    | Repeat1
          |    | Optional
          |    | Proxy
          |    | Intersection
          |    | Exclusion
          |    | Lookahead
          |    | LookaheadNot
          |    | Longest
          |    | '(' `ws*` EIther `ws*` ')'
          |Terminal = '\'' char '\''
          |    | '{' '-'? (char-'-' | [char '-' char])+ '}'
          |String = '"' stringChar* '"'
          |Nonterminal = {a-zA-Z0-9_}+
          |    | '`' nontermNameChar* '`'
          |Repeat0 = Symbol `ws*` '*'
          |Repeat1 = Symbol `ws*` '+'
          |Optional = Symbol `ws*` '?'
          |Proxy = '[' `ws*` Sequence `ws*` ']'
          |Either = Either `ws*` '|' `ws*` Symbol
          |    | Symbol
          |Intersection = Symbol `ws*` '&' `ws*` Symbol
          |Exclusion = Symbol `ws*` '-' `ws*` Symbol
          |Lookahead = '~' `ws*` Symbol
          |LookaheadNot = '!' `ws*` Symbol
          |Longest = '<' `ws*` Symbol `ws*` '>'
          |char = .
          |stringChar = .
          |nontermNameChar = .
          |ws = { \t\n\r}
          |`ws*` = <ws*>
          |`ws+` = <ws+>
          |""".stripMargin
    val correctSamples = Set(
        "S = 'a'? 'b'+ <'c'*>",
        "S = (abc|def)",
        "A = | B | C",
        """S = "asdf"
          |  | A
          |A = ASDF
          |  | "qwer"
          |ASDF = 'c'?
        """.stripMargin,
        metaGrammar
    )
    val incorrectSamples = Set[String]()

}
