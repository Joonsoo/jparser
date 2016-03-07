package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait SymbolsGraph {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = SymbolProgressTerminal
    type NonterminalNode = SymbolProgressNonterminal

    sealed abstract class Edge {
        val from: Node
        val to: Node
        val nodes = Set(from, to)

        def toShortString: String
        override def toString = toShortString
    }
    case class SimpleEdge(from: NonterminalNode, to: Node) extends Edge {
        def toShortString = s"${from.toShortString} -> ${to.toShortString}"
    }
    sealed abstract class AssassinEdge0 extends Edge
    case class LiftAssassinEdge(from: Node, to: Node) extends AssassinEdge0 {
        def toShortString = s"${from.toShortString} -X> ${to.toShortString}"
    }
    case class EagerAssassinEdge(from: Node, to: Node) extends AssassinEdge0 {
        def toShortString = s"${from.toShortString} -XX> ${to.toShortString}"
    }
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge])
}
