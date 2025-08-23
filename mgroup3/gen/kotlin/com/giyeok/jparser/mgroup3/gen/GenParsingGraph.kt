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
  val progressedNodes: MutableSet<GenNode>,
) {
  fun clone(): GenParsingGraph = GenParsingGraph(
    startNode,
    nodes.toMutableSet(),
    edges.toMutableSet(),
    edgesByStart.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    edgesByEnd.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    observingCondSymbolIds.toMutableSet(),
    acceptConditions.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    progressedNodes.toMutableSet(),
  )

  fun addNode(node: GenNode): Boolean {
    val isNewNode = node !in nodes
    nodes.add(node)
    acceptConditions.getOrPut(node) { mutableSetOf() }
    return isNewNode
  }

  fun addEdge(start: GenNode, end: GenNode) {
    check(start in nodes && end in nodes)
    val newEdge = Pair(start, end)
    if (newEdge !in edges) {
      edges.add(newEdge)
      edgesByStart.getOrPut(start) { mutableSetOf() }.add(end)
      edgesByEnd.getOrPut(end) { mutableSetOf() }.add(start)
    }
  }

  fun addProgressedTo(
    before: GenNode,
    after: GenNode,
    acceptConditions: Set<GenAcceptCondition>
  ) {
    check(before in nodes)

    addNode(after)
    this.acceptConditions[after] = this.acceptConditions[after]!! + acceptConditions

    progressedNodes.add(before)
  }
}

data class GenNode(
  val symbolId: Int,
  val pointer: Int,
  val startGen: GenNodeGeneration,
  val endGen: GenNodeGeneration
)

enum class GenNodeGeneration {
  Prev,
  Curr,
  Next,
}

sealed class GenAcceptCondition {
  data class NoLongerMatch(val symbolId: Int): GenAcceptCondition()
  data class NotExists(val symbolId: Int): GenAcceptCondition()
  data class Exists(val symbolId: Int): GenAcceptCondition()
  data class Unless(val symbolId: Int): GenAcceptCondition()
  data class OnlyIf(val symbolId: Int): GenAcceptCondition()
}
