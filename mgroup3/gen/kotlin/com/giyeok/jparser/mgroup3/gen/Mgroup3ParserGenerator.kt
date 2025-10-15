package com.giyeok.jparser.mgroup3.gen

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.mgroup3.gen.GenNodeGeneration.*
import com.giyeok.jparser.mgroup3.proto.*
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData.TermGroupActions
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.proto.TermGroupProto.TermGroup
import com.giyeok.jparser.proto.TermGroupProtobufConverter
import com.giyeok.jparser.utils.TermGrouper
import com.google.common.collect.HashBiMap
import com.google.protobuf.Empty
import scala.jdk.javaapi.CollectionConverters

// TODO tasks.derivedFrom 캐싱 해서 효율성 제고
class Mgroup3ParserGenerator(val grammar: NGrammar) {
  val tasks = GenParsingTaskRunner(grammar)

  val rootPaths = mutableMapOf<Int, PathRootInfo>()

  // milestone group의 모든 GenNode들의 startGen은 prev나 curr여야 하고, endgen은 curr여야 한다
  val milestoneGroups = HashBiMap.create<Int, Set<KernelTemplate>>()

  // mgroup id -> list<(term group, term action)>
  val termActions = mutableMapOf<Int, List<Mgroup3ParserData.TermGroupAction>>()

  // (milestone, mgroup id) -> edge action
  val tipEdgeActions = mutableMapOf<Pair<KernelTemplate, Int>, EdgeAction>()

  // (milestone, milestone) -> edge action
  val midEdgeActions = mutableMapOf<Pair<KernelTemplate, KernelTemplate>, EdgeAction>()


  // 여기부터는 possibleTipEdges, possibleMidEdges 계산을 위한 부가 정보
  // key의 group id 앞에(parent로) 올 수 있는 milestone들
  val possibleParentsOfGroup = mutableMapOf<Int, MutableSet<KernelTemplate>>()

  // key의 group id가 value로 치환되어 reduce trigger 가능
  val edgeActionTriggers = mutableMapOf<Int, MutableSet<Int>>()

  // key group id가 value의 group id로 치환될 수 있음
  val mgroupReplaceables = mutableMapOf<Int, MutableSet<KernelTemplate>>()

  fun generate(): Mgroup3ParserData {
    rootPaths[grammar.startSymbol()] = genRootPathFromSymbol(grammar.startSymbol())

    while (true) {
      val possibleRoots = possibleRootSymbols()
      val remainingMgroups = milestoneGroups.keys - termActions.keys
      val possibleTipEdges = possibleTipEdges()
      val possibleMidEdges = possibleMidEdges()
      val remainingRootPaths = possibleRoots - rootPaths.keys
      val remainingTipEdges = possibleTipEdges.canProgress - tipEdgeActions.keys
      val remainingMidEdges = possibleMidEdges - midEdgeActions.keys
      println(
        "roots=${remainingRootPaths.size}/${possibleRoots.size} " +
          "mg=${remainingMgroups.size}/${milestoneGroups.size} " +
          "tipEdges=${remainingTipEdges.size}/${possibleTipEdges.canProgress.size} " +
          "midEdges=${remainingMidEdges.size}/${possibleMidEdges.size}"
      )

      if (remainingRootPaths.isEmpty() && remainingMgroups.isEmpty() &&
        remainingTipEdges.isEmpty() && remainingMidEdges.isEmpty()
      ) {
        break
      }
      for (rootSymbolId in remainingRootPaths) {
        rootPaths[rootSymbolId] = genRootPathFromSymbol(rootSymbolId)
      }
      for (mgroupId in remainingMgroups) {
        val termActionsOfMgroup = genMgroupTermActions(mgroupId)
        termActions[mgroupId] = termActionsOfMgroup

        for (action in termActionsOfMgroup) {
          for (append in action.termAction.replaceAndAppendsList) {
            mgroupReplaceables.getOrPut(mgroupId) { mutableSetOf() }
              .add(append.replace)
            possibleParentsOfGroup.getOrPut(append.append.milestoneGroupId) { mutableSetOf() }
              .add(append.replace)
          }
          for (progress in action.termAction.replaceAndProgressesList) {
            edgeActionTriggers.getOrPut(mgroupId) { mutableSetOf() }
              .add(progress.replaceMilestoneGroupId)
          }
        }
      }
      for (tipEdge in remainingTipEdges) {
        val edgeAction = genTipEdgeAction(tipEdge.first, tipEdge.second)
        tipEdgeActions[tipEdge] = edgeAction

        for (append in edgeAction.appendMilestoneGroupsList) {
          possibleParentsOfGroup.getOrPut(append.milestoneGroupId) { mutableSetOf() }
            .add(tipEdge.first)
        }
      }
      for (midEdge in remainingMidEdges) {
        val edgeAction = genMidEdgeAction(midEdge.first, midEdge.second)
        midEdgeActions[midEdge] = edgeAction

        for (append in edgeAction.appendMilestoneGroupsList) {
          possibleParentsOfGroup.getOrPut(append.milestoneGroupId) { mutableSetOf() }
            .add(midEdge.first)
        }
      }
    }

    // TODO observing_cond_symbol_ids 에 A가 포함되어 있으면, A에 대한 PathRootInfo의 initial_cond_symbol_ids도 모두 포함되도록 한다
    // -> 안 그래도 파싱시에 다 확인은 하지만 조금이라도 미리 해놓으려고?

    val builder = Mgroup3ParserData.newBuilder()
    builder.grammar = GrammarProtobufConverter.convertNGrammarToProto(grammar)
    builder.startSymbolId = grammar.startSymbol()
    for ((rootSymbolId, rootPath) in rootPaths.entries.sortedBy { it.key }) {
      builder.putPathRoots(rootSymbolId, rootPath)
    }
    for ((mgroupId, milestones) in milestoneGroups.entries.sortedBy { it.key }) {
      // TODO milestones 소팅
      builder.putMilestoneGroups(
        mgroupId,
        Mgroup3ParserData.MilestoneGroup.newBuilder().addAllKernels(milestones).build()
      )
    }
    for ((mgroupId, termActions) in termActions.entries.sortedBy { it.key }) {
      // TODO termActions 소팅
      builder.putTermActions(
        mgroupId,
        TermGroupActions.newBuilder().addAllActions(termActions).build()
      )
    }
    // TODO tipEdgeActions.entries 소팅
    for ((edge, edgeAction) in tipEdgeActions.entries) {
      builder.addTipEdgeActionsBuilder()
        .setParent(edge.first)
        .setTipGroupId(edge.second)
        .setEdgeAction(edgeAction)
    }
    // TODO midEdgeActions.entries 소팅
    for ((edge, edgeAction) in midEdgeActions.entries) {
      builder.addMidEdgeActionsBuilder()
        .setParent(edge.first)
        .setTip(edge.second)
        .setEdgeAction(edgeAction)
    }

    return builder.build()
  }

