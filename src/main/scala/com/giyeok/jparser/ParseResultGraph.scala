package com.giyeok.jparser

import ParseResultGraph._

case class ParseResultGraph(position: Int, length: Int, root: Node, nodes: Set[Node], edges: Set[Edge]) extends ParseResult {
    assert(position == root.position && length == root.length)
    assert(nodes contains root)
    assert(edges forall {
        case BindEdge(start, end) =>
            (nodes contains start) && (nodes contains end)
        case AppendEdge(start, end, _) =>
            (nodes contains start) && (nodes contains end)
        case JoinEdge(start, end, join) =>
            (nodes contains start) && (nodes contains end) && (nodes contains join)
    })

    lazy val outgoingEdges = edges groupBy { _.start }
    def outgoingOf(node: Node): Set[Edge] = outgoingEdges.getOrElse(node, Set())
}

object ParseResultGraphFunc extends ParseResultFunc[ParseResultGraph] {
    def terminal(position: Int, input: Inputs.Input): ParseResultGraph =
        ParseResultGraph(position, 1, Term(position, input), Set(Term(position, input)), Set())
    def bind(symbol: Symbols.Symbol, body: ParseResultGraph): ParseResultGraph = {
        val bindNode = Bind(body.position, body.length, symbol)
        if (body.nodes contains bindNode) {
            ParseResultGraph(body.position, body.length, body.root, body.nodes, body.edges + BindEdge(bindNode, body.root))
        } else {
            ParseResultGraph(body.position, body.length, bindNode, body.nodes + bindNode, body.edges + BindEdge(bindNode, body.root))
        }
    }
    def join(symbol: Symbols.Join, body: ParseResultGraph, join: ParseResultGraph): ParseResultGraph = {
        val position = body.position ensuring (body.position == join.position)
        val length = body.length ensuring (body.length == join.length)
        val joinNode = Join(position, length, symbol)
        val nodes = body.nodes ++ join.nodes
        if (nodes contains joinNode) {
            // 이런 경우가 생길 수 있나?
            ParseResultGraph(position, length, body.root, nodes, body.edges ++ join.edges + JoinEdge(joinNode, body.root, join.root))
        } else {
            ParseResultGraph(position, length, joinNode, nodes + joinNode, body.edges ++ join.edges + JoinEdge(joinNode, body.root, join.root))
        }
    }

    def sequence(position: Int, symbol: Symbols.Sequence): ParseResultGraph = {
        val emptyNode = Sequence(position, 0, symbol, 0)
        ParseResultGraph(position, 0, emptyNode, Set(emptyNode), Set())
    }
    def append(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        assert(sequence.root.isInstanceOf[Sequence])
        val sequenceRoot = sequence.root.asInstanceOf[Sequence]
        val appendedSequence = Sequence(sequence.position, sequence.length + child.length, sequenceRoot.symbol, sequenceRoot.pointer + 1)
        ParseResultGraph(sequence.position, sequence.length + child.length, appendedSequence, (sequence.nodes ++ child.nodes) + appendedSequence,
            (sequence.edges ++ child.edges) + AppendEdge(appendedSequence, sequence.root, true) + AppendEdge(appendedSequence, child.root, true))
    }
    def appendWhitespace(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        assert(sequence.root.isInstanceOf[Sequence])
        val sequenceRoot = sequence.root.asInstanceOf[Sequence]
        val appendedSequence = Sequence(sequence.position, sequence.length + child.length, sequenceRoot.symbol, sequenceRoot.pointer)
        ParseResultGraph(sequence.position, sequence.length + child.length, appendedSequence, (sequence.nodes ++ child.nodes) + appendedSequence,
            (sequence.edges ++ child.edges) + AppendEdge(appendedSequence, sequence.root, true) + AppendEdge(appendedSequence, child.root, false))
    }

    def merge(base: ParseResultGraph, merging: ParseResultGraph): ParseResultGraph = {
        val position = base.position ensuring (base.position == merging.position)
        val length = base.length ensuring (base.length == merging.length)
        val root = base.root
        // 보통은 base.root == merging.root 인데, 싸이클이 생기는 경우엔 아닐 수도 있음. 그럴 땐 base를 존중(사실 아무거나 써도 됨)
        if (base.root != merging.root) {
            assert(merging.nodes contains base.root)
        }
        ParseResultGraph(position, length, root, base.nodes ++ merging.nodes, base.edges ++ merging.edges)
    }

    def termFunc(): ParseResultGraph =
        ParseResultGraph(0, 1, TermFunc(0), Set(TermFunc(0)), Set())
    def substTermFunc(r: ParseResultGraph, position: Int, input: Inputs.Input): ParseResultGraph = {
        def substNode(node: Node): Node = node match {
            case TermFunc(p) => Term(p + position, input)
            case Term(p, input) => Term(p + position, input)
            case Sequence(p, length, symbol, pointer) =>
                Sequence(p + position, length, symbol, pointer)
            case Bind(p, length, symbol) => Bind(p + position, length, symbol)
            case Join(p, length, symbol) => Join(p + position, length, symbol)
        }
        def substEdge(edge: Edge): Edge = edge match {
            case BindEdge(start, end) => BindEdge(substNode(start), substNode(end))
            case AppendEdge(start, end, content) => AppendEdge(substNode(start), substNode(end), content)
            case JoinEdge(start, end, join) => JoinEdge(substNode(start), substNode(end), substNode(join))
        }
        ParseResultGraph(position, r.length, substNode(r.root), r.nodes map { substNode(_) }, r.edges map { substEdge(_) })
    }
}

object ParseResultGraph {
    sealed trait Node {
        val position: Int
        val length: Int

        def range = (position, position + length)
    }

    case class TermFunc(position: Int) extends Node {
        val length = 1
    }
    case class Term(position: Int, input: Inputs.Input) extends Node {
        val length = 1
    }
    case class Sequence(position: Int, length: Int, symbol: Symbols.Sequence, pointer: Int) extends Node
    case class Bind(position: Int, length: Int, symbol: Symbols.Symbol) extends Node
    case class Join(position: Int, length: Int, symbol: Symbols.Join) extends Node

    sealed trait Edge {
        val start: Node
    }
    case class BindEdge(start: Node, end: Node) extends Edge
    case class AppendEdge(start: Node, end: Node, content: Boolean) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge
}
