package com.giyeok.jparser.nparser2

import com.giyeok.jparser.graph.{AbstractEdge, AbstractGraph}
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.Kernel

case class Edge(start: Kernel, end: Kernel) extends AbstractEdge[Kernel]

case class KernelGraph(nodes: Set[Kernel], edges: Set[Edge], edgesByStart: Map[Kernel, Set[Edge]], edgesByEnd: Map[Kernel, Set[Edge]]) extends AbstractGraph[Kernel, Edge, KernelGraph] {
  override def createGraph(nodes: Set[Kernel], edges: Set[Edge], edgesByStart: Map[Kernel, Set[Edge]], edgesByEnd: Map[Kernel, Set[Edge]]): KernelGraph =
    KernelGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object KernelGraph {
  def apply(nodes: Set[Kernel], edges: Set[Edge]): KernelGraph =
    AbstractGraph(nodes, edges, KernelGraph.apply)
}

case class ParsingContext(graph: KernelGraph, acceptConditions: Map[Kernel, AcceptCondition])
