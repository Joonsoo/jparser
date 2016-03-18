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

        def toShortString: String
        override def toString = toShortString
    }
    case class SimpleEdge(start: NonterminalNode, end: Node) extends Edge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString}"
    }
    case class JoinEdge(start: NonterminalNode, end: Node, contraint: Node, reversed: Boolean) extends Edge {
        def toShortString = s"${start.toShortString} -> ${end.toShortString} & ${contraint.toShortString}${if (reversed) " (reverse)" else ""}"
    }
    sealed abstract class AssassinEdge0 extends Edge
    case class LiftAssassinEdge(start: Node, end: Node) extends AssassinEdge0 {
        def toShortString = s"${start.toShortString} -X> ${end.toShortString}"
    }
    case class EagerAssassinEdge(start: Node, end: Node) extends AssassinEdge0 {
        def toShortString = s"${start.toShortString} -XX> ${end.toShortString}"
    }
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge])

    implicit class AugEdges[T <: Edge](edges: Set[T]) {
        def simpleEdges: Set[SimpleEdge] = edges collect { case e: SimpleEdge => e }
        def assassinEdges: Set[AssassinEdge0] = edges collect { case e: AssassinEdge0 => e }
        def liftAssassinEdges: Set[LiftAssassinEdge] = edges collect { case e: LiftAssassinEdge => e }
        def eagerAssassinEdges: Set[EagerAssassinEdge] = edges collect { case e: EagerAssassinEdge => e }

        def incomingSimpleEdgesOf(node: Node): Set[SimpleEdge] = simpleEdges filter { _.end == node }
        def incomingEdgesOf(node: Node): Set[T] = edges filter { _.end == node }
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
