package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.mgroup3.proto.EdgeAction
import com.giyeok.jparser.mgroup3.proto.KernelTemplate
import com.giyeok.jparser.mgroup3.proto.PathRootInfo
import com.giyeok.jparser.mgroup3.proto.TermAction
import com.giyeok.jparser.proto.TermGroupProto.TermGroup
import com.giyeok.jparser.proto.TermGroupProtobufConverter
import com.giyeok.jparser.utils.TermGrouper
import com.google.common.collect.HashBiMap
import scala.jdk.javaapi.CollectionConverters
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.*

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

  fun progressibleTermNodesOf(graph: GenParsingGraph): Set<GenNode> = graph.nodes.filter { node ->
    node.pointer == 0 && grammar.symbolOf(node.symbolId) is NGrammar.NTerminal
  }.toSet()

  fun progressibleTermGroupsOf(graph: GenParsingGraph): Map<TermGroup, Set<GenNode>> {
    val nodes = progressibleTermNodesOf(graph).associateWith { node ->
      grammar.symbolOf(node.symbolId).symbol() as Symbols.Terminal
    }

    val termGroups = TermGrouper.termGroupsOf(CollectionConverters.asScala(nodes.values).toSet())
    return CollectionConverters.asJava(termGroups).associate { tg ->
      val applicables = nodes.filter { it.value.acceptTermGroup(tg) }.keys
      TermGroupProtobufConverter.convertTermGroupToProto(tg) to applicables
    }
  }

  fun milestonesOf(graph: GenParsingGraph): Set<GenNode> =
    (graph.nodes - graph.startNode).filter { node ->
      when (val symbol = grammar.symbolOf(node.symbolId)) {
        is NGrammar.NSequence -> {
          node.pointer in 1..<symbol.sequence().size() &&
            node.startGen == Curr && node.endGen == Next
        }

        else -> false
      }
    }.toSet()

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
