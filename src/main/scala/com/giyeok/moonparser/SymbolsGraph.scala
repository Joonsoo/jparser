package com.giyeok.moonparser

import com.giyeok.moonparser.ParseTree._
import com.giyeok.moonparser.Symbols._

trait SymbolsGraph {
    this: Parser =>

    type Node = SymbolProgress
    type TerminalNode = SymbolProgressTerminal
    type NonterminalNode = SymbolProgressNonterminal

    sealed abstract class Edge(val from: Node, val tos: Set[Node]) extends Ordered[Edge] {
        val nodes = tos + from

        def toShortString: String
        override def toString = toShortString

        def compare(that: Edge): Int = {
            val my = List(from.id) ++ (tos.toList map { _.id }).sorted
            val other = List(that.from.id) ++ (that.tos.toList map { _.id }).sorted

            my.zipAll(other, 0, 0).find(p => p._1 != p._2) match {
                case Some((a, b)) => a - b
                case None => 0
            }
        }
    }
    object DeriveEdge {
        def apply(from: NonterminalNode, to: Node) = new DeriveEdge(from, Set(to))
    }
    case class DeriveEdge(from: NonterminalNode, to: Set[Node]) extends Edge(from, to) {
        def toShortString = s"${from.toShortString} -> ${to map { _.toShortString } mkString " & "}"
    }
    // case class SimpleEdge(from: NonterminalNode, to: Node) extends DeriveEdge(from, Set(to))

    sealed abstract class AssassinEdge0(from: Node, to: Node) extends Edge(from, Set(to))
    case class LiftAssassinEdge(from: Node, to: Node) extends AssassinEdge0(from, to) {
        def toShortString = s"${from.toShortString} -X> ${to.toShortString}"
    }
    case class EagerAssassinEdge(from: Node, to: Node) extends AssassinEdge0(from, to) {
        def toShortString = s"${from.toShortString} -XX> ${to.toShortString}"
    }
    // if `from` lives, `to` will be killed - is used to implement lookahead except and backup
    // is contagious - the nodes derived from `to` will be the target of `from` (recursively)

    case class Graph(nodes: Set[Node], edges: Set[Edge])
}
