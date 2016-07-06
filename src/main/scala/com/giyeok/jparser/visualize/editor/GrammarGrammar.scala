package com.giyeok.jparser.visualize.editor

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.GrammarHelper
import com.giyeok.jparser.Symbols

object GrammarGrammar extends Grammar {
    val whitespace = chars(" \t\n\r").star
    val inlineWS: Set[Symbol] = Set(chars(" \t"))
    val name = "Grammar Notation"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            seq(whitespace,
                n("Rules"),
                whitespace)),
        "Rules" -> ListSet(
            seq(Set[Symbol](chars(" \t\n\r")), n("Rules"), n("NontermDef")),
            n("NontermDef")),
        "NontermDef" -> ListSet(
            seq(inlineWS, n("NontermName"), c('='), n("Productions"))),
        "Productions" -> ListSet(
            n("Production"),
            seq(n("Productions"), whitespace, c('|'), chars(" \t").star, n("Production"))),
        "Production" -> ListSet(
            n("Empty"),
            n("Symbols")),
        "Empty" -> ListSet(
            i("<empty>")),
        "Symbols" -> ListSet(
            n("Symbol"),
            seq(inlineWS, n("Symbols"), n("Symbol"))),
        "Symbol" -> ListSet(
            n("NontermName"),
            n("LongestName"),
            n("LookaheadName"),
            n("LookaheadExName"),
            n("IntersectionName"),
            n("ExclusionName"),
            n("Terminal")),
        "NontermName" -> ListSet(
            longest(oneof(
                oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9')).plus,
                seq(c('`'), oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9'), chars("+*[]-")).plus)))),
        "LongestName" -> ListSet(
            longest(seq(inlineWS, c('L'), c('('), n("Symbol"), c(')')))),
        "LookaheadName" -> ListSet(
            longest(seq(inlineWS, i("la"), c('('), n("Symbol"), c(')')))),
        "LookaheadExName" -> ListSet(
            longest(seq(inlineWS, i("lx"), c('('), n("Symbol"), c(')')))),
        "IntersectionName" -> ListSet(
            longest(seq(inlineWS, n("Symbol"), c('&'), n("Symbol")))),
        "ExclusionName" -> ListSet(
            longest(seq(inlineWS, n("Symbol"), c('-'), n("Symbol")))),
        "Terminal" -> ListSet(
            n("TerminalExactChar"),
            n("TerminalRanges"),
            n("TerminalSet")),
        "TerminalExactChar" -> ListSet(
            seq(c('\''), anychar, c('\''))),
        "TerminalRanges" -> ListSet(
            seq(c('['), n("TerminalRange").plus, c(']'))),
        "TerminalRange" -> ListSet(
            seq(anychar, c('-'), anychar)),
        "TerminalSet" -> ListSet(
            seq(c('{'), n("TerminalAnyChar").plus, c('}'))),
        "TerminalAnyChar" -> ListSet(
            anychar.except(c('-'))))
    val startSymbol = n("Grammar")

    import ParseResultTree._
    import GrammarHelper._

    def childrenOf(node: Node, sym: Symbol): Seq[Node] = node match {
        case BindNode(s, body) if s == sym => Seq(node)
        case BindNode(s, body) => childrenOf(body, sym)
        case s: SequenceNode => s.children flatMap { childrenOf(_, sym) }
        case _ => Seq()
    }
    def textOf(node: Node): String = node match {
        case BindNode(s, body) => textOf(body)
        case s: SequenceNode => (s.children map { textOf(_) }).mkString
        case JoinNode(body, join) => textOf(body)
        case TerminalNode(Inputs.Character(c, _)) => s"$c"
        case _ => ???
    }

    class NewGrammar(val name: String, val rules: ListMap[String, ListSet[Symbols.Symbol]], val startSymbol: Symbols.Nonterminal) extends Grammar

    def translate(tree: ParseResultTree.Node): Option[Grammar] = {
        tree match {
            case BindNode(Start, BindNode(Nonterminal("Grammar"), seq: SequenceNode)) =>
                seq.children(1) match {
                    case BindNode(Nonterminal("Rules"), body) =>
                        val nontermDefs: Seq[(String, Seq[Symbols.Symbol])] = childrenOf(body, Nonterminal("NontermDef")) map {
                            _ match {
                                case BindNode(Nonterminal("NontermDef"), seq: SequenceNode) =>
                                    val name = textOf(seq.children(0))
                                    val productions: Seq[Symbol] = childrenOf(seq.children(2), Nonterminal("Production")) map {
                                        case BindNode(Nonterminal("Production"), BindNode(Nonterminal("Empty"), _)) => empty
                                        case BindNode(Nonterminal("Production"), BindNode(Nonterminal("Symbols"), body)) =>
                                            def mapSymbol(node: Node): Symbols.Symbol = node match {
                                                case BindNode(Nonterminal("Symbol"), body) => body match {
                                                    case BindNode(Nonterminal("NontermName"), body) =>
                                                        Nonterminal(textOf(body))
                                                    case BindNode(Nonterminal("LongestName"), BindNode(_: Longest, seq: SequenceNode)) =>
                                                        longest(mapSymbol(seq.children(2)))
                                                    case BindNode(Nonterminal("LookaheadName"), BindNode(_: Longest, seq: SequenceNode)) =>
                                                        lookahead_is(mapSymbol(seq.children(2)))
                                                    case BindNode(Nonterminal("LookaheadExName"), BindNode(_: Longest, seq: SequenceNode)) =>
                                                        lookahead_except(mapSymbol(seq.children(2)))
                                                    case BindNode(Nonterminal("IntersectionName"), BindNode(_: Longest, seq: SequenceNode)) =>
                                                        join(mapSymbol(seq.children(0)), mapSymbol(seq.children(2)))
                                                    case BindNode(Nonterminal("ExclusionName"), BindNode(_: Longest, seq: SequenceNode)) =>
                                                        mapSymbol(seq.children(0)).except(mapSymbol(seq.children(2)))
                                                    case BindNode(Nonterminal("Terminal"), body) =>
                                                        val terminal: Terminal = body match {
                                                            case BindNode(Nonterminal("TerminalExactChar"), seq: SequenceNode) =>
                                                                chars(textOf(seq.children(1)))
                                                            case BindNode(Nonterminal("TerminalRanges"), seq: SequenceNode) =>
                                                                val chars: Set[Char] = (childrenOf(seq.children(1), Nonterminal("TerminalRange")) flatMap {
                                                                    case BindNode(Nonterminal("TerminalRange"), seq: SequenceNode) =>
                                                                        val x = seq.children(0) match { case BindNode(AnyChar, TerminalNode(Inputs.Character(c, _))) => c }
                                                                        val y = seq.children(2) match { case BindNode(AnyChar, TerminalNode(Inputs.Character(c, _))) => c }
                                                                        (x to y).toSet
                                                                }).toSet
                                                                Chars(chars)
                                                            case BindNode(Nonterminal("TerminalSet"), seq: SequenceNode) =>
                                                                chars(textOf(seq.children(1)))
                                                        }
                                                        terminal
                                                }
                                            }
                                            val children = childrenOf(body, Nonterminal("Symbol")) map { s => proxyIfNeeded(mapSymbol(s)) }
                                            if (children.length == 1) children.head else Symbols.Sequence(children, Set())
                                    }
                                    (name, productions)
                            }
                        }
                        if (nontermDefs.isEmpty) None else {
                            val startSymbolName = nontermDefs.head._1
                            Some(new NewGrammar("New Grammar", ListMap((nontermDefs map { kv => (kv._1, ListSet(kv._2: _*)) }): _*), Nonterminal(startSymbolName)))
                        }
                }
        }
    }
}
