package com.giyeok.jparser.examples.metagram

object MetaGramInMetaGram extends MetaGramExamples {
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
          |ws = {\t\n\r }""".stripMargin

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

    val correctExamples: List[String] = List("A=B-'\n'*",
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
        metaGrammarText2) ++
        (SimpleGrammars.examples map (_.grammar)) ++
        (ExpressionGrammars.examples map (_.grammar)) ++
        (JsonGrammar.examples map (_.grammar)) ++
        (LexicalGrammars.examples map (_.grammar))

    val incorrectExamples: List[String] = List("S = ()")

    val metaGrammar1 = MetaGram1Example("MetaGrammar1", metaGrammarText1,
        correctExamples = correctExamples, incorrectExamples = incorrectExamples)
    val metaGrammar2 = MetaGram1Example("MetaGrammar2", metaGrammarText2,
        correctExamples = correctExamples, incorrectExamples = incorrectExamples)

    val examples: List[MetaGramExample] = List(metaGrammar1, metaGrammar2)
}
