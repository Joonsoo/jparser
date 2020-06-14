package com.giyeok.jparser.metalang3.graphs

import com.giyeok.jparser.metalang3.graphs.GrammarGraph.{Edge, Node}
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

// GrammarGraph는 GrammarGraphGen이 만들어내는 수동적 그래프
class GrammarGraph private(val nodes: Set[Node], val edges: Set[Edge], val edgesByStart: Map[Node, Set[Edge]], val edgesByEnd: Map[Node, Set[Edge]]) extends AbstractGraph[Node, Edge, GrammarGraph] {
    def createGraph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByEnd: Map[Node, Set[Edge]]): GrammarGraph =
        new GrammarGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object GrammarGraph {
    val empty = new GrammarGraph(Set(), Set(), Map(), Map())

    sealed class Node

    case class ParamNode(className: String, index: Int) extends Node

    abstract sealed class Edge extends AbstractEdge[Node] {
        val start: Node
        val end: Node
    }

}
