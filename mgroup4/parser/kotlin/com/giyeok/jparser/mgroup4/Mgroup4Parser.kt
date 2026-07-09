package com.giyeok.jparser.mgroup4

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.*
import java.util.*

// mgroup4 interior group window n. env var MG4_INTERIOR_N 로 전역 오버라이드 —
// 코드 수정 없이 전 스위트를 임의 n 으로 돌리기 위함 (MG3_RECORD_COND_DIFF 패턴).
val mg4EnvInteriorMaxDepth: Int? =
  (System.getenv("MG4_INTERIOR_N") ?: System.getProperty("mg4.interiorN"))?.toIntOrNull()
// env 미설정 시 생성자 기본값.
const val mg4DefaultInteriorMaxDepth: Int = 1

// mgroup4 mean-shape 카운터 opt-in (§5.1) — 파스 출력 무영향, 진단 전용.
val mg4ShapeStatsEnabled: Boolean =
  System.getenv("MG4_SHAPE_STATS") != null || System.getProperty("mg4.shapeStats") != null

// A4 병합-패스 내부 프로파일 opt-in. 매 gen mergeInteriorGroups 안의 세부 단계
// (chainToList / bucket hashing / verdict / fold) 시간을 나노초로 분해. 기본 false
// 라 hot path 에 System.nanoTime() 이 안 들어감 (phaseTiming 과 동일한 branch-predict
// 전략). 정식 시간 측정(별도 JVM)에서는 꺼야 오버헤드 없는 실측이 된다.
val mg4MergeProfileEnabled: Boolean =
  System.getenv("MG4_MERGE_PROFILE") != null || System.getProperty("mg4.mergeProfile") != null

