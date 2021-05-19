package com.giyeok.jparser.fast

import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Node}

case class GraphNoIndex(nodes: Set[Node], edges: Set[Edge])

object GraphNoIndex {
  def fromGraph(graph: Graph): GraphNoIndex = GraphNoIndex(graph.nodes, graph.edges)
}
