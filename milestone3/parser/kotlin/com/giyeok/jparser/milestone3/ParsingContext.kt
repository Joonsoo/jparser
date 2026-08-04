package com.giyeok.jparser.milestone3

data class ParsingContext(
  val gen: Int,
  val paths: List<MilestonePath>,
  val history: List<ParsingHistoryEntry>,
)

data class MilestonePath(
  val rootSymbolId: Int,
  // root pointer는 항상 0
  // watcher path인 경우엔 rootGen != 0 일 수 있음
  // root accept condition은 반드시 Always
  val rootGen: Int,
  val edges: List<MilestoneEdge>,
) {
  fun tip(): MilestoneTip =
    if (edges.isEmpty()) {
      MilestoneTip(rootSymbolId, 0, rootGen)
    } else {
      val last = edges.last().milestone
      MilestoneTip(last.symbolId, last.pointer, last.gen)
    }

  fun tipKernelTemplate(): KernelTemplate =
    if (edges.isEmpty()) {
      KernelTemplate(rootSymbolId, 0)
    } else {
      val last = edges.last().milestone
      KernelTemplate(last.symbolId, last.pointer)
    }

  fun tipEdge(): Pair<KernelTemplate, KernelTemplate> {
    TODO()
  }

  fun appendEdge(newEdge: MilestoneEdge): MilestonePath =
    copy(edges = edges + newEdge)
}

data class MilestoneTip(
  val symbolId: Int,
  val pointer: Int,
  val gen: Int,
)

data class Milestone(
  val symbolId: Int,
  val pointer: Int,
  val gen: Int,
  val acceptCondition: M3AcceptCondition,
)

data class MilestoneEdge(
  val edgeAcceptCondition: M3AcceptCondition,
  val milestone: Milestone,
)

// TODO
data class ParsingHistoryEntry(
  val paths: List<MilestonePath>
)

sealed class M3AcceptCondition

data object Always: M3AcceptCondition()

data object Never: M3AcceptCondition()

data class And(val conds: List<M3AcceptCondition>): M3AcceptCondition()
data class Or(val conds: List<M3AcceptCondition>): M3AcceptCondition()

data class Exists(
  val symbolId: Int,
  val gen: Int,
  val checkFromNextGen: Boolean,
): M3AcceptCondition()

data class NotExists(
  val symbolId: Int,
  val gen: Int,
  val checkFromNextGen: Boolean,
): M3AcceptCondition()

data class OnlyIf(
  val symbolId: Int,
  val gen: Int,
): M3AcceptCondition()

data class Unless(
  val symbolId: Int,
  val gen: Int,
): M3AcceptCondition()