class Mgroup4Parser(
  val data: Mgroup3ParserData,
  // mgroup4: interior milestone group 의 window 크기 n (tip 쪽 마지막 n 개 노드까지
  // group 허용). n=1 ≡ 현행 mgroup3 (병합 패스 미실행 — 제로코스트 하위호환).
  // env var MG4_INTERIOR_N 로 오버라이드 (전 스위트를 임의 n 으로 돌리기 위함 —
  // MG3_RECORD_COND_DIFF 패턴, RecordConditionEvaluator.kt:5 참고).
  interiorGroupMaxDepth: Int = mg4DefaultInteriorMaxDepth,
) {
  // 실효 n — 생성자 인자보다 env var 이 우선.
  val interiorGroupMaxDepth: Int = mg4EnvInteriorMaxDepth ?: interiorGroupMaxDepth

  // protobuf data 를 hot path 에서 한 번씩만 변환된 plain Kotlin record 로 wrap.
  // 모든 hot-path lookup 은 plain.* 사용.
  private val plain = ParserDataPlain(data)
  private var verbose = false
  // 특정 step (= 이 gen 으로 진행되는 parseStep) 에서 trace 출력.
  // null 이면 trace 안 함. setTrace(gen) 으로 활성.
  private var traceGen: Int? = null

  // Phase timing — opt-in for diagnostics only. enablePhaseTiming() 후에만 측정 코드 활성화.
  // 기본 false 라 parseStep 의 hot path 에 System.nanoTime() 호출이 안 들어감 (JIT branch predict).
  private var phaseTimingEnabled: Boolean = false
  val phaseNanos: LongArray = LongArray(8)
  fun enablePhaseTiming() { phaseTimingEnabled = true; resetPhaseTimers() }
  fun disablePhaseTiming() { phaseTimingEnabled = false }
  fun resetPhaseTimers() { for (i in phaseNanos.indices) phaseNanos[i] = 0L }
  fun reportPhaseTimers(): String {
    val labels = listOf("step1", "step1b", "step2", "step3", "step4", "step5_evolve", "step6_prune", "step7_history")
    val total = phaseNanos.sum()
    val sb = StringBuilder("phase timing (total ${total / 1_000_000}ms): ")
    for (i in phaseNanos.indices) {
      val ms = phaseNanos[i] / 1_000_000.0
      val pct = if (total > 0) 100.0 * phaseNanos[i] / total else 0.0
      sb.append("${labels[i]}=${"%.1f".format(ms)}ms(${"%.1f".format(pct)}%) ")
    }
    return sb.toString()
  }
  private inline fun phaseMark(slot: Int, t0: Long): Long {
    if (!phaseTimingEnabled) return 0L
    val t = System.nanoTime()
    phaseNanos[slot] += t - t0
    return t
  }

  // A4 병합-패스 내부 세부 타이머 (mg4MergeProfileEnabled 일 때만 채워짐).
  // slot: 0=chainToList, 1=bucketHash, 2=verdict, 3=fold, 4=mergeTotal(패스 전체 self-time).
  val mergeNanos: LongArray = LongArray(5)
  fun resetMergeTimers() { for (i in mergeNanos.indices) mergeNanos[i] = 0L }
  fun reportMergeTimers(): String {
    val labels = listOf("chainToList", "bucketHash", "verdict", "fold", "mergeTotal")
    val total = mergeNanos[4]
    val sb = StringBuilder("merge timing (mergeTotal ${total / 1_000_000}ms): ")
    for (i in mergeNanos.indices) {
      val ms = mergeNanos[i] / 1_000_000.0
      val pct = if (total > 0) 100.0 * mergeNanos[i] / total else 0.0
      sb.append("${labels[i]}=${"%.1f".format(ms)}ms(${"%.1f".format(pct)}%) ")
    }
    return sb.toString()
  }

  fun setVerbose(): Mgroup4Parser {
    verbose = true
    return this
  }

  fun setTrace(gen: Int): Mgroup4Parser {
    traceGen = gen
    return this
  }

  private fun traceOn(gen: Int): Boolean = traceGen == gen

  // (parent symbol, parent pointer, tip group id) -> edge action
  val tipEdgeActionsMap: Map<Pair<KernelTemplatePair, Int>, EdgeActionPlain> =
    plain.tipEdgeActions.associate {
      Pair(KernelTemplatePair(it.parent.symbolId, it.parent.pointer), it.tipGroupId) to it.edgeAction
    }

  // (parent symbol, parent pointer, tip symbol, tip pointer) -> edge action
  val midEdgeActionsMap: Map<Pair<KernelTemplatePair, KernelTemplatePair>, EdgeActionPlain> =
    plain.midEdgeActions.associate {
      Pair(
        KernelTemplatePair(it.parent.symbolId, it.parent.pointer),
        KernelTemplatePair(it.tip.symbolId, it.tip.pointer)
      ) to it.edgeAction
    }

  // 새로 추적해야 하는 cond symbol들에 대해 cond path를 만든다.
  // 한 cond symbol이 다른 cond symbol을 참조할 수도 있으므로 transitive closure.
  fun condPathsFor(condSymbolIds: Collection<Int>, gen: Int): Map<PathRoot, PathMap> {
    val builder = mutableMapOf<Int, PathShape>()
    val queue: Queue<Int> = LinkedList()
    queue.addAll(condSymbolIds)
    while (queue.isNotEmpty()) {
      val symId = queue.poll()
      if (symId !in builder) {
        val rootInfo = plain.pathRoots[symId] ?: continue
        builder[symId] = PathShape(null, rootInfo.milestoneGroupId)
        queue.addAll(rootInfo.initialCondSymbolIds)
      }
    }
    return builder.mapKeys { (symId, _) -> PathRoot(symId, gen) }
      .mapValues { (_, shape) -> mapOf(shape to Always) }
  }

  fun initCtx(startSymbolId: Int): ParsingCtx {
    val rootInfo = plain.pathRoots[startSymbolId]
      ?: throw IllegalArgumentException("No path root for symbol $startSymbolId")

    val mainRoot = PathRoot(startSymbolId, 0)
    val mainPaths: PathMap = mapOf(PathShape(null, rootInfo.milestoneGroupId) to Always)
    val initialCondPaths = condPathsFor(rootInfo.initialCondSymbolIds, 0)

    // main + cond 모두 같은 map.
    val allPaths = LinkedHashMap<PathRoot, PathMap>()
    allPaths[mainRoot] = mainPaths
    allPaths.putAll(initialCondPaths)

    val initialApps = mutableListOf<ActionApplication>()
    val initialFinishedKernels = mutableListOf<FinishedKernelRecord>()
    if (rootInfo.parsingActions != null) {
      initialApps.add(initialApplication(rootInfo.parsingActions, mainRoot))
    }
    // cond root 들의 초기 derive 결과도 보고 — mgroup2 의 초기 tasksSummary 는 in-graph
    // cond body (join/longest/except) 의 derive closure 를 포함한다.
    for (condRoot in initialCondPaths.keys) {
      val condRootInfo = plain.pathRoots[condRoot.symbolId] ?: continue
      if (condRootInfo.parsingActions != null) {
        initialApps.add(initialApplication(condRootInfo.parsingActions, condRoot))
      }
    }
    var initialMainRootFinish: AcceptCondition? = null
    if (rootInfo.selfFinishAcceptCondition != null) {
      val selfFinishCond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(0, 0, 0)
      initialFinishedKernels.add(
        FinishedKernelRecord(
          Kernel(startSymbolId, 1, 0),
          selfFinishCond,
          mainRoot,
        )
      )
      initialMainRootFinish = selfFinishCond
    }

    // gen 0 의 zero-width self-finish: 초기 cond root 가 빈 span (0,0) 으로 완성될
    // 수 있으면 entry 0 의 condPathFinishes 에 등록 — 빈 입력(또는 gen 0 에 end 가
    // 걸린 bounded 조건)의 평가가 이를 본다. 없으면 nullable join (OnlyIf@gen0) 이
    // finish 부재로 Never 가 되어 빈 입력이 거부된다.
    val initialCondPathFinishes = mutableMapOf<PathRoot, AcceptCondition>()
    for (condRoot in initialCondPaths.keys) {
      val condRootInfo = plain.pathRoots[condRoot.symbolId] ?: continue
      if (condRootInfo.selfFinishAcceptCondition != null) {
        initialCondPathFinishes[condRoot] =
          condRootInfo.selfFinishAcceptCondition.toAcceptCondition(0, 0, 0)
      }
    }

    val initialEntry = HistoryEntry(
      actionApplications = initialApps,
      finishedKernels = initialFinishedKernels,
      condPathFinishes = initialCondPathFinishes.toMap(),
      activeCondPaths = initialCondPaths.keys,
      mainRootFinish = initialMainRootFinish,
      // 초기 cond root 들은 모두 보고 대상 (m2 의 초기 in-graph closure 에 대응).
      reportedCondRoots = initialCondPaths.keys,
    )

    return ParsingCtx(
      gen = 0,
      line = 0,
      col = 0,
      mainRoot = mainRoot,
      paths = allPaths,
      history = arrayListOf(initialEntry),
    )
  }

  fun initCtx(): ParsingCtx = initCtx(plain.startSymbolId)

  // 초기 액션의 모든 태그는 root 의 startGen 으로 resolve (cond root 는 0 이 아닐 수 있음).
  private fun initialApplication(pa: ParsingActionsPlain, root: PathRoot): ActionApplication {
    val base = root.startGen
    return ActionApplication(pa, root, base, base, base, base, base, base, base)
  }

  fun expectedInputsOf(ctx: ParsingCtx): TermSet {
    val mainPathMap = ctx.paths[ctx.mainRoot] ?: emptyMap()
    val termGroups =
      mainPathMap.keys.flatMap { shape ->
        plain.termActions[shape.tipGroupId]?.map { it.termGroup } ?: listOf()
      }
    val termSetBuilder = TermGroupUtil.TermGroupBuilder()
    for (termGroup in termGroups) {
      termSetBuilder.add(termGroup)
    }
    return termSetBuilder.build()
  }

  // (tipGroupId, input) → TermActionPlain? cache.
  private val termActionCache = HashMap<Long, TermActionPlain?>()
  fun findApplicableAction(shape: PathShape, input: Char): TermActionPlain? {
    val key = (shape.tipGroupId.toLong() shl 32) or input.code.toLong()
    if (termActionCache.containsKey(key)) return termActionCache[key]
    val actions = plain.termActions[shape.tipGroupId]
    val result = actions?.find { action -> TermGroupUtil.isMatch(action.termGroup, input) }?.termAction
    termActionCache[key] = result
    return result
  }

  // 한 path 의 (shape, prevCondition) 에 term action 을 적용해서 다음 path 들을 만들고 finish/progress 기록.
  // gen 매핑 (term action):
  //   CURR (Prev) = parent milestone 의 gen (= path 의 tip 이 만들어진 gen)
  //   MID  (Curr) = ctx.gen (직전 입력 후 gen)
  //   NEXT (Next) = gen (이번 입력 후 gen)
  private fun applyTermAction(
    oldShape: PathShape,
    oldCondition: AcceptCondition,
    pathRoot: PathRoot,
    termAction: TermActionPlain,
    midGen: Int,
    gen: Int,
    // 보고 전용 root anchor (same-input starter 는 startGen-1) — 좌표 보고에만 사용.
    rootReportGen: Int,
    nextPathsOut: MutableMap<PathShape, AcceptCondition>,
    appsOut: MutableList<ActionApplication>,
    finishesOut: MutableList<FinishedKernelRecord>,
    addedOut: MutableList<AddedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
    condRootStartersOut: MutableMap<PathRoot, PendingStarter>,
  ) {
    // A3 window-exit (spec §2.3): 이 term action 이 descend (replaceAndAppends) 를
    // 포함하고, shape 에 group 노드가 있으며, descend 로 그 노드의 depth 가 n 을 넘게
    // 되면 (post-descend depth > n) descend 전에 멤버별 분열해 각 멤버에 대해 term
    // action 을 재적용한다. n=1 이면 group 자체가 없으므로 이 분기는 안 탄다.
    // depth 계산은 tip 근처 n+1 칸만 순회 (§1.3 (b)) — group 이 window 안이면 유계.
    if (interiorGroupMaxDepth >= 2 && termAction.replaceAndAppends.isNotEmpty()) {
      val gDepth = groupDepthNearTip(oldShape.milestonePath, interiorGroupMaxDepth)
      if (gDepth >= 0 && gDepth + 1 > interiorGroupMaxDepth) {
        if (mg4ShapeStatsEnabled) mg4WindowExitSplits++
        for (memberShape in explodeShapeFully(oldShape)) {
          applyTermAction(
            oldShape = memberShape,
            oldCondition = oldCondition,
            pathRoot = pathRoot,
            termAction = termAction,
            midGen = midGen,
            gen = gen,
            rootReportGen = rootReportGen,
            nextPathsOut = nextPathsOut,
            appsOut = appsOut,
            finishesOut = finishesOut,
            addedOut = addedOut,
            rootProgressesOut = rootProgressesOut,
            observingSymbolIdsOut = observingSymbolIdsOut,
            condRootStartersOut = condRootStartersOut,
          )
        }
        return
      }
    }

    val parentGen = oldShape.milestonePath?.gen ?: pathRoot.startGen
    val grandGen = oldShape.milestonePath?.milestone?.gen ?: pathRoot.startGen
    // 보고 좌표용 바인딩 — m2 의 term genMap {0→mgroup.gen(갱신된 tip 부착 gen), 1→gen-1, 2→gen}.
    // 조건 resolve 는 런타임 바인딩(parentGen/grandGen) 유지 — cond root anchoring 과 한 몸.
    val reportParentGen = oldShape.milestonePath?.reportGen ?: rootReportGen
    val reportGrandGen = oldShape.milestonePath?.milestoneReportGen ?: rootReportGen

    val pa = termAction.parsingActions
    if (pa != null) {
      // 보고는 lazy — 액션 참조와 바인딩만 기록 (kernelsHistory 가 해석).
      appsOut.add(
        ActionApplication(pa, pathRoot, parentGen, midGen, gen, grandGen, reportParentGen, midGen, reportGrandGen)
      )
    }

    for (rea in termAction.replaceAndAppends) {
      val newAcceptCondition = rea.append.acceptCondition.toAcceptCondition(parentGen, midGen, gen, grandGen)
      val combined = And.from(oldCondition, newAcceptCondition)
      if (combined == Never) continue

      val replaceKernel = Kernel(rea.replace.symbolId, rea.replace.pointer, parentGen)
      val newMilestonePath = MilestonePath(
        gen = gen,
        milestone = replaceKernel,
        parent = oldShape.milestonePath,
        observingCondSymbolIds = rea.append.observingCondSymbolIds,
        // 새 tip group 은 이번 gen 에 부착; replace milestone 의 m2 식 gen 은
        // 직전 tip 의 (갱신된) 부착 gen (m2 replaceAndAppend 가 tip.gen 을 유지하는 것).
        reportGen = gen,
        milestoneReportGen = reportParentGen,
      )
      nextPathsOut.addPath(
        PathShape(milestonePath = newMilestonePath, tipGroupId = rea.append.milestoneGroupId),
        combined,
      )
      observingSymbolIdsOut.addAll(rea.append.observingCondSymbolIds)
      for (starter in rea.append.condRootStarters) {
        val key = starterKeyOf(starter, midGen = midGen, nextGen = gen) ?: continue
        condRootStartersOut[PathRoot(starter.symbolId, key)] =
          PendingStarter(starter.milestoneGroupId, starter.sameInput)
      }
    }

    for (rap in termAction.replaceAndProgresses) {
      val newAcceptCondition = rap.acceptCondition.toAcceptCondition(parentGen, midGen, gen, grandGen)
      val combined = And.from(oldCondition, newAcceptCondition)
      if (combined == Never) continue

      val parentPath = oldShape.milestonePath
      if (parentPath == null) {
        // root 에서 직접 self-progress (start symbol finish).
        val existing = rootProgressesOut[pathRoot]
        rootProgressesOut[pathRoot] = if (existing != null) Or.from(existing, combined) else combined

        finishesOut.add(
          FinishedKernelRecord(
            Kernel(pathRoot.symbolId, 1, rootReportGen),
            combined,
            pathRoot,
          )
        )
        // 보고용: root 의 ptr0 init kernel (mgroup2 progRootMilestone 의 ptr0 대응).
        addedOut.add(
          AddedKernelRecord(pathRoot.symbolId, 0, rootReportGen, rootReportGen, combined, pathRoot)
        )
      } else {
        // A2: reduce 가 tip 인접 노드(parentPath = oldShape.milestonePath)로 pop.
        // 이 노드가 group 이면 멤버별 template 로 tipEdge 조회가 갈리므로 여기서 분열
        // (spec item 2). singleton 이면 1개 멤버로 그대로 처리 (제로코스트).
        // applyEdgeAction 의 invariant: parentPath 는 항상 singleton (group 은 이
        // 진입점과 midEdge 진입점에서 미리 멤버 singleton 으로 펼침).
        val parentMembers = memberSingletonsForEdge(parentPath)
        if (parentMembers.size > 1 && mg4ShapeStatsEnabled) mg4ReduceSplits++
        for (pm in parentMembers) {
          val tipEdgeAction = tipEdgeActionsMap[
            Pair(pm.milestone.kernelTemplate, rap.replaceMilestoneGroupId)
          ] ?: continue
          val grandParentGen = pm.parent?.gen ?: pathRoot.startGen
          applyEdgeAction(
            parentPath = pm,
            edgeAction = tipEdgeAction,
            pathRoot = pathRoot,
            prevCondition = combined,
            grandParentGen = grandParentGen,
            parentGen = pm.gen,
            gen = gen,
            // m2 tip edge = (parent milestone @ milestoneReportGen) -> (tip group @ reportGen)
            reportCurrGen = pm.milestoneReportGen,
            reportMidGen = pm.reportGen,
            rootReportGen = rootReportGen,
            nextPathsOut = nextPathsOut,
            appsOut = appsOut,
            finishesOut = finishesOut,
            addedOut = addedOut,
            rootProgressesOut = rootProgressesOut,
            observingSymbolIdsOut = observingSymbolIdsOut,
            condRootStartersOut = condRootStartersOut,
          )
        }
      }
    }
  }

  // edge action gen 매핑:
  //   CURR (Prev) = grandparent gen
  //   MID  (Curr) = parent gen
  //   NEXT (Next) = gen (현재 step gen)
  //   GRAND = grand-grand-parent gen
  private fun applyEdgeAction(
    parentPath: MilestonePath,
    edgeAction: EdgeActionPlain,
    pathRoot: PathRoot,
    prevCondition: AcceptCondition,
    grandParentGen: Int,
    parentGen: Int,
    gen: Int,
    // 보고 좌표용 바인딩 — m2 edge genMap {0→edge.first.gen, 1→edge.second.gen, 2→gen}.
    // tip edge: first=parent milestone 의 m2 gen, second=tip 의 (갱신된) 부착 gen.
    // mid edge: first/second = 양끝 milestone 의 m2 gen.
    reportCurrGen: Int,
    reportMidGen: Int,
    rootReportGen: Int,
    nextPathsOut: MutableMap<PathShape, AcceptCondition>,
    appsOut: MutableList<ActionApplication>,
    finishesOut: MutableList<FinishedKernelRecord>,
    addedOut: MutableList<AddedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
    condRootStartersOut: MutableMap<PathRoot, PendingStarter>,
  ) {
    // GRAND = parent 의 dot gen. m3 의 rea 부착은 항상 dot+1 (same-input 부착 규약 —
    // 노드 gen = 부착 gen) 이므로 균일하게 parentGen - 1. bounded/longest 조건의
    // span-시작 anchor (생성기의 remapEdgeCondGens Curr/Mid→Grand) 가 이 값을 참조한다.
    val grandGrandParentGen = parentGen - 1
    val reportGrandGen = parentPath.milestoneReportGen
    val pa = edgeAction.parsingActions
    if (pa != null) {
      // 보고는 lazy — 액션 참조와 바인딩만 기록 (kernelsHistory 가 해석).
      appsOut.add(
        ActionApplication(
          pa, pathRoot,
          grandParentGen, parentGen, gen, grandGrandParentGen,
          reportCurrGen, reportMidGen, reportGrandGen,
          condition = prevCondition,
        )
      )
    }

    for (append in edgeAction.appendMilestoneGroups) {
      val condition = append.acceptCondition.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen)
      val combined = And.from(prevCondition, condition)
      if (combined == Never) continue
      // 런타임 gen(mp.gen) 은 처음 부착 gen 고정 (조건 anchoring 과 한 몸 — 갱신 금지).
      // 보고용 reportGen 만 현재 gen 으로 갱신: mgroup2 의 edge action appendings 가
      // MilestoneGroupKt(groupId, gen) 으로 tip group gen 을 갱신하는 것에 대응.
      val newParentPath = parentPath.copy(
        reportGen = gen,
        observingCondSymbolIds = append.observingCondSymbolIds,
      )
      nextPathsOut.addPath(
        PathShape(milestonePath = newParentPath, tipGroupId = append.milestoneGroupId),
        combined,
      )
      observingSymbolIdsOut.addAll(append.observingCondSymbolIds)
      for (starter in append.condRootStarters) {
        // edge frame: 과거 경계(CURR) watcher 는 그 시점에 이미 등록됨 — skip.
        val key = starterKeyOf(starter, midGen = parentGen, nextGen = gen) ?: continue
        condRootStartersOut[PathRoot(starter.symbolId, key)] =
          PendingStarter(starter.milestoneGroupId, starter.sameInput)
      }
    }

    if (edgeAction.startNodeProgress != null) {
      val startNodeProgressCondition =
        edgeAction.startNodeProgress.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen)
      val combined = And.from(prevCondition, startNodeProgressCondition)
      if (combined != Never) {
        val grandParent = parentPath.parent
        if (grandParent == null) {
          val existing = rootProgressesOut[pathRoot]
          rootProgressesOut[pathRoot] = if (existing != null) Or.from(existing, combined) else combined

          finishesOut.add(
            FinishedKernelRecord(
              Kernel(pathRoot.symbolId, 1, rootReportGen),
              combined,
              pathRoot,
            )
          )
          // 보고용: root 의 ptr0 init kernel (mgroup2 progRootMilestone 의 ptr0 대응).
          addedOut.add(
            AddedKernelRecord(pathRoot.symbolId, 0, rootReportGen, rootReportGen, combined, pathRoot)
          )
        } else {
          // A2: reduce 가 grandParent 로 pop. grandParent 가 group 이면 멤버별 template
          // 로 midEdge 조회가 갈리므로 여기서 분열 (spec item 2). 각 멤버 singleton 을
          // 새 frame 의 parentPath 로 재귀 → applyEdgeAction 의 parentPath 는 항상
          // singleton (invariant 유지). parentPath (현 frame) 는 이미 singleton 이므로
          // .milestone.kernelTemplate 은 안전.
          val grandMembers = memberSingletonsForEdge(grandParent)
          if (grandMembers.size > 1 && mg4ShapeStatsEnabled) mg4ReduceSplits++
          for (gm in grandMembers) {
            val midEdge = midEdgeActionsMap[
              Pair(gm.milestone.kernelTemplate, parentPath.milestone.kernelTemplate)
            ] ?: continue
            val grandGrandParentGen2 = gm.parent?.gen ?: pathRoot.startGen
            applyEdgeAction(
              parentPath = gm,
              edgeAction = midEdge,
              pathRoot = pathRoot,
              prevCondition = combined,
              grandParentGen = grandGrandParentGen2,
              parentGen = gm.gen,
              gen = gen,
              // m2 mid edge = (grandParent milestone @ m2 gen) -> (parent milestone @ m2 gen)
              reportCurrGen = gm.milestoneReportGen,
              reportMidGen = parentPath.milestoneReportGen,
              rootReportGen = rootReportGen,
              nextPathsOut = nextPathsOut,
              appsOut = appsOut,
              finishesOut = finishesOut,
              addedOut = addedOut,
              rootProgressesOut = rootProgressesOut,
              observingSymbolIdsOut = observingSymbolIdsOut,
              condRootStartersOut = condRootStartersOut,
            )
          }
        }
      }
    }
  }

  private fun resolveGen(genTag: KernelTemplateGen, currGen: Int, midGen: Int, nextGen: Int, grandGen: Int = currGen): Int =
    when (genTag) {
      KernelTemplateGen.CURR -> currGen
      KernelTemplateGen.MID -> midGen
      KernelTemplateGen.NEXT -> nextGen
      KernelTemplateGen.GRAND -> grandGen
      else -> currGen
    }

  // cond root starter 의 key resolve. MID = ctx.gen (bounded span-정규화), NEXT = gen.
  // CURR (과거 경계) 는 그 시점에 이미 등록된 watcher — 등록하지 않는다 (null).
  private fun starterKeyOf(starter: CondRootStarterPlain, midGen: Int, nextGen: Int): Int? =
    when (starter.keyGen) {
      KernelTemplateGen.MID -> midGen
      KernelTemplateGen.NEXT -> nextGen
      else -> null
    }

  // 시동 대기 중인 cond root starter — sameInput 이면 이번 입력이 watcher 의 첫 글자
  // (key==gen 인 lookahead 구 규약이면 실제 span 은 gen-1 — 보고 anchor 별도 기록).
  class PendingStarter(val milestoneGroupId: Int, val sameInput: Boolean)

  fun parseStep(ctx: ParsingCtx, input: Char, isLastInput: Boolean): ParsingCtx {
    val mainPathsBefore = ctx.paths[ctx.mainRoot] ?: emptyMap()
    if (mainPathsBefore.isEmpty()) {
      throw ParsingError.UnexpectedInput(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx), input)
    }
    val gen = ctx.gen + 1
    val nextLine: Int
    val nextCol: Int
    if (input == '\n') {
      nextLine = ctx.line + 1
      nextCol = 0
    } else {
      nextLine = ctx.line
      nextCol = ctx.col + 1
    }

    // 모든 path 의 next 결과. main 도 cond 도 같은 map.
    val nextPaths = mutableMapOf<PathRoot, MutableMap<PathShape, AcceptCondition>>()
    val appsByGroup = mutableListOf<ActionApplication>()
    val finishesByGroup = mutableListOf<FinishedKernelRecord>()
    val addedByGroup = mutableListOf<AddedKernelRecord>()
    val observingOut = HashSet<Int>()
    val rootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    // 죽는 cond path 의 possible-finish — end 가 직전 gen 인 late 채널.
    val latePfProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    val condRootStartersFromTerm = mutableMapOf<PathRoot, PendingStarter>()

    val trace = traceOn(gen)
    if (trace) {
      println("=== TRACE parseStep gen=$gen input='${if (input == '\n') "\\n" else input.toString()}' ===")
      println("  ctx.paths.size=${ctx.paths.size} (main + ${ctx.paths.size - 1} cond roots)")
    }

    var tPhase = if (phaseTimingEnabled) System.nanoTime() else 0L

    // mgroup4 (A2/A3): A1 의 step 진입 즉시 분열은 제거됐다. group 은 이제 여러 gen 을
    // 살며 (term descend 는 tip 만 만지므로 group 을 유지·심화), 다음 경우에만 분열한다:
    //  (A2) reduce 가 group 노드에 도달 — applyTermAction 의 tipEdge / applyEdgeAction 의
    //       midEdge 조회에서 group 노드의 멤버별 template 를 순회 (memberSingletonsForEdge).
    //  (A3) window-exit — term descend 로 group 노드가 depth > n 이 되기 직전 분열
    //       (applyTermAction 의 replaceAndAppends 안 — explodeIfWindowExit). n=1 이면
    //       병합 자체가 no-op 이라 group 없음 (제로코스트 — 아래 loop 이 현행과 동일).

    // step 1+2: main 과 cond paths 모두 같은 loop 로 termAction 적용.
    for ((root, pathMap) in ctx.paths) {
      val isMain = root == ctx.mainRoot
      val perRootNext = nextPaths.getOrPut(root) { mutableMapOf() }
      for ((shape, cond) in pathMap) {
        val ta = findApplicableAction(shape, input)
        if (ta != null) {
          applyTermAction(
            oldShape = shape,
            oldCondition = cond,
            pathRoot = root,
            termAction = ta,
            midGen = ctx.gen,
            gen = gen,
            rootReportGen = ctx.rootReportGens[root] ?: root.startGen,
            nextPathsOut = perRootNext,
            appsOut = appsByGroup,
            finishesOut = finishesByGroup,
            addedOut = addedByGroup,
            rootProgressesOut = rootProgresses,
            observingSymbolIdsOut = observingOut,
            condRootStartersOut = condRootStartersFromTerm,
          )
        } else if (!isMain) {
          // cond path 가 input 매치 못해 dead — possible_finishes 검사.
          // 이 finish 의 end 는 직전 gen (ctx.gen) — eager finish 와 end 가 다르므로
          // late 채널로 분리 등록한다 (bounded/longest 의 정확한 span discharge 용).
          val mg = plain.milestoneGroups[shape.tipGroupId]
          if (mg != null) {
            for (pf in mg.possibleFinishes) {
              if (pf.symbolId == root.symbolId) {
                val prevGen = shape.milestonePath?.gen ?: root.startGen
                val midGenLocal = ctx.gen
                val pfCond = pf.acceptCondition.toAcceptCondition(prevGen, midGenLocal, gen)
                val combined = And.from(cond, pfCond)
                if (combined != Never) {
                  val existing = latePfProgresses[root]
                  latePfProgresses[root] = if (existing != null) Or.from(existing, combined) else combined
                }
              }
            }
          }
        }
      }
      if (perRootNext.isEmpty()) nextPaths.remove(root)
    }

    if (trace) {
      val nm = nextPaths[ctx.mainRoot] ?: emptyMap()
      println("  after step 1+2: nextMainPaths.size=${nm.size}, total cond roots=${nextPaths.size - if (ctx.mainRoot in nextPaths) 1 else 0}")
    }

    tPhase = phaseMark(0, tPhase)

    // same-input 시동이 죽었을 때 (매치 실패 / 살아남은 path 없음):
    //  - lookahead 계열 key (== gen): 구 규약의 fresh fallback — 같은 key 를 다음 경계
    //    watcher (span gen) 로 재시동한다. 드리프트하는 lookahead anchor 는 같은 key 로
    //    span gen-1 (same-input) 과 span gen (fresh) 양쪽 해석을 요구할 수 있다.
    //  - bounded 계열 key (== ctx.gen): span-정규화 — 그 span 의 매치는 불가로 확정,
    //    key 를 소진시켜 이후 재시동 (span 이 어긋난 zombie watcher) 을 막는다.
    fun starterDied(root: PathRoot, shape: PathShape, out: MutableMap<PathRoot, MutableMap<PathShape, AcceptCondition>>) {
      if (root.startGen == gen) {
        out[root] = mutableMapOf(shape to Always)
      } else {
        ctx.everSeenCondRoots.add(root)
      }
    }

    // step 1b: main path 의 액션에 등록된 cond root starter 들 시동.
    //  - sameInput: 이번 입력이 watcher 의 첫 글자. bounded 계열은 key==ctx.gen (span-정규화),
    //    lookahead 계열은 key==gen (구 규약 — 실제 span 은 gen-1, 보고 anchor 별도 기록).
    //  - !sameInput: fresh — 시동만 하고 소비는 다음 step 부터 (새 경계 watcher).
    for ((starterRoot, pending) in condRootStartersFromTerm) {
      if (starterRoot in ctx.paths.keys) continue
      if (starterRoot in nextPaths.keys) continue
      if (starterRoot in ctx.everSeenCondRoots) continue
      val rootInfo = plain.pathRoots[starterRoot.symbolId] ?: continue
      val starterShape = PathShape(null, pending.milestoneGroupId)
      if (!pending.sameInput) {
        // fresh 시동만.
        nextPaths[starterRoot] = mutableMapOf(starterShape to Always)
      } else {
        val ta = findApplicableAction(starterShape, input)
        if (ta != null) {
          // 실제 span 시작: key==gen (lookahead 구 규약) 이면 gen-1 — 보고 anchor 기록.
          val reportGen = if (starterRoot.startGen == gen) gen - 1 else starterRoot.startGen
          if (reportGen != starterRoot.startGen) ctx.rootReportGens[starterRoot] = reportGen
          val perStarterNext = mutableMapOf<PathShape, AcceptCondition>()
          val ignoredStarters = mutableMapOf<PathRoot, PendingStarter>()
          applyTermAction(
            oldShape = starterShape,
            oldCondition = Always,
            pathRoot = starterRoot,
            termAction = ta,
            midGen = ctx.gen,
            gen = gen,
            rootReportGen = reportGen,
            nextPathsOut = perStarterNext,
            appsOut = appsByGroup,
            finishesOut = finishesByGroup,
            addedOut = addedByGroup,
            rootProgressesOut = rootProgresses,
            observingSymbolIdsOut = observingOut,
            condRootStartersOut = ignoredStarters,
          )
          if (perStarterNext.isNotEmpty()) {
            nextPaths.getOrPut(starterRoot) { mutableMapOf() }.also { acc ->
              perStarterNext.forEach { (s, c) -> acc.addPath(s, c) }
            }
          } else {
            starterDied(starterRoot, starterShape, nextPaths)
          }
        } else {
          starterDied(starterRoot, starterShape, nextPaths)
        }
      }
      if (rootInfo.selfFinishAcceptCondition != null) {
        val cond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(starterRoot.startGen, starterRoot.startGen, gen)
        val existing = rootProgresses[starterRoot]
        rootProgresses[starterRoot] = if (existing != null) Or.from(existing, cond) else cond
      }
    }

    tPhase = phaseMark(1, tPhase)
    // step 2 (이전) 는 step 1+2 통합으로 사라짐. phase timer 2 는 0.
    tPhase = phaseMark(2, tPhase)

    // step 3: 새로 등장한 cond symbol 에 대해 cond path 시작.
    // observingOut 의 transitive closure 를 ParserDataPlain 의 precomputed table 로 union.
    val allObservingSyms = HashSet<Int>(observingOut.size * 2)
    for (sym in observingOut) {
      val closure = plain.transitiveInitialCondSymbols[sym]
      if (closure != null) allObservingSyms.addAll(closure) else allObservingSyms.add(sym)
    }

    val newCondRoots = HashSet<PathRoot>()
    // condition.referencedRoots metadata 사용 — tree traversal 불필요.
    nextPaths.values.forEach { pm -> pm.values.forEach { it.referencedRoots.forEach { r -> newCondRoots.add(r) } } }

    val newCondRootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    for (sym in allObservingSyms) {
      newCondRoots.add(PathRoot(sym, gen))
    }
    for ((root, _) in condRootStartersFromTerm) {
      newCondRoots.add(root)
    }

    // history 에 한 번이라도 등장한 적 있는 cond root 은 skip.
    // ctx.everSeenCondRoots 는 이전 step 까지 누적된 active roots — O(1) amortized 로 share.
    val everSeenCondRoots = ctx.everSeenCondRoots
    for (pathRoot in newCondRoots) {
      // mainRoot 는 cond root 가 아님 (이미 nextPaths 안에 있음).
      if (pathRoot == ctx.mainRoot) continue
      if (pathRoot in ctx.paths.keys || pathRoot in nextPaths.keys) continue
      if (pathRoot in everSeenCondRoots) continue
      val rootInfo = plain.pathRoots[pathRoot.symbolId] ?: continue
      if (rootInfo.selfFinishAcceptCondition != null) {
        val selfCond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(pathRoot.startGen, pathRoot.startGen, gen)
        newCondRootProgresses[pathRoot] = selfCond
      }
      val starterShape = PathShape(null, rootInfo.milestoneGroupId)
      // key(=span 시작) 기준 시동 — step 1b 와 동일한 규칙:
      //  - startGen == gen: fresh 시동만 (소비는 다음 step 부터).
      //  - startGen == ctx.gen: same-input — 이번 입력이 첫 글자. 실패 시 key 소진.
      //  - startGen < ctx.gen: 그 시점에 시동됐어야 하는 watcher — 지금 만들면 span 이
      //    어긋난 zombie 가 되므로 시동하지 않는다.
      // 시동 flavor:
      //  - startGen == gen: lookahead 심볼이면 구 규약 same-input (실제 span gen-1),
      //    그 외 (새 경계 watcher) 는 fresh 시동만.
      //  - startGen == ctx.gen: bounded span-정규화 same-input.
      //  - startGen < ctx.gen: 그 시점에 시동됐어야 하는 watcher — 지금 만들면 span 이
      //    어긋난 zombie 가 되므로 시동하지 않는다.
      val sameInput = when (pathRoot.startGen) {
        gen -> pathRoot.symbolId in plain.lookaheadCondSymbols
        ctx.gen -> true
        else -> continue
      }
      if (!sameInput) {
        nextPaths[pathRoot] = mutableMapOf(starterShape to Always)
      } else {
        val ta = findApplicableAction(starterShape, input)
        if (ta != null) {
          val reportGen = if (pathRoot.startGen == gen) gen - 1 else pathRoot.startGen
          if (reportGen != pathRoot.startGen) ctx.rootReportGens[pathRoot] = reportGen
          val starterNextPaths = mutableMapOf<PathShape, AcceptCondition>()
          val ignoredStarters = mutableMapOf<PathRoot, PendingStarter>()
          applyTermAction(
            oldShape = starterShape,
            oldCondition = Always,
            pathRoot = pathRoot,
            termAction = ta,
            midGen = ctx.gen,
            gen = gen,
            rootReportGen = reportGen,
            nextPathsOut = starterNextPaths,
            appsOut = appsByGroup,
            finishesOut = finishesByGroup,
            addedOut = addedByGroup,
            rootProgressesOut = newCondRootProgresses,
            observingSymbolIdsOut = observingOut,
            condRootStartersOut = ignoredStarters,
          )
          if (starterNextPaths.isNotEmpty()) {
            nextPaths[pathRoot] = starterNextPaths
          } else {
            starterDied(pathRoot, starterShape, nextPaths)
          }
        } else {
          starterDied(pathRoot, starterShape, nextPaths)
        }
      }
    }


    tPhase = phaseMark(3, tPhase)

    // step 4: condPath finish detection — eager (end = gen) 와 late (end = gen-1) 분리.
    val condPathFinishes = mutableMapOf<PathRoot, AcceptCondition>()
    for ((root, cond) in rootProgresses) {
      if (root != ctx.mainRoot) {
        condPathFinishes[root] = cond
      }
    }
    for ((root, cond) in newCondRootProgresses) {
      if (root != ctx.mainRoot) {
        val existing = condPathFinishes[root]
        condPathFinishes[root] = if (existing != null) Or.from(existing, cond) else cond
      }
    }
    val lateCondPathFinishes = mutableMapOf<PathRoot, AcceptCondition>()
    for ((root, cond) in latePfProgresses) {
      if (root != ctx.mainRoot) {
        lateCondPathFinishes[root] = cond
      }
    }

    tPhase = phaseMark(4, tPhase)

    // step 5: 모든 path 의 acceptCondition 을 evolve.
    // activeCondRoots = nextPaths.keys 그대로 — mainRoot 도 leaf condition 의 root 일 수 있으면
    // active 로 봐야 옳음 (main path 살아있는 한). filterTo(HashSet()) 새 set 할당 회피.
    val activeCondRoots: Set<PathRoot> = nextPaths.keys

    if (trace) {
      println("  --- step 5 evolve ---")
      println("  condPathFinishes (${condPathFinishes.size}):")
      for ((r, c) in condPathFinishes) {
        println("    $r => ${c.toString().take(300)}")
      }
      println("  activeCondRoots (${activeCondRoots.size}): ${activeCondRoots.take(20)}")
    }

    fun evolveMap(paths: Map<PathShape, AcceptCondition>, label: String = ""): Map<PathShape, AcceptCondition> {
      if (paths.isEmpty()) return paths
      val result = LinkedHashMap<PathShape, AcceptCondition>()
      for ((shape, cond) in paths) {
        if (trace && label == "main") {
          println("  evolve main tip=${shape.tipGroupId}:")
          println("    in : ${cond.toString().take(300)}")
          evolveTrace = true
        }
        val evolved = evolveAcceptCondition(cond, condPathFinishes, lateCondPathFinishes, activeCondRoots, gen)
        if (trace && label == "main") {
          evolveTrace = false
          println("    out: ${evolved.toString().take(300)}")
        }
        if (evolved == Never) continue
        val existing = result[shape]
        result[shape] = if (existing == null) evolved else Or.from(existing, evolved)
      }
      return result
    }

    val pathsEvolved = LinkedHashMap<PathRoot, Map<PathShape, AcceptCondition>>()
    for ((root, pm) in nextPaths) {
      val label = if (root == ctx.mainRoot) "main" else ""
      val evolved = evolveMap(pm, label)
      if (evolved.isNotEmpty()) pathsEvolved[root] = evolved
    }

    // mgroup4 (§2.1): step 5 evolve 뒤, main root 만, per-gen 재파티션 병합.
    // n=1 이면 no-op (병합 패스 미실행 — 제로코스트). n≥2 면 window 안 한 위치만
    // 상이한 형제 shape 들을 interior group 으로 접는다. A2: 병합된 group 은 여러
    // gen 을 살며, reduce 가 group 노드에 도달하거나 (applyEdgeAction 멤버 분열)
    // window-exit (A3) 시에만 분열한다 → 출력 불변 (G1/G2 오라클).
    if (interiorGroupMaxDepth >= 2) {
      val mainEvolved = pathsEvolved[ctx.mainRoot]
      if (mainEvolved != null && mainEvolved.size >= 2) {
        pathsEvolved[ctx.mainRoot] = mergeInteriorGroups(mainEvolved, interiorGroupMaxDepth, gen)
      }
    }

    val mainPathsEvolved = pathsEvolved[ctx.mainRoot] ?: emptyMap()
    if (trace) {
      println("  after evolve: mainPathsEvolved.size=${mainPathsEvolved.size}")
    }
    if (mg4ShapeStatsEnabled) recordShapeStats(pathsEvolved, ctx.mainRoot)

    tPhase = phaseMark(5, tPhase)

    // step 6: 사용되지 않는 cond path 제거 — mainRoot 는 항상 keep.
    // referencedRoots: 런타임 생존 규칙 — 조건 참조 root + observing 의 dot anchor
    //   (+ lookahead 는 tip/parent anchor 도 — 구 규약의 드리프트 쌍).
    // reportedCondRoots: 보고 대상 — mgroup2 의 trackings 와 같은 규칙
    //   (조건 참조 root + observing 의 parent-gen anchor 만; tip-gen anchor 제외).
    //   m2 는 이 규칙으로 매 step 끝에 root 경로를 필터하므로, 같은 입력에서
    //   m2 가 갖지 않는 cond root (예: 매 gen 재시작된 중복 root) 의 기록이
    //   kernels_history 에 나타나지 않게 한다.
    val referencedRoots = HashSet<PathRoot>()
    val reportedCondRoots = HashSet<PathRoot>()
    fun collectFromShape(shape: PathShape, cond: AcceptCondition) {
      // condition 의 referenced roots — cached metadata.
      cond.referencedRoots.forEach { referencedRoots.add(it); reportedCondRoots.add(it) }
      // milestone chain 의 observing 들. 이건 chain walk 필요 (cache 없음 — 이전 시도에서 회귀).
      var mp = shape.milestonePath
      while (mp != null) {
        for (sid in mp.observingCondSymbolIds) {
          // span-정규화 key anchor — 이 milestone 의 dot(= mp.gen - 1, 부착은 항상 dot+1)
          // 에서 시작한 watcher. 예: "def"&Word 의 Word watcher key = seq dot gen —
          // 조건이 emit 되기 전의 중간 step 들에서도 살아있어야 한다.
          referencedRoots.add(PathRoot(sid, mp.gen - 1))
          reportedCondRoots.add(PathRoot(sid, mp.gen - 1))
          val parentGen = mp.parent?.gen ?: ctx.mainRoot.startGen
          reportedCondRoots.add(PathRoot(sid, parentGen))
          // bounded (except/join/longest) 의 미래 조건 anchor 는 dot 뿐 — term 조건은
          // MID(같은 step 에 starter 로 시동), edge 조건은 GRAND(=dot) 로만 anchoring
          // (remapEdgeCondGens; 실측 scanCondAnchorTags: mulang 전 템플릿에서 bounded
          // 의 CURR anchor 0건). tip(mp.gen)/parent(parentGen) anchor 로만 살아남는
          // bounded 워처가 인접-gen 중복 root 의 원인 (watcher_anchor_dedup.md §1).
          // lookahead 는 edge 조건이 CURR/MID 태그를 유지하므로 (remap 대상 아님)
          // 구 규약의 3 anchor 그대로 — 드리프트 anchor 와 쌍인 자기일관 시스템 (§5).
          if (sid in plain.lookaheadCondSymbols) {
            referencedRoots.add(PathRoot(sid, mp.gen))
            referencedRoots.add(PathRoot(sid, parentGen))
          }
        }
        mp = mp.parent
      }
    }
    for ((_, pm) in pathsEvolved) {
      pm.forEach { (shape, cond) -> collectFromShape(shape, cond) }
    }

    val pathsFiltered = LinkedHashMap<PathRoot, Map<PathShape, AcceptCondition>>()
    for ((root, pm) in pathsEvolved) {
      if (root == ctx.mainRoot || root in referencedRoots) {
        pathsFiltered[root] = pm
      }
    }

    tPhase = phaseMark(6, tPhase)

    // step 7: 입력 종료 시점이 아닌데 main path 모두 사라진 경우 에러
    if (!isLastInput && mainPathsEvolved.isEmpty()) {
      throw ParsingError.UnexpectedInput(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx), input)
    }

    val activeCondPathsForHistory = pathsFiltered.keys.filterTo(HashSet()) { it != ctx.mainRoot }

    // record 는 저장 시점에 필터+dedup — 보고 대상이 아닌 cond root 의 record 와
    // (여러 path 가 같은 action 을 같은 바인딩으로 적용해 생기는) 완전 중복 record 를
    // 버린다. 대형 입력에서 history 의 record 누적이 OOM 을 유발하는 것 방지.
    val prevReported = ctx.history.lastOrNull()?.reportedCondRoots ?: emptySet()
    fun reportableRoot(r: PathRoot): Boolean =
      r == ctx.mainRoot || r in reportedCondRoots || r in prevReported

    val historyEntry = HistoryEntry(
      actionApplications = appsByGroup.filterTo(LinkedHashSet()) { reportableRoot(it.root) }.toList(),
      finishedKernels = finishesByGroup.filterTo(LinkedHashSet()) { reportableRoot(it.root) }.toList(),
      condPathFinishes = condPathFinishes.toMap(),
      lateCondPathFinishes = lateCondPathFinishes.toMap(),
      activeCondPaths = activeCondPathsForHistory,
      mainRootFinish = rootProgresses[ctx.mainRoot],
      addedKernels = addedByGroup.filterTo(LinkedHashSet()) { reportableRoot(it.root) }.toList(),
      reportedCondRoots = reportedCondRoots,
    )

    val nextHistory: ArrayList<HistoryEntry> = ctx.history as? ArrayList<HistoryEntry>
      ?: ArrayList(ctx.history)
    nextHistory.add(historyEntry)
    ctx.everSeenCondRoots.addAll(historyEntry.activeCondPaths)

    phaseMark(7, tPhase)

    return ParsingCtx(
      gen = gen,
      line = nextLine,
      col = nextCol,
      mainRoot = ctx.mainRoot,
      paths = pathsFiltered,
      history = nextHistory,
      everSeenCondRoots = ctx.everSeenCondRoots,
      rootReportGens = ctx.rootReportGens,
    )
  }

  // === mgroup4 interior milestone group — merge / split (Phase A) ===

  // 한 milestone chain 을 root..tip 순서 배열로 편다. index 0 = root-most, 마지막 = tip-most.
  private fun chainToList(tip: MilestonePath?): ArrayList<MilestonePath> {
    val rev = ArrayList<MilestonePath>()
    var cur = tip
    while (cur != null) { rev.add(cur); cur = cur.parent }
    rev.reverse()
    return rev
  }

  // A3 (§1.3 (b)): tip 에서부터 위로 순회하며 group 노드의 tip-relative depth 를 찾는다.
  // depth 1 = tipGroupId (여기 인자는 tip-most interior = depth 2 부터). 없으면 -1.
  // maxScan+1 칸만 순회 — group 이 window(n) 안이면 유계 (window 밖이면 이미 분열됐어야).
  private fun groupDepthNearTip(tip: MilestonePath?, maxScan: Int): Int {
    var cur = tip
    var depth = 2 // tip-most interior 노드
    var steps = 0
    while (cur != null && steps <= maxScan + 1) {
      if (cur.groupMembers != null) return depth
      cur = cur.parent
      depth++
      steps++
    }
    return -1
  }

  // 병합 파티션 버킷 — 캐시된 hashCode 로 버킷팅 후 실등가 검증 (상위 수정 지시 2).
  // A4: chain(ArrayList) 은 lazy — 버킷팅은 노드 캐시 해시(prefixHash + tip-side walk)로
  // 하고, 실제 verdict/fold 에 들어가는 후보(다중-멤버 버킷)만 chainToList 로 물질화한다.
  // tip/length 는 노드 캐시로 O(1) — 대부분 후보(단독 버킷)는 ArrayList 할당을 아예 안 한다.
  private class MergeCandidate(
    val shape: PathShape,
    val cond: AcceptCondition,
    val tip: MilestonePath?,
    val length: Int,
  ) {
    private var _chain: ArrayList<MilestonePath>? = null
    // root..tip 배열 (index 0 = root-most). 필요 시 한 번만 물질화.
    fun chain(): ArrayList<MilestonePath> {
      var c = _chain
      if (c == null) {
        c = ArrayList(length)
        var cur = tip
        while (cur != null) { c.add(cur); cur = cur.parent }
        c.reverse()
        _chain = c
      }
      return c
    }
  }

  // step 5 뒤 main root 병합 (§2.1). d=2..n greedy — Phase 0 merge_greedy 동형.
  // 병합 조건: 같은 length·tip·condition, window 밖 노드 전부 동일, window 안 정확히
  // depth-d 한 위치만 상이. 추가로 (상위 수정 지시 1) depth-d 노드의 gen·observing 이
  // 멤버 간 동일해야 함 (MilestonePath.gen 은 미래 condition anchor). 파티션은 캐시된
  // hashCode 버킷 후 비마스크 부분의 실 equals 통과분만 접는다 (상위 수정 지시 2).
  fun mergeInteriorGroups(
    mainPathMap: Map<PathShape, AcceptCondition>,
    n: Int,
    curGen: Int = -1,
  ): Map<PathShape, AcceptCondition> {
    val profiling = mg4MergeProfileEnabled
    val tMergeStart = if (profiling) System.nanoTime() else 0L
    // 아직 접히지 않은 후보들. 접힌(consumed) shape 는 제거된다.
    // A2 spec item 6: 체인에 이미 group 노드가 있는 shape 는 새 병합 후보에서 제외
    // (2중 group 은 이번 스코프 아님). 그런 shape 는 병합 없이 그대로 결과로 통과시킨다.
    val remaining = LinkedHashMap<PathShape, MergeCandidate>()
    val merged = LinkedHashMap<PathShape, AcceptCondition>()
    val tChain0 = if (profiling) System.nanoTime() else 0L
    for ((shape, cond) in mainPathMap) {
      if (shapeHasGroup(shape)) {
        if (mg4ShapeStatsEnabled) mg4SkipExistingGroup++
        val existing = merged[shape]
        merged[shape] = if (existing == null) cond else Or.from(existing, cond)
        continue
      }
      // A4: chainToList 를 여기서 안 만든다 — tip/length 는 노드 캐시로 O(1) amortized.
      // 실제 chain 배열은 다중-멤버 버킷에 들어간 후보만 lazy 물질화 (MergeCandidate.chain()).
      val tip = shape.milestonePath
      val len = tip?.chainDepthCached() ?: 0
      remaining[shape] = MergeCandidate(shape, cond, tip, len)
    }
    if (profiling) mergeNanos[0] += System.nanoTime() - tChain0

    var d = 2
    while (d <= n) {
      // depth d 노드가 존재하는(chain 크기 >= d-1) 후보를 **구조 키**로 버킷팅. 구조 키는
      // condition 과 diff 노드의 gen/observing 을 **제외** — 그래야 같은 버킷 안에서
      // "구조는 병합 가능하나 condition/gen·observing 불일치로 거부된" 쌍을 세어
      // H5 실현율 신호를 낼 수 있다 (상위 수정 지시 1·4). 실제 병합은 strict equals.
      //
      // A4 가속: 버킷 키 = combine(1, L, tip, idx) 뒤 "idx 제외 전 노드"의 node-local
      // rolling hash. 그 rolling hash 를 O(체인전체) 대신 prefix 캐시 + tip-side walk 로
      // 계산한다 (Rust interior_merge.rs pre/suf 패턴). prefix 성분 [0..idx-1] 은
      // 노드의 prefixHashCached() 로 O(1) (seed=1 정렬), suffix 성분 [idx+1..L-1] 은
      // d-2 칸만 순회 (window 유계). 키의 정확한 비트값은 무의미 — 실병합은 strict equals
      // (mergeVerdictAtDepth) 로 재검증하므로 충돌해도 정확성 불변, 분포만 좋으면 됨.
      val tBucket0 = if (profiling) System.nanoTime() else 0L
      val buckets = HashMap<Long, ArrayList<MergeCandidate>>()
      for (cand in remaining.values) {
        val L = cand.length
        if (d > L) continue // depth d 가 chain 밖 (window 위치 없음)
        val idx = L - (d - 1) // root..tip 배열에서 depth-d 노드의 index
        if (idx < 0) continue
        // diff 노드 = tip 에서 (d-2) 칸 위. 순회 중 tip-side(idx+1..L-1) 노드도 함께 모은다.
        // (suffix 는 d-2 노드 — window 유계.) diff 노드가 group 이면 후보 아님.
        var diffNode = cand.tip
        var stepsUp = d - 2
        // suffix rolling: tip-side 노드들을 root쪽→tip쪽 순서로 접어야 원본 fold 순서와
        // 정합. 위로 걷는 순서는 tip→root 이므로 임시 배열에 담아 역순으로 접는다 (d-2 작음).
        val suffixNodes = if (stepsUp > 0) ArrayList<MilestonePath>(stepsUp) else null
        while (stepsUp > 0 && diffNode != null) {
          suffixNodes!!.add(diffNode) // tip-side, tip→root 순
          diffNode = diffNode.parent
          stepsUp--
        }
        if (diffNode == null) continue // 방어 (idx 범위 보장상 도달 안 함)
        if (diffNode.groupMembers != null) continue // A1: group-of-group 없음
        val prefixNode = diffNode.parent // 노드 [0..idx-1] 의 마지막 (idx-1)
        var h = 1L
        h = 31 * h + L
        h = 31 * h + cand.shape.tipGroupId
        h = 31 * h + idx
        // prefix 성분 [0..idx-1]: prefixHashCached (seed=1 rolling) — O(1).
        val prefixRolling = prefixNode?.prefixHashCached() ?: 1
        h = 31 * h + prefixRolling
        // suffix 성분 [idx+1..L-1]: tip→root 로 모았으니 역순(root쪽→tip쪽)으로 접는다.
        if (suffixNodes != null) {
          for (si in suffixNodes.indices.reversed()) {
            h = 31 * h + suffixNodes[si].nodeLocalHashCached()
          }
        }
        buckets.getOrPut(h) { ArrayList() }.add(cand)
      }
      if (profiling) mergeNanos[1] += System.nanoTime() - tBucket0

      for (bucket in buckets.values) {
        if (bucket.size < 2) continue
        // 해시 버킷 후 실등가로 그룹핑 (해시 충돌 방어 — 상위 수정 지시 2).
        val used = BooleanArray(bucket.size)
        for (i in bucket.indices) {
          if (used[i]) continue
          used[i] = true
          // A4 micro-opt: group ArrayList 을 실제 첫 병합이 확정될 때까지 지연 할당.
          // 다중-멤버 버킷이라도 실병합이 안 나는 경우(reject 다수 — 카운터 참조)가
          // 흔해, 매 i 마다 size-1 리스트를 새로 만드는 낭비를 없앤다.
          var group: ArrayList<MergeCandidate>? = null
          val tVerdict0 = if (profiling) System.nanoTime() else 0L
          for (k in i + 1 until bucket.size) {
            if (used[k]) continue
            when (mergeVerdictAtDepth(bucket[i], bucket[k], d)) {
              MergeVerdict.MERGE -> {
                if (group == null) { group = ArrayList(); group.add(bucket[i]) }
                group.add(bucket[k]); used[k] = true
              }
              MergeVerdict.REJECT_COND -> if (mg4ShapeStatsEnabled) mg4RejectCondDiff++
              MergeVerdict.REJECT_GEN_OBS -> if (mg4ShapeStatsEnabled) mg4RejectGenObsDiff++
              MergeVerdict.REJECT_REPORT_COORD -> if (mg4ShapeStatsEnabled) mg4RejectReportCoordDiff++
              MergeVerdict.NOT_CANDIDATE -> {}
            }
          }
          if (profiling) mergeNanos[2] += System.nanoTime() - tVerdict0
          if (group != null && group.size >= 2) {
            val tFold0 = if (profiling) System.nanoTime() else 0L
            val (foldedShape, foldedCond) = foldGroup(group, d)
            if (profiling) mergeNanos[3] += System.nanoTime() - tFold0
            // consume: 접힌 멤버는 이후 depth 재병합 대상에서 제외.
            for (m in group) remaining.remove(m.shape)
            // 접힌 group shape 를 결과에 (dedup — 같은 group shape 가 이미 있으면 Or).
            val existing = merged[foldedShape]
            merged[foldedShape] = if (existing == null) foldedCond else Or.from(existing, foldedCond)
            if (mg4ShapeStatsEnabled) {
              if (d < mg4MergesAtDepth.size) mg4MergesAtDepth[d] += (group.size - 1).toLong()
              classifyMergeOrigin(group, d, curGen)
            }
          }
        }
      }
      d++
    }
    // 안 접힌 나머지는 그대로.
    for ((shape, cand) in remaining) {
      val existing = merged[shape]
      merged[shape] = if (existing == null) cand.cond else Or.from(existing, cand.cond)
    }
    if (profiling) mergeNanos[4] += System.nanoTime() - tMergeStart
    return merged
  }

  // A2 spec item 7 — 병합 기원 분류 (Phase B 설계 데이터). diff 노드의 런타임 anchor gen
  // (MilestonePath.gen) 이 현재 gen 이거나 직전 gen 이면 "creation-mergeable" (fork 가
  // 이번/직전 gen 에 일어나 parserdata 사전 그룹핑으로 잡을 수 있음), 더 과거면
  // "late-convergence" (tip 이 나중에 수렴해 만나 런타임 packing 필요). collapse 수
  // (group.size - 1) 기준으로 두 카운트를 분리 집계.
  //
  // gen 오프셋 정의 (구현 확인): diff 노드 = chain[L-(d-1)]. 그 노드의 MilestonePath.gen
  // 은 그 노드가 term rea 로 부착된 gen (=fork 가 일어난 step). 병합 패스는 gen=curGen
  // 의 evolve 직후 도므로, diffNode.gen == curGen 은 "이번 입력에서 방금 fork" (depth-2,
  // 즉 d==2 의 흔한 경우), diffNode.gen == curGen-1 은 "직전 입력에서 fork 후 이번에
  // 같은 tip 으로 수렴" (Phase 0 의 depth-2 지배 패턴). 둘 다 사전그룹핑 가능. 그보다
  // 과거는 late-convergence.
  private fun classifyMergeOrigin(group: List<MergeCandidate>, d: Int, curGen: Int) {
    if (curGen < 0) return
    val chain0 = group[0].chain()
    val L = chain0.size
    val idx = L - (d - 1)
    val diffGen = chain0[idx].gen
    val collapses = (group.size - 1).toLong()
    if (diffGen == curGen || diffGen == curGen - 1) {
      mg4CreationMergeable += collapses
    } else {
      mg4LateConvergence += collapses
    }
  }

  private enum class MergeVerdict { MERGE, REJECT_COND, REJECT_GEN_OBS, REJECT_REPORT_COORD, NOT_CANDIDATE }

  // 두 후보의 depth-d 병합 판정. 비마스크 부분(체인 다른 노드·tip·length)이 전부 같고
  // depth-d 노드가 서로 다른 singleton 이면 "병합 후보". 후보 중 condition 이 다르면
  // REJECT_COND, gen/observing 이 다르면 REJECT_GEN_OBS, window 노드의 보고 좌표가
  // 다르면 REJECT_REPORT_COORD (H5/A2 실현율 신호), 셋 다 같으면 MERGE. 구조가 안
  // 맞으면 NOT_CANDIDATE. (구조 == cond/gen/obs/보고좌표 제외한 실등가.)
  private fun mergeVerdictAtDepth(a: MergeCandidate, b: MergeCandidate, d: Int): MergeVerdict {
    if (a.shape.tipGroupId != b.shape.tipGroupId) return MergeVerdict.NOT_CANDIDATE
    if (a.length != b.length) return MergeVerdict.NOT_CANDIDATE
    val aChain = a.chain()
    val bChain = b.chain()
    val L = aChain.size
    val idx = L - (d - 1)
    if (idx < 0 || idx >= L) return MergeVerdict.NOT_CANDIDATE
    val an = aChain[idx]
    val bn = bChain[idx]
    // depth-d 노드: 둘 다 singleton 이어야 (A2 도 group-of-group 없음 — spec item 6).
    if (an.groupMembers != null || bn.groupMembers != null) return MergeVerdict.NOT_CANDIDATE
    // 정확히 이 위치만 상이 — milestone kernel 이 서로 달라야 실제 fork.
    if (an.milestone == bn.milestone) return MergeVerdict.NOT_CANDIDATE
    // 나머지 노드(root-side prefix + window 안 다른 위치 + tip-side)를 **node-local** 비교.
    // MilestonePath.equals 는 parent 재귀라 diff 노드 위쪽(tip-side) 노드는 재귀가 diff
    // 노드에 걸려 항상 불일치 → 반드시 node-local(gen/milestone/observing/group)만 비교.
    // group 노드가 다른 위치에 섞여 있으면 오병합 금지 (spec item 6, nodeLocalEquals 가
    // 한쪽만 group 인 위치를 이미 불일치 처리 — 같은 group 을 딴 위치에서 접는 것 방지).
    for (j in 0 until L) {
      if (j == idx) continue
      if (!nodeLocalEquals(aChain[j], bChain[j])) return MergeVerdict.NOT_CANDIDATE
    }
    // 여기까지 왔으면 구조 병합 후보 — 부착 상태 제약 순서대로 판정.
    // gen/observing 은 diff 위치 노드의 anchor (상위 수정 지시 1).
    if (an.gen != bn.gen || an.observingCondSymbolIds != bn.observingCondSymbolIds) {
      return MergeVerdict.REJECT_GEN_OBS
    }
    if (a.cond != b.cond) return MergeVerdict.REJECT_COND
    // A2 spec item 4: window 노드 (diff 위치 제외 전 노드) 의 보고 좌표
    // (reportGen/milestoneReportGen) 가 멤버 간 동일해야 한다 — 분열 재구성이 대표의
    // window 노드를 복사해 쓰므로 (foldGroup tip-side 재구성), 좌표가 갈리면 byte-exact
    // 복원이 깨진다. diff 노드(idx) 자체의 milestoneReportGen 은 group 이 멤버별 배열로
    // 보존하므로 제외; 그 외 위치만 검사.
    for (j in 0 until L) {
      if (j == idx) continue
      val aj = aChain[j]
      val bj = bChain[j]
      if (aj.reportGen != bj.reportGen || aj.milestoneReportGen != bj.milestoneReportGen) {
        return MergeVerdict.REJECT_REPORT_COORD
      }
    }
    // ★ diff 노드(idx) 의 reportGen (tip 부착 gen) 은 멤버별 배열로 보존하지 않고
    // group 노드의 대표값(node.reportGen)을 memberSingletonsForEdge 가 전 멤버에 쓴다.
    // 따라서 diff 노드의 reportGen 은 멤버 간 동일해야 정확 (milestoneReportGen 과 달리
    // reportGen 은 per-member 배열이 없음). 실측상 항상 동일(같은 gen 에 같은 tip 부착)
    // 이라 이 reject 는 발화하지 않지만, 재구성 정확성의 명시적 계약으로 검사한다.
    if (an.reportGen != bn.reportGen) return MergeVerdict.REJECT_REPORT_COORD
    return MergeVerdict.MERGE
  }

  // (A4) node-local hash 는 MilestonePath.nodeLocalHashCached() 로 이동 — 노드에 lazy
  // 캐시되어 재파티션마다 재계산을 피한다. prefixHashCached() 가 이 값을 누적해 버킷 키의
  // prefix 성분을 O(1) 로 준다. 두 해시의 계약(제외: reportGen 류)은 ParsingCtx.kt 참조.

  // MilestonePath 의 node-local(비재귀) 비교 — gen·milestone·observing·group 만
  // (parent 는 호출자가 위치별로 따로 비교). reportGen 류는 제외 (equals 계약과 동일).
  private fun nodeLocalEquals(a: MilestonePath, b: MilestonePath): Boolean {
    if (a.gen != b.gen) return false
    if (a.observingCondSymbolIds != b.observingCondSymbolIds) return false
    if (a.groupMembers == null) {
      if (b.groupMembers != null) return false
      return a.milestone == b.milestone
    } else {
      if (b.groupMembers == null) return false
      return a.groupMembers == b.groupMembers
    }
  }

  // 병합 그룹을 한 group shape 로 접는다. depth-d 노드를 group 노드로 (멤버 정렬 +
  // 멤버별 milestoneReportGen 병렬 보관), window 구간(depth d..tip)은 새 인스턴스,
  // 공유 prefix(depth > d)는 참조 재사용. condition 은 (동일하므로) 대표 것 그대로.
  private fun foldGroup(group: List<MergeCandidate>, d: Int): Pair<PathShape, AcceptCondition> {
    val rep = group[0]
    val repChain = rep.chain()
    val L = repChain.size
    val idx = L - (d - 1)
    // 멤버 kernel 정렬 (canonical: symbolId, pointer, gen). 멤버별 milestoneReportGen 병렬.
    // 원본 member shape 도 정렬 순서로 함께 보관 (분열 시 byte-exact 복원 — §3.3).
    val sorted = group.sortedWith(
      compareBy({ it.chain()[idx].milestone.symbolId }, { it.chain()[idx].milestone.pointer }, { it.chain()[idx].milestone.gen })
    )
    val members = ArrayList<Kernel>(sorted.size)
    val memberReportGens = IntArray(sorted.size)
    for (i in sorted.indices) {
      members.add(sorted[i].chain()[idx].milestone)
      memberReportGens[i] = sorted[i].chain()[idx].milestoneReportGen
    }
    val diffNode = repChain[idx]
    // 공유 prefix: depth > d (idx 아래) — 대표의 parent 참조 그대로.
    val prefix: MilestonePath? = if (idx == 0) null else repChain[idx - 1]
    // group 노드 (depth d): 대표 milestone = 정렬 첫 멤버. window 구간(depth d-1..1,
    // idx+1..L-1)은 대표에서 새 인스턴스로 재구성. reportGen 은 group 필드에서 복원되므로
    // 파스 상태(gen/milestone/observing/parent)는 정확 — 보고 좌표는 분열 시 원본에서.
    var node = MilestonePath(
      gen = diffNode.gen,
      milestone = members[0],
      parent = prefix,
      observingCondSymbolIds = diffNode.observingCondSymbolIds,
      reportGen = diffNode.reportGen,
      milestoneReportGen = memberReportGens[0],
      groupMembers = members,
      groupMemberReportGens = memberReportGens,
    )
    // tip-side window 노드 (depth d-1..1) 는 대표에서 새 인스턴스로 재구성 (parent 만 교체).
    // ★ 이게 byte-exact 이려면 이 window 노드들의 보고 좌표가 멤버 간 동일해야 한다 —
    // mergeVerdictAtDepth 가 REJECT_REPORT_COORD 로 강제하므로 대표를 써도 안전.
    for (j in idx + 1 until L) {
      val orig = repChain[j]
      node = orig.copy(parent = node)
    }
    return Pair(PathShape(node, rep.shape.tipGroupId), rep.cond)
  }

  private fun shapeHasGroup(shape: PathShape): Boolean {
    var mp = shape.milestonePath
    while (mp != null) {
      if (mp.groupMembers != null) return true
      mp = mp.parent
    }
    return false
  }

  // A2 멤버 복원 기계 (spec item 3) — group 노드를 멤버별 singleton MilestonePath 로 편다.
  // singleton 이면 자기 자신 1개 (제로코스트). group 이면 각 멤버에 대해:
  //   milestone = groupMembers[i], milestoneReportGen = groupMemberReportGens[i],
  //   gen/observing/parent/reportGen 은 group 노드 것 (멤버 공통 — fold 시 검증됨),
  //   groupMembers = null (singleton 화).
  // reduce 진입점(tipEdge/midEdge)에서 이 노드 하나만 필요 (tip-side 노드는 reduce 가
  // 이미 pop 했으므로) — 그래서 전체 shape 재구성이 아니라 노드만 편다.
  private fun memberSingletonsForEdge(node: MilestonePath): List<MilestonePath> {
    val members = node.groupMembers ?: return listOf(node)
    val reportGens = node.groupMemberReportGens
    val out = ArrayList<MilestonePath>(members.size)
    for (i in members.indices) {
      out.add(
        MilestonePath(
          gen = node.gen,
          milestone = members[i],
          parent = node.parent,
          observingCondSymbolIds = node.observingCondSymbolIds,
          reportGen = node.reportGen,
          milestoneReportGen = reportGens?.get(i) ?: node.milestoneReportGen,
          groupMembers = null,
          groupMemberReportGens = null,
        )
      )
    }
    return out
  }

  // A3 window-exit 완전 분열 (spec item 3, 전체 shape 재구성) — 한 group shape 를 멤버별
  // singleton shape 로 편다. group 노드 자리를 멤버 singleton 으로 바꾸고, 그 위(tip-side)
  // 노드들은 parent 만 새 멤버 노드로 바꿔 복사. group 노드 아래(prefix)는 공유 참조.
  // ★ tip-side 노드 복사가 byte-exact 이려면 그 노드들의 보고 좌표가 멤버 공통이어야
  // 하는데, tip-side 노드는 group 형성 이후 term descend 로 folded shape 위에 한 번만
  // 쌓인 공유 노드들이라 정의상 멤버 공통 (같은 인스턴스) → 안전.
  fun explodeShapeFully(shape: PathShape): List<PathShape> {
    val chain = chainToList(shape.milestonePath)
    var gIdx = -1
    for (j in chain.indices) if (chain[j].groupMembers != null) { gIdx = j; break }
    if (gIdx < 0) return listOf(shape)
    val groupNode = chain[gIdx]
    val memberNodes = memberSingletonsForEdge(groupNode) // 각 멤버 singleton (parent=prefix)
    val out = ArrayList<PathShape>(memberNodes.size)
    for (mnode in memberNodes) {
      // tip-side (gIdx+1..end) 를 대표 chain 에서 복사, parent 만 멤버 노드로 교체.
      var node = mnode
      for (j in gIdx + 1 until chain.size) {
        node = chain[j].copy(parent = node)
      }
      out.add(PathShape(node, shape.tipGroupId))
    }
    return out
  }

  // === mgroup4 측정 카운터 (§5.1) — 파스 출력 무영향, opt-in ===
  var mg4MergedShapeSum: Long = 0L    // 병합 후 main shape 수 누적
  var mg4BaseShapeSum: Long = 0L      // 병합 안 했을 때 (멤버 총수) 누적
  var mg4Gens: Long = 0L
  // 병합 거부 사유별 카운트 (H5 실현율 신호 — 상위 수정 지시 1).
  var mg4RejectCondDiff: Long = 0L    // condition 불일치로 병합 거부된 후보 쌍
  var mg4RejectGenObsDiff: Long = 0L  // gen/observing 불일치로 병합 거부된 후보 쌍
  // depth 별 실제 병합 collapse 수 (index = depth, 0/1 미사용). A1 은 group 이 1 gen 만
  // 살아 대부분 d2 (A2 에서 d≥3 이 늘어난다 — 핸드오프 지표).
  val mg4MergesAtDepth: LongArray = LongArray(16)
  // 병합 후보의 window 노드 보고 좌표가 멤버 간 달라 거부된 쌍 (A2 spec item 4).
  var mg4RejectReportCoordDiff: Long = 0
  // 이미 group 노드가 있어 새 병합 후보에서 제외된 shape 수 (A2 spec item 6).
  var mg4SkipExistingGroup: Long = 0
  // 병합 기원 분류 (A2 spec item 7, Phase B 데이터). collapse 수 기준.
  var mg4CreationMergeable: Long = 0   // diff 노드 gen 이 anchor gen (현재/직전) — parserdata 사전그룹핑 가능
  var mg4LateConvergence: Long = 0     // diff 노드 gen 이 더 과거 — 런타임 packing 필요
  // A2 reduce 분열 / A3 window-exit 분열 카운터 (진단).
  var mg4ReduceSplits: Long = 0
  var mg4WindowExitSplits: Long = 0
  fun resetMg4Stats() {
    mg4MergedShapeSum = 0; mg4BaseShapeSum = 0; mg4Gens = 0
    mg4RejectCondDiff = 0; mg4RejectGenObsDiff = 0
    mg4RejectReportCoordDiff = 0; mg4SkipExistingGroup = 0
    mg4CreationMergeable = 0; mg4LateConvergence = 0
    mg4ReduceSplits = 0; mg4WindowExitSplits = 0
    for (i in mg4MergesAtDepth.indices) mg4MergesAtDepth[i] = 0
  }
  fun reportMg4Stats(): String {
    val meanMerged = if (mg4Gens > 0) mg4MergedShapeSum.toDouble() / mg4Gens else 0.0
    val meanBase = if (mg4Gens > 0) mg4BaseShapeSum.toDouble() / mg4Gens else 0.0
    val ratio = if (meanMerged > 0) meanBase / meanMerged else 1.0
    val byDepth = mg4MergesAtDepth.withIndex().filter { it.value > 0 }.joinToString(",") { "d${it.index}=${it.value}" }
    return "mg4 n=$interiorGroupMaxDepth: gens=$mg4Gens meanBase=%.2f meanMerged=%.2f ratio=%.3f rejectCondDiff=$mg4RejectCondDiff rejectGenObsDiff=$mg4RejectGenObsDiff rejectReportCoordDiff=$mg4RejectReportCoordDiff skipExistingGroup=$mg4SkipExistingGroup creationMergeable=$mg4CreationMergeable lateConvergence=$mg4LateConvergence reduceSplits=$mg4ReduceSplits windowExitSplits=$mg4WindowExitSplits mergesByDepth=[$byDepth]"
      .format(meanBase, meanMerged, ratio)
  }

  // 병합 후 main shape 수와 가상 base(멤버 총수)를 누적. group 노드의 members.size 합이 base.
  private fun recordShapeStats(pathsEvolved: Map<PathRoot, Map<PathShape, AcceptCondition>>, mainRoot: PathRoot) {
    val mainMap = pathsEvolved[mainRoot] ?: return
    var merged = 0
    var base = 0
    for (shape in mainMap.keys) {
      merged++
      // 이 shape 의 base 기여 = group 노드가 있으면 그 멤버 수, 없으면 1.
      var contrib = 1
      var mp = shape.milestonePath
      while (mp != null) {
        val g = mp.groupMembers
        if (g != null) contrib *= g.size
        mp = mp.parent
      }
      base += contrib
    }
    mg4MergedShapeSum += merged
    mg4BaseShapeSum += base
    mg4Gens++
  }

  fun parse(text: String): ParsingCtx {
    var ctx = initCtx()
    for ((idx, t) in text.withIndex()) {
      ctx = parseStep(ctx, t, text.length == idx + 1)
    }
    return ctx
  }

  fun isAccepted(ctx: ParsingCtx): Boolean {
    // accept 판정은 main root 의 progress 조건 전용 채널(mainRootFinish)만 사용.
    // finishedKernels 는 보고(kernelsHistory) 전용 — 보고 좌표가 바뀌어도
    // (예: edge 템플릿의 start symbol finish 가 begin=0 으로 보고되어도)
    // accept 판정에 영향을 주지 않는다.
    val lastEntry = ctx.history.lastOrNull() ?: return false
    val cond = lastEntry.mainRootFinish ?: return false
    val evaluator = RecordConditionEvaluator(ctx.history, endOfInputLateFins(ctx))
    return evaluator.evaluate(cond, ctx.history.size - 1)
  }

  // 입력 끝에서 아직 살아있는 cond path 들의 zero-width possible-finish 들 —
  // 입력이 끝나서 "죽음" 이 더는 step 으로 관찰되지 않으므로, 마지막 gen 에 끝나는
  // finish 들을 여기서 한 번 쓸어 모아 최종 평가의 가상 late step 으로 사용한다.
  // (end = ctx.gen — bounded 조건의 endGen == 마지막 gen 인 경우의 discharge 용.)
  private fun endOfInputLateFins(ctx: ParsingCtx): Map<PathRoot, AcceptCondition> {
    val result = mutableMapOf<PathRoot, AcceptCondition>()
    for ((root, pathMap) in ctx.paths) {
      if (root == ctx.mainRoot) continue
      for ((shape, cond) in pathMap) {
        val mg = plain.milestoneGroups[shape.tipGroupId] ?: continue
        for (pf in mg.possibleFinishes) {
          if (pf.symbolId == root.symbolId) {
            val prevGen = shape.milestonePath?.gen ?: root.startGen
            val pfCond = pf.acceptCondition.toAcceptCondition(prevGen, ctx.gen, ctx.gen + 1)
            val combined = And.from(cond, pfCond)
            if (combined != Never) {
              val existing = result[root]
              result[root] = if (existing != null) Or.from(existing, combined) else combined
            }
          }
        }
      }
    }
    return result
  }

  fun parseOrThrow(text: String): ParsingCtx {
    val ctx = parse(text)
    if (!isAccepted(ctx)) {
      throw ParsingError.UnexpectedEof(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx))
    }
    return ctx
  }

  fun kernelsHistory(ctx: ParsingCtx): List<KernelSet> {
    // record 조건 평가는 replay 재생 대신 leaf-직접 조회 + 메모 (RecordConditionEvaluator).
    // evaluator 의 인덱스/메모는 이 호출 로컬 — 파서 인스턴스는 상태를 갖지 않는다.
    val evaluator = RecordConditionEvaluator(ctx.history, endOfInputLateFins(ctx))
    return ctx.history.mapIndexed { gen, entry ->
      // root 기반 보고 필터(main root + 이번/직전 entry 의 reportedCondRoots)는
      // parseStep 의 record/application 저장 시점에 이미 적용됨.
      val kernels = mutableSetOf<com.giyeok.jparser.ktlib.Kernel>()
      // 액션 적용을 lazy 해석: 조건은 런타임 바인딩(rt*)으로 resolve 해 replay 평가,
      // kernel 좌표는 보고 바인딩(rep*)으로 resolve. (mgroup2 kernelsHistory 의
      // tasksSummary 해석 대응.)
      for (app in entry.actionApplications) {
        // edge action 은 그 적용을 구동한 runtime 조건으로 전체 게이팅 (m2 kernelsHistory 의
        // progressedKgroups/progressedKernels 조건 게이트 대응).
        if (app.condition != Always && !evaluator.evaluate(app.condition, gen)) continue
        val pa = app.actions
        for (finished in pa.finished) {
          val cond = finished.finishCondition.toAcceptCondition(app.rtCurr, app.rtMid, app.next, app.rtGrand)
          if (evaluator.evaluate(cond, gen)) {
            val begin = resolveGen(finished.startGen, app.repCurr, app.repMid, app.next, app.repGrand)
            kernels.add(
              com.giyeok.jparser.ktlib.Kernel(finished.symbolId, finished.pointer, begin, gen)
            )
          }
        }
        // pa.progressed 는 방출하지 않는다 — progress 의 source/next kernel 은
        // added 채널이 동일 좌표로 (조건과 함께) 커버한다. progressed 는 조건이 없어
        // 무조건 방출하면 m2 가 조건으로 거르는 kernel (예: longest 실패한 cond root
        // 의 진행) 이 새어 나온다.
        for (added in pa.added) {
          val cond = added.acceptCondition.toAcceptCondition(app.rtCurr, app.rtMid, app.next, app.rtGrand)
          if (evaluator.evaluate(cond, gen)) {
            kernels.add(
              com.giyeok.jparser.ktlib.Kernel(
                added.symbolId,
                added.pointer,
                resolveGen(added.startGen, app.repCurr, app.repMid, app.next, app.repGrand),
                resolveGen(added.endGen, app.repCurr, app.repMid, app.next, app.repGrand),
              )
            )
          }
        }
      }
      for (rec in entry.finishedKernels) {
        if (evaluator.evaluate(rec.condition, gen)) {
          kernels.add(
            com.giyeok.jparser.ktlib.Kernel(
              rec.kernel.symbolId,
              rec.kernel.pointer,
              rec.kernel.gen,
              gen,
            )
          )
        }
      }
      for (rec in entry.addedKernels) {
        if (evaluator.evaluate(rec.condition, gen)) {
          kernels.add(
            com.giyeok.jparser.ktlib.Kernel(
              rec.symbolId,
              rec.pointer,
              rec.beginGen,
              rec.endGen,
            )
          )
        }
      }
      KernelSet(kernels.toSet())
    }
  }

  // bounded shape (except/join) 의 span end 와 longest 의 본문 end 는 템플릿의
  // end 태그가 정적으로 지정한다 (조건 방출 시점의 시뮬레이션 위치 — curr-phase 의
  // zero-width/pop 체인 진행은 MID, post-char 진행은 NEXT 등). 런타임 바인딩으로
  // resolve 하면 정확한 span 의 finish 만 discharge 에 쓰인다.
  private fun AcceptConditionTemplate.toAcceptCondition(prevGen: Int, midGen: Int, gen: Int, grandGen: Int = prevGen): AcceptCondition =
    when (conditionCase) {
      AcceptConditionTemplate.ConditionCase.ALWAYS -> Always
      AcceptConditionTemplate.ConditionCase.AND ->
        And.from(this.and.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) })

      AcceptConditionTemplate.ConditionCase.OR ->
        Or.from(this.or.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) })

      AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH -> {
        val startGen = resolveGen(noLongerMatch.startGen, prevGen, midGen, gen, grandGen)
        val bodyEndGen = resolveGen(noLongerMatch.bodyEndGen, prevGen, midGen, gen, grandGen)
        NoLongerMatch(noLongerMatch.symbolId, startGen, minEndGen = bodyEndGen + 1)
      }

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_FOUND -> {
        val startGen = resolveGen(lookaheadFound.startGen, prevGen, midGen, gen, grandGen)
        Exists(lookaheadFound.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOTFOUND -> {
        val startGen = resolveGen(lookaheadNotfound.startGen, prevGen, midGen, gen, grandGen)
        NotExists(lookaheadNotfound.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.EXCEPT -> {
        val startGen = resolveGen(except.startGen, prevGen, midGen, gen, grandGen)
        val endGen = resolveGen(except.endGen, prevGen, midGen, gen, grandGen)
        Unless(except.symbolId, startGen, endGen)
      }

      AcceptConditionTemplate.ConditionCase.JOIN -> {
        val startGen = resolveGen(join.startGen, prevGen, midGen, gen, grandGen)
        val endGen = resolveGen(join.endGen, prevGen, midGen, gen, grandGen)
        OnlyIf(join.symbolId, startGen, endGen)
      }

      AcceptConditionTemplate.ConditionCase.CONDITION_NOT_SET ->
        throw IllegalArgumentException("Condition not set")

      null -> throw IllegalArgumentException("Condition is null")
    }
}

sealed class ParsingError: Exception() {
  data class UnexpectedInput(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: TermSet,
    val actual: Char,
  ): ParsingError()

  data class UnexpectedEof(
    val loc: Int,
    val locLine: Int,
    val locCol: Int,
    val expected: TermSet,
  ): ParsingError()
}
