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
    // lookahead 가 감시하는 심볼들 — 런타임 step 3 의 시동 flavor (구 규약: same-input) 판별용.
    builder.addAllLookaheadCondSymbolIds(tasks.lookaheadCondSymbolIds.sorted())

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
  fun observedCondSymbolsFromAcc(condition: GenAcceptCondition, out: MutableSet<ObservedCondSym>) {
    when (condition) {
      GenAcceptCondition.Always -> {}
      is GenAcceptCondition.And -> condition.conds.forEach { observedCondSymbolsFromAcc(it, out) }
      is GenAcceptCondition.Or -> condition.conds.forEach { observedCondSymbolsFromAcc(it, out) }
      is GenAcceptCondition.Exists -> out.add(ObservedCondSym(condition.symbolId, condition.startGen, isLookahead = true))
      is GenAcceptCondition.NotExists -> out.add(ObservedCondSym(condition.symbolId, condition.startGen, isLookahead = true))
      is GenAcceptCondition.NoLongerMatch -> out.add(ObservedCondSym(condition.symbolId, condition.startGen, isLookahead = false))
      is GenAcceptCondition.Unless -> out.add(ObservedCondSym(condition.symbolId, condition.startGen, isLookahead = false))
      is GenAcceptCondition.OnlyIf -> out.add(ObservedCondSym(condition.symbolId, condition.startGen, isLookahead = false))
    }
  }

  // cond root starter 들을 (symbolId, keyGen, sameInput) 로 dedup/정렬해 emit.
  //
  // 모든 watcher 계열 (bounded = except/join/longest, lookahead = Exists/NotExists)
  // 공통 span-정규화 key:
  //   pos Curr/Mid (term) → (MID, same-input): key=ctx.gen, 이번 입력이 첫 글자.
  //   pos Curr/Mid (edge) → 과거 경계 — 그 시점에 이미 등록된 watcher, emit 생략.
  //   pos Next → (NEXT, fresh): key=gen, 소비는 다음 step 부터.
  //
  // 2026-07-30: lookahead 도 이 규약으로 통일 (bug B 수정). 이전에는 lookahead 만
  // (NEXT, same-input) = "key 는 등록 gen, span 은 key-1" 구 규약이었고, 조건 leaf 의
  // anchor 는 frame 에 따라 span 또는 span+1 로 resolve 되어 한 key 가 두 span 을
  // 뜻했다 (`starterDied` 의 fresh fallback 이 그 충돌을 임시로 봉합). remapEdgeCondGens
  // 가 lookahead anchor 도 Grand(=dot) 로 리맵하게 되면서 anchor = key = span 시작으로
  // 일치한다. 상세: mgroup3/docs/watcher_anchor_dedup.md §9.
  private fun emitCondRootStarters(
    observed: Set<ObservedCondSym>,
    edgeFrame: Boolean,
    add: (symbolId: Int, milestoneGroupId: Int, keyGen: KernelTemplateGen, sameInput: Boolean) -> Unit,
  ) {
    val keyed = observed.mapNotNull { obs ->
      when (obs.pos) {
        Curr, GenNodeGeneration.Mid ->
          if (edgeFrame) null else Triple(obs.symbolId, KernelTemplateGen.MID, true)
        Next -> Triple(obs.symbolId, KernelTemplateGen.NEXT, false)
        else -> null
      }
    }.distinct().sortedWith(compareBy({ it.first }, { it.second.number }, { it.third }))
    for ((sym, keyGen, sameInput) in keyed) {
      val rootInfo = rootPaths[sym] ?: genRootPathFromSymbol(sym)
      rootPaths[sym] = rootInfo
      add(sym, rootInfo.milestoneGroupId, keyGen, sameInput)
    }
  }

  fun genRootPathFromSymbol(symbolId: Int): PathRootInfo {
    val builder = PathRootInfo.newBuilder()

    val startNode = GenNode(symbolId, 0, Curr, Curr)

    builder.symbolId = symbolId
    builder.milestoneGroupId = milestoneGroupIdOf(setOf(startNode))

    val graph = tasks.derivedFrom(setOf(startNode))

    // start node로부터 도달 가능한 cond symbol들 모두 수집
    builder.addAllInitialCondSymbolIds(graph.observingCondSymbolIds.map { it.symbolId }.distinct().sorted())

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
    // 초기(gen 0) 에 등장하는 모든 kernel 을 보고용 added 로 기록.
    // 초기 해석은 모든 태그가 0 으로 resolve 되므로 리맵 불필요.
    // m2 의 초기 summary 는 derive run 전체를 담으므로 fullGraph.
    emitAddedKernels(
      parsingActionsBuilder, graph,
      remapEdgeReportGens = false,
      starts = emptySet(),
      derivePhaseNodes = emptySet(),
      excludeFromAdded = emptySet(),
      fullGraph = true,
    )

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
    // derive 단계의 노드 집합 — added 보고에서 closure 를 제외하는 기준 (emitAddedKernels 참고)
    val derivePhaseNodes = graph.nodes.toSet()

    val actions = mutableListOf<Mgroup3ParserData.TermGroupAction>()

    val termGroups = progressibleTermGroupsOf(graph)
    val sortedTermGroups = termGroups.sortedWith(
      compareBy { it.first.toString() }
    )
    for ((termGroup, termNodes) in sortedTermGroups) {
      val actionBuilder = Mgroup3ParserData.TermGroupAction.newBuilder()
        .setTermGroup(termGroup)

      val taBuilder = actionBuilder.termActionBuilder
      // milestone 들을 barrier 로 — m2 의 term 시뮬레이션처럼 milestone 위쪽 cascade 는
      // 템플릿에 넣지 않는다 (런타임의 tip/mid edge 액션이 전담). milestone 의 progress
      // 조건은 g2.barrierProgressConditions 로 수집됨.
      val g2 = tasks.progressedFrom(graph, termNodes, Next, barrierNodes = milestoneNodes)

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
            val condObserved = mutableSetOf<ObservedCondSym>()
            condObserved.addAll(g2.observingCondSymbolIds)
            observedCondSymbolsFromAcc(acc, condObserved)
            // 새 milestone group 의 derive graph 의 observingCondSymbolIds.
            // 새 group 의 milestone 들에서 시작하는 derive 가 NJoin/NLongest 등 만나면 그 cond_sym 도 추적해야 함.
            // 새 group 은 이번 step 의 gen 에 dot 이 놓이므로, 그 프레임의 관찰은 전부
            // 현재 프레임의 Next (= fresh watcher, span gen).
            val newMgroupNodes = subReachables.map {
              GenNode(it.symbolId, it.pointer, Prev, Curr)
            }.toSet()
            val newMgroupGraph = tasks.derivedFrom(newMgroupNodes)
            condObserved.addAll(newMgroupGraph.observingCondSymbolIds.map { ObservedCondSym(it.symbolId, Next, it.isLookahead) })
            append.addAllObservingCondSymbolIds(condObserved.map { it.symbolId }.distinct().sorted())
            // mgroup2 의 lookahead_requiring_symbols 와 동일한 정보: 각 cond root sym 의 starter milestone group id.
            // runtime 에서 main path 가 이 milestone group 을 attach 하는 시점에 starter 도 같이 시작.
            emitCondRootStarters(condObserved, edgeFrame = false) { sym, mgid, kg, si ->
              replaceAndAppendBuilder.appendBuilder.addCondRootStartersBuilder().apply {
                symbolId = sym
                milestoneGroupId = mgid
                keyGen = kg
                sameInput = si
              }
            }
          }
        }
      }

      // replace_and_progresses: graph의 milestone 중 g2에서 progress된 것들
      // (즉, 자기 자신의 끝까지 진행된 milestone들). barrier 라 progressedNodes 에는
      // 없고 barrierProgressConditions 에 조건이 수집되어 있다.
      val progressedMilestones = g2.barrierProgressConditions.keys.intersect(milestoneNodes)
        .groupBy { parentMilestone ->
          g2.barrierProgressConditions[parentMilestone]!!
        }
      val progressedEntries = progressedMilestones.entries.sortedBy { it.key }
      for ((acc, subMilestones) in progressedEntries) {
        val replaceAndProgressBuilder = taBuilder.addReplaceAndProgressesBuilder()
        // subMilestones가 진행된 결과의 mgroup. 단, replace target은 진행 전(원래 milestone) 들의 mgroup이어야 함
        val replaceKernels = subMilestones.map { it.toKernelTemplateProto() }.toSet()
        replaceAndProgressBuilder.setReplaceMilestoneGroupId(milestoneGroupIdOfKernelTemplates(replaceKernels))
        replaceAndProgressBuilder.setAcceptCondition(acc.toProto())
      }

      // parsing actions — barrier 시뮬레이션이라 milestone 위쪽 cascade 는 g2 에 없음.
      // milestone 자신은 added 보고에서 제외 (m2 의 barrier 가 start kernel 의 progress 를
      // summary 에서 빼는 것에 대응 — 그 kernel 들은 edge 액션 쪽 보고가 커버).
      fillParsingActions(
        taBuilder.parsingActionsBuilder, g2,
        starts = milestoneNodes,
        derivePhaseFinishedNodes = derivePhaseFinishedNodes,
        includeProgressOfStarts = false,
        derivePhaseNodes = derivePhaseNodes,
        excludeFromAdded = milestoneNodes,
      )

      actions.add(actionBuilder.build())
    }

    return actions
  }

  // graph (g2)에서 일어난 finish/progress들을 parsingActions에 기록.
  // derive 단계에서 이미 일어난 finish/progress는 제외하고 progress phase에서 새로 등장한 것만 기록.
  // includeProgressOfStarts: starts의 progress(즉 startNodeProgress)도 기록할지 여부
  //
  // remapEdgeReportGens: edge action 전용 보고 좌표 리맵.
  //   graph 내부 좌표는 join key/condition 과 co-designed 라 불변으로 두고,
  //   proto 로 내보내는 "보고용" gen 태그만 mgroup2/milestone2 의 의미에 맞춘다.
  //   edge 템플릿에서 parent 의 left-edge chain 노드들은 graph 상 Curr 좌표로
  //   derive 되지만, 의미상으로는 parent milestone 의 dot gen(=Prev, 런타임
  //   grandParentGen = mgroup2 의 edge.first.gen)에서 시작한다. milestone2 는
  //   edge 템플릿을 startingCtxFrom(start, -1) 로 만들어 chain 의 begin 이
  //   tag 0 이 되도록 하는데, 그에 대응.
  //   - finished/progressed 의 startGen: Curr → Prev
  //   - progressed 의 midGen(=before.endGen):
  //       before ∈ starts(tip progs) → Curr 유지 (tip 의 dot gen),
  //       before ∈ derivePhaseNodes → Curr → Prev (derive 단계의 nullable 진행),
  //       그 외(Next 등) → 유지.
  private fun fillParsingActions(
    builder: ParsingActions.Builder,
    g2: GenParsingGraph,
    starts: Set<GenNode>,
    derivePhaseFinishedNodes: Set<GenNode>,
    includeProgressOfStarts: Boolean,
    remapEdgeReportGens: Boolean = false,
    // pointer==0 parent 의 edge frame: 보고용 조건의 dot anchor 를 Grand 로
    // (edgeActionFrom 의 condGens 와 동일한 이유 — replay 가 런타임과 같은
    //  watcher key 규약으로 fin 을 찾아야 함).
    remapCondGensToGrand: Boolean = false,
    derivePhaseNodes: Set<GenNode> = emptySet(),
    excludeFromAdded: Set<GenNode> = emptySet(),
    // finished/progressed/added 모든 보고 채널에서 제외할 노드들.
    // term action 의 milestone-위쪽 cascade (m2 의 barrier 바깥 — 런타임 edge 액션이 전담).
    excludeFromReports: Set<GenNode> = emptySet(),
  ) {
    fun reportStartGen(tag: GenNodeGeneration): GenNodeGeneration =
      if (remapEdgeReportGens && tag == Curr) Prev else tag

    fun reportCond(cond: GenAcceptCondition): GenAcceptCondition =
      if (remapCondGensToGrand) remapEdgeCondGens(cond) else cond

    for (finished in g2.finishedNodes.sortedWith(compareBy({ it.symbolId }, { it.pointer }))) {
      // derive phase에서 이미 finish된 노드는 제외
      if (finished in derivePhaseFinishedNodes) continue
      if (finished in excludeFromReports) continue
      builder.addFinishedBuilder().apply {
        this.symbolId = finished.symbolId
        this.pointer = finished.pointer
        this.startGen = reportStartGen(finished.startGen).toProto()
        this.finishCondition = reportCond(g2.acceptConditions[finished]!!).toProto()
      }
    }
    for ((before, after) in g2.progressedNodes.entries.sortedWith(
      compareBy({ it.key.symbolId }, { it.key.pointer })
    )) {
      if (!includeProgressOfStarts && before in starts) continue
      if (before in excludeFromReports || after in excludeFromReports) continue
      // edge action 에서 derive 단계 안에서 완결된 progress (nullable 진행 —
      // after 도 derive-phase 노드) 는 이번 step 의 사건이 아니므로 보고하지 않음.
      // (그 결과 상태는 이후 단계의 progressed/added 가 커버.)
      if (remapEdgeReportGens && after in derivePhaseNodes) continue
      // (sym, ptr, before.startGen, before.endGen) → (sym, ptr+1, after.startGen, after.endGen)
      // 즉 startGen=before.startGen=after.startGen (NSequence는 startGen 유지),
      // mid_gen=before.endGen, end_gen=after.endGen=NEXT
      val reportMidGen =
        if (remapEdgeReportGens && before.endGen == Curr && before !in starts && before in derivePhaseNodes) Prev
        else before.endGen
      builder.addProgressedBuilder().apply {
        this.symbolId = before.symbolId
        this.pointer = before.pointer
        this.startGen = reportStartGen(before.startGen).toProto()
        this.midGen = reportMidGen.toProto()
      }
    }

    emitAddedKernels(
      builder, g2, remapEdgeReportGens, starts, derivePhaseNodes,
      excludeFromAdded + excludeFromReports, remapCondGensToGrand = remapCondGensToGrand,
    )
  }

  // added kernels: 보고 좌표로 기록하는 kernel 들 (kernels_history 전용).
  // mgroup2 의 TasksSummary2.added_kernels 에 대응 — m2 의 summary 는 progress-phase
  // task run 의 kernel 들만 담는다 (ProgressTask 의 source+next, 새 derive, finish).
  // derive closure 는 포함되지 않는다 (closure 는 startingCtxFrom 의 별도 run).
  // 따라서 fullGraph=false 면 progressedNodes 의 양변 + progress phase 에 새로 생긴
  // 노드만 선택. fullGraph=true 는 초기 액션 전용 (m2 의 초기 summary 는 derive run 포함).
  // 같은 보고 좌표로 합쳐지는 노드는 Or 로 병합.
  private fun emitAddedKernels(
    builder: ParsingActions.Builder,
    g: GenParsingGraph,
    remapEdgeReportGens: Boolean,
    starts: Set<GenNode>,
    derivePhaseNodes: Set<GenNode>,
    excludeFromAdded: Set<GenNode>,
    fullGraph: Boolean = false,
    remapCondGensToGrand: Boolean = false,
  ) {
    data class AddedKey(
      val symbolId: Int,
      val pointer: Int,
      val startGen: KernelTemplateGen,
      val endGen: KernelTemplateGen,
    )

    val selectedNodes: Collection<GenNode> = if (fullGraph) g.nodes else buildSet {
      // progress phase 에 새로 생긴 노드들 (progressed 결과 + 새 derive)
      addAll(g.nodes)
      removeAll(derivePhaseNodes)
      // progress 의 source 노드들 (closure 안의 노드라도 m2 의 ProgressTask kernel 에 해당)
      addAll(g.progressedNodes.keys)
      addAll(g.progressedNodes.values)
    }
    val addedConds = LinkedHashMap<AddedKey, GenAcceptCondition>()
    for (n in selectedNodes) {
      if (n in excludeFromAdded) continue
      val startTag = if (remapEdgeReportGens && n.startGen == Curr) Prev else n.startGen
      val endTag =
        if (remapEdgeReportGens && n !in starts && n in derivePhaseNodes && n.endGen == Curr) Prev
        else n.endGen
      val key = AddedKey(n.symbolId, n.pointer, startTag.toProto(), endTag.toProto())
      val rawCond = g.acceptConditions[n] ?: GenAcceptCondition.Always
      // 보고용 조건의 dot anchor 리맵 (fillParsingActions.reportCond 와 동일한 이유).
      val cond = if (remapCondGensToGrand) remapEdgeCondGens(rawCond) else rawCond
      val existing = addedConds[key]
      addedConds[key] = if (existing != null) GenAcceptCondition.Or.from(existing, cond) else cond
    }
    val sorted = addedConds.entries.sortedWith(
      compareBy({ it.key.symbolId }, { it.key.pointer }, { it.key.startGen.number }, { it.key.endGen.number })
    )
    for ((key, cond) in sorted) {
      builder.addAddedBuilder().apply {
        this.symbolId = key.symbolId
        this.pointer = key.pointer
        this.startGen = key.startGen
        this.endGen = key.endGen
        this.acceptCondition = cond.toProto()
      }
    }
  }

  private fun edgeActionFrom(
    graph: GenParsingGraph,
    parentNode: GenNode,
    starts: Set<GenNode>,
    derivePhaseFinishedNodes: Set<GenNode>,
    derivePhaseNodes: Set<GenNode>,
  ): EdgeAction {
    val builder = EdgeAction.newBuilder()

    val appendingMilestones = appendingMilestonesOf(graph, Curr)
    // parentNode 자체에서 도달 가능한 appendingMilestones들만 의미가 있으므로 필터링
    val reachables = graph.reachablesFrom(parentNode, appendingMilestones)
    val groupedAppendings = reachables.groupBy { graph.acceptConditions[it]!! }
    val sortedAppendings = groupedAppendings.entries.sortedBy { it.key }
    // edge frame 의 bounded/longest 조건 anchor 를 Grand(=parent 의 dot) 로 리맵.
    // m3 의 rea 부착은 항상 dot+1 에 일어나므로 (same-input 부착 규약) parent 의 dot 은
    // 균일하게 parentGen-1 — 런타임 edge GRAND 바인딩이 이 값으로 정의된다.
    val remapDotToGrand = true
    fun condGens(c: GenAcceptCondition): GenAcceptCondition =
      if (remapDotToGrand) remapEdgeCondGens(c) else c

    for ((acc, appending) in sortedAppendings) {
      val appendBuilder = builder.addAppendMilestoneGroupsBuilder()
      appendBuilder.milestoneGroupId = milestoneGroupIdOf(appending.toSet())
      appendBuilder.acceptCondition = condGens(acc).toProto()
      val condObserved = mutableSetOf<ObservedCondSym>()
      condObserved.addAll(graph.observingCondSymbolIds)
      observedCondSymbolsFromAcc(acc, condObserved)
      appendBuilder.addAllObservingCondSymbolIds(condObserved.map { it.symbolId }.distinct().sorted())
      // mgroup2 의 lookahead_requiring_symbols 와 동일.
      emitCondRootStarters(condObserved, edgeFrame = true) { sym, mgid, kg, si ->
        appendBuilder.addCondRootStartersBuilder().apply {
          symbolId = sym
          milestoneGroupId = mgid
          keyGen = kg
          sameInput = si
        }
      }
    }

    // parentNode가 progress되는 경우 (즉, parent의 시작 노드까지 reduce 가능한 경우).
    // parentNode 는 barrier 라 progress 가 적용되지 않고 조건만 수집된다.
    val parentProgressCond = graph.barrierProgressConditions[parentNode]
    if (parentProgressCond != null) {
      builder.startNodeProgress = condGens(parentProgressCond).toProto()
    } else {
      builder.clearStartNodeProgress()
    }

    // parentNode 는 added 보고에서 제외 — parent kernel 의
    // 실제 begin 은 이 템플릿의 gen 태그로 표현 불가 (상위 edge 의 보고가 커버).
    val excludeFromAdded = setOf(parentNode)
    fillParsingActions(
      builder.parsingActionsBuilder, graph,
      starts = starts,
      derivePhaseFinishedNodes = derivePhaseFinishedNodes,
      includeProgressOfStarts = true,
      remapEdgeReportGens = true,
      remapCondGensToGrand = remapDotToGrand,
      derivePhaseNodes = derivePhaseNodes,
      excludeFromAdded = excludeFromAdded,
    )

    return builder.build()
  }

  fun genTipEdgeAction(parent: KernelTemplate, tipMgroupId: Int): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val tipMgroup = milestoneGroups[tipMgroupId]!!
    // tip edge: parent --[derive]--> ... --> child(tip)들
    val graph = tasks.derivedFrom(setOf(parentNode))
    val derivePhaseFinishedNodes = graph.finishedNodes.toSet()
    // derive 단계의 노드 집합 (보고용 gen 리맵 판별에 사용 — fillParsingActions 참고)
    val derivePhaseNodes = graph.nodes.toSet()
    // tip mgroup의 milestone들을 startGen=Curr 기준으로 graph에 추가
    // (이미 등장한 init node로 들어오는 incoming edge를 progressed milestone으로 동일하게 추가)
    val progs = tipMgroup.map {
      GenNode(it.symbolId, it.pointer, Curr, Curr)
    }.toSet()
    addProgsWithIncomingEdges(graph, progs)
    // parentNode 는 barrier — parent 위쪽 cascade 는 템플릿에 넣지 않는다 (m2 의
    // edge 시뮬레이션 barrier 대응; 그 사건은 런타임의 상위 edge 액션이 전담).
    val g2 = tasks.progressedFrom(graph, progs, Next, barrierNodes = setOf(parentNode))

    return edgeActionFrom(g2, parentNode, progs, derivePhaseFinishedNodes, derivePhaseNodes)
  }

  fun genMidEdgeAction(parent: KernelTemplate, child: KernelTemplate): EdgeAction {
    val parentNode = GenNode(parent.symbolId, parent.pointer, Prev, Curr)
    val graph = tasks.derivedFrom(setOf(parentNode))
    val derivePhaseFinishedNodes = graph.finishedNodes.toSet()
    val derivePhaseNodes = graph.nodes.toSet()
    // mid edge: parent --[derive]--> ... --> child(이미 진행 중)
    // child의 startGen은 Curr (parent와 같은 layer)이고 endGen은 Mid (이전 mgroup의 진행 결과)
    val prog = GenNode(child.symbolId, child.pointer, Curr, Mid)
    addProgsWithIncomingEdges(graph, setOf(prog))
    // parentNode 는 barrier (genTipEdgeAction 의 주석 참고).
    val g2 = tasks.progressedFrom(graph, setOf(prog), Next, barrierNodes = setOf(parentNode))

    return edgeActionFrom(g2, parentNode, setOf(prog), derivePhaseFinishedNodes, derivePhaseNodes)
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
      b.noLongerMatch = NoLongerMatchTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).setBodyEndGen(bodyEndGen.toProto()).build()
    is GenAcceptCondition.Exists ->
      b.lookaheadFound = LookaheadFoundTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.NotExists ->
      b.lookaheadNotfound = LookaheadNotFoundTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).build()
    is GenAcceptCondition.Unless ->
      b.except = ExceptTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).setEndGen(endGen.toProto()).build()
    is GenAcceptCondition.OnlyIf ->
      b.join = JoinTemplate.newBuilder().setSymbolId(symbolId).setStartGen(startGen.toProto()).setEndGen(endGen.toProto()).build()
  }
  return b.build()
}

fun GenNode.toKernelTemplateProto(): KernelTemplate =
  KernelTemplate.newBuilder().setSymbolId(symbolId).setPointer(pointer).build()
