package com.giyeok.jparser.mgroup3.gen

import java.util.*

class GenParsingGraph(
  val startNodes: Set<GenNode>,
  val nodes: MutableSet<GenNode>,
  val edges: MutableSet<Pair<GenNode, GenNode>>,
  val edgesByStart: MutableMap<GenNode, MutableSet<GenNode>>,
  val edgesByEnd: MutableMap<GenNode, MutableSet<GenNode>>,
  val observingCondSymbolIds: MutableSet<Int>,
  val acceptConditions: MutableMap<GenNode, GenAcceptCondition>,
  // key -> value 로 progress되었음
  val progressedNodes: MutableMap<GenNode, GenNode>,
) {
  fun toDot(): String {
    fun id(node: GenNode): String =
      "n${node.symbolId}_${node.pointer}_${node.startGen}_${node.endGen}"

    val writer = StringBuilder()
    writer.append("digraph G {\n")
    nodes.forEach {
      writer.append("${id(it)} [label=\"${it.symbolId} ${it.pointer} ${it.startGen} ${it.endGen}\"];\n")
    }
    edges.forEach {
      writer.append("${id(it.first)} -> ${id(it.second)};\n")
    }
    progressedNodes.forEach {
      writer.append("${id(it.key)} -> ${id(it.value)} [style=dotted];\n")
    }
    writer.append("}\n")
    return writer.toString()
  }

  fun clone(): GenParsingGraph = GenParsingGraph(
    startNodes = startNodes,
    nodes = nodes.toMutableSet(),
    edges = edges.toMutableSet(),
    edgesByStart = edgesByStart.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    edgesByEnd = edgesByEnd.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap(),
    observingCondSymbolIds = observingCondSymbolIds.toMutableSet(),
    acceptConditions = acceptConditions.toMutableMap(),
    progressedNodes = progressedNodes.toMutableMap(),
  )

  fun addNode(node: GenNode, condition: GenAcceptCondition): Boolean {
    val isNewNode = node !in nodes
    if (isNewNode) {
      nodes.add(node)
      acceptConditions[node] = condition
    }
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
    acceptCondition: GenAcceptCondition
  ): Boolean {
    check(before in nodes)

    val isUpdated: Boolean
    if (after in nodes) {
      val newCond = GenAcceptCondition.Or.from(acceptConditions[after]!!, acceptCondition)
      isUpdated = newCond != acceptConditions[after]!!
      acceptConditions[after] = newCond
    } else {
      isUpdated = true
      nodes.add(after)
      acceptConditions[after] = acceptCondition
    }

    val prev = progressedNodes[before]
    if (prev == null) {
      progressedNodes[before] = after
    } else {
      check(prev == after)
    }
    return isUpdated
  }

  // start에서 도달 가능한 end들을 반환
  fun reachablesFrom(start: GenNode, end: Set<GenNode>): Set<GenNode> {
    val queue: Queue<GenNode> = LinkedList()
    val visited = mutableSetOf<GenNode>()
    val reachables = mutableSetOf<GenNode>()

    queue.add(start)
    visited.add(start)
    reachables.addAll(end.intersect(setOf(start)))
    while (queue.isNotEmpty()) {
      val next = queue.poll()
      ((edgesByStart[next] ?: setOf()) + setOfNotNull(progressedNodes[next])).let { nexts ->
        val newNodes = nexts.toSet() - visited
        reachables.addAll(newNodes.intersect(end))
        visited.addAll(newNodes)
        queue.addAll(newNodes)
      }
    }
    return reachables
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
  Mid, // mid-edge 계산 도중에 등장함
  Next,
}

sealed class GenAcceptCondition: Comparable<GenAcceptCondition> {
  override fun compareTo(other: GenAcceptCondition): Int =
    this.toString().compareTo(other.toString())

  data object Always: GenAcceptCondition()
  data class And(val conds: Set<GenAcceptCondition>): GenAcceptCondition() {
    companion object {
      fun from(a: GenAcceptCondition, b: GenAcceptCondition): GenAcceptCondition = when {
        a == Always -> b
        b == Always -> a
        a is And && b is And -> And(a.conds + b.conds)
        a is And -> And(a.conds + b)
        b is And -> And(b.conds + a)
        else -> And(setOf(a, b))
      }
    }
  }

  data class Or(val conds: Set<GenAcceptCondition>): GenAcceptCondition() {
    companion object {
      fun from(a: GenAcceptCondition, b: GenAcceptCondition): GenAcceptCondition = when {
        a == Always || b == Always -> Always
        a is Or && b is Or -> Or(a.conds + b.conds)
        a is Or -> Or(a.conds + b)
        b is Or -> Or(b.conds + a)
        else -> Or(setOf(a, b))
      }
    }
  }

  data class NoLongerMatch(val symbolId: Int): GenAcceptCondition()
  data class NotExists(val symbolId: Int): GenAcceptCondition()
  data class Exists(val symbolId: Int): GenAcceptCondition()
  data class Unless(val symbolId: Int): GenAcceptCondition()
  data class OnlyIf(val symbolId: Int): GenAcceptCondition()
}