  fun milestoneGroupIdOf(nodes: Set<GenNode>): Int {
    val kts = nodes.map {
      KernelTemplate.newBuilder().setSymbolId(it.symbolId).setPointer(it.pointer).build()
    }.toSet()
    return milestoneGroupIdOfKernelTemplates(kts)
  }

  fun milestoneGroupIdOfKernelTemplates(nodes: Set<KernelTemplate>): Int {
    val existing = milestoneGroups.inverse()[nodes]
    if (existing != null) {
      return existing
    }
    val newId = milestoneGroups.size + 1
    milestoneGroups[newId] = nodes
    return newId
  }

  fun progressibleTermGroupsOf(graph: GenParsingGraph): Map<TermGroup, Set<GenNode>> {
    val progressibleTermNodes = graph.nodes.filter { node ->
      node.pointer == 0 && grammar.symbolOf(node.symbolId) is NGrammar.NTerminal
    }.toSet()
    val nodes = progressibleTermNodes.associateWith { node ->
      grammar.symbolOf(node.symbolId).symbol() as Symbols.Terminal
    }

    val termGroups = TermGrouper.termGroupsOf(CollectionConverters.asScala(nodes.values).toSet())
    return CollectionConverters.asJava(termGroups).associate { tg ->
      val applicables = nodes.filter { it.value.acceptTermGroup(tg) }.keys
      TermGroupProtobufConverter.convertTermGroupToProto(tg) to applicables
    }
  }

  fun milestonesOf(graph: GenParsingGraph): Set<GenNode> =
    (graph.nodes - graph.startNodes).filter { node ->
      when (val symbol = grammar.symbolOf(node.symbolId)) {
        is NGrammar.NSequence -> {
          node.pointer in 1..<symbol.sequence().size() &&
            node.startGen == Curr && node.endGen == Next
        }

        else -> false
      }
    }.toSet()

