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
    case class EagerAssassinEdge(from: Node, to: Node) extends Edge
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge]) {
        val simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        val eagerAssassinEdges: Set[EagerAssassinEdge] = edges collect { case e: EagerAssassinEdge => e }

        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.to == node }
        def outgoingEdges(node: Node): Set[Edge] = ???

        def trackRootsOf(node: Node): Set[SimpleEdge] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[SimpleEdge]): Set[SimpleEdge] =
                queue match {
                    case node +: rest =>
                        val incomings = incomingSimpleEdgesOf(node) -- cc
                        trackRoots(rest ++ (incomings.toList map { _.from }), cc ++ incomings)
                    case List() => cc
                }
            trackRoots(List(node), Set())
        }
    }
}
