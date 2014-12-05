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
    }
    case class SimpleEdge(from: NonterminalNode, to: Node) extends Edge
    case class AssassinEdge(from: Node, to: Node) extends Edge
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge]) {
        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] =
            edges collect { case e: SimpleEdge => e } filter { _.to == node }
        def outgoingEdges(node: Node): Set[Edge] = ???
    }
}