  fun genRootPathFromSymbol(symbolId: Int): PathRootInfo {
    val builder = PathRootInfo.newBuilder()

    val startNode = GenNode(symbolId, 0, Curr, Curr)

    builder.symbolId = symbolId
    builder.milestoneGroupId = milestoneGroupIdOf(setOf(startNode))

    val graph = tasks.derivedFrom(setOf(startNode))
    builder.addAllInitialCondSymbolIds(graph.observingCondSymbolIds)
    if (startNode in graph.progressedNodes) {
      builder.selfFinishAcceptCondition =
        graph.acceptConditions[graph.progressedNodes[startNode]!!]!!.toProto()
    } else {
      builder.clearSelfFinishAcceptCondition()
    }

    // TODO builder.parsingActions
    for (finished in graph.finishedNodes) {
      val b = builder.parsingActionsBuilder.addFinishedBuilder()
      b.symbolId = finished.symbolId
      b.pointer = finished.pointer
      b.startGen = finished.startGen.toProto()
    }

    return builder.build()
  }

  fun genMgroupTermActions(mgroupId: Int): List<Mgroup3ParserData.TermGroupAction> {
    val milestones = milestoneGroups[mgroupId]!!.map {
      GenNode(it.symbolId, it.pointer, Prev, Curr)
    }.toSet()
    val graph = tasks.derivedFrom(milestones)

    val actions = mutableListOf<Mgroup3ParserData.TermGroupAction>()

    val terms = progressibleTermGroupsOf(graph)
    for ((termGroup, nodes) in terms) {
      val actionBuilder = Mgroup3ParserData.TermGroupAction.newBuilder()
        .setTermGroup(termGroup)

      val builder = actionBuilder.termActionBuilder
      val g2 = tasks.progressedFrom(graph, nodes, Next)
      // println(g2.toDot())

      // replace_and_appends
      val appendingMilestones = milestonesOf(g2)
      for (parentMilestone in milestones) {
        val reachables = appendingMilestones
        // TODO reachability를 따질 필요가 있나? 없는 것 같은데.. g2.reachablesFrom(parentMilestone, appendingMilestones)
        if (reachables.isNotEmpty()) {
          val reachableGroups = reachables.groupBy { g2.acceptConditions[it]!! }
          // reachableGroups가 proto에 추가되는 순서를 정의하기 위함
          val reachableGroupsEntries = reachableGroups.entries.sortedBy { it.key }
          for ((acc, subReachables) in reachableGroupsEntries) {
            val replaceAndAppendBuilder = builder.addReplaceAndAppendsBuilder()
            replaceAndAppendBuilder.setReplace(parentMilestone.toKernelTemplateProto())
            val append = replaceAndAppendBuilder.appendBuilder
            append.milestoneGroupId = milestoneGroupIdOf(subReachables.toSet())
            append.acceptCondition = acc.toProto()
            // TODO 실제로는 parentMilestone -> subReachables + 각 subReachables의 derive에서 나오는 observing cond symbol ids
            append.addAllObservingCondSymbolIds(g2.observingCondSymbolIds)
          }
        }
      }

      val progressedMilestones = g2.progressedNodes.keys.intersect(milestones)
        .groupBy { parentMilestone ->
          g2.acceptConditions[g2.progressedNodes[parentMilestone]!!]!!
        }
      val progressedMilestonesEntries = progressedMilestones.entries.sortedBy { it.key }
      for ((acc, subMilestones) in progressedMilestonesEntries) {
        val replaceAndProgressBuilder = builder.addReplaceAndProgressesBuilder()
        replaceAndProgressBuilder.setReplaceMilestoneGroupId(milestoneGroupIdOf(subMilestones.toSet()))
        replaceAndProgressBuilder.setAcceptCondition(acc.toProto())
      }

      // TODO builder.parsingActions
      for (finished in g2.finishedNodes) {
        val b = builder.parsingActionsBuilder.addFinishedBuilder()
        b.symbolId = finished.symbolId
        b.pointer = finished.pointer
        b.startGen = finished.startGen.toProto()
      }

      actions.add(actionBuilder.build())
    }

    return actions
  }

  private fun edgeActionFrom(graph: GenParsingGraph, parentNode: GenNode): EdgeAction {
    val builder = EdgeAction.newBuilder()

    val appendings = milestonesOf(graph).groupBy { graph.acceptConditions[it]!! }
    val appendingsEntries = appendings.entries.sortedBy { it.key }
    for ((acc, appendingMilestones) in appendingsEntries) {
      val appendBuilder = builder.addAppendMilestoneGroupsBuilder()
      appendBuilder.milestoneGroupId = milestoneGroupIdOf(appendingMilestones.toSet())
      appendBuilder.acceptCondition = acc.toProto()
      // TODO 실제로는 parentNode -> appendingMilestones + 각 appendingMilestones의 derive에서 나오는 observing cond symbol ids
      appendBuilder.addAllObservingCondSymbolIds(graph.observingCondSymbolIds)
    }

    // TODO builder.parsingActions
    for (finished in graph.finishedNodes) {
      val b = builder.parsingActionsBuilder.addFinishedBuilder()
      b.symbolId = finished.symbolId
      b.pointer = finished.pointer
      b.startGen = finished.startGen.toProto()
    }

    return builder.build()

  }

