package com.giyeok.jparser.ktparser.mgroup2

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.ParsingErrorKt
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto.*
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto.*

class MilestoneGroupParserKt(val parserData: MilestoneGroupParserData) {
  private var verbose: Boolean = false
  private val termActionsByGroupId = parserData.termActionsList.associateBy { it.groupId }

  fun setVerbose(): MilestoneGroupParserKt {
    verbose = true
    return this
  }

  val initialMilestone: MilestoneKt = MilestoneKt(parserData.grammar.startSymbol, 0, 0)

  val initialPath = MilestoneGroupPathKt(
    initialMilestone,
    PathList.Nil,
    MilestoneGroupKt(parserData.startGroupId, 0),
    MilestoneAcceptConditionKt.Always
  )

  val initialCtx = ParsingContextKt(
    0,
    listOf(initialPath),
    HistoryEntryList.Nil(HistoryEntryKt(listOf(initialPath), GenActionsKt.empty)),
  )

  fun findTermAction(groupId: Int, input: Char): TermAction? =
    termActionsByGroupId[groupId]!!.actionsList.find {
      TermGroupUtil.isMatch(it.termGroup, input)
    }?.termAction

  fun getTipEdgeProgressAction(start: MilestoneKt, end: Int): EdgeAction {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return parserData.tipEdgeActionsList.find {
      it.start.symbolId == start.symbolId && it.start.pointer == start.pointer &&
        it.end == end
    }!!.edgeAction
  }

  fun getMidEdgeProgressAction(start: MilestoneKt, end: MilestoneKt): EdgeAction {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return parserData.midEdgeActionsList.find {
      it.start.symbolId == start.symbolId && it.start.pointer == start.pointer &&
        it.end.symbolId == end.symbolId && it.end.pointer == end.pointer
    }!!.edgeAction
  }

  fun getTipEdgeRequiredSymbols(
    startSymbolId: Int,
    startPointer: Int,
    endGroupId: Int
  ): Collection<Int> {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return parserData.tipEdgeRequiredSymbolsList.find {
      it.start.symbolId == startSymbolId && it.start.pointer == startPointer && it.end == endGroupId
    }!!.requiredSymbolIdsList
  }

  fun getMidEdgeRequiredSymbols(
    startSymbolId: Int,
    startPointer: Int,
    endSymbolId: Int,
    endPointer: Int
  ): Collection<Int> {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return parserData.midEdgeRequiredSymbolsList.find {
      it.start.symbolId == startSymbolId && it.start.pointer == startPointer &&
        it.end.symbolId == endSymbolId && it.end.pointer == endPointer
    }!!.requiredSymbolIdsList
  }

  fun milestonesOfGroup(groupId: Int): Collection<KernelTemplate> {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    return parserData.milestoneGroupsList.find { it.groupId == groupId }!!.milestonesList
  }

  fun doesGroupContainMilestone(groupId: Int, symbolId: Int, pointer: Int): Boolean {
    // TODO 미리 인덱스 만들어서 성능 개선. 단 메모리 카피는 피해서
    val milestones = milestonesOfGroup(groupId)
    return milestones.any { it.symbolId == symbolId && it.pointer == pointer }
  }


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

