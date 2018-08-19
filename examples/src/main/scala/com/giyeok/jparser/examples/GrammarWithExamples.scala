package com.giyeok.jparser.examples

import com.giyeok.jparser.{Grammar, Symbols}

case class GrammarWithExamples(grammar: Grammar, examples: Seq[String] = Seq()) extends Grammar {
    override val name: String = grammar.name
    override val rules: RuleMap = grammar.rules
    override val startSymbol: Symbols.Nonterminal = grammar.startSymbol

    def example(example: String) = GrammarWithExamples(grammar, examples :+ example)
}
