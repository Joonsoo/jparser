package com.giyeok.moonparser

trait Grammar {
    type RuleMap = Map[String, Set[Symbols.Symbol]]

    val name: String
    val rules: RuleMap
    val startSymbol: String
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)
case class AmbiguousGrammarException(msg: String) extends Exception(msg)
