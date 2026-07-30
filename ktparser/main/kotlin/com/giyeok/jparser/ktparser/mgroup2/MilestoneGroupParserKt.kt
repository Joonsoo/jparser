package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.ParsingErrorKt
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.ktlib.TermSet
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto.*
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto.*
import java.nio.file.Path

class MilestoneGroupParserKt(val parserData: MilestoneGroupParserDataKt) {
  constructor(proto: MilestoneGroupParserData): this(MilestoneGroupParserDataKt(proto))

  private var verbose: Boolean = false
  private var debugging: Boolean = false

  private var debuggingLog: DebuggingLogBuilder? = null

  fun setVerbose(): MilestoneGroupParserKt {
    verbose = true
    return this
  }

  fun setDebugging(): MilestoneGroupParserKt {
    debugging = true
    debuggingLog = DebuggingLogBuilder()
    return this
  }

  fun debuggingLogJsonString(): String? = debuggingLog?.toJsonString()

  fun saveDebuggingLog(path: Path) {
    debuggingLog?.writeTo(path)
  }

  val initialMilestone: MilestoneKt = MilestoneKt(parserData.grammar.startSymbol, 0, 0)

  val initialPath = MilestoneGroupPathKt(
    initialMilestone,
    PathList.Nil,
    MilestoneGroupKt(parserData.startGroupId, 0),
    MilestoneAcceptConditionKt.Always
  )

  // 초기 컨텍스트의 조건 템플릿들이 감시하는 심볼들의 루트 경로.
  // gen 0에서 zero-width로 progress된 lookahead/longest 조건의 추적 루트가 초기
  // 컨텍스트부터 존재해야 gen 0 시점의 조건 평가가 잘못 해소되지 않는다.
  // (Scala MilestoneGroupParser.initialConditionPaths와 동일)
  private val initialConditionPaths: List<MilestoneGroupPathKt> =
    parserData.initialTasksSummary.addedKernelsList
      .map { MilestoneAcceptConditionKt.reify(it.acceptCondition, 0, 0) }
      .flatMap { it.milestones() }
      .distinct()
      .filter { it != initialMilestone }
      .mapNotNull { milestone ->
        val groupEntry = parserData.milestoneGroups.entries.find { (_, templates) ->
          templates.size == 1 &&
            templates[0].symbolId == milestone.symbolId &&
            templates[0].pointer == milestone.pointer
        }
        groupEntry?.let {
          MilestoneGroupPathKt(milestone, PathList.Nil, MilestoneGroupKt(it.key, 0), MilestoneAcceptConditionKt.Always)
        }
      }

  val initialCtx = ParsingContextKt(
    0,
    listOf(initialPath) + initialConditionPaths,
    HistoryEntryList.Nil(HistoryEntryKt(listOf(initialPath) + initialConditionPaths, GenActionsKt.empty)),
  )

