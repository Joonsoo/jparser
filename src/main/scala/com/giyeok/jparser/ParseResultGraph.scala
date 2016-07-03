package com.giyeok.jparser

import ParseResultGraph._

case class ParseResultGraph(length: Int, root: Node, nodes: Set[Node], edges: Set[Edge]) extends ParseResult {
}

object ParseResultGraphFunc extends ParseResultFunc[ParseResultGraph] {
    def terminal(position: Int, input: Inputs.Input): ParseResultGraph =
        ParseResultGraph(1, Term(position, input), Set(Term(position, input)), Set())
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

    def sequence(position: Int): ParseResultGraph =
        ParseResultGraph(0, Empty(position), Set(Empty(position)), Set())
    def append(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        ParseResultGraph(sequence.length + child.length, sequence.root, sequence.nodes ++ child.nodes, sequence.edges ++ child.edges + AppendEdge(sequence.root, child.root, true))
    }
    def appendWhitespace(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        ParseResultGraph(sequence.length + child.length, sequence.root, sequence.nodes ++ child.nodes, sequence.edges ++ child.edges + AppendEdge(sequence.root, child.root, false))
    }

    def merge(base: ParseResultGraph, merging: ParseResultGraph): ParseResultGraph = {
        val length = base.length ensuring (base.length == merging.length)
        if (base.root != merging.root) {
            println(base.root)
            println(merging.root)
        }
        val root = base.root ensuring (base.root == merging.root)
        ParseResultGraph(length, root, base.nodes ++ merging.nodes, base.edges ++ merging.edges)
    }

    def termFunc(): ParseResultGraph =
        ParseResultGraph(1, TermFunc(0), Set(TermFunc(0)), Set())
    def substTermFunc(r: ParseResultGraph, position: Int, input: Inputs.Input): ParseResultGraph = {
        def substNode(node: Node): Node = node match {
            case TermFunc(p) => Term(p + position, input)
            case Term(p, input) => Term(p + position, input)
            case Join(p, length, symbol) => Join(p + position, length, symbol)
            case Bind(p, length, symbol) => Bind(p + position, length, symbol)
            case Empty(p) => Empty(p + position)
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

        val range = (position, position + length)
    }

    case class TermFunc(position: Int) extends Node {
        val length = 1
    }
    case class Term(position: Int, input: Inputs.Input) extends Node {
        val length = 1
    }
    case class Join(position: Int, length: Int, symbol: Symbols.Join) extends Node
    case class Bind(position: Int, length: Int, symbol: Symbols.Symbol) extends Node
    case class Empty(position: Int) extends Node {
        val length = 0
    }

    sealed trait Edge
    case class BindEdge(start: Node, end: Node) extends Edge
    case class AppendEdge(start: Node, end: Node, content: Boolean) extends Edge {
        val whitespace = !content
    }
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge
}
