package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait GraphDataStructure {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = SymbolProgressTerminal
    type NonterminalNode = SymbolProgressNonterminal

    sealed abstract class Edge {
        val start: Node
        val end: Node
        val nodes = Set(start, end)

        def endTo(end: Node): Boolean = (this.end == end)

        def toShortString: String
        override def toString = toShortString
    }
    sealed abstract class DeriveEdge extends Edge {
        override val start: NonterminalNode
    }
    case class SimpleEdge(start: NonterminalNode, end: Node) extends DeriveEdge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString}"
    }
    case class JoinEdge(start: NonterminalNode, end: Node, constraint: Node, endConstraintReversed: Boolean) extends DeriveEdge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString} & ${constraint.toShortString}${if (endConstraintReversed) " (reverse)" else ""}"
        override def endTo(end: Node): Boolean = super.endTo(end) || end == this.constraint
    }
    sealed abstract class AssassinEdge extends Edge
    case class LiftAssassinEdge(start: Node, end: Node) extends AssassinEdge {
        def toShortString = s"${start.toShortString} -X> ${end.toShortString}"
    }
    case class EagerAssassinEdge(start: Node, end: Node) extends AssassinEdge {
        def toShortString = s"${start.toShortString} -XX> ${end.toShortString}"
    }
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge])

    implicit class AugEdges[T <: Edge](edges: Set[T]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        def deriveEdges: Set[DeriveEdge] = edges collect { case e: DeriveEdge => e }
        def assassinEdges: Set[AssassinEdge] = edges collect { case e: AssassinEdge => e }
        def liftAssassinEdges: Set[LiftAssassinEdge] = edges collect { case e: LiftAssassinEdge => e }
        def eagerAssassinEdges: Set[EagerAssassinEdge] = edges collect { case e: EagerAssassinEdge => e }

        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.endTo(node) }
        def incomingDeriveEdgesOf(node: Node): Set[DeriveEdge] = deriveEdges filter { _.endTo(node) }
        def incomingEdgesOf(node: Node): Set[T] = edges filter { _.endTo(node) }
        def outgoingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.start == node }

        def rootsOf(node: Node): Set[T] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[T]): Set[T] =
                queue match {
                    case node +: rest =>
                        val incomings = incomingEdgesOf(node) -- cc
                        trackRoots(rest ++ (incomings.toList map { _.start }), cc ++ incomings)
                    case List() => cc
                }
            trackRoots(List(node), Set())
        }
    }

}
