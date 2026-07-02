package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.mgroup3.proto.KernelTemplateGen
import java.util.*

// 시뮬레이션 그래프에서 관찰된 cond symbol — pos 는 그 심볼의 watcher span 이 시작하는
// 시뮬레이션 좌표 (= 해당 atomic (NExcept/NJoin/NLongest/lookahead) 의 derive 위치).
// isLookahead: 관찰 주체가 lookahead (NLookaheadIs/Except) 인지 —
//   bounded (except/join/longest) watcher 는 span-정규화 key (MID=ctx.gen, same-input),
//   lookahead watcher 는 구 규약 (key=등록 gen, same-input, 드리프트 anchor 와 쌍).
data class ObservedCondSym(val symbolId: Int, val pos: GenNodeGeneration, val isLookahead: Boolean)

class GenParsingGraph(
  val startNodes: Set<GenNode>,
  val nodes: MutableSet<GenNode>,
  val edges: MutableSet<Pair<GenNode, GenNode>>,
  val edgesByStart: MutableMap<GenNode, MutableSet<GenNode>>,
  val edgesByEnd: MutableMap<GenNode, MutableSet<GenNode>>,
  val observingCondSymbolIds: MutableSet<ObservedCondSym>,
  val acceptConditions: MutableMap<GenNode, GenAcceptCondition>,
  // key -> value 로 progress되었음. 현재 phase에서의 progress만 (derive 또는 progress 단계)
  val progressedNodes: MutableMap<GenNode, GenNode>,
  val finishedNodes: MutableSet<GenNode>,
  // derivedFrom 단계에서 만들어진 progressedNodes의 사본. progressedFrom 호출 시 보존되어
  // reachables 계산이나 chain 추적에 사용.
  val derivePhaseProgressedNodes: MutableMap<GenNode, GenNode> = mutableMapOf(),
) {
  // progress 를 적용하지 않고 조건만 수집할 노드들 (mgroup2 의 progress barrier 대응).
  // progressedFrom 에서 설정. barrier 노드로의 progress 조건은 barrierProgressConditions
  // 에 Or 로 누적된다 (startNodeProgress / replace_and_progresses 의 조건 원천).
  var barrierNodes: Set<GenNode> = emptySet()
  val barrierProgressConditions: MutableMap<GenNode, GenAcceptCondition> = mutableMapOf()

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
  // edges와 progressedNodes(현재 phase) 및 derivePhaseProgressedNodes(이전 derive phase)를 따라가되,
  // *start 자신의* progress 링크는 따라가지 않는다: start 의 zero-width progress 쌍둥이
  // (예: nullable WS 를 빈 매치로 통과한 dot+1 milestone) 는 같은 milestone group 의
  // 별도 멤버로 존재하고 그 서브트리는 쌍둥이 자신에게 귀속된다 — 여기서 따라가면
  // 같은 서브트리가 pre/post dot 양쪽의 replace_and_appends 로 중복 emit 되어
  // 런타임 path 체인이 dot 변형별로 복제된다 (jar.bbx 급 입력에서 레벨마다 ×2 —
  // kernels_history_optimization.md §0.2). m2 의 per-milestone naive 시뮬레이션은
  // pre→post edge 가 없어 귀속이 유일한 것에 대응.
  // start 보다 깊은 노드의 progress 링크는 유지해야 한다: appended group 의
  // 쌍둥이 멤버십 (예: {B:1, B:2}) 이 그 경로로 수집된다 (m2 도 동일 구성).
  fun reachablesFrom(start: GenNode, end: Set<GenNode>): Set<GenNode> {
    val queue: Queue<GenNode> = LinkedList()
    val visited = mutableSetOf<GenNode>()
    val reachables = mutableSetOf<GenNode>()

    queue.add(start)
    visited.add(start)
    reachables.addAll(end.intersect(setOf(start)))
    while (queue.isNotEmpty()) {
      val next = queue.poll()
      var nexts = edgesByStart[next] ?: setOf()
      if (next != start) {
        nexts = nexts +
          setOfNotNull(progressedNodes[next]) +
          setOfNotNull(derivePhaseProgressedNodes[next])
      }
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
  Next,
  // Grand: main path 의 parent milestone 의 *parent* milestone gen (= 마지막 milestone 의 startGen).
  // NJoin/NLongest/NExcept 같이 매치 시작 시점이 milestone startGen 인 case 에 사용.
  Grand;

  fun toProto(): KernelTemplateGen = when (this) {
    Prev -> KernelTemplateGen.CURR
    Curr -> KernelTemplateGen.MID
    Mid -> KernelTemplateGen.MID
    Next -> KernelTemplateGen.NEXT
    Grand -> KernelTemplateGen.GRAND
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
  data class NoLongerMatch(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev, val bodyEndGen: GenNodeGeneration = GenNodeGeneration.Next): GenAcceptCondition()
  data class NotExists(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class Exists(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev): GenAcceptCondition()
  data class Unless(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev, val endGen: GenNodeGeneration = GenNodeGeneration.Next): GenAcceptCondition()
  data class OnlyIf(val symbolId: Int, val startGen: GenNodeGeneration = GenNodeGeneration.Prev, val endGen: GenNodeGeneration = GenNodeGeneration.Next): GenAcceptCondition()
}

// edge action 템플릿의 bounded/longest 조건 gen 태그를 span 시작 좌표로 리맵 (Curr/Mid → Grand).
//
// edge 시뮬레이션의 derive phase 는 parent 의 dot(Curr)에서 일어난다. m3 의 rea 부착은
// 항상 dot+1 에 일어나므로 (same-input 부착 규약: 노드 gen = 부착 gen = dot+1),
// parent 의 dot 의 런타임 값은 균일하게 parentGen - 1 — edge 액션의 GRAND 바인딩이
// 이 값으로 정의된다. Curr→MID=parentGen 은 dot 보다 +1 이라 Grand 로 리맵해야
// watcher root 의 key (= span 시작 gen 으로 정규화된 cond root starter) 와 일치한다.
//
// Exists/NotExists (lookahead) 는 리맵하지 않는다: lookahead 의 span 은 자신의 derive
// 위치에서 시작하고, watcher key 규약(derive 시점 gen)과 이미 쌍이 맞는다.
fun remapEdgeCondGens(cond: GenAcceptCondition): GenAcceptCondition {
  fun remap(tag: GenNodeGeneration): GenNodeGeneration = when (tag) {
    GenNodeGeneration.Curr, GenNodeGeneration.Mid -> GenNodeGeneration.Grand
    else -> tag
  }
  return when (cond) {
    GenAcceptCondition.Always -> cond
    is GenAcceptCondition.And -> GenAcceptCondition.And(cond.conds.map { remapEdgeCondGens(it) }.toSet())
    is GenAcceptCondition.Or -> GenAcceptCondition.Or(cond.conds.map { remapEdgeCondGens(it) }.toSet())
    is GenAcceptCondition.NoLongerMatch ->
      cond.copy(startGen = remap(cond.startGen), bodyEndGen = remap(cond.bodyEndGen))
    is GenAcceptCondition.Unless ->
      cond.copy(startGen = remap(cond.startGen), endGen = remap(cond.endGen))
    is GenAcceptCondition.OnlyIf ->
      cond.copy(startGen = remap(cond.startGen), endGen = remap(cond.endGen))
    is GenAcceptCondition.NotExists -> cond
    is GenAcceptCondition.Exists -> cond
  }
}