  fun progressTip(
    pathFirst: MilestoneKt,
    path: PathList.Cons,
    pathCondition: MilestoneAcceptConditionKt,
    gen: Int,
    action: EdgeAction,
    pathsCollector: MutableList<MilestoneGroupPathKt>,
    actionsCollector: GenActionsKtBuilder
  ) {
    val tip = path.milestone

    for (appending in action.appendingMilestoneGroupsList) {
      val newCondition = MilestoneAcceptConditionKt.reify(appending.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptConditionKt.conjunct(pathCondition, newCondition)
      val newPath =
        MilestoneGroupPathKt(pathFirst, path, MilestoneGroupKt(appending.groupId, gen), condition)
      pathsCollector.add(newPath)
    }
    if (action.hasStartNodeProgress()) {
      val startNodeProgressCondition = action.startNodeProgress!!

      val newCondition = MilestoneAcceptConditionKt.reify(startNodeProgressCondition, tip.gen, gen)
      val condition = MilestoneAcceptConditionKt.conjunct(pathCondition, newCondition)

      when (path.parent) {
        is PathList.Cons -> {
          val tipParent = path.parent.milestone
          actionsCollector.addProgressedKernel(tip, tipParent.gen, condition)

          val edgeAction = parserData.getMidEdgeProgressAction(tipParent, tip)
          actionsCollector.addMidEdgeAction(tipParent, tip, edgeAction)
          progressTip(
            pathFirst,
            path.parent,
            condition,
            gen,
            edgeAction,
            pathsCollector,
            actionsCollector
          )
        }

        PathList.Nil -> {
          // do nothing
          actionsCollector.addProgressedRootMilestone(tip, condition)
        }
      }
    }
    for (required in action.lookaheadRequiringSymbolsList) {
      val newPath = MilestoneGroupPathKt(
        MilestoneKt(required.symbolId, 0, gen),
        PathList.Nil,
        MilestoneGroupKt(required.groupId, gen),
        MilestoneAcceptConditionKt.Always
      )
      pathsCollector.add(newPath)
    }
  }

  fun applyTermAction(
    path: MilestoneGroupPathKt,
    gen: Int,
    action: TermAction,
    pathsCollector: MutableList<MilestoneGroupPathKt>,
    actionsCollector: GenActionsKtBuilder
  ) {
    val tipGen = path.tip.gen

    for (appending in action.appendingMilestoneGroupsList) {
      val newCondition =
        MilestoneAcceptConditionKt.reify(appending.append.acceptCondition, tipGen, gen)
      val condition = MilestoneAcceptConditionKt.conjunct(path.acceptCondition, newCondition)
      val newPath = path.replaceAndAppend(
        appending.replace,
        MilestoneGroupKt(appending.append.groupId, gen),
        condition
      )
      pathsCollector.add(newPath)
    }
    for (startNodeProgress in action.startNodeProgressesList) {
      val replaceGroupId = startNodeProgress.replaceGroupId
      val startNodeProgressCondition = startNodeProgress.acceptCondition
      val replacedTip = MilestoneGroupKt(replaceGroupId, tipGen)
      val newCondition = MilestoneAcceptConditionKt.reify(startNodeProgressCondition, tipGen, gen)
      val condition = MilestoneAcceptConditionKt.conjunct(path.acceptCondition, newCondition)

      when (path.path) {
        is PathList.Cons -> {
          val tipParent = path.path.milestone
          actionsCollector.addProgressedKernelGroup(replacedTip, tipParent.gen, condition)

          val edgeAction = parserData.getTipEdgeProgressAction(tipParent, replaceGroupId)
          actionsCollector.addTipEdgeAction(tipParent, replacedTip, edgeAction)
          progressTip(
            path.first,
            path.path,
            condition,
            gen,
            edgeAction,
            pathsCollector,
            actionsCollector
          )
        }

        PathList.Nil -> {
          actionsCollector.addProgressedRootMilestoneGroup(replacedTip, condition)
        }
      }
    }
    for (required in action.lookaheadRequiringSymbolsList) {
      val newPath = MilestoneGroupPathKt(
        MilestoneKt(required.symbolId, 0, gen),
        PathList.Nil,
        MilestoneGroupKt(required.groupId, gen),
        MilestoneAcceptConditionKt.Always
      )
      pathsCollector.add(newPath)
    }
  }

  fun getProgressConditionOf(
    genActions: GenActionsKt,
    milestone: MilestoneKt
  ): MilestoneAcceptConditionKt {
    val groups = genActions.progressedRootMgroups
      .filterKeys { mgroup -> mgroup.gen == milestone.gen }
      .filterKeys { mgroup ->
        parserData.doesGroupContainMilestone(mgroup.groupId, milestone.symbolId, milestone.pointer)
      }
      .values
    val progressCondition =
      genActions.progressedRootMilestones[milestone] ?: MilestoneAcceptConditionKt.Never

    return MilestoneAcceptConditionKt.disjunctMulti(*(groups + progressCondition).toTypedArray())
  }

  // 누적 기록(seen)에 담아도 되는 감시 심볼들 = lookahead(!A/^A)의 body에서 longest(<A>)의
  // body를 뺀 것. longest 조건도 NotExists로 인코딩되지만 그 창 하한("이번에 취한 매치보다
  // 더 긴 매치")은 checkFromNextGen 한 비트로만 표현되고 첫 evolve에서 소진되므로, 누적
  // 기록을 보게 하면 이미 취한 매치가 다시 witness로 잡혀 longest가 Never로 무너진다.
  // (Scala MilestoneGroupParser.cumulativeRecordSymbols와 동일)
  private val cumulativeRecordSymbols: Set<Int> = run {
    val lookaheadBodies = mutableSetOf<Int>()
    val longestBodies = mutableSetOf<Int>()
    for (symbol in parserData.grammar.symbolsMap.values) {
      if (symbol.hasLookaheadIs()) lookaheadBodies.add(symbol.lookaheadIs.lookahead)
      if (symbol.hasLookaheadExcept()) lookaheadBodies.add(symbol.lookaheadExcept.lookahead)
      if (symbol.hasLongest()) longestBodies.add(symbol.longest.body)
    }
    lookaheadBodies - longestBodies
  }

  // lookahead watcher root 경로는 trackings와 무관하게 살려 둔다.
  // (Scala MilestoneGroupParser.isLookaheadWatcherRoot와 동일)
  //
  // bug-B-partial (2026-07-30): 이 계통(milestone2 / mgroup2-scala / mgroup2-kotlin)은
  // gen>0에 anchor된 guard에 대해 watcher root를 애초에 *만들지* 않는다. 그래서 bug-B
  // 계열은 여기서 부분적으로만 닫힌다 — 누적 채널(seenProgressedRootMilestones)은
  // "늦게 태어난 leaf가 과거 관찰을 읽는" 쪽만 고치고, "관찰할 watcher 자체가 없는" 쪽은
  // 고치지 못한다. 올바른 구현은 mgroup3 / mgroup3-native다 (span-정규화된 cond root
  // starter가 매 경계에서 watcher를 시동한다). 실측 잔여 형태: `nested-stmts`의
  // `{fn();}`, `keyclash`의 `bbx;e` / `bbbx;ee` — naive2는 REJECT, 이 세 계통은 ACCEPT
  // (mgroup3는 전부 REJECT). 문법 형태와 회귀 가드는
  // mgroup3/test/kotlin/com/giyeok/jparser/mgroup3/Mgroup3ParserKnownIssuesTest.kt 의
  // testNegativeLookaheadDroppedWhenBodyOutrunsLookahead 참고.
  private fun isLookaheadWatcherRoot(first: MilestoneKt): Boolean =
    first.pointer == 0 && cumulativeRecordSymbols.contains(first.symbolId)

  // Exists/NotExists(unbounded lookahead)가 볼 수 있는 관찰: 이번 세대의 progress +
  // 이전 세대들의 누적 기록(seen). 근거는 ParsingContextKt.seenProgressedRootMilestones
  // 주석. bounded(OnlyIf/Unless)는 정확한 span만 discharge해야 하므로 이 함수를 쓰지 않는다.
  // (Scala MilestoneGroupParser.observedProgressConditionOf와 동일)
  private fun observedProgressConditionOf(
    genActions: GenActionsKt,
    seenProgressedRootMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt>,
    milestone: MilestoneKt,
  ): MilestoneAcceptConditionKt {
    val progressCondition = getProgressConditionOf(genActions, milestone)
    val seenCondition = seenProgressedRootMilestones[milestone]
    return if (seenCondition == null || seenCondition == progressCondition) progressCondition
    else MilestoneAcceptConditionKt.disjunctMulti(progressCondition, seenCondition)
  }

  // 이번 세대의 root progress 관찰을 누적 기록에 접어 넣은 새 기록. 순서가 load-bearing:
  //  1) 이번 세대의 관찰(progressedRootMilestones + 멤버로 펼친 progressedRootMgroups)을
  //     기존 엔트리와 disjoin해서 병합
  //  2) 그 다음 *모든* non-constant 엔트리를 이번 세대에서 evolve (병합 직후 상태만 읽도록
  //     업데이트를 모아 두었다가 일괄 적용). 관찰 세대 자신의 evolve를 건너뛰면
  //     OnlyIf(sym, g) 형태의 progress 조건이 그 세대에 discharge되지 못해 관찰이 사라진다.
  //     Always/Never는 고정점이라 skip. 결과 Never는 "그 관찰은 불가능했다"이므로 제거한다.
  // (Scala MilestoneGroupParser.updatedSeenProgressedRootMilestones와 동일)
  fun updatedSeenProgressedRootMilestones(
    seenProgressedRootMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt>,
    paths: List<MilestoneGroupPathKt>,
    genActions: GenActionsKt,
  ): Map<MilestoneKt, MilestoneAcceptConditionKt> {
    // 문법에 (longest body가 아닌) lookahead 감시 심볼이 없으면 기록은 언제나 비어 있다.
    if (cumulativeRecordSymbols.isEmpty()) return seenProgressedRootMilestones
    if (seenProgressedRootMilestones.isEmpty() &&
      genActions.progressedRootMilestones.isEmpty() &&
      genActions.progressedRootMgroups.isEmpty()
    ) {
      return seenProgressedRootMilestones
    }

    val merged = HashMap(seenProgressedRootMilestones)

    fun merge(milestone: MilestoneKt, condition: MilestoneAcceptConditionKt) {
      if (condition == MilestoneAcceptConditionKt.Never) return
      if (!cumulativeRecordSymbols.contains(milestone.symbolId)) return
      val existing = merged[milestone]
      merged[milestone] = if (existing == null || existing == condition) condition
      else MilestoneAcceptConditionKt.disjunctMulti(existing, condition)
    }

    for ((milestone, condition) in genActions.progressedRootMilestones) {
      merge(milestone, condition)
    }
    // root mgroup progress는 그 그룹 멤버 milestone들에 대한 관찰이다
    // (getProgressConditionOf의 멤버십 검사와 동일). 조건 leaf는 언제나 pointer 0을 보므로
    // pointer 0만 담는다.
    for ((mgroup, condition) in genActions.progressedRootMgroups) {
      for (template in parserData.milestonesOfGroup(mgroup.groupId)) {
        if (template.pointer == 0) {
          merge(MilestoneKt(template.symbolId, 0, mgroup.gen), condition)
        }
      }
    }

    var updates: MutableMap<MilestoneKt, MilestoneAcceptConditionKt>? = null
    for ((milestone, condition) in merged) {
      if (condition == MilestoneAcceptConditionKt.Always ||
        condition == MilestoneAcceptConditionKt.Never
      ) continue
      val evolved = evolveAcceptCondition(paths, genActions, merged, condition)
      if (evolved != condition) {
        if (updates == null) updates = HashMap()
        updates[milestone] = evolved
      }
    }
    updates?.forEach { (milestone, condition) ->
      if (condition == MilestoneAcceptConditionKt.Never) merged.remove(milestone)
      else merged[milestone] = condition
    }
    return merged
  }

  // visiting: 재귀 도중 이미 진행 중인 root milestone들 — 누적 기록의 조건이 자기 자신을
  // 참조하는 문법에서 무한 재귀를 끊는다 (순환은 witness가 아니라고 보고 leaf를 보류).
  fun evolveAcceptCondition(
    paths: List<MilestoneGroupPathKt>,
    genActions: GenActionsKt,
    seenProgressedRootMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt>,
    condition: MilestoneAcceptConditionKt,
    visiting: Set<MilestoneKt> = emptySet(),
  ): MilestoneAcceptConditionKt {
    fun evolve(
      cond: MilestoneAcceptConditionKt,
      nextVisiting: Set<MilestoneKt> = visiting
    ): MilestoneAcceptConditionKt =
      evolveAcceptCondition(paths, genActions, seenProgressedRootMilestones, cond, nextVisiting)

    return when (condition) {
      MilestoneAcceptConditionKt.Always -> MilestoneAcceptConditionKt.Always
      MilestoneAcceptConditionKt.Never -> MilestoneAcceptConditionKt.Never
      is MilestoneAcceptConditionKt.And -> {
        val subConds = condition.conditions.map { evolve(it) }.toTypedArray()
        MilestoneAcceptConditionKt.conjunct(*subConds)
      }

      is MilestoneAcceptConditionKt.Or -> {
        val subConds = condition.conditions.map { evolve(it) }.toTypedArray()
        MilestoneAcceptConditionKt.disjunctMulti(*subConds)
      }

      is MilestoneAcceptConditionKt.Exists ->
        if (condition.checkFromNextGen) {
          MilestoneAcceptConditionKt.Exists(condition.symbolId, condition.gen, false)
        } else if (visiting.contains(condition.milestone)) {
          condition
        } else {
          val moreTrackingNeeded = paths.any { it.first == condition.milestone }
          val progressCondition =
            observedProgressConditionOf(genActions, seenProgressedRootMilestones, condition.milestone)
          val evolvedCondition = evolve(progressCondition, visiting + condition.milestone)
          if (moreTrackingNeeded) {
            MilestoneAcceptConditionKt.disjunct(condition, evolvedCondition)
          } else {
            evolvedCondition
          }
        }

      is MilestoneAcceptConditionKt.NotExists ->
        if (condition.checkFromNextGen) {
          MilestoneAcceptConditionKt.NotExists(condition.symbolId, condition.gen, false)
        } else if (visiting.contains(condition.milestone)) {
          condition
        } else {
          val moreTrackingNeeded = paths.any { it.first == condition.milestone }
          val progressCondition =
            observedProgressConditionOf(genActions, seenProgressedRootMilestones, condition.milestone)
          val evolvedCondition = evolve(progressCondition, visiting + condition.milestone).negation()
          if (moreTrackingNeeded) {
            MilestoneAcceptConditionKt.conjunct(condition, evolvedCondition)
          } else {
            evolvedCondition
          }
        }

      is MilestoneAcceptConditionKt.OnlyIf -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evolve(progressCondition)
      }

      is MilestoneAcceptConditionKt.Unless -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evolve(progressCondition).negation()
      }
    }
  }

  fun evaluateAcceptCondition(
    genActions: GenActionsKt,
    seenProgressedRootMilestones: Map<MilestoneKt, MilestoneAcceptConditionKt>,
    condition: MilestoneAcceptConditionKt,
    visiting: Set<MilestoneKt> = emptySet(),
  ): Boolean {
    fun evaluate(
      cond: MilestoneAcceptConditionKt,
      nextVisiting: Set<MilestoneKt> = visiting
    ): Boolean =
      evaluateAcceptCondition(genActions, seenProgressedRootMilestones, cond, nextVisiting)

    return when (condition) {
      MilestoneAcceptConditionKt.Always -> true
      MilestoneAcceptConditionKt.Never -> false

      is MilestoneAcceptConditionKt.And -> condition.conditions.all { evaluate(it) }

      is MilestoneAcceptConditionKt.Or -> condition.conditions.any { evaluate(it) }

      is MilestoneAcceptConditionKt.Exists ->
        if (condition.checkFromNextGen) {
          false
        } else if (visiting.contains(condition.milestone)) {
          false
        } else {
          val progressCondition =
            observedProgressConditionOf(genActions, seenProgressedRootMilestones, condition.milestone)
          evaluate(progressCondition, visiting + condition.milestone)
        }

      is MilestoneAcceptConditionKt.NotExists ->
        if (condition.checkFromNextGen) {
          true
        } else if (visiting.contains(condition.milestone)) {
          true
        } else {
          val progressCondition =
            observedProgressConditionOf(genActions, seenProgressedRootMilestones, condition.milestone)
          !evaluate(progressCondition, visiting + condition.milestone)
        }

      is MilestoneAcceptConditionKt.OnlyIf -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evaluate(progressCondition)
      }

      is MilestoneAcceptConditionKt.Unless -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        !evaluate(progressCondition)
      }
    }
  }

  fun collectTrackings(paths: List<MilestoneGroupPathKt>): Set<MilestoneKt> {
    val trackings = mutableSetOf<MilestoneKt>()

    for (path in paths) {
      // tipEdgeRequires
      when (path.path) {
        is PathList.Cons -> {
          val tipParent = path.path.milestone
          val tipRequiredSymbols = parserData.getTipEdgeRequiredSymbols(
            tipParent.symbolId,
            tipParent.pointer,
            path.tip.groupId
          )
          for (symbolId in tipRequiredSymbols) {
            trackings.add(MilestoneKt(symbolId, 0, tipParent.gen))
          }
        }

        PathList.Nil -> {
          // do nothing
        }
      }

      // folded
      fun traverse(tip: MilestoneKt, rest: PathList) {
        when (rest) {
          is PathList.Cons -> {
            val parent = rest.milestone
            for (symbolId in parserData.getMidEdgeRequiredSymbols(
              parent.symbolId,
              parent.pointer,
              tip.symbolId,
              tip.pointer
            )) {
              trackings.add(MilestoneKt(symbolId, 0, parent.gen))
            }
            traverse(parent, rest.parent)
          }

          PathList.Nil -> {
            // do nothing
          }
        }
      }

      when (path.path) {
        is PathList.Cons -> traverse(path.path.milestone, path.path.parent)
        PathList.Nil -> {
          // do nothing
        }
      }

      // acceptConditions.milestones
      trackings.addAll(path.acceptCondition.milestones())
    }

    return trackings.toSet()
  }

  fun expectedTermsOf(ctx: ParsingContextKt): TermSet {
    val termGroups = ctx.paths
      .filter { it.first == initialMilestone }
      .flatMap { path ->
        val termActions = parserData.termActionsByGroupId.getValue(path.tip.groupId)
        termActions.actionsList.map { it.termGroup }
      }
    return TermGroupUtil.merge(termGroups)
  }

  fun parseStep(ctx: ParsingContextKt, input: Char): ParsingContextKt {
    val gen = ctx.gen + 1
    if (verbose) {
      println("  === $gen $input ${ctx.paths.size}")
    }
    if (debugging) {
      debuggingLog?.beginParseStep(gen, input, ctx.paths.size)
    }
    if (ctx.paths.all { it.first != initialMilestone }) {
      throw ParsingErrorKt.UnexpectedInput(gen, expectedTermsOf(ctx), input)
    } else {
      val pathsCollector = mutableListOf<MilestoneGroupPathKt>()
      val actionsCollector = GenActionsKtBuilder()

      val pendedAppendings = mutableMapOf<KernelTemplate, MutableSet<AppendingMilestoneGroup>>()
      val pendedProgressConditions = mutableMapOf<KernelTemplate, MilestoneAcceptConditionKt>()

      for (path in ctx.paths) {
        val termAction = parserData.findTermAction(path.tip.groupId, input)
        if (termAction != null) {
          actionsCollector.addTermActions(path.tip, termAction)
          for (pended in termAction.pendedAcceptConditionKernelsList) {
            pendedAppendings.getOrPut(pended.kernelTemplate) { mutableSetOf() }
              .addAll(pended.appendingsList)

            if (pended.hasFirstKernelProgressCondition()) {
              val firstKernelProgressCondition =
                MilestoneAcceptConditionKt.reify(pended.firstKernelProgressCondition, ctx.gen, gen)
              pendedProgressConditions[pended.kernelTemplate] =
                pendedProgressConditions[pended.kernelTemplate]?.let { existingCondition ->
                  MilestoneAcceptConditionKt.disjunct(
                    existingCondition,
                    firstKernelProgressCondition
                  )
                } ?: firstKernelProgressCondition
            }
          }
          applyTermAction(path, gen, termAction, pathsCollector, actionsCollector)
        }
      }

      for ((first, appendings) in pendedAppendings) {
        val firstMilestone = MilestoneKt(first.symbolId, first.pointer, ctx.gen)
        for (appending in appendings) {
          val condition = MilestoneAcceptConditionKt.reify(appending.acceptCondition, ctx.gen, gen)
          val newPath = MilestoneGroupPathKt(
            firstMilestone,
            PathList.Cons(firstMilestone, PathList.Nil),
            MilestoneGroupKt(appending.groupId, gen),
            condition
          )
          pathsCollector.add(newPath)
        }
      }
      for ((first, progressCondition) in pendedProgressConditions) {
        val firstMilestone = MilestoneKt(first.symbolId, first.pointer, ctx.gen)
        actionsCollector.addProgressedRootMilestone(firstMilestone, progressCondition)
      }

      if (verbose || debugging) {
        val groupSummaries =
          pathsCollector.map { it.tip.groupId }.distinct().sorted().map { groupId ->
            val milestones = parserData.milestonesOfGroup(groupId)
            DebuggingLogBuilder.groupSummary(groupId, milestones)
          }

        if (verbose) {
          for (path in pathsCollector) {
            println(path.prettyString())
          }
          for (groupSummary in groupSummaries) {
            val milestones = groupSummary.milestones
            val milestonesString = milestones.joinToString(", ") { "${it.symbolId} ${it.pointer}" }
            println("${groupSummary.groupId} => (${milestones.size}) $milestonesString")
          }
        }
        if (debugging) {
          debuggingLog?.recordCollectedPaths(pathsCollector, groupSummaries)
        }
      }

      val genActions = actionsCollector.build()

      val newPaths = pathsCollector.toList()

      val newConditions =
        newPaths.map { it.acceptCondition }.toSet() + genActions.progressedKernels.values
      val newConditionUpdates = newConditions.associateWith { condition ->
        evolveAcceptCondition(
          newPaths,
          genActions,
          ctx.seenProgressedRootMilestones,
          condition
        )
      }

      val updatedPaths = newPaths.map { path ->
        val newCondition = newConditionUpdates[path.acceptCondition]!!
        path.copy(acceptCondition = newCondition)
      }.filter { it.acceptCondition != MilestoneAcceptConditionKt.Never }
      if (verbose) {
        println("  ===== condition updated")
        for (path in updatedPaths) {
          println(path.prettyString())
        }
      }
      if (debugging) {
        debuggingLog?.recordConditionUpdated(updatedPaths)
      }

      val trackings = collectTrackings(newPaths)
      val filteredPaths = updatedPaths.filter { path ->
        path.first == initialMilestone || trackings.contains(path.first) ||
          isLookaheadWatcherRoot(path.first)
      }

      if (verbose) {
        println("  ===== filtered (trackings=${trackings.joinToString(", ") { it.prettyString() }})")
        for (path in filteredPaths) {
          println(path.prettyString())
        }
      }
      if (debugging) {
        debuggingLog?.recordFiltered(trackings, filteredPaths)
      }

      // 이번 세대의 관찰은 위의 evolve(per-generation 채널)가 이미 처리했으므로, 누적 기록에
      // 접어 넣는 것은 evolve 이후 — 이 기록은 *다음* 세대부터 유효하다.
      val newSeen =
        updatedSeenProgressedRootMilestones(ctx.seenProgressedRootMilestones, newPaths, genActions)

      return ParsingContextKt(
        gen,
        filteredPaths,
        HistoryEntryList.Cons(HistoryEntryKt(newPaths, genActions), ctx.history),
        newSeen
      )
    }
  }

  fun parse(source: String): ParsingContextKt {
    if (debugging) {
      debuggingLog = DebuggingLogBuilder()
    }
    var ctx = initialCtx
    for (input in source) {
      ctx = parseStep(ctx, input)
    }
    // TODO ctx의 마지막 genActions에서 start symbol이 progress되지 않았으면 unexpected eof
    return ctx
  }

  fun kernelsHistory(context: ParsingContextKt): List<KernelSet> {
    val history = context.history.toList()

    // seenHistory[g] = 세대 g의 evolve/evaluate에서 참조할 누적 기록. parseStep과 동일한
    // 재귀를 history prefix fold로 재구성한다 (Scala mgroup2와 동일):
    // seenHistory[0] = 빈 기록, seenHistory[g + 1] = updated(seenHistory[g], history[g]).
    val seenHistory = ArrayList<Map<MilestoneKt, MilestoneAcceptConditionKt>>(history.size)
    var seenAcc: Map<MilestoneKt, MilestoneAcceptConditionKt> = emptyMap()
    for (entry in history) {
      seenHistory.add(seenAcc)
      seenAcc = updatedSeenProgressedRootMilestones(seenAcc, entry.untrimmedPaths, entry.genActions)
    }

    fun isEventuallyAccepted(
      history: List<HistoryEntryKt>,
      gen: Int,
      condition: MilestoneAcceptConditionKt,
      conditionMemos: List<AcceptConditionMemoize>
    ): Boolean = conditionMemos[gen].useMemo(condition) {
      val entry = history[gen]
      when (condition) {
        MilestoneAcceptConditionKt.Always -> true
        MilestoneAcceptConditionKt.Never -> false
        else -> {
          if (gen + 1 == history.size) {
            evaluateAcceptCondition(entry.genActions, seenHistory[gen], condition)
          } else {
            val evolved = evolveAcceptCondition(
              entry.untrimmedPaths, entry.genActions, seenHistory[gen], condition
            )
            isEventuallyAccepted(history, gen + 1, evolved, conditionMemos)
          }
        }
      }
    }

    fun addKernelsFromTasksSummary(
      builder: KernelSet.Builder,
      history: List<HistoryEntryKt>,
      beginGen: Int,
      gen: Int,
      tasksSummary: TasksSummary2,
      genMap: Map<Int, Int>,
      conditionMemos: List<AcceptConditionMemoize>,
    ) {
      for (pair in tasksSummary.addedKernelsList) {
        val condition = MilestoneAcceptConditionKt.reify(pair.acceptCondition, beginGen, gen)
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          for (kernel in pair.kernelsList) {
            builder.addKernel(kernel, genMap)
          }
        }
      }
      for (kernel in tasksSummary.progressedKernelsList) {
        builder.addKernel(kernel, genMap)
      }
    }

    val conditionMemos = (0..history.size).map { AcceptConditionMemoize() }

    // TODO initialKernels를 별도 처리할 필요가 있나..?
    val initialKernels = KernelSet.Builder()
    addKernelsFromTasksSummary(
      initialKernels,
      history,
      0,
      0,
      parserData.initialTasksSummary,
      mapOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0),
      conditionMemos
    )

    val kernels = history.mapIndexed { gen, entry ->
      val genActions = entry.genActions
      val kernelsBuilder = KernelSet.Builder()

      for ((mgroup, termAction) in genActions.termActions) {
        addKernelsFromTasksSummary(
          kernelsBuilder,
          history,
          gen - 1,
          gen,
          termAction.tasksSummary,
          mapOf(0 to mgroup.gen, 1 to gen - 1, 2 to gen),
          conditionMemos
        )
      }
      for ((edge, edgeAction) in genActions.tipEdgeActions) {
        val endCondition = genActions.progressedKgroups.getValue(Pair(edge.second, edge.first.gen))
        if (isEventuallyAccepted(history, gen, endCondition, conditionMemos)) {
          addKernelsFromTasksSummary(
            kernelsBuilder,
            history,
            edge.first.gen,
            gen,
            edgeAction.tasksSummary,
            mapOf(0 to edge.first.gen, 1 to edge.second.gen, 2 to gen),
            conditionMemos
          )
        }
      }
      for ((edge, edgeAction) in genActions.midEdgeActions) {
        val endCondition = genActions.progressedKernels.getValue(Pair(edge.second, edge.first.gen))
        if (isEventuallyAccepted(history, gen, endCondition, conditionMemos)) {
          addKernelsFromTasksSummary(
            kernelsBuilder,
            history,
            edge.first.gen,
            gen,
            edgeAction.tasksSummary,
            mapOf(0 to edge.first.gen, 1 to edge.second.gen, 2 to gen),
            conditionMemos
          )
        }
      }
      for ((kernel, condition) in genActions.progressedKernels) {
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          val (milestone, parentGen) = kernel
          kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer, parentGen, milestone.gen)
          kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
        }
      }
      for ((milestone, condition) in genActions.progressedRootMilestones) {
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          kernelsBuilder.addKernel(
            milestone.symbolId,
            milestone.pointer,
            milestone.gen,
            milestone.gen
          )
          kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, milestone.gen, gen)
        }
      }
      for ((kgroup, condition) in genActions.progressedKgroups) {
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          val (mgroup, parentGen) = kgroup

          for (milestone in parserData.milestonesOfGroup(mgroup.groupId)) {
            kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer, parentGen, mgroup.gen)
            kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
          }
        }
      }
      for ((mgroup, condition) in genActions.progressedRootMgroups) {
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          for (milestone in parserData.milestonesOfGroup(mgroup.groupId)) {
            kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer, mgroup.gen, mgroup.gen)
            kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, mgroup.gen, gen)
          }
        }
      }
      kernelsBuilder.build()
    }
    return listOf(initialKernels.build()) + kernels.drop(1)
  }
}
