package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.*
import java.util.*

class Mgroup3Parser(val data: Mgroup3ParserData) {
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

  fun setVerbose(): Mgroup3Parser {
    verbose = true
    return this
  }

  fun setTrace(gen: Int): Mgroup3Parser {
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
    condRootStartersOut: MutableMap<PathRoot, Int>,
  ) {
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
        condRootStartersOut[PathRoot(starter.symbolId, gen)] = starter.milestoneGroupId
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
        val tipEdgeAction = tipEdgeActionsMap[
          Pair(parentPath.milestone.kernelTemplate, rap.replaceMilestoneGroupId)
        ]
        if (tipEdgeAction != null) {
          val grandParentGen = parentPath.parent?.gen ?: pathRoot.startGen
          applyEdgeAction(
            parentPath = parentPath,
            edgeAction = tipEdgeAction,
            pathRoot = pathRoot,
            prevCondition = combined,
            grandParentGen = grandParentGen,
            parentGen = parentPath.gen,
            gen = gen,
            // m2 tip edge = (parent milestone @ milestoneReportGen) -> (tip group @ reportGen)
            reportCurrGen = parentPath.milestoneReportGen,
            reportMidGen = parentPath.reportGen,
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
    condRootStartersOut: MutableMap<PathRoot, Int>,
  ) {
    val grandGrandParentGen = parentPath.milestone.gen
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
        condRootStartersOut[PathRoot(starter.symbolId, gen)] = starter.milestoneGroupId
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
          val midEdge = midEdgeActionsMap[
            Pair(grandParent.milestone.kernelTemplate, parentPath.milestone.kernelTemplate)
          ]
          if (midEdge != null) {
            val grandGrandParentGen2 = grandParent.parent?.gen ?: pathRoot.startGen
            applyEdgeAction(
              parentPath = grandParent,
              edgeAction = midEdge,
              pathRoot = pathRoot,
              prevCondition = combined,
              grandParentGen = grandGrandParentGen2,
              parentGen = grandParent.gen,
              gen = gen,
              // m2 mid edge = (grandParent milestone @ m2 gen) -> (parent milestone @ m2 gen)
              reportCurrGen = grandParent.milestoneReportGen,
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
    val condRootStartersFromTerm = mutableMapOf<PathRoot, Int>()

    val trace = traceOn(gen)
    if (trace) {
      println("=== TRACE parseStep gen=$gen input='${if (input == '\n') "\\n" else input.toString()}' ===")
      println("  ctx.paths.size=${ctx.paths.size} (main + ${ctx.paths.size - 1} cond roots)")
    }

    var tPhase = if (phaseTimingEnabled) System.nanoTime() else 0L

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

    // fresh / same-input 시동 판별 — 조건 leaf 의 종류와 anchoring 으로 결정:
    //  - NEXT 경계(gen)에 anchoring 된 Exists/NotExists 의 root: span 이 새 boundary
    //    에서 시작 → 첫 글자는 다음 step 입력. 이번 입력(position gen-1)을 먹이면
    //    가짜 finish 가 생긴다 (maximal-munch: 'abc' 의 watcher 가 'c' 를 보고 즉시
    //    매치 → NotExists 오판). fresh 로 시동만 한다.
    //  - Unless/OnlyIf/NoLongerMatch 류가 참조하는 root (-1 anchoring): span 이
    //    gen-1 에서 시작 → same-input 이 맞다. 같은 root 를 두 종류가 동시에
    //    참조하면 same-input 우선.
    val freshLookaheadRoots = HashSet<PathRoot>()
    val sameInputWantedRoots = HashSet<PathRoot>()
    fun classifyStarterKinds(c: AcceptCondition) {
      when (c) {
        is And -> c.forEach { classifyStarterKinds(it) }
        is Or -> c.forEach { classifyStarterKinds(it) }
        is Exists -> if (c.startGen == gen) freshLookaheadRoots.add(PathRoot(c.symbolId, c.startGen))
        is NotExists -> if (c.startGen == gen) freshLookaheadRoots.add(PathRoot(c.symbolId, c.startGen))
        is Unless -> sameInputWantedRoots.add(PathRoot(c.symbolId, c.startGen))
        is OnlyIf -> sameInputWantedRoots.add(PathRoot(c.symbolId, c.startGen))
        is NoLongerMatch -> sameInputWantedRoots.add(PathRoot(c.symbolId, c.startGen))
        is NeedLongerMatch -> sameInputWantedRoots.add(PathRoot(c.symbolId, c.startGen))
        else -> {}
      }
    }
    nextPaths.values.forEach { pm -> pm.values.forEach { classifyStarterKinds(it) } }
    rootProgresses.values.forEach { classifyStarterKinds(it) }
    freshLookaheadRoots.removeAll(sameInputWantedRoots)

    // step 1b: main path 가 새 milestone 추가 시 같이 등록된 cond root starter 들에 같은 input 적용.
    // (fresh lookahead root 는 시동만 — step 3 의 fresh seeding 이 처리.)
    for ((starterRoot, mgroupId) in condRootStartersFromTerm) {
      if (starterRoot in ctx.paths.keys) continue
      if (starterRoot in nextPaths.keys) continue
      if (starterRoot in ctx.everSeenCondRoots) continue
      if (starterRoot in freshLookaheadRoots) continue
      val rootInfo = plain.pathRoots[starterRoot.symbolId] ?: continue
      val starterShape = PathShape(null, mgroupId)
      val ta = findApplicableAction(starterShape, input)
      if (ta != null) {
        // same-input 적용 — 이 root 의 실제 span 은 (생성 gen - 1) 부터. 보고 anchor 기록.
        ctx.rootReportGens[starterRoot] = gen - 1
        val perStarterNext = mutableMapOf<PathShape, AcceptCondition>()
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = starterRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          rootReportGen = gen - 1,
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
      // fresh lookahead root: 이번 입력을 먹이지 않고 시동만 — 소비는 다음 step 부터.
      val ta = if (pathRoot in freshLookaheadRoots) null else findApplicableAction(starterShape, input)
      val starterNextPaths = mutableMapOf<PathShape, AcceptCondition>()
      if (ta != null) {
        // same-input 적용 — span 은 (생성 gen - 1) 부터 (이번 gen 의 시작 root 에 한함).
        val starterReportGen = if (pathRoot.startGen == gen) gen - 1 else pathRoot.startGen
        ctx.rootReportGens[pathRoot] = starterReportGen
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = pathRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          rootReportGen = starterReportGen,
          nextPathsOut = starterNextPaths,
          appsOut = appsByGroup,
          finishesOut = finishesByGroup,
          addedOut = addedByGroup,
          rootProgressesOut = newCondRootProgresses,
          observingSymbolIdsOut = observingOut,
          condRootStartersOut = ignoredStarters,
        )
      }
      if (starterNextPaths.isNotEmpty()) {
        nextPaths[pathRoot] = starterNextPaths
      } else if (pathRoot.startGen == gen) {
        nextPaths[pathRoot] = mutableMapOf(starterShape to Always)
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

    val mainPathsEvolved = pathsEvolved[ctx.mainRoot] ?: emptyMap()
    if (trace) {
      println("  after evolve: mainPathsEvolved.size=${mainPathsEvolved.size}")
    }

    tPhase = phaseMark(5, tPhase)

    // step 6: 사용되지 않는 cond path 제거 — mainRoot 는 항상 keep.
    // referencedRoots: 런타임 생존 규칙 (tip-gen anchor 포함 — 기존 동작 유지).
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
          referencedRoots.add(PathRoot(sid, mp.gen))
          val parentGen = mp.parent?.gen ?: ctx.mainRoot.startGen
          referencedRoots.add(PathRoot(sid, parentGen))
          reportedCondRoots.add(PathRoot(sid, parentGen))
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
    return evaluateRecordCondition(cond, ctx.history, ctx.history.size - 1, endOfInputLateFins(ctx))
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

  // record 가 생성된 시점(recordGen)부터 매 step 의 evolve 를 재생한 뒤 최종 평가.
  // 파스 중 live path 의 조건이 겪는 것과 동일한 단계별 진화이므로, longest(NoLongerMatch
  // fromNextGen — 다음 step 에서 resolve)나 join/except(Unless/OnlyIf — 생성 step 의
  // condPathFinishes 로 resolve = 정확한 span) 의 타이밍 의미가 보존된다.
  // mgroup2 kernelsHistory 의 타이밍 인지 조건 평가에 대응.
  private fun evaluateRecordCondition(
    cond: AcceptCondition,
    history: List<HistoryEntry>,
    recordGen: Int,
    endLateFins: Map<PathRoot, AcceptCondition> = emptyMap(),
  ): Boolean {
    var c = cond
    for (g in recordGen until history.size) {
      if (c == Always) return true
      if (c == Never) return false
      val entry = history[g]
      c = evolveAcceptCondition(c, entry.condPathFinishes, entry.lateCondPathFinishes, entry.activeCondPaths, g)
    }
    if (c == Always) return true
    if (c == Never) return false
    // 가상 late step: 입력 끝에서 살아있던 root 들의 마지막-gen zero-width finish 들.
    if (endLateFins.isNotEmpty()) {
      c = evolveAcceptCondition(c, emptyMap(), endLateFins, emptySet(), history.size)
    }
    return evaluateAtEndOfInput(c)
  }

  // replay 를 마지막 entry 까지 마친 뒤 남은 residual 조건의 입력-끝 평가.
  // residual leaf 는 "마지막 step 까지 해당 finish 가 없었고 root 가 아직 미완"을 뜻한다:
  // 더 들어올 입력이 없으므로 NoLongerMatch/NotExists/Unless 는 true,
  // NeedLongerMatch/Exists/OnlyIf 는 false 로 확정된다.
  // (마지막 step 의 finish 는 evolve 가 이미 소비했으므로 여기서 다시 보면 안 된다 —
  //  특히 NoLongerMatch 는 같은 step 의 finish 가 "더 긴 매치"가 아니다.)
  private fun evaluateAtEndOfInput(c: AcceptCondition): Boolean = when (c) {
    Always -> true
    Never -> false
    is And -> {
      var result = true
      c.forEach { if (!evaluateAtEndOfInput(it)) result = false }
      result
    }
    is Or -> {
      var result = false
      c.forEach { if (evaluateAtEndOfInput(it)) result = true }
      result
    }
    is NoLongerMatch -> true
    is NeedLongerMatch -> false
    is NotExists -> true
    is Exists -> false
    is Unless -> true
    is OnlyIf -> false
  }

  fun parseOrThrow(text: String): ParsingCtx {
    val ctx = parse(text)
    if (!isAccepted(ctx)) {
      throw ParsingError.UnexpectedEof(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx))
    }
    return ctx
  }

  fun kernelsHistory(ctx: ParsingCtx): List<KernelSet> {
    val endLateFins = endOfInputLateFins(ctx)
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
        if (app.condition != Always && !evaluateRecordCondition(app.condition, ctx.history, gen, endLateFins)) continue
        val pa = app.actions
        for (finished in pa.finished) {
          val cond = finished.finishCondition.toAcceptCondition(app.rtCurr, app.rtMid, app.next, app.rtGrand)
          if (evaluateRecordCondition(cond, ctx.history, gen, endLateFins)) {
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
          if (evaluateRecordCondition(cond, ctx.history, gen, endLateFins)) {
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
        if (evaluateRecordCondition(rec.condition, ctx.history, gen, endLateFins)) {
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
        if (evaluateRecordCondition(rec.condition, ctx.history, gen, endLateFins)) {
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
