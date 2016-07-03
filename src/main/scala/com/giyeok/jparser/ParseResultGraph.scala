package com.giyeok.jparser

import ParseResultGraph._

case class ParseResultGraph(length: Int, root: Node, nodes: Set[Node], edges: Set[Edge]) extends ParseResult {
    def shift(distance: Int): ParseResultGraph = {
        val shiftedNodes = nodes map { _.shift(distance) }
        val shiftedEdges = edges map { _.shift(distance) }
        ParseResultGraph(length, root.shift(distance), shiftedNodes, shiftedEdges)
    }
}

object ParseResultGraphFunc extends ParseResultFunc[ParseResultGraph] {
    def terminal(input: Inputs.Input): ParseResultGraph =
        ParseResultGraph(1, Term(0, input), Set(Term(0, input)), Set())
    def bind(symbol: Symbols.Symbol, body: ParseResultGraph): ParseResultGraph = {
        val bindNode = Bind(0, body.length, symbol)
        if (body.nodes contains bindNode) {
            ParseResultGraph(body.length, body.root, body.nodes, body.edges + BindEdge(body.root, bindNode))
        } else {
            ParseResultGraph(body.length, bindNode, body.nodes + bindNode, body.edges + BindEdge(body.root, bindNode))
        }
    }
    def join(symbol: Symbols.Join, body: ParseResultGraph, join: ParseResultGraph): ParseResultGraph = {
        val length = body.length ensuring (body.length == join.length)
        val joinNode = Join(0, length, symbol)
        val nodes = body.nodes ++ join.nodes
        if (nodes contains joinNode) {
            // 이런 경우가 생길 수 있나?
            ParseResultGraph(length, body.root, nodes, body.edges + JoinEdge(joinNode, body.root, join.root))
        } else {
            ParseResultGraph(length, joinNode, nodes, body.edges + JoinEdge(joinNode, body.root, join.root))
        }
    }

    def sequence(): ParseResultGraph =
        ParseResultGraph(0, Empty(0), Set(Empty(0)), Set())
    def append(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        val shiftedChild = child.shift(sequence.length)
        ParseResultGraph(sequence.length + child.length, sequence.root, sequence.nodes ++ shiftedChild.nodes, sequence.edges ++ shiftedChild.edges + AppendEdge(sequence.root, shiftedChild.root, true))
    }
    def appendWhitespace(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        val shiftedChild = child.shift(sequence.length)
        ParseResultGraph(sequence.length + child.length, sequence.root, sequence.nodes ++ shiftedChild.nodes, sequence.edges ++ shiftedChild.edges + AppendEdge(sequence.root, shiftedChild.root, false))
    }

    def merge(base: ParseResultGraph, merging: ParseResultGraph): ParseResultGraph = {
        val length = base.length ensuring (base.length == merging.length)
        val root = base.root ensuring (base.root == merging.root)
        ParseResultGraph(length, root, base.nodes ++ merging.nodes, base.edges ++ merging.edges)
    }

    def termFunc(): ParseResultGraph =
        ParseResultGraph(1, TermFunc(0), Set(TermFunc(0)), Set())
    def substTermFunc(r: ParseResultGraph, input: Inputs.Input): ParseResultGraph = {
        def substNode(node: Node): Node = node match {
            case TermFunc(position) => Term(position, input)
            case d => d
        }
        def substEdge(edge: Edge): Edge = edge match {
            case BindEdge(start, end) => BindEdge(substNode(start), substNode(end))
            case AppendEdge(start, end, content) => AppendEdge(substNode(start), substNode(end), content)
            case JoinEdge(start, end, join) => JoinEdge(substNode(start), substNode(end), substNode(join))
        }
        ParseResultGraph(r.length, substNode(r.root), r.nodes map { substNode(_) }, r.edges map { substEdge(_) })
    }
}

object ParseResultGraph {
    sealed trait Node {
        val position: Int
        val length: Int
        def shift(distance: Int): Node

        val range = (position, position + length)
    }

    case class TermFunc(position: Int) extends Node {
        val length = 1
        def shift(distance: Int) = TermFunc(position + distance)
    }
    case class Term(position: Int, input: Inputs.Input) extends Node {
        val length = 1
        def shift(distance: Int) = Term(position + distance, input)
    }
    case class Join(position: Int, length: Int, symbol: Symbols.Join) extends Node {
        def shift(distance: Int) = Join(position + distance, length, symbol)
    }
    case class Bind(position: Int, length: Int, symbol: Symbols.Symbol) extends Node {
        def shift(distance: Int) = Bind(position + distance, length, symbol)
    }
    case class Empty(position: Int) extends Node {
        val length = 0
        def shift(distance: Int) = Empty(position + distance)
    }

    sealed trait Edge {
        def shift(distance: Int): Edge
    }
    case class BindEdge(start: Node, end: Node) extends Edge {
        def shift(distance: Int): Edge = BindEdge(start.shift(distance), end.shift(distance))
    }
    case class AppendEdge(start: Node, end: Node, content: Boolean) extends Edge {
        val whitespace = !content
        def shift(distance: Int): Edge = AppendEdge(start.shift(distance), end.shift(distance), content)
    }
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge {
        def shift(distance: Int): Edge = JoinEdge(start.shift(distance), end.shift(distance), join.shift(distance))
    }
}
