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
  // 한 cond symbol이 다른 cond symbol을 참조할 수도 있으므로 transitive closure
  fun condPathsFor(condSymbolIds: Collection<Int>, gen: Int): Map<PathRoot, List<ParsingPath>> {
    val builder = mutableMapOf<Int, ParsingPath>()
    val queue: Queue<Int> = LinkedList()
    queue.addAll(condSymbolIds)
    while (queue.isNotEmpty()) {
      val symId = queue.poll()
      if (symId !in builder) {
        val rootInfo = data.pathRootsMap[symId] ?: continue
        builder[symId] = ParsingPath(null, rootInfo.milestoneGroupId, Always)
        queue.addAll(rootInfo.initialCondSymbolIdsList)
      }
    }
    return builder.mapKeys { (symId, _) -> PathRoot(symId, gen) }
      .mapValues { (_, path) -> listOf(path) }
  }

  fun initCtx(startSymbolId: Int): ParsingCtx {
    val rootInfo = data.pathRootsMap[startSymbolId]
      ?: throw IllegalArgumentException("No path root for symbol $startSymbolId")

    val mainRoot = PathRoot(startSymbolId, 0)
    val mainPath = ParsingPath(null, rootInfo.milestoneGroupId, Always)

    val initialCondPaths = condPathsFor(rootInfo.initialCondSymbolIdsList, 0)

    val initialFinishedKernels = mutableListOf<FinishedKernelRecord>()
    val initialProgressedKernels = mutableListOf<ProgressedKernelRecord>()
    if (rootInfo.hasParsingActions()) {
      collectInitialActions(rootInfo.parsingActions, 0, initialFinishedKernels, initialProgressedKernels)
    }
    if (rootInfo.hasSelfFinishAcceptCondition()) {
      // start symbol이 처음에 자체 finish되는 경우
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
      mainPaths = listOf(mainPath),
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
    // initial: parentGen=0, midGen=0, nextGen=0
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
      ctx.mainPaths.flatMap { path ->
        data.termActionsMap[path.tipGroupId]?.actionsList?.map { it.termGroup } ?: listOf()
      }
    val termSetBuilder = TermGroupUtil.TermGroupBuilder()
    for (termGroup in termGroups) {
      termSetBuilder.add(termGroup)
    }
    return termSetBuilder.build()
  }

  fun findApplicableAction(path: ParsingPath, input: Char): TermAction? =
    data.termActionsMap[path.tipGroupId]?.actionsList?.find { action ->
      TermGroupUtil.isMatch(action.termGroup, input)
    }?.termAction

  // 한 path에 term action을 적용해서 다음 path들을 만들고, 해당 step에서 발생한 finish/progress들을 기록
  // gen 매핑 (term action):
  //   CURR (Prev) = parent milestone의 gen (= path의 tip이 만들어진 gen)
  //   MID  (Curr) = ctx.gen (직전 입력 후 gen)
  //   NEXT (Next) = gen (이번 입력 후 gen)
  private fun applyTermAction(
    oldPath: ParsingPath,
    pathRoot: PathRoot,
    termAction: TermAction,
    midGen: Int,
    gen: Int,
    nextPathsOut: MutableList<ParsingPath>,
    finishesOut: MutableList<FinishedKernelRecord>,
    progressesOut: MutableList<ProgressedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
    // path의 tip이 만들어진 gen (= parent milestone gen)
    val parentGen = oldPath.milestonePath?.gen ?: pathRoot.startGen

    // term action에서 발생한 finish/progress 기록
    if (termAction.hasParsingActions()) {
      for (finished in termAction.parsingActions.finishedList) {
        val startGen = resolveGen(finished.startGen, parentGen, midGen, gen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(parentGen, midGen, gen),
          )
        )
      }
      for (prog in termAction.parsingActions.progressedList) {
        val startGen = resolveGen(prog.startGen, parentGen, midGen, gen)
        val mGen = resolveGen(prog.midGen, parentGen, midGen, gen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
    }

    // replace_and_appends: tip이 진행되어 milestone이 되고, 그 milestone 뒤에 새 mgroup이 붙음
    for (rea in termAction.replaceAndAppendsList) {
      val newAcceptCondition = rea.append.acceptCondition.toAcceptCondition(parentGen, midGen, gen)
      val combined = And.from(oldPath.acceptCondition, newAcceptCondition)
      if (combined == Never) continue

      // replace milestone (tip이 진행된 결과)을 새 milestone path로 만들고 그 뒤에 mgroup이 tip
      // replace는 진행 후의 (replaced) milestone이 어떤 (symbolId, pointer)인지를 나타냄
      // 그 milestone이 만들어진 gen은 parentGen (즉 path의 tip의 gen)
      val replaceKernel = Kernel(rea.replace.symbolId, rea.replace.pointer, parentGen)
      val newMilestonePath = MilestonePath(
        gen = gen,
        milestone = replaceKernel,
        parent = oldPath.milestonePath,
        observingCondSymbolIds = rea.append.observingCondSymbolIdsList.toSet(),
      )
      nextPathsOut.add(
        ParsingPath(
          milestonePath = newMilestonePath,
          tipGroupId = rea.append.milestoneGroupId,
          acceptCondition = combined,
        )
      )
      observingSymbolIdsOut.addAll(rea.append.observingCondSymbolIdsList)
    }

    // replace_and_progresses: tip의 일부 milestone들이 자체 sequence 끝까지 진행되어 reduce 발생
    for (rap in termAction.replaceAndProgressesList) {
      val newAcceptCondition = rap.acceptCondition.toAcceptCondition(parentGen, midGen, gen)
      val combined = And.from(oldPath.acceptCondition, newAcceptCondition)
      if (combined == Never) continue

      val parentPath = oldPath.milestonePath
      if (parentPath == null) {
        // root에서 직접 self-progress된 경우 (start symbol이 finish됨)
        val existing = rootProgressesOut[pathRoot]
        rootProgressesOut[pathRoot] = if (existing != null) {
          Or.from(existing, combined)
        } else combined

        // root 자체가 finish 된 것이므로 (rootSymbolId, 1, root.startGen)이 finish되었다고 기록
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(pathRoot.symbolId, 1, pathRoot.startGen),
            combined,
          )
        )
      } else {
        // parent의 tip edge action을 발견해서 적용
        val tipEdgeAction = tipEdgeActionsMap[
          Pair(parentPath.milestone.kernelTemplate, rap.replaceMilestoneGroupId)
        ]
        if (tipEdgeAction != null) {
          // tip이 진행되었음을 progressed로 기록 (parent의 (sym, ptr+1, parentParentGen) 형태)
          val grandParentGen = parentPath.parent?.gen ?: pathRoot.startGen
          // 사실 tip의 진행은 mgroup parsing actions에서 자동으로 처리되어 있을 수 있음
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
          )
        }
      }
    }
  }

  // edge action gen 매핑:
  //   CURR (Prev) = grandparent gen
  //   MID  (Curr) = parent gen
  //   NEXT (Next) = gen (현재 step gen)
  private fun applyEdgeAction(
    parentPath: MilestonePath,
    edgeAction: EdgeAction,
    pathRoot: PathRoot,
    prevCondition: AcceptCondition,
    grandParentGen: Int,
    parentGen: Int,
    gen: Int,
    nextPathsOut: MutableList<ParsingPath>,
    finishesOut: MutableList<FinishedKernelRecord>,
    progressesOut: MutableList<ProgressedKernelRecord>,
    rootProgressesOut: MutableMap<PathRoot, AcceptCondition>,
    observingSymbolIdsOut: MutableSet<Int>,
  ) {
    // edge action 자체에서 발생한 finish/progress 기록
    if (edgeAction.hasParsingActions()) {
      for (finished in edgeAction.parsingActions.finishedList) {
        val startGen = resolveGen(finished.startGen, grandParentGen, parentGen, gen)
        finishesOut.add(
          FinishedKernelRecord(
            Kernel(finished.symbolId, finished.pointer, startGen),
            finished.finishCondition.toAcceptCondition(grandParentGen, parentGen, gen),
          )
        )
      }
      for (prog in edgeAction.parsingActions.progressedList) {
        val startGen = resolveGen(prog.startGen, grandParentGen, parentGen, gen)
        val mGen = resolveGen(prog.midGen, grandParentGen, parentGen, gen)
        progressesOut.add(ProgressedKernelRecord(prog.symbolId, prog.pointer, startGen, mGen, gen))
      }
    }

    // append milestone groups: parent를 그대로 유지한 채로 tip만 새 mgroup으로 교체
    // (mgroup2와 같은 의미: path 구조는 그대로, tip만 바뀜)
    // 단, tip의 observingCondSymbolIds는 새 mgroup의 것으로 갱신
    for (append in edgeAction.appendMilestoneGroupsList) {
      val condition = append.acceptCondition.toAcceptCondition(grandParentGen, parentGen, gen)
      val combined = And.from(prevCondition, condition)
      if (combined == Never) continue
      // parentPath를 그대로 사용하되, observingCondSymbolIds를 새 것으로 교체
      val newParentPath = parentPath.copy(
        observingCondSymbolIds = append.observingCondSymbolIdsList.toSet(),
      )
      nextPathsOut.add(
        ParsingPath(
          milestonePath = newParentPath,
          tipGroupId = append.milestoneGroupId,
          acceptCondition = combined,
        )
      )
      observingSymbolIdsOut.addAll(append.observingCondSymbolIdsList)
    }

    // start node progress: parent 자체가 진행되어 reduce
    if (edgeAction.hasStartNodeProgress()) {
      val startNodeProgressCondition =
        edgeAction.startNodeProgress.toAcceptCondition(grandParentGen, parentGen, gen)
      val combined = And.from(prevCondition, startNodeProgressCondition)
      if (combined != Never) {
        val grandParent = parentPath.parent
        if (grandParent == null) {
          // root path까지 reduce되는 경우
          val existing = rootProgressesOut[pathRoot]
          rootProgressesOut[pathRoot] = if (existing != null) {
            Or.from(existing, combined)
          } else combined

          // root 자체가 finish됨을 기록
          finishesOut.add(
            FinishedKernelRecord(
              Kernel(pathRoot.symbolId, 1, pathRoot.startGen),
              combined,
            )
          )
        } else {
          // mid edge로 더 위로 reduce
          val midEdge = midEdgeActionsMap[
            Pair(grandParent.milestone.kernelTemplate, parentPath.milestone.kernelTemplate)
          ]
          if (midEdge != null) {
            val grandGrandParentGen = grandParent.parent?.gen ?: pathRoot.startGen
            applyEdgeAction(
              parentPath = grandParent,
              edgeAction = midEdge,
              pathRoot = pathRoot,
              prevCondition = combined,
              grandParentGen = grandGrandParentGen,
              parentGen = grandParent.gen,
              gen = gen,
              nextPathsOut = nextPathsOut,
              finishesOut = finishesOut,
              progressesOut = progressesOut,
              rootProgressesOut = rootProgressesOut,
              observingSymbolIdsOut = observingSymbolIdsOut,
            )
          }
        }
      }
    }
  }

  private fun resolveGen(genTag: KernelTemplateGen, currGen: Int, midGen: Int, nextGen: Int): Int =
    when (genTag) {
      KernelTemplateGen.CURR -> currGen
      KernelTemplateGen.MID -> midGen
      KernelTemplateGen.NEXT -> nextGen
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

    val nextMainPaths = mutableListOf<ParsingPath>()
    val finishesByGroup = mutableListOf<FinishedKernelRecord>()
    val progressesByGroup = mutableListOf<ProgressedKernelRecord>()
    val observingOut = mutableSetOf<Int>()
    val rootProgresses = mutableMapOf<PathRoot, AcceptCondition>()

    // step 1: main paths에 term action 적용
    for (path in ctx.mainPaths) {
      val ta = findApplicableAction(path, input) ?: continue
      applyTermAction(
        oldPath = path,
        pathRoot = ctx.mainRoot,
        termAction = ta,
        midGen = ctx.gen,
        gen = gen,
        nextPathsOut = nextMainPaths,
        finishesOut = finishesByGroup,
        progressesOut = progressesByGroup,
        rootProgressesOut = rootProgresses,
        observingSymbolIdsOut = observingOut,
      )
    }

    // step 2: cond paths에 term action 적용
    val nextCondPaths = mutableMapOf<PathRoot, MutableList<ParsingPath>>()
    for ((root, paths) in ctx.condPaths) {
      val nextPaths = mutableListOf<ParsingPath>()
      for (path in paths) {
        val ta = findApplicableAction(path, input) ?: continue
        applyTermAction(
          oldPath = path,
          pathRoot = root,
          termAction = ta,
          midGen = ctx.gen,
          gen = gen,
          nextPathsOut = nextPaths,
          finishesOut = finishesByGroup,
          progressesOut = progressesByGroup,
          rootProgressesOut = rootProgresses,
          observingSymbolIdsOut = observingOut,
        )
      }
      if (nextPaths.isNotEmpty()) {
        nextCondPaths[root] = nextPaths
      }
    }

    // step 3: 새로 등장한 cond symbol에 대해 cond path 시작.
    // 이 cond path들은 main path가 이번 step의 input을 통해 본 새 cond symbol에 대응되며,
    // condition.startGen=gen (현재 step gen)으로 등록된다.
    // 이 cond path들도 *이번 step의 input을* 함께 처리한 결과로 간주되어야 한다.
    // 즉 starter cond path가 input에 매치될 수 있으면 그 input 매치 결과를 nextCondPaths에 반영,
    // 그 과정에서 finish가 일어나면 condPathFinishes에 누적.
    val newCondRootSyms = observingOut.toSet()
    // 추적할 모든 cond symbol (transitive closure)
    val allObservingSyms = mutableSetOf<Int>()
    val queue: java.util.Queue<Int> = java.util.LinkedList()
    queue.addAll(newCondRootSyms)
    while (queue.isNotEmpty()) {
      val s = queue.poll()
      if (s !in allObservingSyms) {
        allObservingSyms.add(s)
        val info = data.pathRootsMap[s]
        if (info != null) queue.addAll(info.initialCondSymbolIdsList)
      }
    }
    // nextMainPaths/nextCondPaths의 acceptCondition 에서 정확한 (sym, startGen) PathRoot 추출.
    // condition reify 시점에 startGen 결정되었으므로 정확한 시작 gen 을 가짐.
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
    nextMainPaths.forEach { extractRootsFromCond(it.acceptCondition) }
    nextCondPaths.values.forEach { paths -> paths.forEach { extractRootsFromCond(it.acceptCondition) } }

    // observingCondSymbolIds 는 추가로 추적할 cond symbol들 (predictive). condition에 아직 안 등장한
    // cond root이 등장할 가능성 있음. 그 경우 PathRoot(sym, gen) 으로 등록 (보수적으로).
    val newCondRootProgresses = mutableMapOf<PathRoot, AcceptCondition>()
    for (sym in allObservingSyms) {
      // 이미 condition에서 발견된 root 외, observing sym 도 새 root 으로 추가 (gen 시점).
      // 매 step PathRoot(sym, gen) 등록 — condition 안 같은 sym 에 다른 startGen 있을 수도 있어서
      // 양쪽 모두 추적.
      newCondRoots.add(PathRoot(sym, gen))
    }

    for (pathRoot in newCondRoots) {
      // 이미 ctx.condPaths/nextCondPaths 에 있는 root 은 skip.
      if (pathRoot in ctx.condPaths.keys || pathRoot in nextCondPaths.keys) continue
      val rootInfo = data.pathRootsMap[pathRoot.symbolId] ?: continue
      // root가 self-finish 가능한 경우 (예: 'a'? 같은 nullable cond symbol) 즉시 finish
      if (rootInfo.hasSelfFinishAcceptCondition()) {
        val cond = rootInfo.selfFinishAcceptCondition.toAcceptCondition(pathRoot.startGen, pathRoot.startGen, gen)
        newCondRootProgresses[pathRoot] = cond
      }
      // starter cond path 처리.
      // pathRoot.startGen == gen (fromNextGen=true 의미): starter는 NEXT step부터 진행. input 적용 안 함.
      // pathRoot.startGen < gen: starter는 startGen 시점부터. 이번 step input 까지 적용.
      val starterPath = ParsingPath(null, rootInfo.milestoneGroupId, Always)
      if (pathRoot.startGen == gen) {
        nextCondPaths[pathRoot] = mutableListOf(starterPath)
      } else {
        val ta = findApplicableAction(starterPath, input)
        if (ta != null) {
          val nextPaths = mutableListOf<ParsingPath>()
          applyTermAction(
            oldPath = starterPath,
            pathRoot = pathRoot,
            termAction = ta,
            midGen = ctx.gen,
            gen = gen,
            nextPathsOut = nextPaths,
            finishesOut = finishesByGroup,
            progressesOut = progressesByGroup,
            rootProgressesOut = newCondRootProgresses,
            observingSymbolIdsOut = observingOut,
          )
          if (nextPaths.isNotEmpty()) {
            nextCondPaths[pathRoot] = nextPaths
          }
        }
      }
    }


    // step 4: condPath finish detection
    val condPathFinishes = mutableMapOf<PathRoot, AcceptCondition>()
    for ((root, cond) in rootProgresses) {
      if (root != ctx.mainRoot) {
        condPathFinishes[root] = cond
      }
    }
    // 새 cond path들의 finish (newCondRootProgresses의 self-finish 포함)
    for ((root, cond) in newCondRootProgresses) {
      if (root != ctx.mainRoot) {
        val existing = condPathFinishes[root]
        condPathFinishes[root] = if (existing != null) Or.from(existing, cond) else cond
      }
    }

    // step 5: 모든 path의 acceptCondition을 evolve
    val activeCondRoots = nextCondPaths.keys

    fun evolveAndPrune(paths: List<ParsingPath>): List<ParsingPath> =
      paths.mapNotNull { path ->
        val evolved = evolveAcceptCondition(path.acceptCondition, condPathFinishes, activeCondRoots, gen)
        if (evolved == Never) null
        else if (evolved == path.acceptCondition) path
        else path.copy(acceptCondition = evolved)
      }

    // path dedup: 같은 (milestonePath, tipGroupId)에 대해 acceptCondition은 OR로 합침.
    // 같은 path가 여러 source에서 만들어질 때 폭증을 막음.
    fun dedupPaths(paths: List<ParsingPath>): List<ParsingPath> {
      if (paths.size <= 1) return paths
      val merged = LinkedHashMap<Pair<MilestonePath?, Int>, AcceptCondition>()
      for (p in paths) {
        val key = Pair(p.milestonePath, p.tipGroupId)
        val existing = merged[key]
        merged[key] = if (existing == null) p.acceptCondition else Or.from(existing, p.acceptCondition)
      }
      return merged.map { (key, cond) -> ParsingPath(key.first, key.second, cond) }
    }

    val mainPathsEvolved = dedupPaths(evolveAndPrune(nextMainPaths))
    val condPathsEvolved = nextCondPaths.mapValues { (_, paths) -> dedupPaths(evolveAndPrune(paths)) }
      .filter { (_, paths) -> paths.isNotEmpty() }

    // step 6: 사용되지 않는 cond path 제거 - 살아있는 path들의 condition에서 참조되는 root만 keep
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
    fun collectFromPath(path: ParsingPath) {
      collectReferenced(path.acceptCondition)
      // mp의 observingCondSymbolIds는 그 milestone이 만들어진 시점에 추적해야 할 cond symbol들.
      // 그 cond symbol들은 path가 살아있는 한 추적되어야 (예: NJoin이 path 끝에 등장할 때 OnlyIf condition으로 사용).
      // 이때 startGen은 milestone의 gen.
      var mp = path.milestonePath
      while (mp != null) {
        for (sid in mp.observingCondSymbolIds) {
          // 가능한 두 startGen으로 keep:
          // - mp.gen: milestone이 만들어진 시점 (fromNextGen=false 매핑)
          // - 그 이전 path의 gen (즉 grandparent의 gen): condition reify의 prevGen에 해당
          referencedRoots.add(PathRoot(sid, mp.gen))
          val parentGen = mp.parent?.gen ?: ctx.mainRoot.startGen
          referencedRoots.add(PathRoot(sid, parentGen))
        }
        mp = mp.parent
      }
    }
    mainPathsEvolved.forEach { collectFromPath(it) }
    condPathsEvolved.values.flatten().forEach { collectFromPath(it) }

    val condPathsFiltered = condPathsEvolved.filter { (root, _) -> root in referencedRoots }

    // step 7: 입력 종료 시점이 아닌데 main path가 모두 사라진 경우 에러
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

  // 입력 종료 후 mainRoot가 finish 가능한지 확인
  fun isAccepted(ctx: ParsingCtx): Boolean {
    val startSymbolId = data.startSymbolId
    val lastEntry = ctx.history.lastOrNull() ?: return false

    val activeCondPaths = ctx.condPaths.keys

    // mainRoot symbol(=startSymbol)이 (gen 0에서 시작해서) 마지막에 finish되었고 그 condition이 true이면 accepted
    for (record in lastEntry.finishedKernels) {
      if (record.kernel.symbolId == startSymbolId && record.kernel.gen == 0 && record.kernel.pointer >= 1) {
        if (evaluateConditionWithHistory(record.condition, ctx.history, activeCondPaths)) {
          return true
        }
      }
    }
    return false
  }

  // history-aware condition evaluation. NoLongerMatch / Unless 등은 endGen 이후의 finish가 있을 때만 영향.
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
      // longest: endGen 이후 (gen > endGen)에 (s, startGen) finish가 더 일어났으면 false (longer match).
      // input 끝까지 active 자체는 longer match가 아님 (실제로 finish해야 longer match).
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
      // startGen 이후 (gen >= startGen) PathRoot(symbolId, startGen) finish가 evaluable이면 false (lookahead 성립 안 함)
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

  // history의 step gen >= minGen 인 entry들에서 PathRoot에 대한 finish condition들을 모음
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

  // history의 step gen > strictlyAfter 인 entry들에서 PathRoot에 대한 finish condition들을 모음
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

  // history의 마지막 step에서 PathRoot이 active 했는지 (또는 strictlyAfter 이후 모두 active이었는지)
  private fun isActiveAfter(
    history: List<HistoryEntry>,
    root: PathRoot,
    strictlyAfter: Int,
  ): Boolean {
    val last = history.lastOrNull() ?: return false
    return root in last.activeCondPaths
  }

  fun parseOrThrow(text: String): ParsingCtx {
    val ctx = parse(text)
    if (!isAccepted(ctx)) {
      throw ParsingError.UnexpectedEof(ctx.gen, ctx.line, ctx.col, expectedInputsOf(ctx))
    }
    return ctx
  }

  // 각 step별로 그 step에서 추가/진행된 kernel들의 KernelSet을 반환.
  // mulang AST 생성 등에서 사용. 0번째 entry는 init context 결과 (gen=0).
  fun kernelsHistory(ctx: ParsingCtx): List<KernelSet> {
    // condPathFins는 history 전체 누적 (ctx.history.condPathFinishes 모두 OR로 모음)
    val finalActiveCondPaths = ctx.condPaths.keys

    return ctx.history.mapIndexed { gen, entry ->
      val kernels = mutableSetOf<com.giyeok.jparser.ktlib.Kernel>()
      for (rec in entry.finishedKernels) {
        if (evaluateConditionWithHistory(rec.condition, ctx.history, finalActiveCondPaths)) {
          kernels.add(
            com.giyeok.jparser.ktlib.Kernel(
              rec.kernel.symbolId,
              rec.kernel.pointer,
              rec.kernel.gen, // beginGen
              gen, // endGen
            )
          )
        }
      }
      for (rec in entry.progressedKernels) {
        // (sym, ptr, startGen, midGen) 와 (sym, ptr+1, startGen, endGen) 둘 다 추가
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

  private fun AcceptConditionTemplate.toAcceptCondition(prevGen: Int, midGen: Int, gen: Int): AcceptCondition =
    when (conditionCase) {
      AcceptConditionTemplate.ConditionCase.ALWAYS -> Always
      AcceptConditionTemplate.ConditionCase.AND ->
        And.from(this.and.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen) }.toSet())

      AcceptConditionTemplate.ConditionCase.OR ->
        Or.from(this.or.conditionsList.map { it.toAcceptCondition(prevGen, midGen, gen) }.toSet())

      AcceptConditionTemplate.ConditionCase.NO_LONGER_MATCH -> {
        val startGen = resolveGen(noLongerMatch.startGen, prevGen, midGen, gen)
        NoLongerMatch(noLongerMatch.symbolId, startGen, gen)
      }

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_FOUND -> {
        val startGen = resolveGen(lookaheadFound.startGen, prevGen, midGen, gen)
        Exists(lookaheadFound.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.LOOKAHEAD_NOTFOUND -> {
        val startGen = resolveGen(lookaheadNotfound.startGen, prevGen, midGen, gen)
        NotExists(lookaheadNotfound.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.EXCEPT -> {
        val startGen = resolveGen(except.startGen, prevGen, midGen, gen)
        Unless(except.symbolId, startGen)
      }

      AcceptConditionTemplate.ConditionCase.JOIN -> {
        val startGen = resolveGen(join.startGen, prevGen, midGen, gen)
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
