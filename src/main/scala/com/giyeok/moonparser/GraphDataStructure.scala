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
    case class JoinEdge(start: JoinProgress, end: Node, constraint: Node, endConstraintReversed: Boolean) extends DeriveEdge {
        override val nodes = Set(start, end, constraint)
        override def endTo(end: Node): Boolean = super.endTo(end) || end == this.constraint
        def toShortString = s"${start.toShortString} -> ${end.toShortString} & ${constraint.toShortString}${if (endConstraintReversed) " (reverse)" else ""}"
    }
    sealed abstract class KillEdge extends Edge
    case class LiftTriggeredLiftKillEdge(start: Node, end: Node) extends KillEdge {
        def toShortString = s"${start.toShortString} -X> ${end.toShortString}"
    }
    case class LiftTriggeredNodeKillEdge(start: Node, end: Node) extends KillEdge {
        def toShortString = s"${start.toShortString} -XX> ${end.toShortString}"
    }
    case class AliveTriggeredNodeKillEdge(start: Node, end: Node) extends KillEdge {
        def toShortString = s"${start.toShortString} =XX> ${end.toShortString}"
    }

    case class Graph(nodes: Set[Node], edges: Set[Edge])

    implicit class AugEdges(edges: Set[Edge]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        def deriveEdges: Set[DeriveEdge] = edges collect { case e: DeriveEdge => e }
        def assassinEdges: Set[KillEdge] = edges collect { case e: KillEdge => e }
        def liftTriggeredLiftKillEdges: Set[LiftTriggeredLiftKillEdge] = edges collect { case e: LiftTriggeredLiftKillEdge => e }
        def liftTriggeredNodeKillEdges: Set[LiftTriggeredNodeKillEdge] = edges collect { case e: LiftTriggeredNodeKillEdge => e }
        def aliveTriggeredNodeKillEdges: Set[AliveTriggeredNodeKillEdge] = edges collect { case e: AliveTriggeredNodeKillEdge => e }

        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.end == node }
        def incomingDeriveEdgesOf(node: Node): Set[DeriveEdge] = deriveEdges filter { _.end == node }
        def incomingEdgesOf(node: Node): Set[Edge] = edges filter { _.endTo(node) }
        def outgoingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.start == node }

        def rootsOf(node: Node): Set[Edge] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[Edge]): Set[Edge] =
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
