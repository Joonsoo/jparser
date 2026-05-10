package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.mgroup3.proto.KernelTemplateGen
import java.util.*

class GenParsingGraph(
  val startNodes: Set<GenNode>,
  val nodes: MutableSet<GenNode>,
  val edges: MutableSet<Pair<GenNode, GenNode>>,
  val edgesByStart: MutableMap<GenNode, MutableSet<GenNode>>,
  val edgesByEnd: MutableMap<GenNode, MutableSet<GenNode>>,
  val observingCondSymbolIds: MutableSet<Int>,
  val acceptConditions: MutableMap<GenNode, GenAcceptCondition>,
  // key -> value 로 progress되었음. 현재 phase에서의 progress만 (derive 또는 progress 단계)
  val progressedNodes: MutableMap<GenNode, GenNode>,
  val finishedNodes: MutableSet<GenNode>,
  // derivedFrom 단계에서 만들어진 progressedNodes의 사본. progressedFrom 호출 시 보존되어
  // reachables 계산이나 chain 추적에 사용.
  val derivePhaseProgressedNodes: MutableMap<GenNode, GenNode> = mutableMapOf(),
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
    finishedNodes = finishedNodes.toMutableSet(),
    derivePhaseProgressedNodes = derivePhaseProgressedNodes.toMutableMap(),
  )

  fun addNode(node: GenNode, condition: GenAcceptCondition): Boolean {
    val isNewNode = node !in nodes
    if (isNewNode) {
      nodes.add(node)
      acceptConditions[node] = condition
    }
    return isNewNode
  }

  fun addEdge(start: GenNode, end: GenNode): Boolean {
    check(start in nodes && end in nodes)
    val newEdge = Pair(start, end)
    if (newEdge !in edges) {
      edges.add(newEdge)
      edgesByStart.getOrPut(start) { mutableSetOf() }.add(end)
      edgesByEnd.getOrPut(end) { mutableSetOf() }.add(start)
      return true
    }
    return false
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

  // start에서 도달 가능한 end들을 반환.
  // edges와 progressedNodes(현재 phase) 및 derivePhaseProgressedNodes(이전 derive phase)를 모두 따라간다.
  fun reachablesFrom(start: GenNode, end: Set<GenNode>): Set<GenNode> {
    val queue: Queue<GenNode> = LinkedList()
    val visited = mutableSetOf<GenNode>()
    val reachables = mutableSetOf<GenNode>()

    queue.add(start)
    visited.add(start)
    reachables.addAll(end.intersect(setOf(start)))
    while (queue.isNotEmpty()) {
      val next = queue.poll()
      val nexts = (edgesByStart[next] ?: setOf()) +
        setOfNotNull(progressedNodes[next]) +
        setOfNotNull(derivePhaseProgressedNodes[next])
      val newNodes = nexts.toSet() - visited
      reachables.addAll(newNodes.intersect(end))
      visited.addAll(newNodes)
      queue.addAll(newNodes)
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

// GenNodeGeneration <-> KernelTemplateGen 매핑
// Prev = 0 = CURR  (proto): start gen of the path root or grand-parent of edge action
// Curr = 1 = MID   (proto): "current" step의 시작 gen (=직전 input 처리 직후 gen)
// Mid  = 1 = MID   (proto): mid edge가 만들어지는 도중에 사용 (Curr와 같은 인덱스로 매핑)
// Next = 2 = NEXT  (proto): 현재 input 처리 후의 gen
//
// term action (mgroup 안에서 input을 처리)의 경우:
//   parent milestone의 gen = Prev에 매핑 (CURR)
//   ctx.gen (직전 입력 후의 gen) = Curr에 매핑 (MID)
//   gen (이번 입력 처리 후 gen) = Next에 매핑 (NEXT)
// tip edge action (parent 노드 위에서 edge를 발생)의 경우:
//   grandparent gen = Prev (CURR)
//   parent gen = Curr (MID)
//   현재 gen = Next (NEXT)
// mid edge action도 위와 동일.
enum class GenNodeGeneration {
  Prev,
  Curr,
  Mid, // mid-edge 계산 도중에 등장함 (Curr와 같은 의미)
  Next;

  fun toProto(): KernelTemplateGen = when (this) {
    Prev -> KernelTemplateGen.CURR
    Curr -> KernelTemplateGen.MID
    Mid -> KernelTemplateGen.MID
    Next -> KernelTemplateGen.NEXT
  }
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

  // startGen: cond path 시작 시점. condition이 등장한 atomic symbol (NLongest, Lookahead 등) 의
  // derive 시점에 해당. GenNodeGeneration 으로 표현 (Prev/Curr/Mid/Next).
  data class NoLongerMatch(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class NotExists(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class Exists(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class Unless(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class OnlyIf(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
}
