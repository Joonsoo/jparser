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

// Mgroup3 parser data generator.
// 기본 알고리즘은 mgroup2와 비슷하지만:
// - condition 처리는 별도 cond paths로 추적할 것이라 가정하고, observing_cond_symbol_ids만 함께 기록
// - parsing actions는 (kernel template, gen) 단위로 기록
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

  // key의 group id가 value(replace mgroup id)로 치환되어 reduce trigger 가능
  val edgeActionTriggers = mutableMapOf<Int, MutableSet<Int>>()

  // key group id가 value의 milestone들로 치환될 수 있음 (mid edge에서 사용)
  val mgroupReplaceables = mutableMapOf<Int, MutableSet<KernelTemplate>>()

  fun generate(): Mgroup3ParserData {
    rootPaths[grammar.startSymbol()] = genRootPathFromSymbol(grammar.startSymbol())
    // start symbol에 대한 milestone group은 항상 1번
    milestoneGroupIdOfKernelTemplates(setOf(KernelTemplate.newBuilder().setSymbolId(grammar.startSymbol()).setPointer(0).build()))

    while (true) {
      val possibleRoots = possibleRootSymbols()
      val remainingMgroups = milestoneGroups.keys - termActions.keys
      val possibleTipEdges = possibleTipEdges()
      val possibleMidEdges = possibleMidEdges()
      val remainingRootPaths = possibleRoots - rootPaths.keys
      val remainingTipEdges = possibleTipEdges.canProgress - tipEdgeActions.keys
      val remainingMidEdges = possibleMidEdges - midEdgeActions.keys

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

    val builder = Mgroup3ParserData.newBuilder()
    builder.grammar = GrammarProtobufConverter.convertNGrammarToProto(grammar)
    builder.startSymbolId = grammar.startSymbol()
    for ((rootSymbolId, rootPath) in rootPaths.entries.sortedBy { it.key }) {
      builder.putPathRoots(rootSymbolId, rootPath)
    }
    for ((mgroupId, milestones) in milestoneGroups.entries.sortedBy { it.key }) {
      val sortedKernels = milestones.toList().sortedWith(compareBy({ it.symbolId }, { it.pointer }))
      val mgBuilder = Mgroup3ParserData.MilestoneGroup.newBuilder()
        .addAllKernels(sortedKernels)
      // possible finishes: 이 milestone group 의 milestone 에서 출발한 derive graph 에서, 추가 input 없이
      // 발생할 수 있는 finish 들. derivedFrom 결과 graph 의 finishedNodes 에 직접 등장.
      // 이건 milestone path 의 reduce chain 이 input 없이 도달 가능한 finish 를 나타낸다.
      val milestoneNodes = milestones.map {
        GenNode(it.symbolId, it.pointer, Prev, Curr)
      }.toSet()
      val graph = tasks.derivedFrom(milestoneNodes)
      // finishedNodes 중 startNode 의 reduce chain 으로 도달 가능한 것들 (milestoneNodes 자체이거나 그 ancestor)
      // 단순화: graph.finishedNodes 모두 추가.
      val finishesBySym = mutableMapOf<Int, GenAcceptCondition>()
      for (finished in graph.finishedNodes) {
        if (finished.pointer == 1 && finished !in milestoneNodes) {
          // pointer=1 은 atomic symbol finish. milestoneNodes 자체는 제외 (그건 tip 시작점, 아직 finish 아님).
          val cond = graph.acceptConditions[finished] ?: continue
          val existing = finishesBySym[finished.symbolId]
          finishesBySym[finished.symbolId] =
            if (existing != null) GenAcceptCondition.Or.from(existing, cond) else cond
        }
      }
      for ((sym, cond) in finishesBySym.entries.sortedBy { it.key }) {
        mgBuilder.addPossibleFinishesBuilder().apply {
          symbolId = sym
          acceptCondition = cond.toProto()
        }
      }
      builder.putMilestoneGroups(mgroupId, mgBuilder.build())
    }
    for ((mgroupId, termActionList) in termActions.entries.sortedBy { it.key }) {
      builder.putTermActions(
        mgroupId,
        TermGroupActions.newBuilder().addAllActions(termActionList).build()
      )
    }
    val sortedTipEdges = tipEdgeActions.entries.sortedWith(
      compareBy({ it.key.first.symbolId }, { it.key.first.pointer }, { it.key.second })
    )
    for ((edge, edgeAction) in sortedTipEdges) {
      builder.addTipEdgeActionsBuilder()
        .setParent(edge.first)
        .setTipGroupId(edge.second)
        .setEdgeAction(edgeAction)
    }
    val sortedMidEdges = midEdgeActions.entries.sortedWith(
      compareBy(
        { it.key.first.symbolId }, { it.key.first.pointer },
        { it.key.second.symbolId }, { it.key.second.pointer }
      )
    )
    for ((edge, edgeAction) in sortedMidEdges) {
      builder.addMidEdgeActionsBuilder()
        .setParent(edge.first)
        .setTip(edge.second)
        .setEdgeAction(edgeAction)
    }

    return builder.build()
  }

  fun milestoneGroupIdOf(nodes: Set<GenNode>): Int {
    val kts = nodes.map { it.toKernelTemplateProto() }.toSet()
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

  fun progressibleTermGroupsOf(graph: GenParsingGraph): List<Pair<TermGroup, Set<GenNode>>> {
    val progressibleTermNodes = graph.nodes.filter { node ->
      node.pointer == 0 && grammar.symbolOf(node.symbolId) is NGrammar.NTerminal
    }.toSet()
    val nodes = progressibleTermNodes.associateWith { node ->
      grammar.symbolOf(node.symbolId).symbol() as Symbols.Terminal
    }

    val termGroups = TermGrouper.termGroupsOf(CollectionConverters.asScala(nodes.values).toSet())
    // 같은 proto term group으로 변환되는 것들이 여러 개 있을 수 있으므로 dedup해서 합침
    // (protobuf 객체 비교 의존성을 피하기 위해 byteString을 dedup key로 사용)
    val byKey = mutableMapOf<com.google.protobuf.ByteString, Pair<TermGroup, MutableSet<GenNode>>>()
    for (tg in CollectionConverters.asJava(termGroups)) {
      val proto = TermGroupProtobufConverter.convertTermGroupToProto(tg)
      val key = proto.toByteString()
      val applicables = nodes.filter { it.value.acceptTermGroup(tg) }.keys
      val pair = byKey.getOrPut(key) { proto to mutableSetOf() }
      pair.second.addAll(applicables)
    }
    return byKey.values.map { it.first to it.second.toSet() }
  }

  // graph에서 milestone (NSequence이고 pointer > 0)인 노드들 중 startGen이 expectedStart, endGen이 Next인 것들
  fun appendingMilestonesOf(graph: GenParsingGraph, expectedStart: GenNodeGeneration): Set<GenNode> =
    graph.nodes.filter { node ->
      node !in graph.startNodes &&
        node.startGen == expectedStart && node.endGen == Next &&
        when (val symbol = grammar.symbolOf(node.symbolId)) {
          is NGrammar.NSequence ->
            node.pointer in 1..<symbol.sequence().size()

          else -> false
        }
    }.toSet()

  // accept condition이나 cond path를 만들어내야 하는 심볼들 (Exists/NotExists/Unless/OnlyIf의 대상)
  // mgroup3에서는 longest도 별도 cond path가 아닌 main path에서 처리하지만
  // 일관성을 위해 일단 모두 cond symbol로 추적한다.
  // 추후 최적화 가능 (longest 심볼이 main path에 있으면 cond path 생성 생략)
  fun observedCondSymbolsFromAcc(condition: GenAcceptCondition, out: MutableSet<Int>) {
    when (condition) {
      GenAcceptCondition.Always -> {}
      is GenAcceptCondition.And -> condition.conds.forEach { observedCondSymbolsFromAcc(it, out) }
      is GenAcceptCondition.Or -> condition.conds.forEach { observedCondSymbolsFromAcc(it, out) }
      is GenAcceptCondition.Exists -> out.add(condition.symbolId)
      is GenAcceptCondition.NotExists -> out.add(condition.symbolId)
      is GenAcceptCondition.NoLongerMatch -> out.add(condition.symbolId)
      is GenAcceptCondition.Unless -> out.add(condition.symbolId)
      is GenAcceptCondition.OnlyIf -> out.add(condition.symbolId)
    }
  }

  fun genRootPathFromSymbol(symbolId: Int): PathRootInfo {
    val builder = PathRootInfo.newBuilder()

    val startNode = GenNode(symbolId, 0, Curr, Curr)

    builder.symbolId = symbolId
    builder.milestoneGroupId = milestoneGroupIdOf(setOf(startNode))

    val graph = tasks.derivedFrom(setOf(startNode))

    // start node로부터 도달 가능한 cond symbol들 모두 수집
    builder.addAllInitialCondSymbolIds(graph.observingCondSymbolIds.sorted())

    // start node가 derive 도중 progress될 수 있는 경우 (empty match) self finish condition을 기록
    val progressed = graph.progressedNodes[startNode]
    if (progressed != null) {
      builder.selfFinishAcceptCondition = graph.acceptConditions[progressed]!!.toProto()
    } else {
      builder.clearSelfFinishAcceptCondition()
    }

    // root path의 derive 단계에서 자동으로 일어나는 finish/progress들을 parsingActions에 기록
    val parsingActionsBuilder = builder.parsingActionsBuilder
    for (finished in graph.finishedNodes.sortedWith(compareBy({ it.symbolId }, { it.pointer }))) {
      parsingActionsBuilder.addFinishedBuilder().apply {
        this.symbolId = finished.symbolId
        this.pointer = finished.pointer
        this.startGen = finished.startGen.toProto()
        this.finishCondition = graph.acceptConditions[finished]!!.toProto()
      }
    }
    for ((before, after) in graph.progressedNodes.entries.sortedWith(
      compareBy({ it.key.symbolId }, { it.key.pointer })
    )) {
      // start node 자체의 progress는 selfFinishAcceptCondition에서 처리
      if (before == startNode) continue
      parsingActionsBuilder.addProgressedBuilder().apply {
        this.symbolId = before.symbolId
        this.pointer = before.pointer
        this.startGen = before.startGen.toProto()
        this.midGen = before.endGen.toProto()
      }
    }

    return builder.build()
  }

  fun genMgroupTermActions(mgroupId: Int): List<Mgroup3ParserData.TermGroupAction> {
    val mgroup = milestoneGroups[mgroupId]!!
    // 모든 milestone들이 같은 starts(Prev -> Curr)에서 시작한다고 가정
    val milestoneNodes = mgroup.map {
      GenNode(it.symbolId, it.pointer, Prev, Curr)
    }.toSet()
    val graph = tasks.derivedFrom(milestoneNodes)
    // derive 단계에서 이미 finish된 노드들 (이건 init context에서 한 번만 일어나는 것이므로 termAction에 포함되면 안 됨)
    val derivePhaseFinishedNodes = graph.finishedNodes.toSet()

    val actions = mutableListOf<Mgroup3ParserData.TermGroupAction>()

    val termGroups = progressibleTermGroupsOf(graph)
    val sortedTermGroups = termGroups.sortedWith(
      compareBy { it.first.toString() }
    )
    for ((termGroup, termNodes) in sortedTermGroups) {
      val actionBuilder = Mgroup3ParserData.TermGroupAction.newBuilder()
        .setTermGroup(termGroup)

      val taBuilder = actionBuilder.termActionBuilder
      val g2 = tasks.progressedFrom(graph, termNodes, Next)

      val appendingMilestones = appendingMilestonesOf(g2, Curr)
      // 각 parent milestone에 대해, 도달 가능한 appending milestone들을 condition별로 묶어서 replace_and_appends 생성
      val sortedMilestoneNodes = milestoneNodes.toList().sortedWith(
        compareBy({ it.symbolId }, { it.pointer })
      )
      for (parentMilestone in sortedMilestoneNodes) {
        if (!g2.nodes.contains(parentMilestone)) continue
        val reachables = g2.reachablesFrom(parentMilestone, appendingMilestones)
        if (reachables.isNotEmpty()) {
          val reachableGroups = reachables.groupBy { g2.acceptConditions[it]!! }
          val reachableGroupsEntries = reachableGroups.entries.sortedBy { it.key }
          for ((acc, subReachables) in reachableGroupsEntries) {
            val replaceAndAppendBuilder = taBuilder.addReplaceAndAppendsBuilder()
            replaceAndAppendBuilder.setReplace(parentMilestone.toKernelTemplateProto())
            val append = replaceAndAppendBuilder.appendBuilder
            val newMgroupId = milestoneGroupIdOf(subReachables.toSet())
            append.milestoneGroupId = newMgroupId
            append.acceptCondition = acc.toProto()
            // 추가되는 group의 cond symbols + 그 acc에서 사용되는 cond symbols +
            // 새 milestone group 의 derive 결과 observing cond syms (mgroup2 의 lookaheadRequiringSymbols 와 동일).
            val condSymbols = mutableSetOf<Int>()
            condSymbols.addAll(g2.observingCondSymbolIds)
            observedCondSymbolsFromAcc(acc, condSymbols)
            // 새 milestone group 의 derive graph 의 observingCondSymbolIds.
            // 새 group 의 milestone 들에서 시작하는 derive 가 NJoin/NLongest 등 만나면 그 cond_sym 도 추적해야 함.
            val newMgroupNodes = subReachables.map {
              GenNode(it.symbolId, it.pointer, Prev, Curr)
            }.toSet()
            val newMgroupGraph = tasks.derivedFrom(newMgroupNodes)
            condSymbols.addAll(newMgroupGraph.observingCondSymbolIds)
            append.addAllObservingCondSymbolIds(condSymbols.sorted())
            // mgroup2 의 lookahead_requiring_symbols 와 동일한 정보: 각 cond root sym 의 starter milestone group id.
            // runtime 에서 main path 가 이 milestone group 을 attach 하는 시점에 starter 도 같이 시작.
            for (sym in condSymbols.sorted()) {
              val rootInfo = rootPaths[sym] ?: genRootPathFromSymbol(sym)
              rootPaths[sym] = rootInfo
              replaceAndAppendBuilder.appendBuilder.addCondRootStartersBuilder().apply {
                symbolId = sym
                milestoneGroupId = rootInfo.milestoneGroupId
              }
            }
          }
        }
      }

      // replace_and_progresses: graph의 milestone 중 g2에서 progress된 것들
      // (즉, 자기 자신의 끝까지 진행된 milestone들)
      val progressedMilestones = g2.progressedNodes.keys.intersect(milestoneNodes)
        .groupBy { parentMilestone ->
          g2.acceptConditions[g2.progressedNodes[parentMilestone]!!]!!
        }
      val progressedEntries = progressedMilestones.entries.sortedBy { it.key }
      for ((acc, subMilestones) in progressedEntries) {
        val replaceAndProgressBuilder = taBuilder.addReplaceAndProgressesBuilder()
        // subMilestones가 진행된 결과의 mgroup. 단, replace target은 진행 전(원래 milestone) 들의 mgroup이어야 함
        val replaceKernels = subMilestones.map { it.toKernelTemplateProto() }.toSet()
        replaceAndProgressBuilder.setReplaceMilestoneGroupId(milestoneGroupIdOfKernelTemplates(replaceKernels))
        replaceAndProgressBuilder.setAcceptCondition(acc.toProto())
      }

      // parsing actions
      fillParsingActions(
        taBuilder.parsingActionsBuilder, g2,
        starts = milestoneNodes,
        derivePhaseFinishedNodes = derivePhaseFinishedNodes,
        includeProgressOfStarts = true,
      )

      actions.add(actionBuilder.build())
    }

    return actions
  }

  // graph (g2)에서 일어난 finish/progress들을 parsingActions에 기록.
  // derive 단계에서 이미 일어난 finish/progress는 제외하고 progress phase에서 새로 등장한 것만 기록.
  // includeProgressOfStarts: starts의 progress(즉 startNodeProgress)도 기록할지 여부
  private fun fillParsingActions(
    builder: ParsingActions.Builder,
    g2: GenParsingGraph,
    starts: Set<GenNode>,
    derivePhaseFinishedNodes: Set<GenNode>,
    includeProgressOfStarts: Boolean,
  ) {
    for (finished in g2.finishedNodes.sortedWith(compareBy({ it.symbolId }, { it.pointer }))) {
      // derive phase에서 이미 finish된 노드는 제외
      if (finished in derivePhaseFinishedNodes) continue
      builder.addFinishedBuilder().apply {
        this.symbolId = finished.symbolId
        this.pointer = finished.pointer
        this.startGen = finished.startGen.toProto()
        this.finishCondition = g2.acceptConditions[finished]!!.toProto()
      }
    }
    for ((before, after) in g2.progressedNodes.entries.sortedWith(
      compareBy({ it.key.symbolId }, { it.key.pointer })
    )) {
      if (!includeProgressOfStarts && before in starts) continue
      // (sym, ptr, before.startGen, before.endGen) → (sym, ptr+1, after.startGen, after.endGen)
      // 즉 startGen=before.startGen=after.startGen (NSequence는 startGen 유지),
      // mid_gen=before.endGen, end_gen=after.endGen=NEXT
      builder.addProgressedBuilder().apply {
        this.symbolId = before.symbolId
        this.pointer = before.pointer
        this.startGen = before.startGen.toProto()
        this.midGen = before.endGen.toProto()
      }
    }
  }

  private fun edgeActionFrom(
    graph: GenParsingGraph,
    parentNode: GenNode,
    starts: Set<GenNode>,
    derivePhaseFinishedNodes: Set<GenNode>,
  ): EdgeAction {
    val builder = EdgeAction.newBuilder()

    val appendingMilestones = appendingMilestonesOf(graph, Curr)
    // parentNode 자체에서 도달 가능한 appendingMilestones들만 의미가 있으므로 필터링
    val reachables = graph.reachablesFrom(parentNode, appendingMilestones)
    val groupedAppendings = reachables.groupBy { graph.acceptConditions[it]!! }
    val sortedAppendings = groupedAppendings.entries.sortedBy { it.key }
    for ((acc, appending) in sortedAppendings) {
      val appendBuilder = builder.addAppendMilestoneGroupsBuilder()
      appendBuilder.milestoneGroupId = milestoneGroupIdOf(appending.toSet())
      appendBuilder.acceptCondition = acc.toProto()
      val condSymbols = mutableSetOf<Int>()
      condSymbols.addAll(graph.observingCondSymbolIds)
      observedCondSymbolsFromAcc(acc, condSymbols)
      appendBuilder.addAllObservingCondSymbolIds(condSymbols.sorted())
      // mgroup2 의 lookahead_requiring_symbols 와 동일.
      for (sym in condSymbols.sorted()) {
        val rootInfo = rootPaths[sym] ?: genRootPathFromSymbol(sym)
        rootPaths[sym] = rootInfo
        appendBuilder.addCondRootStartersBuilder().apply {
          symbolId = sym
          milestoneGroupId = rootInfo.milestoneGroupId
        }
      }
    }

    // parentNode가 progress되는 경우 (즉, parent의 시작 노드까지 reduce 가능한 경우)
    val parentProgressed = graph.progressedNodes[parentNode]
    if (parentProgressed != null) {
      builder.startNodeProgress = graph.acceptConditions[parentProgressed]!!.toProto()
    } else {
      builder.clearStartNodeProgress()
    }

    fillParsingActions(
      builder.parsingActionsBuilder, graph,
      starts = starts,
      derivePhaseFinishedNodes = derivePhaseFinishedNodes,
      includeProgressOfStarts = true,
    )

    return builder.build()
  }

  fun genTipEdgeAction(parent: KernelTemplate, tipMgroupId: Int): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val tipMgroup = milestoneGroups[tipMgroupId]!!
    // tip edge: parent --[derive]--> ... --> child(tip)들
    val graph = tasks.derivedFrom(setOf(parentNode))
    val derivePhaseFinishedNodes = graph.finishedNodes.toSet()
    // tip mgroup의 milestone들을 startGen=Curr 기준으로 graph에 추가
    // (이미 등장한 init node로 들어오는 incoming edge를 progressed milestone으로 동일하게 추가)
    val progs = tipMgroup.map {
      GenNode(it.symbolId, it.pointer, Curr, Curr)
    }.toSet()
    addProgsWithIncomingEdges(graph, progs)
    val g2 = tasks.progressedFrom(graph, progs, Next)

    return edgeActionFrom(g2, parentNode, progs, derivePhaseFinishedNodes)
  }

  fun genMidEdgeAction(parent: KernelTemplate, child: KernelTemplate): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val graph = tasks.derivedFrom(setOf(parentNode))
    val derivePhaseFinishedNodes = graph.finishedNodes.toSet()
    // mid edge: parent --[derive]--> ... --> child(이미 진행 중)
    // child의 startGen은 Curr (parent와 같은 layer)이고 endGen은 Mid (이전 mgroup의 진행 결과)
    val prog = GenNode(child.symbolId, child.pointer, Curr, Mid)
    addProgsWithIncomingEdges(graph, setOf(prog))
    val g2 = tasks.progressedFrom(graph, setOf(prog), Next)

    return edgeActionFrom(g2, parentNode, setOf(prog), derivePhaseFinishedNodes)
  }

  // prog 노드들을 graph에 추가하면서, prog의 init form (symbolId, 0, prog.startGen, prog.startGen)으로
  // 들어오는 incoming edges를 prog 노드로 동일하게 추가한다.
  // 이는 derive로 등장한 init node에서 finish가 일어나야 progress가 incoming edges로 propagate되도록 하기 위함.
  private fun addProgsWithIncomingEdges(graph: GenParsingGraph, progs: Set<GenNode>) {
    for (prog in progs) {
      val initNode = GenNode(prog.symbolId, 0, prog.startGen, prog.startGen)
      graph.addNode(prog, GenAcceptCondition.Always)
      // initNode가 graph에 있으면 그 incoming edges를 prog에 복사
      val incomingStarts = graph.edgesByEnd[initNode]?.toSet() ?: emptySet()
      for (start in incomingStarts) {
        graph.addEdge(start, prog)
      }
    }
  }

  // 추적해야 하는 모든 root path symbol들
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

fun GenAcceptCondition.toProto(): AcceptConditionTemplate {
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

    is GenAcceptCondition.NoLongerMatch ->
      b.noLongerMatch = NoLongerMatchTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.Exists ->
      b.lookaheadFound = LookaheadFoundTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.NotExists ->
      b.lookaheadNotfound = LookaheadNotFoundTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.Unless ->
      b.except = ExceptTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.OnlyIf ->
      b.join = JoinTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
  }
  return b.build()
}

fun GenNode.toKernelTemplateProto(): KernelTemplate =
  KernelTemplate.newBuilder().setSymbolId(symbolId).setPointer(pointer).build()
