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

    val initialFinishedKernels = mutableListOf<FinishedKernelRecord>()
    val initialProgressedKernels = mutableListOf<ProgressedKernelRecord>()
    if (rootInfo.parsingActions != null) {
      collectInitialActions(rootInfo.parsingActions, 0, initialFinishedKernels, initialProgressedKernels)
    }
    if (rootInfo.selfFinishAcceptCondition != null) {
      initialFinishedKernels.add(
        FinishedKernelRecord(
          Kernel(startSymbolId, 1, 0),
          rootInfo.selfFinishAcceptCondition.toAcceptCondition(0, 0, 0),
        )
      )
    }

    val initialEntry = HistoryEntry(initialFinishedKernels, initialProgressedKernels)

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

  private fun collectInitialActions(
    pa: ParsingActionsPlain,
    gen: Int,
    finishedOut: MutableList<FinishedKernelRecord>,
    progressedOut: MutableList<ProgressedKernelRecord>,
  ) {
    for (finished in pa.finished) {
      val startGen = resolveGen(finished.startGen, 0, 0, 0)
      finishedOut.add(
        FinishedKernelRecord(
          Kernel(finished.symbolId, finished.pointer, startGen),
          finished.finishCondition.toAcceptCondition(0, 0, gen),
        )
      )
    }
    for (prog in pa.progressed) {
      val startGen = resolveGen(prog.startGen, 0, 0, 0)
      val midGen = resolveGen(prog.midGen, 0, 0, 0)
      progressedOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, midGen, gen))
    }
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
    nextPathsOut: MutableMap<PathShape, AcceptCondition>,
    finishesOut: MutableList<FinishedKernelRecord>,
    progressesOut: MutableList<ProgressedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
    condRootStartersOut: MutableMap<PathRoot, Int>,
  ) {
    val parentGen = oldShape.milestonePath?.gen ?: pathRoot.startGen
    val grandGen = oldShape.milestonePath?.milestone?.gen ?: pathRoot.startGen

    val pa = termAction.parsingActions
    if (pa != null) {
      for (finished in pa.finished) {
        val startGen = resolveGen(finished.startGen, parentGen, midGen, gen, grandGen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(parentGen, midGen, gen, grandGen),
          )
        )
      }
      for (prog in pa.progressed) {
        val startGen = resolveGen(prog.startGen, parentGen, midGen, gen, grandGen)
        val mGen = resolveGen(prog.midGen, parentGen, midGen, gen, grandGen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
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
            Kernel(pathRoot.symbolId, 1, pathRoot.startGen),
            combined,
          )
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
            nextPathsOut = nextPathsOut,
            finishesOut = finishesOut,
            progressesOut = progressesOut,
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
    nextPathsOut: MutableMap<PathShape, AcceptCondition>,
    finishesOut: MutableList<FinishedKernelRecord>,
    progressesOut: MutableList<ProgressedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
    condRootStartersOut: MutableMap<PathRoot, Int>,
  ) {
    val grandGrandParentGen = parentPath.milestone.gen
    val pa = edgeAction.parsingActions
    if (pa != null) {
      for (finished in pa.finished) {
        val startGen = resolveGen(finished.startGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen),
          )
        )
      }
      for (prog in pa.progressed) {
        val startGen = resolveGen(prog.startGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        val mGen = resolveGen(prog.midGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
    }

    for (append in edgeAction.appendMilestoneGroups) {
      val condition = append.acceptCondition.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen)
      val combined = And.from(prevCondition, condition)
      if (combined == Never) continue
      val newParentPath = parentPath.copy(
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
              Kernel(pathRoot.symbolId, 1, pathRoot.startGen),
              combined,
            )
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
              nextPathsOut = nextPathsOut,
              finishesOut = finishesOut,
              progressesOut = progressesOut,
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
    val finishesByGroup = mutableListOf<FinishedKernelRecord>()
    val progressesByGroup = mutableListOf<ProgressedKernelRecord>()
    val observingOut = mutableSetOf<Int>()
    val rootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
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
            nextPathsOut = perRootNext,
            finishesOut = finishesByGroup,
            progressesOut = progressesByGroup,
            rootProgressesOut = rootProgresses,
            observingSymbolIdsOut = observingOut,
            condRootStartersOut = condRootStartersFromTerm,
          )
        } else if (!isMain) {
          // cond path 가 input 매치 못해 dead — possible_finishes 검사.
          val mg = plain.milestoneGroups[shape.tipGroupId]
          if (mg != null) {
            for (pf in mg.possibleFinishes) {
              if (pf.symbolId == root.symbolId) {
                val prevGen = shape.milestonePath?.gen ?: root.startGen
                val midGenLocal = ctx.gen
                val pfCond = pf.acceptCondition.toAcceptCondition(prevGen, midGenLocal, gen)
                val combined = And.from(cond, pfCond)
                if (combined != Never) {
                  val existing = rootProgresses[root]
                  rootProgresses[root] = if (existing != null) Or.from(existing, combined) else combined
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

    // step 1b: main path 가 새 milestone 추가 시 같이 등록된 cond root starter 들에 같은 input 적용.
    for ((starterRoot, mgroupId) in condRootStartersFromTerm) {
      if (starterRoot in ctx.paths.keys) continue
      if (starterRoot in nextPaths.keys) continue
      if (starterRoot in ctx.everSeenCondRoots) continue
      val rootInfo = plain.pathRoots[starterRoot.symbolId] ?: continue
      val starterShape = PathShape(null, mgroupId)
      val ta = findApplicableAction(starterShape, input)
      if (ta != null) {
        val perStarterNext = mutableMapOf<PathShape, AcceptCondition>()
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = starterRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          nextPathsOut = perStarterNext,
          finishesOut = finishesByGroup,
          progressesOut = progressesByGroup,
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
    val newCondRootSyms = observingOut.toSet()
    val allObservingSyms = mutableSetOf<Int>()
    val queue: Queue<Int> = LinkedList()
    queue.addAll(newCondRootSyms)
    while (queue.isNotEmpty()) {
      val s = queue.poll()
      if (s !in allObservingSyms) {
        allObservingSyms.add(s)
        val info = plain.pathRoots[s]
        if (info != null) queue.addAll(info.initialCondSymbolIds)
      }
    }

    val newCondRoots = mutableSetOf<PathRoot>()
    fun extractRootsFromCond(cond: AcceptCondition) {
      when (cond) {
        Always, Never -> {}
        is And -> cond.forEach { extractRootsFromCond(it) }
        is Or -> cond.forEach { extractRootsFromCond(it) }
        is NoLongerMatch -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NeedLongerMatch -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NotExists -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Exists -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Unless -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is OnlyIf -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
      }
    }
    nextPaths.values.forEach { pm -> pm.values.forEach { extractRootsFromCond(it) } }

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
      val ta = findApplicableAction(starterShape, input)
      val starterNextPaths = mutableMapOf<PathShape, AcceptCondition>()
      if (ta != null) {
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = pathRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          nextPathsOut = starterNextPaths,
          finishesOut = finishesByGroup,
          progressesOut = progressesByGroup,
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

    // step 4: condPath finish detection
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

    tPhase = phaseMark(4, tPhase)

    // step 5: 모든 path 의 acceptCondition 을 evolve.
    // activeCondRoots: 살아있는 cond root 들 (mainRoot 제외).
    val activeCondRoots = nextPaths.keys.filterTo(HashSet()) { it != ctx.mainRoot }

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
        val evolved = evolveAcceptCondition(cond, condPathFinishes, activeCondRoots, gen)
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
    val referencedRoots = mutableSetOf<PathRoot>()
    fun collectReferenced(cond: AcceptCondition) {
      when (cond) {
        Always, Never -> {}
        is And -> cond.forEach { collectReferenced(it) }
        is Or -> cond.forEach { collectReferenced(it) }
        is NoLongerMatch -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NeedLongerMatch -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NotExists -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Exists -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Unless -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is OnlyIf -> referencedRoots.add(PathRoot(cond.symbolId, cond.startGen))
      }
    }
    fun collectFromShape(shape: PathShape, cond: AcceptCondition) {
      collectReferenced(cond)
      var mp = shape.milestonePath
      while (mp != null) {
        for (sid in mp.observingCondSymbolIds) {
          referencedRoots.add(PathRoot(sid, mp.gen))
          val parentGen = mp.parent?.gen ?: ctx.mainRoot.startGen
          referencedRoots.add(PathRoot(sid, parentGen))
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

    val historyEntry = HistoryEntry(
      finishedKernels = finishesByGroup,
      progressedKernels = progressesByGroup,
      condPathFinishes = condPathFinishes.toMap(),
      activeCondPaths = activeCondPathsForHistory,
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
    val startSymbolId = plain.startSymbolId
    val lastEntry = ctx.history.lastOrNull() ?: return false
    val activeCondPaths = ctx.condPaths.keys

    for (record in lastEntry.finishedKernels) {
      if (record.kernel.symbolId == startSymbolId && record.kernel.gen == 0 && record.kernel.pointer >= 1) {
        if (evaluateConditionWithHistory(record.condition, ctx.history, activeCondPaths)) {
          return true
        }
      }
    }
    return false
  }

  private fun evaluateConditionWithHistory(
    cond: AcceptCondition,
    history: List<HistoryEntry>,
    activeCondPaths: Set<PathRoot>,
  ): Boolean = when (cond) {
    Always -> true
    Never -> false
    is And -> {
      var result = true
      cond.forEach { if (!evaluateConditionWithHistory(it, history, activeCondPaths)) result = false }
      result
    }
    is Or -> {
      var result = false
      cond.forEach { if (evaluateConditionWithHistory(it, history, activeCondPaths)) result = true }
      result
    }

    is NoLongerMatch -> {
      // evolve 가 매 step 단순화하므로 evaluate 시 살아남은 NoLongerMatch 는
      // 마지막 step 의 condPathFinishes 로 결정. fromNextGen=true 이면 무조건 true (이번 step 의
      // finish 와 결합 안 함). mgroup2 의 NotExists(_, _, true) => true 와 같은 의미.
      if (cond.fromNextGen) true
      else {
        val root = PathRoot(cond.symbolId, cond.startGen)
        val finCond = history.lastOrNull()?.condPathFinishes?.get(root)
        if (finCond == null) true
        else !evaluateConditionWithHistory(finCond, history, activeCondPaths)
      }
    }

    is NeedLongerMatch -> {
      if (cond.fromNextGen) false
      else {
        val root = PathRoot(cond.symbolId, cond.startGen)
        val finCond = history.lastOrNull()?.condPathFinishes?.get(root)
        if (finCond == null) false
        else evaluateConditionWithHistory(finCond, history, activeCondPaths)
      }
    }

    is NotExists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finishes = collectFinishesAtOrAfter(history, root, cond.startGen)
      if (finishes.isEmpty()) true
      else !finishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
    }

    is Exists -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finishes = collectFinishesAtOrAfter(history, root, cond.startGen)
      finishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
    }

    is Unless -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finishes = collectFinishesAtOrAfter(history, root, cond.startGen)
      if (finishes.isEmpty()) true
      else !finishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
    }

    is OnlyIf -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val finishes = collectFinishesAtOrAfter(history, root, cond.startGen)
      finishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
    }
  }

  private fun collectFinishesAtOrAfter(
    history: List<HistoryEntry>,
    root: PathRoot,
    minGen: Int,
  ): List<AcceptCondition> {
    val result = mutableListOf<AcceptCondition>()
    for ((gen, entry) in history.withIndex()) {
      if (gen < minGen) continue
      val cond = entry.condPathFinishes[root]
      if (cond != null) result.add(cond)
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
    val finalActiveCondPaths = ctx.condPaths.keys

    return ctx.history.mapIndexed { gen, entry ->
      val kernels = mutableSetOf<com.giyeok.jparser.ktlib.Kernel>()
      for (rec in entry.finishedKernels) {
        if (evaluateConditionWithHistory(rec.condition, ctx.history, finalActiveCondPaths)) {
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
      for (rec in entry.progressedKernels) {
        kernels.add(
          com.giyeok.jparser.ktlib.Kernel(
            rec.symbolId,
            rec.pointer,
            rec.startGen,
            rec.midGen,
          )
        )
        kernels.add(
          com.giyeok.jparser.ktlib.Kernel(
            rec.symbolId,
            rec.pointer + 1,
            rec.startGen,
            rec.endGen,
          )
        )
      }
      KernelSet(kernels.toSet())
    }
  }

  private fun AcceptConditionTemplate.toAcceptCondition(prevGen: Int, midGen: Int, gen: Int, grandGen: Int = prevGen): AcceptCondition =
    when (conditionCase) {
      AcceptConditionTemplate.ConditionCase.ALWAYS -> Always
      AcceptConditionTemplate.ConditionCase.AND ->
        And.from(this.and.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) })

      AcceptConditionTemplate.ConditionCase.OR ->
        Or.from(this.or.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) })

      AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH -> {
        val startGen = resolveGen(noLongerMatch.startGen, prevGen, midGen, gen, grandGen)
        NoLongerMatch(noLongerMatch.symbolId, startGen, fromNextGen = true)
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
        Unless(except.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.JOIN -> {
        val startGen = resolveGen(join.startGen, prevGen, midGen, gen, grandGen)
        OnlyIf(join.symbolId, startGen)
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