  fun genTipEdgeAction(parent: KernelTemplate, tipMgroupId: Int): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val graph = tasks.derivedFrom(setOf(parentNode))
    val progs = milestoneGroups[tipMgroupId]!!.map {
      GenNode(it.symbolId, it.pointer, Prev, Curr)
    }.toSet()
    // TODO progs가 graph에 모두 있는 상태인가..?
    for (prog in progs) {
      graph.addNode(prog, GenAcceptCondition.Always)
    }
    val g2 = tasks.progressedFrom(graph, progs, Next)

    return edgeActionFrom(g2, parentNode)
  }

  fun genMidEdgeAction(parent: KernelTemplate, child: KernelTemplate): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val graph = tasks.derivedFrom(setOf(parentNode))
    val prog = GenNode(child.symbolId, child.pointer, Prev, Mid)
    graph.addNode(prog, GenAcceptCondition.Always)
    val g2 = tasks.progressedFrom(graph, setOf(prog), Next)

    return edgeActionFrom(g2, parentNode)
  }

  // TODO 효율성을 위해 그래프 처리할 때 새로 등장한 애들 쌓아서 사용하기 -
  fun possibleRootSymbols(): Set<Int> {
    val condsByRoots = rootPaths.values.flatMap { it.initialCondSymbolIdsList }
    val byTermActions = termActions.values.flatMap { actions ->
      actions.flatMap { action ->
        action.termAction.replaceAndAppendsList.flatMap { it.append.observingCondSymbolIdsList }
      }
    }
    val byTipEdges = tipEdgeActions.values.flatMap { edgeAction ->
      edgeAction.appendMilestoneGroupsList.flatMap { it.observingCondSymbolIdsList }
    }
    val byMidEdges = midEdgeActions.values.flatMap { edgeAction ->
      edgeAction.appendMilestoneGroupsList.flatMap { it.observingCondSymbolIdsList }
    }
    return (condsByRoots + byTermActions + byTipEdges + byMidEdges).toSet()
  }

  data class PossibleTipEdges(
    val canProgress: Set<Pair<KernelTemplate, Int>>,
    val canExist: Set<Pair<KernelTemplate, Int>>,
  )

  fun possibleTipEdges(): PossibleTipEdges {
    val canProgress = mutableSetOf<Pair<KernelTemplate, Int>>()
    val canExist = mutableSetOf<Pair<KernelTemplate, Int>>()

    for ((mgroupId, replaces) in edgeActionTriggers) {
      possibleParentsOfGroup[mgroupId]?.forEach { parent ->
        for (replace in replaces) {
          canProgress.add(parent to replace)
        }
      }
    }
    for ((mgroupId, parents) in possibleParentsOfGroup) {
      for (parent in parents) {
        canExist.add(parent to mgroupId)
      }
    }
    return PossibleTipEdges(canProgress, canExist)
  }

  fun possibleMidEdges(): Set<Pair<KernelTemplate, KernelTemplate>> {
    val midEdges = mutableSetOf<Pair<KernelTemplate, KernelTemplate>>()
    for ((mgroupId, replaces) in mgroupReplaceables) {
      possibleParentsOfGroup[mgroupId]?.forEach { parent ->
        for (replace in replaces) {
          midEdges.add(parent to replace)
        }
      }
    }
    return midEdges
  }
}

private fun GenAcceptCondition.toProto(): AcceptConditionTemplate {
  val b = AcceptConditionTemplate.newBuilder()
  when (this) {
    GenAcceptCondition.Always -> b.setAlways(Empty.getDefaultInstance())
    is GenAcceptCondition.And -> {
      b.and = MultiAcceptConditions.newBuilder()
        .addAllConditions(this.conds.map { it.toProto() }).build()
    }

    is GenAcceptCondition.Or -> {
      b.or = MultiAcceptConditions.newBuilder()
        .addAllConditions(this.conds.map { it.toProto() }).build()
    }

    is GenAcceptCondition.NoLongerMatch -> b.setNoLongerMatch(symbolId)
    is GenAcceptCondition.Exists -> b.setLookaheadFound(symbolId)
    is GenAcceptCondition.NotExists -> b.setLookaheadNotfound(symbolId)
    is GenAcceptCondition.Unless -> b.setExcept(symbolId)
    is GenAcceptCondition.OnlyIf -> b.setJoin(symbolId)
  }
  return b.build()
}

fun GenNode.toKernelTemplateProto(): KernelTemplate =
  KernelTemplate.newBuilder().setSymbolId(symbolId).setPointer(pointer).build()
