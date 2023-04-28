package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInputByTermGroups}
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, Always, And, Exists, KernelTemplate, Milestone, MilestoneAcceptCondition, MilestonePath, Never, NotExists, OnlyIf, Or, TasksSummary2, Unless}
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.utils.Memoize

import scala.collection.mutable

class MilestoneGroupParser(val parserData: MilestoneGroupParserData) {
  private var verbose = false

  def setVerbose(): MilestoneGroupParser = {
    verbose = true
    this
  }

  val initialMilestone: Milestone = Milestone(parserData.grammar.startSymbol, 0, 0)

  val initialCtx: ParsingContext = ParsingContext(0,
    List(MilestoneGroupPath(initialMilestone, List(), MilestoneGroup(parserData.startGroupId, 0), Always)),
    List())

  def progressTip(path: MilestonePath, gen: Int, action: EdgeAction, actionsCollector: GenActionsBuilder): List[MilestoneGroupPath] = {
    val tip = path.tip

    val appended = action.appendingMilestoneGroups.map { appending =>
      val newCondition = MilestoneAcceptCondition.reify(appending.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      MilestoneGroupPath(path.first, path.path, MilestoneGroup(appending.groupId, gen), condition)
    }
    val reduced = action.startNodeProgress match {
      case Some(startNodeProgressCondition) =>
        val newCondition = MilestoneAcceptCondition.reify(startNodeProgressCondition, tip.gen, gen)
        val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))

        actionsCollector.addProgressedMilestone(tip, condition)

        path.tipParent match {
          case Some(tipParent) =>
            val edgeAction = parserData.midEdgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            actionsCollector.midEdgeActions += ((tipParent -> tip) -> edgeAction)
            actionsCollector.addProgressedMilestoneParentGen(tip, tipParent.gen)
            progressTip(path.pop(condition), gen, edgeAction, actionsCollector)
          case None => List()
        }
      case None => List()
    }
    val lookaheadPaths = action.lookaheadRequiringSymbols.map(req =>
      MilestoneGroupPath(Milestone(req.symbolId, 0, gen), MilestoneGroup(req.groupId, gen)))
    appended ++ reduced ++ lookaheadPaths
  }

  def applyTermAction(path: MilestoneGroupPath, gen: Int, action: TermAction, actionsCollector: GenActionsBuilder): List[MilestoneGroupPath] = {
    val tipGen = path.tip.gen

    val appended = action.appendingMilestoneGroups.map { appending =>
      val newCondition = MilestoneAcceptCondition.reify(appending._2.acceptCondition, tipGen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      path.replaceAndAppend(appending._1, MilestoneGroup(appending._2.groupId, gen), condition)
    }
    val reduced: List[MilestoneGroupPath] = action.startNodeProgress.flatMap { startNodeProgress =>
      val (replaceGroupId, startNodeProgressCondition) = startNodeProgress
      val replacedTip = MilestoneGroup(replaceGroupId, tipGen)
      val newCondition = MilestoneAcceptCondition.reify(startNodeProgressCondition, tipGen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))

      actionsCollector.addProgressedMilestoneGroup(replacedTip, condition)

      path.tipParent match {
        case Some(tipParent) =>
          val edgeAction = parserData.tipEdgeProgressActions(tipParent.kernelTemplate -> replaceGroupId)
          actionsCollector.tipEdgeActions += ((tipParent -> replacedTip) -> edgeAction)
          actionsCollector.addProgressedMilestoneGroupParentGen(replacedTip, tipParent.gen)
          progressTip(MilestonePath(path.first, path.path, condition), gen, edgeAction, actionsCollector)
        case None => List()
      }
    }
    val lookaheadPaths = action.lookaheadRequiringSymbols.map(req =>
      MilestoneGroupPath(Milestone(req.symbolId, 0, gen), MilestoneGroup(req.groupId, gen)))
    appended ++ reduced ++ lookaheadPaths
  }

  def collectTrackings(paths: List[MilestoneGroupPath]): Set[Milestone] =
    paths.flatMap { path =>
      def traverse(tip: Milestone, rest: List[Milestone]): Set[Milestone] =
        rest match {
          case Nil => Set()
          case parent +: next =>
            val requiredSymbols = parserData.midEdgeRequiredSymbols(parent.kernelTemplate -> tip.kernelTemplate)
            requiredSymbols.map(Milestone(_, 0, parent.gen)) ++ traverse(parent, next)
        }

      val tipEdgeRequires: List[Milestone] = path.tipParent match {
        case Some(tipParent) =>
          val requiredSymbols = parserData.tipEdgeRequiredSymbols(tipParent.kernelTemplate -> path.tip.groupId)
          requiredSymbols.map(Milestone(_, 0, tipParent.gen)).toList
        case None => List()
      }

      val folded = if (path.path.isEmpty) Set() else traverse(path.path.head, path.path.tail)
      // TODO path.acceptCondition.milestones가 필요한건가..? edge required symbols로 해결돼야 되지 않나?
      path.acceptCondition.milestones ++ tipEdgeRequires ++ folded
    }.toSet

  def expectedInputsOf(ctx: ParsingContext): Set[Inputs.TermGroupDesc] =
    ctx.paths.filter(_.first == initialMilestone)
      .flatMap(path => parserData.termActions(path.tip.groupId).map(_._1)).toSet

  def getProgressConditionOf(genActions: GenActions, milestone: Milestone): Option[MilestoneAcceptCondition] = {
    val groups = genActions.progressedMgroups
      .filter { pair => pair._1.gen == milestone.gen }
      .filter { pair => parserData.milestoneGroups(pair._1.groupId).contains(milestone.kernelTemplate) }
      .values.toSet
    genActions.progressedMilestones.get(milestone) match {
      case Some(progressCondition) =>
        Some(MilestoneAcceptCondition.disjunct(groups + progressCondition))
      case None =>
        if (groups.isEmpty) None else Some(MilestoneAcceptCondition.disjunct(groups))
    }
  }

  def evolveAcceptCondition(
    paths: List[MilestoneGroupPath],
    genActions: GenActions,
    condition: MilestoneAcceptCondition
  ): MilestoneAcceptCondition = {
    condition match {
      case Always => Always
      case Never => Never
      case And(conditions) =>
        MilestoneAcceptCondition.conjunct(conditions.map(evolveAcceptCondition(paths, genActions, _)).toSet)
      case Or(conditions) =>
        MilestoneAcceptCondition.disjunct(conditions.map(evolveAcceptCondition(paths, genActions, _)).toSet)
      case Exists(milestone, true) =>
        Exists(milestone, checkFromNextGen = false)
      case Exists(milestone, false) =>
        val moreTrackingNeeded = paths.exists(_.first == milestone)
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            val evolvedCondition = evolveAcceptCondition(paths, genActions, progressCondition)
            if (moreTrackingNeeded) {
              MilestoneAcceptCondition.disjunct(Set(condition, evolvedCondition))
            } else {
              evolvedCondition
            }
          case None =>
            if (moreTrackingNeeded) condition else Never
        }
      case NotExists(milestone, true) =>
        NotExists(milestone, checkFromNextGen = false)
      case NotExists(milestone, false) =>
        val moreTrackingNeeded = paths.exists(_.first == milestone)
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            val evolvedCondition = evolveAcceptCondition(paths, genActions, progressCondition).negation
            if (moreTrackingNeeded) {
              MilestoneAcceptCondition.conjunct(Set(condition, evolvedCondition))
            } else {
              evolvedCondition
            }
          case None =>
            if (moreTrackingNeeded) condition else Always
        }
      case OnlyIf(milestone) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            evolveAcceptCondition(paths, genActions, progressCondition)
          case None => Never
        }
      case Unless(milestone) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            evolveAcceptCondition(paths, genActions, progressCondition).negation
          case None => Always
        }
    }
  }

  def evaluateAcceptCondition(
    genActions: GenActions,
    condition: MilestoneAcceptCondition
  ): Boolean = {
    condition match {
      case Always => true
      case Never => false
      case And(conditions) =>
        conditions.forall(evaluateAcceptCondition(genActions, _))
      case Or(conditions) =>
        conditions.exists(evaluateAcceptCondition(genActions, _))
      case Exists(milestone, true) => false
      case Exists(milestone, false) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            evaluateAcceptCondition(genActions, progressCondition)
          case None => false
        }
      case NotExists(milestone, true) => true
      case NotExists(milestone, false) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            !evaluateAcceptCondition(genActions, progressCondition)
          case None => true
        }
      case OnlyIf(milestone) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            evaluateAcceptCondition(genActions, progressCondition)
          case None => false
        }
      case Unless(milestone) =>
        getProgressConditionOf(genActions, milestone) match {
          case Some(progressCondition) =>
            !evaluateAcceptCondition(genActions, progressCondition)
          case None => true
        }
    }
  }

  def parseStep(ctx: ParsingContext, input: Inputs.Input): Either[ParsingError, ParsingContext] = {
    val gen = ctx.gen + 1
    if (verbose) {
      println(s"  === $gen $input ${ctx.paths.size}")
    }
    if (!ctx.paths.exists(_.first == initialMilestone)) {
      // start symbol에 의한 path가 없으면 해당 input이 invalid하다는 뜻
      Left(UnexpectedInputByTermGroups(input, expectedInputsOf(ctx), gen))
    } else {
      val actionsCollector = new GenActionsBuilder()
      val pendedCollection = mutable.Map[KernelTemplate, (List[AppendingMilestoneGroup], Option[AcceptConditionTemplate])]()

      val termActionApplied = ctx.paths.flatMap { path =>
        val termAction = parserData.termActions(path.tip.groupId)
          .find(_._1.contains(input))
        termAction match {
          case Some((_, action)) =>
            actionsCollector.termActions += (path.tip -> action)
            pendedCollection ++= action.pendedAcceptConditionKernels
            applyTermAction(path, gen, action, actionsCollector)
          case None => List()
        }
      }
      val pended = pendedCollection.flatMap { case (first, (appendings, progressCondition)) =>
        val firstMilestone = Milestone(first, ctx.gen)
        progressCondition match {
          case Some(progressCondition) =>
            actionsCollector.addProgressedMilestone(firstMilestone, MilestoneAcceptCondition.reify(progressCondition, ctx.gen, gen))
          case None => // do nothing
        }
        appendings.map { appending =>
          val condition = MilestoneAcceptCondition.reify(appending.acceptCondition, ctx.gen, gen)
          MilestoneGroupPath(firstMilestone, List(firstMilestone), MilestoneGroup(appending.groupId, gen), condition)
        }
      }

      val newPaths = termActionApplied ++ pended
      if (verbose) {
        newPaths.foreach(path => println(path.prettyString))
        newPaths.map(_.tip.groupId).distinct.sorted.foreach { groupId =>
          val milestones = parserData.milestoneGroups(groupId)
          println(s"$groupId => (${milestones.size}) ${milestones.toList.sorted}")
        }
      }

      val genActions = actionsCollector.build()

      if (verbose) {
        println("  ===== genActions")
        genActions.tipEdgeActions.map(_._1).foreach(println)
        genActions.midEdgeActions.map(_._1).foreach(println)
        genActions.progressedMilestones.foreach(println)
        genActions.progressedMgroups.foreach(println)
      }

      val newConditions = (newPaths.map(_.acceptCondition) ++ genActions.progressedMilestones.values).distinct
      val newConditionUpdates = newConditions
        .map(cond => cond -> evolveAcceptCondition(newPaths, genActions, cond)).toMap

      // newPaths와 수행된 액션을 바탕으로 condition evaluate
      val newPathsUpdated = newPaths
        .map(path => path.copy(acceptCondition = newConditionUpdates(path.acceptCondition)))
        .filter(_.acceptCondition != Never)
      if (verbose) {
        println("  ===== condition updated")
        newPathsUpdated.foreach(path => println(path.prettyString))
      }

      // first가 (start symbol, 0, 0)이거나 현재 존재하는 엣지의 trackingMilestones인 경우만 제외하고 모두 제거
      // TODO 원래는 collectTrackings(newPaths)를 collectTrackings(newPathsUpdated)로 변경해도 괜찮은지 확인 - 괜찮으면 milestone2 파서도 함께 변경
      val trackings = collectTrackings(newPaths)
      val newPathsFiltered = newPathsUpdated
        .filter(path => path.first == initialMilestone || trackings.contains(path.first))

      if (verbose) {
        println(s"  ===== filtered (trackings=$trackings)")
        newPathsFiltered.foreach(path => println(path.prettyString))
      }

      Right(ParsingContext(gen, newPathsFiltered, HistoryEntry(newPaths, genActions) +: ctx.history))
    }
  }

  def parse(inputSeq: Seq[Inputs.Input]): Either[ParsingError, ParsingContext] = {
    if (verbose) {
      println("  === initial")
      initialCtx.paths.foreach(t => println(s"${t.prettyString}"))
      initialCtx.paths.map(_.tip.groupId).distinct.sorted.foreach { groupId =>
        println(s"$groupId => ${parserData.milestoneGroups(groupId)}")
      }
    }
    inputSeq.foldLeft[Either[ParsingError, ParsingContext]](Right(initialCtx)) { (m, nextInput) =>
      m match {
        case Right(currCtx) =>
          parseStep(currCtx, nextInput)
        case error => error
      }
    }
  }

  def parseOrThrow(inputSeq: Seq[Inputs.Input]): ParsingContext = {
    parse(inputSeq) match {
      case Right(result) => result
      case Left(parseError) => throw parseError
    }
  }

  def kernelsHistory(parsingContext: ParsingContext): Vector[Set[Kernel]] = {
    def mapGen(kernel: Kernel, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer, genMap(kernel.beginGen), genMap(kernel.endGen))

    def isEventuallyAccepted(
      history: Vector[HistoryEntry],
      gen: Int,
      condition: MilestoneAcceptCondition,
      conditionMemos: Vector[Memoize[MilestoneAcceptCondition, Boolean]],
    ): Boolean = conditionMemos(gen)(condition) {
      val entry = history(gen)
      condition match {
        case Always => true
        case Never => false
        case _ =>
          val result = if (gen + 1 == history.length) {
            evaluateAcceptCondition(entry.genActions, condition)
          } else {
            val evolved = evolveAcceptCondition(entry.untrimmedPaths, entry.genActions, condition)
            isEventuallyAccepted(history, gen + 1, evolved, conditionMemos)
          }
          // println(s"isEventuallyAccepted $gen $condition => $result")
          result
      }
    }

    def kernelsFrom(
      history: Vector[HistoryEntry],
      beginGen: Int,
      gen: Int,
      tasksSummary: TasksSummary2,
      genMap: Map[Int, Int],
      conditionMemos: Vector[Memoize[MilestoneAcceptCondition, Boolean]],
    ): Set[Kernel] = {
      val collector = mutable.Set[Kernel]()
      addKernelsFrom(collector, history, beginGen, gen, tasksSummary, genMap, conditionMemos)
      collector.toSet
    }

    def addKernelsFrom(
      kernelsCollector: mutable.Set[Kernel],
      history: Vector[HistoryEntry],
      beginGen: Int,
      gen: Int,
      tasksSummary: TasksSummary2,
      genMap: Map[Int, Int],
      conditionMemos: Vector[Memoize[MilestoneAcceptCondition, Boolean]],
    ): Unit = {
      tasksSummary.addedKernels.foreach { pair =>
        val condition = MilestoneAcceptCondition.reify(pair._1, beginGen, gen)
        if (isEventuallyAccepted(history, gen, condition, conditionMemos)) {
          pair._2.foreach { added =>
            kernelsCollector += mapGen(added, genMap)
          }
        }
      }
      tasksSummary.progressedKernels.foreach { kernel =>
        kernelsCollector += mapGen(kernel, genMap)
      }
    }

    // TODO initialHistoryEntry의 progressedMilestones와 progressedMilestoneParentGens 추가
    val initialHistoryEntry = HistoryEntry(initialCtx.paths, GenActions(List(), List(), List(), Map(), Map(), Map(), Map()))
    val history = (initialHistoryEntry +: parsingContext.history.reverse).toVector

    val conditionMemos = (0 until history.length).map { _ =>
      Memoize[MilestoneAcceptCondition, Boolean]()
    }.toVector

    val initialKernels = kernelsFrom(history, 0, 0, parserData.initialTasksSummary, Map(-1 -> 0, 0 -> 0, 1 -> 0, 2 -> 0), conditionMemos)

    val kernelsHistory = history.zipWithIndex.drop(1).map { case (entry, gen) =>
      val genActions = entry.genActions

      val kernels = mutable.Set[Kernel]()

      // kernels by term actions
      genActions.termActions.foreach { case (milestone, termAction) =>
        addKernelsFrom(
          kernels,
          history,
          gen - 1,
          gen,
          termAction.tasksSummary,
          Map(0 -> milestone.gen, 1 -> (gen - 1), 2 -> gen),
          conditionMemos)
      }
      genActions.tipEdgeActions.foreach { case ((start, end), edgeAction) =>
        if (isEventuallyAccepted(history, gen, genActions.progressedMgroups(end), conditionMemos)) {
          addKernelsFrom(
            kernels,
            history,
            start.gen,
            gen,
            edgeAction.tasksSummary,
            Map(0 -> start.gen, 1 -> end.gen, 2 -> gen),
            conditionMemos)
        }
      }
      genActions.midEdgeActions.foreach { case ((start, end), edgeAction) =>
        if (isEventuallyAccepted(history, gen, genActions.progressedMilestones(end), conditionMemos)) {
          addKernelsFrom(
            kernels,
            history,
            start.gen,
            gen,
            edgeAction.tasksSummary,
            Map(0 -> start.gen, 1 -> end.gen, 2 -> gen),
            conditionMemos)
        }
      }
      genActions.progressedMilestones.foreach {
        case (milestone, condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          val parentGens = genActions.progressedMilestoneParentGens.getOrElse(milestone, Set(milestone.gen))
          parentGens.foreach { parentGen =>
            kernels += Kernel(milestone.symbolId, milestone.pointer, parentGen, milestone.gen)
            kernels += Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
          }
        case _ => // do nothing
      }
      genActions.progressedMgroups.foreach {
        case (mgroup, condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          val parentGens = genActions.progressedMgroupParentGens.getOrElse(mgroup, Set(mgroup.gen))
          parserData.milestoneGroups(mgroup.groupId).foreach { milestone =>
            parentGens.foreach { parentGen =>
              kernels += Kernel(milestone.symbolId, milestone.pointer, parentGen, mgroup.gen)
              kernels += Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
            }
          }
        case _ => // do nothing
      }
      kernels.toSet
    }
    (initialKernels +: kernelsHistory)
  }
}
