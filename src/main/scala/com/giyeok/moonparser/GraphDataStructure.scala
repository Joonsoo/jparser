package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait GraphDataStructure {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = SymbolProgressTerminal
    type NonterminalNode = SymbolProgressNonterminal

    sealed abstract class Edge(val start: Node, val ends: Set[Node]) extends Ordered[Edge] {
        val nodes = ends + start

        def toShortString: String
        override def toString = toShortString

        def compare(that: Edge): Int = {
            val my = List(start.id) ++ (ends.toList map { _.id }).sorted
            val other = List(that.start.id) ++ (that.ends.toList map { _.id }).sorted

            my.zipAll(other, 0, 0).find(p => p._1 != p._2) match {
                case Some((a, b)) => a - b
                case None => 0
            }
        }
    }
    object DeriveEdge {
        def apply(from: NonterminalNode, to: Node) = new DeriveEdge(from, Set(to))
    }
    case class DeriveEdge(start: NonterminalNode, ends: Set[Node]) extends Edge(start, ends) {
        def toShortString = s"${start.toShortString} -> ${ends map { _.toShortString } mkString " & "}"
    }

    sealed abstract class AssassinEdge0(start: Node, end: Node) extends Edge(start, Set(end))
    case class LiftAssassinEdge(start: Node, end: Node) extends AssassinEdge0(start, end) {
        def toShortString = s"${start.toShortString} -X> ${end.toShortString}"
    }
    case class EagerAssassinEdge(start: Node, end: Node) extends AssassinEdge0(start, end) {
        def toShortString = s"${start.toShortString} -XX> ${end.toShortString}"
    }
    // if `start` lives, `end` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `end` will be the target of `start` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge])
}
