package com.giyeok.jparser

import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet

trait Grammar {
    type RuleMap = ListMap[String, ListSet[Symbols.Symbol]]

    type Symbol = Symbols.Symbol

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
                if (visited contains symbol) cc
                else {
                    visited += symbol
                    symbol match {
                        case Start => traverse(grammar.startSymbol, cc)
                        case _: Terminal => cc
                        case Nonterminal(name) =>
                            (grammar.rules get name) match {
                                case Some(rhs) => rhs.foldRight(cc) { traverse(_, _) }
                                case None => cc + symbol
                            }
                        case Sequence(seq, _) => seq.foldRight(cc) { traverse(_, _) }
                        case OneOf(syms) => syms.foldRight(cc) { traverse(_, _) }
                        case Except(sym, except) => traverse(sym, traverse(except, cc))
                        case r: Repeat => traverse(r.sym, cc)
                        case LookaheadIs(lookahead) => traverse(lookahead, cc)
                        case LookaheadExcept(except) => traverse(except, cc)
                        case Join(sym, join) => traverse(sym, traverse(join, cc))
                        case Proxy(sym) => traverse(sym, cc)
                        case Longest(sym) => traverse(sym, cc)
                        case EagerLongest(sym) => traverse(sym, cc)
                    }
                }
            }
            traverse(Start, Set())
        }

        def wrongLookaheads: Set[Symbol] = {
            var visited = Set[Symbol]()
            def traverse(symbol: Symbol, parent: Option[Symbol], cc: Set[Symbol]): Set[Symbol] = {
                if (visited contains symbol) Set()
                else {
                    visited += symbol
                    symbol match {
                        case Start => traverse(grammar.startSymbol, Some(Start), cc)
                        case _: Terminal => cc
                        case Nonterminal(name) =>
                            (grammar.rules get name) match {
                                case Some(rhs) => rhs.foldRight(cc) { traverse(_, Some(symbol), _) }
                                case None => cc
                            }
                        case Sequence(seq, _) => seq.foldRight(cc) { traverse(_, Some(symbol), _) }
                        case OneOf(syms) => syms.foldRight(cc) { traverse(_, Some(symbol), _) }
                        case Except(sym, except) => traverse(sym, Some(symbol), traverse(except, Some(symbol), cc))
                        case r: Repeat => traverse(r.sym, Some(symbol), cc)
                        case LookaheadIs(lookahead) =>
                            parent match {
                                case Some(Sequence(_, _)) => traverse(lookahead, Some(symbol), cc + symbol)
                                case _ => traverse(lookahead, Some(symbol), cc)
                            }
                        case LookaheadExcept(except) =>
                            parent match {
                                case Some(Sequence(_, _)) => traverse(except, Some(symbol), cc + symbol)
                                case _ => traverse(except, Some(symbol), cc)
                            }
                        case Join(sym, join) => traverse(sym, Some(symbol), traverse(join, Some(symbol), cc))
                        case Proxy(sym) => traverse(sym, Some(symbol), cc)
                        case Longest(sym) => traverse(sym, Some(symbol), cc)
                        case EagerLongest(sym) => traverse(sym, Some(symbol), cc)
                    }
                }
            }
            traverse(Start, None, Set())
        }

        def usedSymbols: Set[Symbol] = {
            def traverse(symbol: Symbol, cc: Set[Symbol]): Set[Symbol] = {
                if (cc contains symbol) cc else
                    symbol match {
                        case Start => traverse(grammar.startSymbol, cc + Start)
                        case _: Terminal => cc + symbol
                        case Nonterminal(name) =>
                            (grammar.rules get name) match {
                                case Some(rhs) => rhs.foldRight(cc + symbol) { traverse(_, _) }
                                case None => cc + symbol
                            }
                        case Sequence(seq, _) => seq.foldRight(cc + symbol) { traverse(_, _) }
                        case OneOf(syms) => (syms).foldRight(cc + symbol) { traverse(_, _) }
                        case Except(sym, except) => traverse(sym, traverse(except, cc + symbol))
                        case repeat: Repeat =>
                            traverse(repeat.repeatSeq, traverse(repeat.baseSeq, cc + symbol))
                        case LookaheadIs(lookahead) => traverse(lookahead, cc + symbol)
                        case LookaheadExcept(except) => traverse(except, cc + symbol)
                        case Join(sym, join) => traverse(sym, traverse(join, cc + symbol))
                        case Proxy(sym) => traverse(sym, cc + symbol)
                        case Longest(sym) => traverse(sym, cc + symbol)
                        case EagerLongest(sym) => traverse(sym, cc + symbol)
                    }
            }
            traverse(Start, Set())
        }

        def unusedSymbols: Set[Symbol] =
            ((grammar.rules.keySet map { Nonterminal(_) }): Set[Symbol]) -- usedSymbols
    }
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)
case class AmbiguousGrammarException(msg: String) extends Exception(msg)
case class AmbiguousParsingException(name: String) extends Exception
case class NoDefinitionOfNonterminalException(name: String) extends Exception
