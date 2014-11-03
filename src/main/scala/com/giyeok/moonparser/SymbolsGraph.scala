package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait SymbolsGraph {
    this: Parser =>

    type Node = SymbolProgress

    sealed abstract class Edge {
        val nodes: Set[Node]
        val from: Node
    }
    case class SimpleEdge(from: Node, to: Node) extends Edge {
        val nodes = Set(from, to)
    }
    case class DoubleEdge(from: Node, to: Node, doub: Node) extends Edge {
        val nodes = Set(from, to, doub)
    }
    case class AssassinEdge(from: Node, to: Node) extends Edge {
        // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
        // is contagious - the nodes derived from `to` will be the target of `from` (recursively)
        val nodes = Set(from, to)
    }

    case class Graph(nodes: Set[Node], edges: Set[Edge]) {
        def incomingEdges(node: Node): Set[Edge] = ???
        def outgoingEdges(node: Node): Set[Edge] = ???
    }
}
