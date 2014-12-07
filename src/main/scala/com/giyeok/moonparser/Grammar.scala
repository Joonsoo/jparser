package com.giyeok.moonparser

import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet

trait Grammar {
    type RuleMap = ListMap[String, ListSet[Symbols.Symbol]]

    val name: String
    val rules: RuleMap
    val startSymbol: Symbols.Nonterminal
}

object Grammar {
    implicit class GrammarChecker(grammar: Grammar) {
        import Symbols._

        def missingSymbols: Set[Symbol] = {
            var visited = Set[Symbol]()
            def traverse(symbol: Symbol, cc: Set[Symbol]): Set[Symbol] = {
                if (visited contains symbol) Set()
                else {
                    visited += symbol
                    symbol match {
                        case Nonterminal(name) => if (grammar.rules contains name) Set() else Set(symbol)
                        case Sequence(seq, ws) => (seq ++ ws).foldRight(cc) { traverse(_, _) }
                        case OneOf(syms) => syms.foldRight(cc) { traverse(_, _) }
                        case Except(sym, except) => traverse(sym, traverse(except, cc))
                        case Repeat(sym, _) => traverse(sym, cc)
                        case LookaheadExcept(except) => traverse(except, cc)
                        case Backup(sym, backup) => traverse(sym, traverse(backup, cc))
                    }
                }
            }
            traverse(grammar.startSymbol, Set())
        }

        def wrongLookaheads: Set[Symbol] = {
            var visited = Set[Symbol]()
            def traverse(symbol: Symbol, parent: Option[Symbol], cc: Set[Symbol]): Set[Symbol] = {
                if (visited contains symbol) Set()
                else {
                    visited += symbol
                    symbol match {
                        case Nonterminal(name) => cc
                        case Sequence(seq, ws) => (seq ++ ws).foldRight(cc) { traverse(_, Some(symbol), _) }
                        case OneOf(syms) => syms.foldRight(cc) { traverse(_, Some(symbol), _) }
                        case Except(sym, except) => traverse(sym, Some(symbol), traverse(except, Some(symbol), cc))
                        case Repeat(sym, _) => traverse(sym, Some(symbol), cc)
                        case LookaheadExcept(except) =>
                            parent match {
                                case Some(Sequence(_, _)) => traverse(except, Some(symbol), cc + symbol)
                                case _ => traverse(except, Some(symbol), cc)
                            }
                        case Backup(sym, backup) => traverse(sym, Some(symbol), traverse(backup, Some(symbol), cc))
                    }
                }
            }
            traverse(grammar.startSymbol, None, Set())
        }

        def usedSymbols: Set[Symbol] = {
            def traverse(symbol: Symbol, cc: Set[Symbol]): Set[Symbol] = {
                if (cc contains symbol) cc else
                    symbol match {
                        case Empty => cc + symbol
                        case _: Terminal => cc + symbol
                        case Nonterminal(name) => grammar.rules(name).foldRight(cc + symbol) { traverse(_, _) }
                        case Sequence(seq, ws) => (seq ++ ws).foldRight(cc + symbol) { traverse(_, _) }
                        case OneOf(syms) => (syms).foldRight(cc + symbol) { traverse(_, _) }
                        case Except(sym, except) => traverse(sym, traverse(except, cc + symbol))
                        case Repeat(sym, _) => traverse(sym, cc + symbol)
                        case LookaheadExcept(except) => traverse(except, cc + symbol)
                        case Backup(sym, backup) => traverse(sym, traverse(backup, cc + symbol))
                    }
            }
            traverse(grammar.startSymbol, Set())
        }

        def unusedSymbols: Set[Symbol] =
            ((grammar.rules.keySet map { Nonterminal(_) }): Set[Symbol]) -- usedSymbols
    }
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)
case class AmbiguousGrammarException(msg: String) extends Exception(msg)
