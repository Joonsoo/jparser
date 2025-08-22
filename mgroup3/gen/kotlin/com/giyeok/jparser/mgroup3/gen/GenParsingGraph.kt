package com.giyeok.jparser.mgroup3.gen

class GenParsingGraph(
  val startNode: GenNode,
  val nodes: MutableSet<GenNode>,
  val edges: MutableSet<Pair<GenNode, GenNode>>,
  val edgesByStart: MutableMap<GenNode, MutableSet<GenNode>>,
  val edgesByEnd: MutableMap<GenNode, MutableSet<GenNode>>,
  val observingCondSymbolIds: MutableSet<Int>,
  val acceptConditions: MutableMap<GenNode, Set<GenAcceptCondition>>,
  // key -> value 로 progress되었음
  val progressedTo: MutableMap<GenNode, Pair<GenNode, Set<GenAcceptCondition>>>,
) {
  fun clone(): GenParsingGraph = GenParsingGraph(
    startNode,
    nodes.toMutableSet(),
    edges.toMutableSet(),
    edgesByStart.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    edgesByEnd.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    observingCondSymbolIds.toMutableSet(),
    acceptConditions.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    progressedTo.toMutableMap(),
  )

  fun addNode(node: GenNode): Boolean {
    val isNewNode = node !in nodes
    nodes.add(node)
    acceptConditions.getOrPut(node) { mutableSetOf() }
    return isNewNode
  }

  fun addEdge(start: GenNode, end: GenNode) {
    check(start in nodes && end in nodes)
    edges.add(Pair(start, end))
    edgesByStart.getOrPut(start) { mutableSetOf() }.add(end)
    edgesByEnd.getOrPut(end) { mutableSetOf() }.add(start)
  }

  fun addProgressedTo(start: GenNode, end: GenNode, acceptConditions: Set<GenAcceptCondition>) {
    TODO()
  }
}

data class GenNode(val symbolId: Int, val pointer: Int)

sealed class GenAcceptCondition {
  data class NoLongerMatch(val symbolId: Int): GenAcceptCondition()
  data class NotExists(val symbolId: Int): GenAcceptCondition()
  data class Exists(val symbolId: Int): GenAcceptCondition()
  data class Unless(val symbolId: Int): GenAcceptCondition()
  data class OnlyIf(val symbolId: Int): GenAcceptCondition()
}
