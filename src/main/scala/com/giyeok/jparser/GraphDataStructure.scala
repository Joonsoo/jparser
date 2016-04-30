package com.giyeok.jparser

import com.giyeok.jparser.ParseTree._
import com.giyeok.jparser.Symbols._

trait GraphDataStructure {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = TerminalSymbolProgress
    type NonterminalNode = NonterminalSymbolProgress

    sealed abstract class DeriveEdge {
        val start: NonterminalNode
        val end: Node
        val nodes = Set(start, end)

        def endTo(end: Node): Boolean = (this.end == end)

        def toShortString: String
        override def toString = toShortString
    }
    case class SimpleEdge(start: NonterminalNode, end: Node) extends DeriveEdge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString}"
    }
    case class JoinEdge(start: JoinSymbolProgress, end: Node, constraint: Node, endConstraintReversed: Boolean) extends DeriveEdge {
        override val nodes = Set(start, end, constraint)
        override def endTo(end: Node): Boolean = super.endTo(end) || end == this.constraint
        def toShortString = s"${start.toShortString} -> ${end.toShortString} & ${constraint.toShortString}${if (endConstraintReversed) " (reverse)" else ""}"
    }

    implicit class AugEdges(edges: Set[DeriveEdge]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }

        def incomingEdgesOf(node: Node): Set[DeriveEdge] = edges filter { _.end == node }
        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.end == node }
        def outgoingEdgesOf(node: Node): Set[DeriveEdge] = edges filter { _.start == node }
        def outgoingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.start == node }

        def rootsOf(node: Node): Set[DeriveEdge] = {
            def trackRoots(queue: List[SymbolProgress], cc: Set[DeriveEdge]): Set[DeriveEdge] =
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
