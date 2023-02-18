package com.giyeok.jparser

import com.giyeok.jparser.NGrammar.{NJoin, NSequence, NSymbol}
import com.giyeok.jparser.ParseResultGraph._

case class ParseResultGraph(left: Int, right: Int, root: Node, nodes: Set[Node], edges: Set[Edge]) extends ParseResult {
    assert(left == root.left && right == root.right)
    assert(nodes contains root)
    assert(edges forall { edge => edge.nodes subsetOf nodes })

    lazy val outgoingEdges = edges groupBy {
        _.start
    }

    def outgoingOf(node: Node): Set[Edge] = outgoingEdges.getOrElse(node, Set())

    def asParseForest: ParseForest = transform(ParseForestFunc)

    def transform[R <: ParseResult](resultFunc: ParseResultFunc[R]): R = {
        def reconstruct(node: Node, visited: Set[Node]): R = {
            if (visited contains node) {
                resultFunc.cyclicBind(node.left, node.right, node.asInstanceOf[NonTerm].symbol)
            } else {
                node match {
                    case Term(left, input) =>
                        resultFunc.terminal(left, input)
                    case Sequence(left, right, symbol, pointer) =>
                        // TODO Sequence는 Sequence대로 만들고 append하도록 수정
                        if (pointer == 0) {
                            resultFunc.sequence(left, right, symbol, pointer)
                        } else {
                            val outgoings = outgoingOf(node)
                            val bodies: Set[R] = outgoings collect {
                                case AppendEdge(_, end, true) =>
                                    val contents = resultFunc.merge(outgoings collect {
                                        case AppendEdge(_, child, false) if child.left == end.right =>
                                            reconstruct(child, visited + node)
                                    })
                                    val prev = reconstruct(end, visited + node)
                                    resultFunc.append(prev, contents)
                            }
                            resultFunc.merge(bodies)
                        }
                    case Bind(left, right, symbol) =>
                        val bodies: Set[R] = outgoingOf(node) map {
                            case BindEdge(_, end) =>
                                reconstruct(end, visited + node)
                            case _ => assert(false); ???
                        }
                        resultFunc.bind(left, right, symbol, resultFunc.merge(bodies))
                    case Join(left, right, symbol) =>
                        val bodies: Set[R] = outgoingOf(node) map {
                            case JoinEdge(_, end, join) =>
                                resultFunc.join(left, right, symbol, reconstruct(end, visited + node), reconstruct(join, visited + node))
                            case _ => assert(false); ???
                        }
                        resultFunc.merge(bodies)
                }
            }
        }

        reconstruct(root, Set())
    }
}

object ParseResultGraphFunc extends ParseResultFunc[ParseResultGraph] {
    def terminal(left: Int, input: Inputs.Input): ParseResultGraph =
        ParseResultGraph(left, left + 1, Term(left, input), Set(Term(left, input)), Set())

    def bind(left: Int, right: Int, symbol: NSymbol, body: ParseResultGraph): ParseResultGraph = {
        val bindNode = Bind(left, right, symbol) ensuring (left == body.left && right == body.right)
        if (body.nodes contains bindNode) {
            ParseResultGraph(left, right, body.root, body.nodes, body.edges + BindEdge(bindNode, body.root))
        } else {
            ParseResultGraph(left, right, bindNode, body.nodes + bindNode, body.edges + BindEdge(bindNode, body.root))
        }
    }

    def cyclicBind(left: Int, right: Int, symbol: NSymbol): ParseResultGraph = {
        val bindNode = Bind(left, right, symbol)
        ParseResultGraph(left, right, bindNode, Set(bindNode), Set())
    }

    def join(left: Int, right: Int, symbol: NJoin, body: ParseResultGraph, join: ParseResultGraph): ParseResultGraph = {
        assert(left == body.left && left == join.left)
        assert(right == body.right && right == join.right)
        val joinNode = Join(left, right, symbol)
        val nodes = body.nodes ++ join.nodes
        if (nodes contains joinNode) {
            // 이런 경우가 생길 수 있나?
            ParseResultGraph(left, right, body.root, nodes, body.edges ++ join.edges + JoinEdge(joinNode, body.root, join.root))
        } else {
            ParseResultGraph(left, right, joinNode, nodes + joinNode, body.edges ++ join.edges + JoinEdge(joinNode, body.root, join.root))
        }
    }

    def sequence(left: Int, right: Int, symbol: NSequence, pointer: Int): ParseResultGraph = {
        val emptyNode = Sequence(left, right, symbol, pointer)
        ParseResultGraph(left, right, emptyNode, Set(emptyNode), Set())
    }

    def append(sequence: ParseResultGraph, child: ParseResultGraph): ParseResultGraph = {
        assert(sequence.root.isInstanceOf[Sequence])
        assert(sequence.right == child.left)
        val sequenceRoot = sequence.root.asInstanceOf[Sequence]
        val appendedSequence = Sequence(sequence.left, child.right, sequenceRoot.symbol, sequenceRoot.pointer + 1)
        ParseResultGraph(sequence.left, child.right, appendedSequence, (sequence.nodes ++ child.nodes) + appendedSequence,
            (sequence.edges ++ child.edges) + AppendEdge(appendedSequence, sequence.root, true) + AppendEdge(appendedSequence, child.root, false))
    }

    def merge(base: ParseResultGraph, merging: ParseResultGraph): ParseResultGraph = {
        assert(base.left == merging.left && base.right == merging.right)
        // 보통은 base.root == merging.root 인데, 싸이클이 생기는 경우엔 아닐 수도 있음.
        // assert(merging.nodes contains base.root)
        ParseResultGraph(base.left, base.right, base.root, base.nodes ++ merging.nodes, base.edges ++ merging.edges)
    }
}

object ParseResultGraph {

    sealed trait Node {
        val left: Int
        val right: Int

        def range: (Int, Int) = (left, right)
    }

    sealed trait NonTerm extends Node {
        val symbol: NSymbol
    }

    case class Term(left: Int, input: Inputs.Input) extends Node {
        val right = left + 1

        override val hashCode: Int = (left, input).hashCode()
    }

    case class Sequence(left: Int, right: Int, symbol: NSequence, pointer: Int) extends NonTerm {
        override val hashCode: Int = (left, right, symbol, pointer).hashCode()
    }

    case class Bind(left: Int, right: Int, symbol: NSymbol) extends NonTerm {
        override val hashCode: Int = (left, right, symbol).hashCode()
    }

    case class Join(left: Int, right: Int, symbol: NJoin) extends NonTerm {
        override val hashCode: Int = (left, right, symbol).hashCode()
    }

    sealed trait Edge {
        val start: Node

        def nodes: Set[Node]
    }

    case class BindEdge(start: Node, end: Node) extends Edge {
        def nodes = Set(start, end)

        override val hashCode: Int = (start, end).hashCode()
    }

    case class AppendEdge(start: Node, end: Node, isBase: Boolean) extends Edge {
        def nodes = Set(start, end)

        override val hashCode: Int = (start, end, isBase).hashCode()
    }

    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge {
        def nodes = Set(start, end, join)

        override val hashCode: Int = (start, end, join).hashCode()
    }

}
