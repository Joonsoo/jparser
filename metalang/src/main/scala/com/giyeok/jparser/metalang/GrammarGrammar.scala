package com.giyeok.jparser.metalang

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.NGrammar.{NLongest, NNonterminal, NStart, NTerminal}
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.{Grammar, Inputs, ParseResultTree, Symbols}
import com.giyeok.jparser.Symbols._

import scala.collection.immutable.{ListMap, ListSet}

object GrammarGrammar extends Grammar {
    val whitespace = chars(" \t\n\r").star
    val inlineWS = chars(" \t").star
    val name = "Grammar Notation"
    val rules: RuleMap = ListMap(
        "Grammar" -> ListSet(
            seq(
                whitespace,
                n("Rules"),
                whitespace
            )
        ),
        "Rules" -> ListSet(
            seqWS(whitespace, n("Rules"), n("NontermDef")),
            n("NontermDef")
        ),
        "NontermDef" -> ListSet(
            seqWS(inlineWS, n("NontermName"), c('='), n("Productions"))
        ),
        "Productions" -> ListSet(
            n("Production"),
            seq(n("Productions"), whitespace, c('|'), inlineWS, n("Production"))
        ),
        "Production" -> ListSet(
            n("Empty"),
            n("Symbols")
        ),
        "Empty" -> ListSet(
            i("<empty>")
        ),
        "Symbols" -> ListSet(
            n("Symbol"),
            seqWS(inlineWS, n("Symbols"), n("Symbol"))
        ),
        "Symbol" -> ListSet(
            n("NontermName"),
            n("LongestName"),
            n("LookaheadName"),
            n("LookaheadExName"),
            n("IntersectionName"),
            n("ExclusionName"),
            n("Terminal")
        ),
        "NontermName" -> ListSet(
            longest(oneof(
                oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9')).plus,
                seq(c('`'), oneof(chars('a' to 'z', 'A' to 'Z', '0' to '9'), chars("+*[]-")).plus)
            ))
        ),
        "LongestName" -> ListSet(
            longest(seqWS(inlineWS, c('L'), c('('), n("Symbol"), c(')')))
        ),
        "LookaheadName" -> ListSet(
            longest(seqWS(inlineWS, i("la"), c('('), n("Symbol"), c(')')))
        ),
        "LookaheadExName" -> ListSet(
            longest(seqWS(inlineWS, i("lx"), c('('), n("Symbol"), c(')')))
        ),
        "IntersectionName" -> ListSet(
            longest(seqWS(inlineWS, n("Symbol"), c('&'), n("Symbol")))
        ),
        "ExclusionName" -> ListSet(
            longest(seqWS(inlineWS, n("Symbol"), c('-'), n("Symbol")))
        ),
        "Terminal" -> ListSet(
            n("TerminalExactChar"),
            n("TerminalRanges"),
            n("TerminalSet")
        ),
        "TerminalExactChar" -> ListSet(
            seq(c('\''), anychar, c('\''))
        ),
        "TerminalRanges" -> ListSet(
            seq(c('['), n("TerminalRange").plus, c(']'))
        ),
        "TerminalRange" -> ListSet(
            seq(anychar, c('-'), anychar)
        ),
        "TerminalSet" -> ListSet(
            seq(c('{'), n("TerminalAnyChar").plus, c('}'))
        ),
        "TerminalAnyChar" -> ListSet(
            anychar.except(c('}'))
        )
    )
    val startSymbol = n("Grammar")

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
        case TerminalNode(_, Inputs.Character(c)) => s"$c"
        case _ => ???
    }

    class NewGrammar(val name: String, val rules: ListMap[String, ListSet[Symbols.Symbol]], val startSymbol: Symbols.Nonterminal) extends Grammar

    def translate(tree: ParseResultTree.Node): Option[Grammar] = {
        val BindNode(_: NStart, BindNode(NNonterminal(_, Nonterminal("Grammar"), _), seq: SequenceNode)) = tree
        val BindNode(NNonterminal(_, Nonterminal("Rules"), _), body) = seq.children(1)
        val nontermDefs: Seq[(String, Seq[Symbols.Symbol])] = childrenOf(body, Nonterminal("NontermDef")) collect {
            case BindNode(NNonterminal(_, Nonterminal("NontermDef"), _), seq: SequenceNode) =>
                val name = textOf(seq.children(0))
                val productions: Seq[Symbol] = childrenOf(seq.children(2), Nonterminal("Production")) collect {
                    case BindNode(NNonterminal(_, Nonterminal("Production"), _), BindNode(NNonterminal(_, Nonterminal("Empty"), _), _)) => empty
                    case BindNode(NNonterminal(_, Nonterminal("Production"), _), BindNode(NNonterminal(_, Nonterminal("Symbols"), _), body)) =>
                        def mapSymbol(node: Node): Symbols.Symbol = {
                            val BindNode(NNonterminal(_, Nonterminal("Symbol"), _), body) = node
                            body match {
                                case BindNode(NNonterminal(_, Nonterminal("NontermName"), _), body) =>
                                    Nonterminal(textOf(body))
                                case BindNode(NNonterminal(_, Nonterminal("LongestName"), _), BindNode(_: NLongest, seq: SequenceNode)) =>
                                    longest(mapSymbol(seq.children(2)))
                                case BindNode(NNonterminal(_, Nonterminal("LookaheadName"), _), BindNode(_: NLongest, seq: SequenceNode)) =>
                                    lookahead_is(mapSymbol(seq.children(2)))
                                case BindNode(NNonterminal(_, Nonterminal("LookaheadExName"), _), BindNode(_: NLongest, seq: SequenceNode)) =>
                                    lookahead_except(mapSymbol(seq.children(2)))
                                case BindNode(NNonterminal(_, Nonterminal("IntersectionName"), _), BindNode(_: NLongest, seq: SequenceNode)) =>
                                    join(mapSymbol(seq.children(0)), mapSymbol(seq.children(2)))
                                case BindNode(NNonterminal(_, Nonterminal("ExclusionName"), _), BindNode(_: NLongest, seq: SequenceNode)) =>
                                    mapSymbol(seq.children(0)).except(mapSymbol(seq.children(2)))
                                case BindNode(NNonterminal(_, Nonterminal("Terminal"), _), body) =>
                                    val terminal: Terminal = body match {
                                        case BindNode(NNonterminal(_, Nonterminal("TerminalExactChar"), _), seq: SequenceNode) =>
                                            chars(textOf(seq.children(1)))
                                        case BindNode(NNonterminal(_, Nonterminal("TerminalRanges"), _), seq: SequenceNode) =>
                                            val chars: Set[Char] = (childrenOf(seq.children(1), Nonterminal("TerminalRange")) flatMap {
                                                case BindNode(NNonterminal(_, Nonterminal("TerminalRange"), _), seq: SequenceNode) =>
                                                    val BindNode(NTerminal(_, AnyChar), TerminalNode(_, Inputs.Character(c1))) = seq.children(0)
                                                    val BindNode(NTerminal(_, AnyChar), TerminalNode(_, Inputs.Character(c2))) = seq.children(2)
                                                    (c1 to c2).toSet
                                                case _ => ???
                                            }).toSet
                                            Chars(chars)
                                        case BindNode(NNonterminal(_, Nonterminal("TerminalSet"), _), seq: SequenceNode) =>
                                            chars(textOf(seq.children(1)))
                                        case _ => ???
                                    }
                                    terminal
                                case _ => ???
                            }
                        }
                        val children = childrenOf(body, Nonterminal("Symbol")) map { s => proxyIfNeeded(mapSymbol(s)) }
                        if (children.length == 1) children.head else Symbols.Sequence(children)
                }
                (name, productions)
        }
        if (nontermDefs.isEmpty) None else {
            val startSymbolName = nontermDefs.head._1
            Some(new NewGrammar("New Grammar", ListMap((nontermDefs map { kv => (kv._1, ListSet(kv._2: _*)) }): _*), Nonterminal(startSymbolName)))
        }
    }
}
