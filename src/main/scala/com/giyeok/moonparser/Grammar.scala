package com.giyeok.moonparser

trait Grammar {
    type RuleMap = Map[String, Set[Symbols.Symbol]]

    val name: String
    val rules: RuleMap
    val startSymbol: Symbols.Nonterminal
}

trait GrammarChecker {
    val grammar: Grammar

    lazy val checkFromStart: Boolean = {
        import Symbols._
        def checkSymbol(symbol: Symbol): Boolean = symbol match {
            case Nonterminal(name) => grammar.rules contains name
            case Sequence(seq, ws) => (seq forall checkSymbol) && (ws forall checkSymbol)
            case OneOf(syms) => syms forall checkSymbol
            case Both(sym, also) => checkSymbol(sym) && checkSymbol(also)
            case Except(sym, except) => checkSymbol(sym) && checkSymbol(except)
            case Repeat(sym, _) => checkSymbol(sym)
            case LookaheadExcept(except) => checkSymbol(except)
            case Backup(sym, backup) => checkSymbol(sym) && checkSymbol(backup)
            case _ => true
        }
        checkSymbol(grammar.startSymbol)
    }
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)
case class AmbiguousGrammarException(msg: String) extends Exception(msg)
