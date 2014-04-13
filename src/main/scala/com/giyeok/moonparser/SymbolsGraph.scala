package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait SymbolsGraph {
    this: Parser =>

    type Node = SymbolProgress
    sealed abstract class EdgeEnd {
        def toEdge(from: Node): Edge
    }
    case class SimpleEdgeEnd(to: Node) extends EdgeEnd {
        def toEdge(from: Node) = SimpleEdge(from, this)
    }
    case class DoubleEdgeEnd(to: Node, doub: Node) extends EdgeEnd {
        def toEdge(from: Node) = DoubleEdge(from, this)
    }

    sealed abstract class Edge {
        val nodes: Set[Node]
        val end: EdgeEnd
    }
    case class SimpleEdge(from: Node, end: SimpleEdgeEnd) extends Edge {
        val nodes = Set(from, end.to)
    }
    case class DoubleEdge(from: Node, end: DoubleEdgeEnd) extends Edge {
        val nodes = Set(from, end.to, end.doub)
    }

    sealed abstract class LiftingEdge {
        val edge: Edge
        val parsed: ParsedSymbol[Symbol]
    }
    case class LiftingSimpleEdge(edge: SimpleEdge, parsed: ParsedSymbol[Symbol]) extends LiftingEdge
    case class LiftingDoubleEdge(edge: DoubleEdge, parsed: ParsedSymbol[Symbol], doub: Option[ParsedSymbol[Symbol]]) extends LiftingEdge

    case class Graph(nodes: Set[Node], edges: Set[Edge])
}
