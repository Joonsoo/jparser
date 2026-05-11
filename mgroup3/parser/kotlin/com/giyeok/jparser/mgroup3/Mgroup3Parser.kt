package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup3.proto.*
import java.util.*

class Mgroup3Parser(val data: Mgroup3ParserData) {
  private var verbose = false

  fun setVerbose(): Mgroup3Parser {
    verbose = true
    return this
  }

  // (parent symbol, parent pointer, tip group id) -> edge action
  val tipEdgeActionsMap: Map<Pair<KernelTemplatePair, Int>, EdgeAction> =
    data.tipEdgeActionsList.associate {
      Pair(KernelTemplatePair(it.parent.symbolId, it.parent.pointer), it.tipGroupId) to it.edgeAction
    }

  // (parent symbol, parent pointer, tip symbol, tip pointer) -> edge action
  val midEdgeActionsMap: Map<Pair<KernelTemplatePair, KernelTemplatePair>, EdgeAction> =
    data.midEdgeActionsList.associate {
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
        val rootInfo = data.pathRootsMap[symId] ?: continue
        builder[symId] = PathShape(null, rootInfo.milestoneGroupId)
        queue.addAll(rootInfo.initialCondSymbolIdsList)
      }
    }
    return builder.mapKeys { (symId, _) -> PathRoot(symId, gen) }
      .mapValues { (_, shape) -> mapOf(shape to Always) }
  }

  fun initCtx(startSymbolId: Int): ParsingCtx {
    val rootInfo = data.pathRootsMap[startSymbolId]
      ?: throw IllegalArgumentException("No path root for symbol $startSymbolId")

    val mainRoot = PathRoot(startSymbolId, 0)
    val mainPaths: PathMap = mapOf(PathShape(null, rootInfo.milestoneGroupId) to Always)

    val initialCondPaths = condPathsFor(rootInfo.initialCondSymbolIdsList, 0)

    val initialFinishedKernels = mutableListOf<FinishedKernelRecord>()
    val initialProgressedKernels = mutableListOf<ProgressedKernelRecord>()
    if (rootInfo.hasParsingActions()) {
      collectInitialActions(rootInfo.parsingActions, 0, initialFinishedKernels, initialProgressedKernels)
    }
    if (rootInfo.hasSelfFinishAcceptCondition()) {
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
      mainPaths = mainPaths,
      condPaths = initialCondPaths,
      history = listOf(initialEntry),
    )
  }

  fun initCtx(): ParsingCtx = initCtx(data.startSymbolId)

  private fun collectInitialActions(
    pa: ParsingActions,
    gen: Int,
    finishedOut: MutableList<FinishedKernelRecord>,
    progressedOut: MutableList<ProgressedKernelRecord>,
  ) {
    for (finished in pa.finishedList) {
      val startGen = resolveGen(finished.startGen, 0, 0, 0)
      finishedOut.add(
        FinishedKernelRecord(
          Kernel(finished.symbolId, finished.pointer, startGen),
          finished.finishCondition.toAcceptCondition(0, 0, gen),
        )
      )
    }
    for (prog in pa.progressedList) {
      val startGen = resolveGen(prog.startGen, 0, 0, 0)
      val midGen = resolveGen(prog.midGen, 0, 0, 0)
      progressedOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, midGen, gen))
    }
  }

  fun expectedInputsOf(ctx: ParsingCtx): TermSet {
    val termGroups =
      ctx.mainPaths.keys.flatMap { shape ->
        data.termActionsMap[shape.tipGroupId]?.actionsList?.map { it.termGroup } ?: listOf()
      }
    val termSetBuilder = TermGroupUtil.TermGroupBuilder()
    for (termGroup in termGroups) {
      termSetBuilder.add(termGroup)
    }
    return termSetBuilder.build()
  }

  fun findApplicableAction(shape: PathShape, input: Char): TermAction? =
    data.termActionsMap[shape.tipGroupId]?.actionsList?.find { action ->
      TermGroupUtil.isMatch(action.termGroup, input)
    }?.termAction

  // 한 path 의 (shape, prevCondition) 에 term action 을 적용해서 다음 path 들을 만들고 finish/progress 기록.
  // gen 매핑 (term action):
  //   CURR (Prev) = parent milestone 의 gen (= path 의 tip 이 만들어진 gen)
  //   MID  (Curr) = ctx.gen (직전 입력 후 gen)
  //   NEXT (Next) = gen (이번 입력 후 gen)
  private fun applyTermAction(
    oldShape: PathShape,
    oldCondition: AcceptCondition,
    pathRoot: PathRoot,
    termAction: TermAction,
    midGen: Int,
    gen: Int,
    nextPathsOut: MutableMap<PathShape, AcceptCondition>,
    finishesOut: MutableList<FinishedKernelRecord>,
    progressesOut: MutableList<ProgressedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
    // (cond root sym, milestone group id) — mgroup2 의 lookahead_requiring_symbols 처럼,
    // main path 가 새 milestone 을 attach 하는 시점에 같이 등록할 cond root starter 들.
    condRootStartersOut: MutableMap<PathRoot, Int>,
  ) {
    val parentGen = oldShape.milestonePath?.gen ?: pathRoot.startGen
    val grandGen = oldShape.milestonePath?.milestone?.gen ?: pathRoot.startGen

    if (termAction.hasParsingActions()) {
      for (finished in termAction.parsingActions.finishedList) {
        val startGen = resolveGen(finished.startGen, parentGen, midGen, gen, grandGen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(parentGen, midGen, gen, grandGen),
          )
        )
      }
      for (prog in termAction.parsingActions.progressedList) {
        val startGen = resolveGen(prog.startGen, parentGen, midGen, gen, grandGen)
        val mGen = resolveGen(prog.midGen, parentGen, midGen, gen, grandGen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
    }

    for (rea in termAction.replaceAndAppendsList) {
      val newAcceptCondition = rea.append.acceptCondition.toAcceptCondition(parentGen, midGen, gen, grandGen)
      val combined = And.from(oldCondition, newAcceptCondition)
      if (combined == Never) continue

      val replaceKernel = Kernel(rea.replace.symbolId, rea.replace.pointer, parentGen)
      val newMilestonePath = MilestonePath(
        gen = gen,
        milestone = replaceKernel,
        parent = oldShape.milestonePath,
        observingCondSymbolIds = rea.append.observingCondSymbolIdsList.toSet(),
      )
      nextPathsOut.addPath(
        PathShape(milestonePath = newMilestonePath, tipGroupId = rea.append.milestoneGroupId),
        combined,
      )
      observingSymbolIdsOut.addAll(rea.append.observingCondSymbolIdsList)
      for (starter in rea.append.condRootStartersList) {
        condRootStartersOut[PathRoot(starter.symbolId, midGen)] = starter.milestoneGroupId
      }
    }

    for (rap in termAction.replaceAndProgressesList) {
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
    edgeAction: EdgeAction,
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
    if (edgeAction.hasParsingActions()) {
      for (finished in edgeAction.parsingActions.finishedList) {
        val startGen = resolveGen(finished.startGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen),
          )
        )
      }
      for (prog in edgeAction.parsingActions.progressedList) {
        val startGen = resolveGen(prog.startGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        val mGen = resolveGen(prog.midGen, grandParentGen, parentGen, gen, grandGrandParentGen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
    }

    for (append in edgeAction.appendMilestoneGroupsList) {
      val condition = append.acceptCondition.toAcceptCondition(grandParentGen, parentGen, gen, grandGrandParentGen)
      val combined = And.from(prevCondition, condition)
      if (combined == Never) continue
      val newParentPath = parentPath.copy(
        observingCondSymbolIds = append.observingCondSymbolIdsList.toSet(),
      )
      nextPathsOut.addPath(
        PathShape(milestonePath = newParentPath, tipGroupId = append.milestoneGroupId),
        combined,
      )
      observingSymbolIdsOut.addAll(append.observingCondSymbolIdsList)
      for (starter in append.condRootStartersList) {
        condRootStartersOut[PathRoot(starter.symbolId, parentGen)] = starter.milestoneGroupId
      }
    }

    if (edgeAction.hasStartNodeProgress()) {
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
    if (ctx.mainPaths.isEmpty()) {
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

    val nextMainPaths = mutableMapOf<PathShape, AcceptCondition>()
    val finishesByGroup = mutableListOf<FinishedKernelRecord>()
    val progressesByGroup = mutableListOf<ProgressedKernelRecord>()
    val observingOut = mutableSetOf<Int>()
    val rootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    val condRootStartersFromTerm = mutableMapOf<PathRoot, Int>()

    // step 1: main paths 에 term action 적용
    for ((shape, cond) in ctx.mainPaths) {
      val ta = findApplicableAction(shape, input) ?: continue
      applyTermAction(
        oldShape = shape,
        oldCondition = cond,
        pathRoot = ctx.mainRoot,
        termAction = ta,
        midGen = ctx.gen,
        gen = gen,
        nextPathsOut = nextMainPaths,
        finishesOut = finishesByGroup,
        progressesOut = progressesByGroup,
        rootProgressesOut = rootProgresses,
        observingSymbolIdsOut = observingOut,
        condRootStartersOut = condRootStartersFromTerm,
      )
    }

    // step 1b: main path termAction 결과 등록된 condRootStartersFromTerm 의 starter 들에 같은 input 적용.
    val nextCondPaths = mutableMapOf<PathRoot, MutableMap<PathShape, AcceptCondition>>()
    for ((starterRoot, mgroupId) in condRootStartersFromTerm) {
      if (starterRoot in ctx.condPaths.keys) continue
      if (ctx.history.any { starterRoot in it.activeCondPaths }) continue
      val rootInfo = data.pathRootsMap[starterRoot.symbolId] ?: continue
      val starterShape = PathShape(null, mgroupId)
      val ta = findApplicableAction(starterShape, input)
      if (ta != null) {
        val nextPaths = mutableMapOf<PathShape, AcceptCondition>()
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = starterRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          nextPathsOut = nextPaths,
          finishesOut = finishesByGroup,
          progressesOut = progressesByGroup,
          rootProgressesOut = rootProgresses,
          observingSymbolIdsOut = observingOut,
          condRootStartersOut = ignoredStarters,
        )
        if (nextPaths.isNotEmpty()) {
          nextCondPaths.getOrPut(starterRoot) { mutableMapOf() }.also { acc ->
            nextPaths.forEach { (s, c) -> acc.addPath(s, c) }
          }
        }
      }
      if (rootInfo.hasSelfFinishAcceptCondition()) {
        val cond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(starterRoot.startGen, starterRoot.startGen, gen)
        val existing = rootProgresses[starterRoot]
        rootProgresses[starterRoot] = if (existing != null) Or.from(existing, cond) else cond
      }
    }

    // step 2: cond paths 에 term action 적용
    for ((root, pathMap) in ctx.condPaths) {
      val nextPaths = mutableMapOf<PathShape, AcceptCondition>()
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
            nextPathsOut = nextPaths,
            finishesOut = finishesByGroup,
            progressesOut = progressesByGroup,
            rootProgressesOut = rootProgresses,
            observingSymbolIdsOut = observingOut,
            condRootStartersOut = condRootStartersFromTerm,
          )
        } else {
          // path 가 input 매치 못해 dead — milestone group 의 possible_finishes 에서
          // root.symbolId 와 일치하는 finish 있으면 cond path finish 로 등록.
          val mg = data.milestoneGroupsMap[shape.tipGroupId]
          if (mg != null) {
            for (pf in mg.possibleFinishesList) {
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
      if (nextPaths.isNotEmpty()) {
        nextCondPaths[root] = nextPaths
      }
    }

    // step 3: 새로 등장한 cond symbol 에 대해 cond path 시작.
    val newCondRootSyms = observingOut.toSet()
    val allObservingSyms = mutableSetOf<Int>()
    val queue: Queue<Int> = LinkedList()
    queue.addAll(newCondRootSyms)
    while (queue.isNotEmpty()) {
      val s = queue.poll()
      if (s !in allObservingSyms) {
        allObservingSyms.add(s)
        val info = data.pathRootsMap[s]
        if (info != null) queue.addAll(info.initialCondSymbolIdsList)
      }
    }

    val newCondRoots = mutableSetOf<PathRoot>()
    fun extractRootsFromCond(cond: AcceptCondition) {
      when (cond) {
        Always, Never -> {}
        is And -> cond.conds.forEach { extractRootsFromCond(it) }
        is Or -> cond.conds.forEach { extractRootsFromCond(it) }
        is NoLongerMatch -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NeedLongerMatch -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is NotExists -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Exists -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is Unless -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
        is OnlyIf -> newCondRoots.add(PathRoot(cond.symbolId, cond.startGen))
      }
    }
    nextMainPaths.values.forEach { extractRootsFromCond(it) }
    nextCondPaths.values.forEach { pm -> pm.values.forEach { extractRootsFromCond(it) } }

    val newCondRootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    for (sym in allObservingSyms) {
      newCondRoots.add(PathRoot(sym, gen))
    }
    for ((root, _) in condRootStartersFromTerm) {
      newCondRoots.add(root)
    }

    // history 에 한 번이라도 등장한 적 있는 cond root 은 skip.
    val everSeenCondRoots = mutableSetOf<PathRoot>()
    for (entry in ctx.history) {
      everSeenCondRoots.addAll(entry.activeCondPaths)
    }
    for (pathRoot in newCondRoots) {
      if (pathRoot in ctx.condPaths.keys || pathRoot in nextCondPaths.keys) continue
      if (pathRoot in everSeenCondRoots) continue
      val rootInfo = data.pathRootsMap[pathRoot.symbolId] ?: continue
      if (rootInfo.hasSelfFinishAcceptCondition()) {
        val selfCond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(pathRoot.startGen, pathRoot.startGen, gen)
        newCondRootProgresses[pathRoot] = selfCond
      }
      val starterShape = PathShape(null, rootInfo.milestoneGroupId)
      val ta = findApplicableAction(starterShape, input)
      val nextPaths = mutableMapOf<PathShape, AcceptCondition>()
      if (ta != null) {
        val ignoredStarters = mutableMapOf<PathRoot, Int>()
        applyTermAction(
          oldShape = starterShape,
          oldCondition = Always,
          pathRoot = pathRoot,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          nextPathsOut = nextPaths,
          finishesOut = finishesByGroup,
          progressesOut = progressesByGroup,
          rootProgressesOut = newCondRootProgresses,
          observingSymbolIdsOut = observingOut,
          condRootStartersOut = ignoredStarters,
        )
      }
      if (nextPaths.isNotEmpty()) {
        nextCondPaths[pathRoot] = nextPaths
      } else if (pathRoot.startGen == gen) {
        nextCondPaths[pathRoot] = mutableMapOf(starterShape to Always)
      }
    }


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

    // step 5: 모든 path 의 acceptCondition 을 evolve
    val activeCondRoots = nextCondPaths.keys

    fun evolveMap(paths: Map<PathShape, AcceptCondition>): Map<PathShape, AcceptCondition> {
      if (paths.isEmpty()) return paths
      val result = LinkedHashMap<PathShape, AcceptCondition>()
      for ((shape, cond) in paths) {
        val evolved = evolveAcceptCondition(cond, condPathFinishes, activeCondRoots, gen)
        if (evolved == Never) continue
        val existing = result[shape]
        result[shape] = if (existing == null) evolved else Or.from(existing, evolved)
      }
      return result
    }

    val mainPathsEvolved = evolveMap(nextMainPaths)
    val condPathsEvolved = nextCondPaths
      .mapValues { (_, pm) -> evolveMap(pm) }
      .filter { (_, pm) -> pm.isNotEmpty() }

    // step 6: 사용되지 않는 cond path 제거
    val referencedRoots = mutableSetOf<PathRoot>()
    fun collectReferenced(cond: AcceptCondition) {
      when (cond) {
        Always, Never -> {}
        is And -> cond.conds.forEach { collectReferenced(it) }
        is Or -> cond.conds.forEach { collectReferenced(it) }
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
    mainPathsEvolved.forEach { (shape, cond) -> collectFromShape(shape, cond) }
    condPathsEvolved.values.flatMap { it.entries }.forEach { (shape, cond) -> collectFromShape(shape, cond) }

    val condPathsFiltered = condPathsEvolved.filter { (root, _) -> root in referencedRoots }

    // step 7: 입력 종료 시점이 아닌데 main path 모두 사라진 경우 에러
    if (!isLastInput && mainPathsEvolved.isEmpty()) {
      throw ParsingError.UnexpectedInput(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx), input)
    }

    val historyEntry = HistoryEntry(
      finishedKernels = finishesByGroup,
      progressedKernels = progressesByGroup,
      condPathFinishes = condPathFinishes.toMap(),
      activeCondPaths = condPathsFiltered.keys.toSet(),
    )

    return ParsingCtx(
      gen = gen,
      line = nextLine,
      col = nextCol,
      mainRoot = ctx.mainRoot,
      mainPaths = mainPathsEvolved,
      condPaths = condPathsFiltered,
      history = ctx.history + historyEntry,
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
    val startSymbolId = data.startSymbolId
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
    is And -> cond.conds.all { evaluateConditionWithHistory(it, history, activeCondPaths) }
    is Or -> cond.conds.any { evaluateConditionWithHistory(it, history, activeCondPaths) }

    is NoLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val laterFinishes = collectFinishesAfter(history, root, cond.endGen)
      if (laterFinishes.isEmpty()) true
      else !laterFinishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
    }

    is NeedLongerMatch -> {
      val root = PathRoot(cond.symbolId, cond.startGen)
      val laterFinishes = collectFinishesAfter(history, root, cond.endGen)
      laterFinishes.any { evaluateConditionWithHistory(it, history, activeCondPaths) }
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

  private fun collectFinishesAfter(
    history: List<HistoryEntry>,
    root: PathRoot,
    strictlyAfter: Int,
  ): List<AcceptCondition> {
    val result = mutableListOf<AcceptCondition>()
    for ((gen, entry) in history.withIndex()) {
      if (gen <= strictlyAfter) continue
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
        And.from(this.and.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) }.toSet())

      AcceptConditionTemplate.ConditionCase.OR ->
        Or.from(this.or.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen, grandGen) }.toSet())

      AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH -> {
        val startGen = resolveGen(noLongerMatch.startGen, prevGen, midGen, gen, grandGen)
        NoLongerMatch(noLongerMatch.symbolId, startGen, gen)
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
