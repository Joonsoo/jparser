package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.mgroup3.proto.EdgeAction
import com.giyeok.jparser.mgroup3.proto.KernelTemplate
import com.giyeok.jparser.mgroup3.proto.PathRootInfo
import com.giyeok.jparser.mgroup3.proto.TermAction
import com.giyeok.jparser.proto.TermGroupProto.TermGroup
import com.google.common.collect.HashBiMap

class Mgroup3ParserGenerator(val grammar: NGrammar) {
  val tasks = GenParsingTaskRunner(grammar)

  val rootPaths = mutableMapOf<Int, PathRootInfo>()
  val milestoneGroups = HashBiMap.create<Int, Set<GenNode>>()

  // mgroup id -> list<(term group, term action)>
  val termGroups = mutableMapOf<Int, List<Pair<TermGroup, TermAction>>>()

  // (milestone, mgroup id) -> edge action
  val tipEdgeActions = mutableMapOf<Pair<KernelTemplate, Int>, EdgeAction>()

  // (milestone, milestone) -> edge action
  val midEdgeActions = mutableMapOf<Pair<KernelTemplate, KernelTemplate>, EdgeAction>()

  fun milestoneGroupIdOf(nodes: Set<GenNode>): Int {
    val existing = milestoneGroups.inverse()[nodes]
    if (existing != null) {
      return existing
    }
    val newId = milestoneGroups.size + 1
    milestoneGroups[newId] = nodes
    return newId
  }

  data class PossibleTipEdges(
    val canProgress: Set<Pair<KernelTemplate, Int>>,
    val canExist: Set<Pair<KernelTemplate, Int>>,
  )

  fun possibleTipEdges(): PossibleTipEdges {
    TODO()
  }

  fun possibleMidEdges(): Set<Pair<KernelTemplate, KernelTemplate>> {
    TODO()
  }
}