    action.appendingMilestoneGroupsList.forEach { appending ->
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

      actionsCollector.addProgressedMilestone(tip, condition)

      when (path.parent) {
        is PathList.Cons -> {
          val tipParent = path.parent.milestone
          val edgeAction = getMidEdgeProgressAction(tipParent, tip)
          actionsCollector.addMidEdgeAction(tipParent, tip, edgeAction)
          actionsCollector.addProgressedMilestoneParentGen(tip, tipParent.gen)
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
        }
      }
    }
    action.lookaheadRequiringSymbolsList.forEach { required ->
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

    action.appendingMilestoneGroupsList.forEach { appending ->
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
    action.startNodeProgressesList.forEach { startNodeProgress ->
      val replaceGroupId = startNodeProgress.replaceGroupId
      val startNodeProgressCondition = startNodeProgress.acceptCondition
      val replacedTip = MilestoneGroupKt(replaceGroupId, tipGen)
      val newCondition = MilestoneAcceptConditionKt.reify(startNodeProgressCondition, tipGen, gen)
      val condition = MilestoneAcceptConditionKt.conjunct(path.acceptCondition, newCondition)

      actionsCollector.addProgressedMilestoneGroup(replacedTip, condition)

      when (path.path) {
        is PathList.Cons -> {
          val tipParent = path.path.milestone
          val edgeAction = getTipEdgeProgressAction(tipParent, replaceGroupId)
          actionsCollector.addTipEdgeAction(tipParent, replacedTip, edgeAction)
          actionsCollector.addProgressedMilestoneGroupParentGen(replacedTip, tipParent.gen)
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
          // do nothing
        }
      }
    }
    action.lookaheadRequiringSymbolsList.forEach { required ->
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
  ): MilestoneAcceptConditionKt? {
    val groups = genActions.progressedMgroups
      .filterKeys { mgroup -> mgroup.gen == milestone.gen }
      .filterKeys { mgroup ->
        doesGroupContainMilestone(mgroup.groupId, milestone.symbolId, milestone.pointer)
      }
      .values
    val progressCondition = genActions.progressedMilestones[milestone]
    return if (progressCondition != null) {
      MilestoneAcceptConditionKt.disjunct(*(groups + progressCondition).toTypedArray())
    } else {
      if (groups.isEmpty()) null else MilestoneAcceptConditionKt.disjunct(*groups.toTypedArray())
    }
  }

  fun evolveAcceptCondition(
    paths: List<MilestoneGroupPathKt>,
    genActions: GenActionsKt,
    condition: MilestoneAcceptConditionKt
  ): MilestoneAcceptConditionKt =
    when (condition) {
      MilestoneAcceptConditionKt.Always -> MilestoneAcceptConditionKt.Always
      MilestoneAcceptConditionKt.Never -> MilestoneAcceptConditionKt.Never
      is MilestoneAcceptConditionKt.And -> {
        val subConds = condition.conditions.map {
          evolveAcceptCondition(paths, genActions, it)
        }.toTypedArray()
        MilestoneAcceptConditionKt.conjunct(*subConds)
      }

      is MilestoneAcceptConditionKt.Or -> {
        val subConds = condition.conditions.map {
          evolveAcceptCondition(paths, genActions, it)
        }.toTypedArray()
        MilestoneAcceptConditionKt.disjunct(*subConds)
      }

      is MilestoneAcceptConditionKt.Exists ->
        if (condition.checkFromNextGen) {
          MilestoneAcceptConditionKt.Exists(condition.milestone, false)
        } else {
          val moreTrackingNeeded = paths.any { it.first == condition.milestone }
          val progressCondition = getProgressConditionOf(genActions, condition.milestone)
          if (progressCondition != null) {
            val evolvedCondition = evolveAcceptCondition(paths, genActions, progressCondition)
            if (moreTrackingNeeded) {
              MilestoneAcceptConditionKt.disjunct(condition, evolvedCondition)
            } else {
              evolvedCondition
            }
          } else {
            if (moreTrackingNeeded) condition else MilestoneAcceptConditionKt.Never
          }
        }

      is MilestoneAcceptConditionKt.NotExists ->
        if (condition.checkFromNextGen) {
          MilestoneAcceptConditionKt.NotExists(condition.milestone, false)
        } else {
          val moreTrackingNeeded = paths.any { it.first == condition.milestone }
          val progressCondition = getProgressConditionOf(genActions, condition.milestone)
          if (progressCondition != null) {
            val evolvedCondition =
              evolveAcceptCondition(paths, genActions, progressCondition).negation()
            if (moreTrackingNeeded) {
              MilestoneAcceptConditionKt.conjunct(condition, evolvedCondition)
            } else {
              evolvedCondition
            }
          } else {
            if (moreTrackingNeeded) condition else MilestoneAcceptConditionKt.Always
          }
        }

      is MilestoneAcceptConditionKt.OnlyIf -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        if (progressCondition != null) {
          evolveAcceptCondition(paths, genActions, progressCondition)
        } else {
          MilestoneAcceptConditionKt.Never
        }
      }

      is MilestoneAcceptConditionKt.Unless -> {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        if (progressCondition != null) {
          evolveAcceptCondition(paths, genActions, progressCondition).negation()
        } else {
          MilestoneAcceptConditionKt.Always
        }
      }
    }

  fun evaluateAcceptCondition(
    genActions: GenActionsKt,
    condition: MilestoneAcceptConditionKt
  ): Boolean = when (condition) {
    MilestoneAcceptConditionKt.Always -> true
    MilestoneAcceptConditionKt.Never -> false

    is MilestoneAcceptConditionKt.And ->
      condition.conditions.all { evaluateAcceptCondition(genActions, it) }

    is MilestoneAcceptConditionKt.Or ->
      condition.conditions.any { evaluateAcceptCondition(genActions, it) }

    is MilestoneAcceptConditionKt.Exists ->
      if (condition.checkFromNextGen) {
        false
      } else {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        if (progressCondition != null) {
          evaluateAcceptCondition(genActions, progressCondition)
        } else {
          false
        }
      }

    is MilestoneAcceptConditionKt.NotExists ->
      if (condition.checkFromNextGen) {
        true
      } else {
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        if (progressCondition != null) {
          !evaluateAcceptCondition(genActions, progressCondition)
        } else {
          true
        }
      }

    is MilestoneAcceptConditionKt.OnlyIf -> {
      val progressCondition = getProgressConditionOf(genActions, condition.milestone)
      if (progressCondition != null) {
        evaluateAcceptCondition(genActions, progressCondition)
      } else {
        false
      }
    }

    is MilestoneAcceptConditionKt.Unless -> {
      val progressCondition = getProgressConditionOf(genActions, condition.milestone)
      if (progressCondition != null) {
        !evaluateAcceptCondition(genActions, progressCondition)
      } else {
        true
      }
    }
  }

  fun collectTrackings(paths: List<MilestoneGroupPathKt>): Set<MilestoneKt> {
    val trackings = mutableSetOf<MilestoneKt>()

    paths.forEach { path ->
      when (path.path) {
        is PathList.Cons -> {
          getTipEdgeRequiredSymbols(
            path.path.milestone.symbolId,
            path.path.milestone.pointer,
            path.tip.groupId
          ).forEach { symbolId ->
            trackings.add(MilestoneKt(symbolId, 0, path.path.milestone.gen))
          }
        }

        PathList.Nil -> {
          // do nothing
        }
      }

      fun traverse(tip: MilestoneKt, rest: PathList) {
        when (rest) {
          is PathList.Cons -> {
            getMidEdgeRequiredSymbols(
              rest.milestone.symbolId,
              rest.milestone.pointer,
              tip.symbolId,
              tip.pointer
            ).forEach { symbolId ->
              trackings.add(MilestoneKt(symbolId, 0, rest.milestone.gen))
            }
            traverse(rest.milestone, rest.parent)
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

      // TODO path.acceptCondition.milestones
    }

    return trackings.toSet()
  }

  fun parseStep(ctx: ParsingContextKt, input: Char): ParsingContextKt {
    val gen = ctx.gen + 1
    if (verbose) {
      println("  === $gen $input ${ctx.paths.size}")
    }
    val pathsCollector = mutableListOf<MilestoneGroupPathKt>()
    val actionsCollector = GenActionsKtBuilder()
    val pendedCollection =
      mutableMapOf<KernelTemplate, Pair<List<AppendingMilestoneGroup>, AcceptConditionTemplate?>>()

    ctx.paths.forEach { path ->
      val termAction = findTermAction(path.tip.groupId, input)
      termAction?.let {
        actionsCollector.addTermActions(path.tip, termAction)
        termAction.pendedAcceptConditionKernelsList.forEach { pended ->
          val firstKernelProgressCondition =
            if (pended.hasFirstKernelProgressCondition()) pended.firstKernelProgressCondition else null
          pendedCollection[pended.kernelTemplate] =
            Pair(pended.appendingsList, firstKernelProgressCondition)
        }
        applyTermAction(path, gen, termAction, pathsCollector, actionsCollector)
      }
    }
    pendedCollection.forEach { (first, pair) ->
      val (appendings, progressCondition) = pair
      val firstMilestone = MilestoneKt(first.symbolId, first.pointer, ctx.gen)
      if (progressCondition != null) {
        actionsCollector.addProgressedMilestone(
          firstMilestone,
          MilestoneAcceptConditionKt.reify(progressCondition, ctx.gen, gen)
        )
      }
      appendings.forEach { appending ->
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

    if (verbose) {
      pathsCollector.forEach { path ->
        println(path)
      }
    }

    if (pathsCollector.all { it.first != initialMilestone }) {
      throw ParsingErrorKt()
    } else {
      val genActions = actionsCollector.build()

      val newPaths = pathsCollector.toList()

      val newConditions =
        newPaths.map { it.acceptCondition }.toSet() + genActions.progressedMilestones.values
      val newConditionUpdates = newConditions.associateWith { condition ->
        evolveAcceptCondition(
          newPaths,
          genActions,
          condition
        )
      }

      val updatedPaths = newPaths.map { path ->
        val newCondition = newConditionUpdates[path.acceptCondition]!!
        path.copy(acceptCondition = newCondition)
      }.filter { it.acceptCondition != MilestoneAcceptConditionKt.Never }

      val trackings = collectTrackings(updatedPaths)
      val filteredPaths = updatedPaths.filter { path ->
        path.first == initialMilestone || trackings.contains(path.first)
      }

      return ParsingContextKt(
        gen,
        filteredPaths,
        HistoryEntryList.Cons(HistoryEntryKt(newPaths, genActions), ctx.history)
      )
    }
  }

  fun parse(source: String): ParsingContextKt {
    var ctx = initialCtx
    for (input in source) {
      ctx = parseStep(ctx, input)
    }
    return ctx
  }

  fun kernelsHistory(context: ParsingContextKt): List<KernelSet> {
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
            evaluateAcceptCondition(entry.genActions, condition)
          } else {
            val evolved = evolveAcceptCondition(entry.untrimmedPaths, entry.genActions, condition)
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
      tasksSummary.addedKernelsList.forEach { pair ->
        val condition = MilestoneAcceptConditionKt.reify(pair.acceptCondition, beginGen, gen)
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          pair.kernelsList.forEach { kernel ->
            builder.addKernel(kernel, genMap)
          }
        }
      }
      tasksSummary.progressedKernelsList.forEach { kernel ->
        builder.addKernel(kernel, genMap)
      }
    }

    val history = context.history.toList()
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

      genActions.termActions.forEach { (mgroup, termAction) ->
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
      genActions.tipEdgeActions.forEach { (edge, edgeAction) ->
        val endCondition = genActions.progressedMgroups.getValue(edge.second)
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
      genActions.midEdgeActions.forEach { (edge, edgeAction) ->
        val endCondition = genActions.progressedMilestones.getValue(edge.second)
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
      genActions.progressedMilestones.forEach { (milestone, condition) ->
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          // TODO elvis op 부분이 필요할까..? 이거 왜 넣었지?
          val parentGens =
            genActions.progressedMilestoneParentGens[milestone] ?: setOf(milestone.gen)
          parentGens.forEach { parentGen ->
            kernelsBuilder.addKernel(
              milestone.symbolId,
              milestone.pointer,
              parentGen,
              milestone.gen
            )
            kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
          }
        }
      }
      genActions.progressedMgroups.forEach { (mgroup, condition) ->
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          // TODO ditto
          val parentGens = genActions.progressedMgroupParentGens[mgroup] ?: setOf(mgroup.gen)

          milestonesOfGroup(mgroup.groupId).forEach { milestone ->
            parentGens.forEach { parentGen ->
              kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer, parentGen, mgroup.gen)
              kernelsBuilder.addKernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
            }
          }
        }
      }
      kernelsBuilder.build()
    }
    return listOf(initialKernels.build()) + kernels.drop(1)
  }
}
