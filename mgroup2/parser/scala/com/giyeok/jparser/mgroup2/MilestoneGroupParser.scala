package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.{Inputs, NGrammar}
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

  // 초기 컨텍스트의 조건 템플릿들이 감시하는 심볼들의 루트 경로.
  // gen 0에서 zero-width로 progress된 lookahead/longest 조건의 추적 루트가 초기
  // 컨텍스트부터 존재해야 gen 0 시점의 조건 평가가 Never/Always로 오판되지 않는다.
  // (milestone2 MilestoneParser.initialConditionMilestones와 동일한 이유)
  private val initialConditionPaths: List[MilestoneGroupPath] =
    parserData.initialTasksSummary.addedKernels.keySet.toList
      .map(MilestoneAcceptCondition.reify(_, 0, 0))
      .flatMap(_.milestones)
      .distinct
      .filter(_ != initialMilestone)
      .flatMap { milestone =>
        parserData.milestoneGroups
          .find(_._2 == Set(KernelTemplate(milestone.symbolId, milestone.pointer)))
          .map { case (groupId, _) => MilestoneGroupPath(milestone, MilestoneGroup(groupId, 0)) }
      }

  val initialCtx: ParsingContext = ParsingContext(0,
    MilestoneGroupPath(initialMilestone, List(), MilestoneGroup(parserData.startGroupId, 0), Always) +: initialConditionPaths,
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

        path.tipParent match {
          case Some(tipParent) =>
            actionsCollector.addProgressedKernel(tip, tipParent.gen, condition)

            val edgeAction = parserData.midEdgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            actionsCollector.midEdgeActions += ((tipParent -> tip) -> edgeAction)
            progressTip(path.pop(condition), gen, edgeAction, actionsCollector)
          case None =>
            actionsCollector.addProgressedRootMilestone(tip, condition)

            List()
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

      path.tipParent match {
        case Some(tipParent) =>
          actionsCollector.addProgressedKernelGroup(replacedTip, tipParent.gen, condition)
          val edgeAction = parserData.tipEdgeProgressActions(tipParent.kernelTemplate -> replaceGroupId)
          actionsCollector.tipEdgeActions += ((tipParent -> replacedTip) -> edgeAction)
          progressTip(MilestonePath(path.first, path.path, condition), gen, edgeAction, actionsCollector)
        case None =>
          actionsCollector.addProgressedRootMilestoneGroup(replacedTip, condition)
          List()
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

  def getProgressConditionOf(genActions: GenActions, milestone: Milestone): MilestoneAcceptCondition = {
    val groups = genActions.progressedRootMgroups
      .filter { pair => pair._1.gen == milestone.gen }
      .filter { pair => parserData.milestoneGroups(pair._1.groupId).contains(milestone.kernelTemplate) }
      .values.toSet
    val progressCondition = genActions.progressedRootMilestones.getOrElse(milestone, Never)

    MilestoneAcceptCondition.disjunct(groups + progressCondition)
  }

  // 누적 기록(seen)에 담아도 되는 감시 심볼들 = lookahead(!A/^A)의 body에서 longest(<A>)의
  // body를 뺀 것. longest 조건도 NotExists로 인코딩되지만 그 창 하한("이번에 취한 매치보다
  // 더 긴 매치")은 checkFromNextGen 한 비트로만 표현되고 첫 evolve에서 소진되므로, 누적
  // 기록을 보게 하면 이미 취한 매치가 다시 witness로 잡혀 longest가 Never로 무너진다.
  // (자세한 근거와 한계는 milestone2 MilestoneParser.cumulativeRecordSymbols 주석)
  private val cumulativeRecordSymbols: Set[Int] = {
    val lookaheadBodies = parserData.grammar.nsymbols.values.collect {
      case NGrammar.NLookaheadIs(_, _, _, lookahead) => lookahead
      case NGrammar.NLookaheadExcept(_, _, _, lookahead) => lookahead
    }.toSet
    val longestBodies = parserData.grammar.nsymbols.values.collect {
      case NGrammar.NLongest(_, _, body) => body
    }.toSet
    lookaheadBodies -- longestBodies
  }

  // lookahead watcher root 경로는 trackings와 무관하게 살려 둔다.
  // (근거는 milestone2 MilestoneParser.isLookaheadWatcherRoot 주석 — 생성기의 edge
  //  requiredSymbols가 조건이 그 엣지에서 물질화될 때만 감시 심볼을 요구해서, body가
  //  operand보다 길게 뻗으면 watcher가 완성 전에 trimming으로 죽는다)
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
  private def isLookaheadWatcherRoot(first: Milestone): Boolean =
    first.pointer == 0 && cumulativeRecordSymbols.contains(first.symbolId)

  // Exists/NotExists(unbounded lookahead)가 볼 수 있는 관찰: 이번 세대의 progress +
  // 이전 세대들의 누적 기록(seen). 근거는 ParsingContext의 seenProgressedRootMilestones
  // 주석. bounded(OnlyIf/Unless)는 정확한 span만 discharge해야 하므로 이 함수를 쓰지 않는다.
  private def observedProgressConditionOf(
    genActions: GenActions,
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    milestone: Milestone,
  ): MilestoneAcceptCondition =
    MilestoneAcceptCondition.disjunct(Set(
      getProgressConditionOf(genActions, milestone),
      seenProgressedRootMilestones.getOrElse(milestone, Never)))

  // 이번 세대의 root progress 관찰을 누적 기록에 접어 넣은 새 기록.
  // (milestone2 MilestoneParser.updatedSeenProgressedRootMilestones와 동일한 규칙 —
  //  merge 후 그 세대에서 전체 evolve; 순서가 load-bearing한 이유는 그 쪽 주석 참고)
  def updatedSeenProgressedRootMilestones(
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    paths: List[MilestoneGroupPath],
    genActions: GenActions,
  ): Map[Milestone, MilestoneAcceptCondition] = {
    // 문법에 (longest body가 아닌) lookahead 감시 심볼이 없으면 기록은 언제나 비어 있다.
    if (cumulativeRecordSymbols.isEmpty) seenProgressedRootMilestones else {
    def merge(
      acc: Map[Milestone, MilestoneAcceptCondition],
      milestone: Milestone,
      condition: MilestoneAcceptCondition,
    ): Map[Milestone, MilestoneAcceptCondition] =
      if (condition == Never || !cumulativeRecordSymbols.contains(milestone.symbolId)) acc
      else acc + (milestone -> (acc.get(milestone) match {
        case Some(existing) => MilestoneAcceptCondition.disjunct(Set(existing, condition))
        case None => condition
      }))

    val mergedMilestones = genActions.progressedRootMilestones.foldLeft(seenProgressedRootMilestones) {
      case (acc, (milestone, condition)) => merge(acc, milestone, condition)
    }
    // root mgroup progress는 그 그룹의 멤버 milestone들에 대한 관찰이다
    // (getProgressConditionOf의 멤버십 검사와 동일). 조건 leaf는 언제나 pointer 0을
    // 보므로(Exists/NotExists.milestone) pointer 0만 담는다.
    val merged = genActions.progressedRootMgroups.foldLeft(mergedMilestones) {
      case (acc, (mgroup, condition)) =>
        parserData.milestoneGroups(mgroup.groupId).foldLeft(acc) { (acc2, template) =>
          if (template.pointer != 0) acc2
          else merge(acc2, Milestone(template.symbolId, 0, mgroup.gen), condition)
        }
    }
    if (merged.isEmpty) merged else {
      val updates = merged.collect {
        case (milestone, condition) if condition != Always && condition != Never =>
          milestone -> evolveAcceptCondition(paths, genActions, merged, condition)
      }
      updates.foldLeft(merged) { case (acc, (milestone, condition)) =>
        if (condition == Never) acc - milestone else acc + (milestone -> condition)
      }
    }
    }
  }

  // visiting: 재귀 도중 이미 진행 중인 root milestone들 — 누적 기록의 조건이 자기 자신을
  // 참조하는 문법에서 무한 재귀를 끊는다 (순환은 witness가 아니라고 보고 leaf를 보류).
  def evolveAcceptCondition(
    paths: List[MilestoneGroupPath],
    genActions: GenActions,
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    condition: MilestoneAcceptCondition,
    visiting: Set[Milestone] = Set(),
  ): MilestoneAcceptCondition = {
    def evolve(cond: MilestoneAcceptCondition, nextVisiting: Set[Milestone] = visiting): MilestoneAcceptCondition =
      evolveAcceptCondition(paths, genActions, seenProgressedRootMilestones, cond, nextVisiting)

    condition match {
      case Always => Always
      case Never => Never
      case And(conditions) =>
        MilestoneAcceptCondition.conjunct(conditions.map(evolve(_)).toSet)
      case Or(conditions) =>
        MilestoneAcceptCondition.disjunct(conditions.map(evolve(_)).toSet)
      case Exists(symbolId, gen, true) =>
        Exists(symbolId, gen, checkFromNextGen = false)
      case condition: Exists =>
        val milestone = condition.milestone
        if (visiting.contains(milestone)) condition else {
          val moreTrackingNeeded = paths.exists(_.first == milestone)

          val progressCondition = observedProgressConditionOf(genActions, seenProgressedRootMilestones, milestone)
          val evolvedCondition = evolve(progressCondition, visiting + milestone)

          if (moreTrackingNeeded) {
            MilestoneAcceptCondition.disjunct(Set(condition, evolvedCondition))
          } else {
            evolvedCondition
          }
        }
      case NotExists(symbolId, gen, true) =>
        NotExists(symbolId, gen, checkFromNextGen = false)
      case condition: NotExists =>
        val milestone = condition.milestone
        if (visiting.contains(milestone)) condition else {
          val moreTrackingNeeded = paths.exists(_.first == milestone)

          val progressCondition = observedProgressConditionOf(genActions, seenProgressedRootMilestones, milestone)
          val evolvedCondition = evolve(progressCondition, visiting + milestone).negation

          if (moreTrackingNeeded) {
            MilestoneAcceptCondition.conjunct(Set(condition, evolvedCondition))
          } else {
            evolvedCondition
          }
        }
      case condition: OnlyIf =>
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evolve(progressCondition)
      case condition: Unless =>
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evolve(progressCondition).negation
    }
  }

  def evaluateAcceptCondition(
    genActions: GenActions,
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    condition: MilestoneAcceptCondition,
    visiting: Set[Milestone] = Set(),
  ): Boolean = {
    def evaluate(cond: MilestoneAcceptCondition, nextVisiting: Set[Milestone] = visiting): Boolean =
      evaluateAcceptCondition(genActions, seenProgressedRootMilestones, cond, nextVisiting)

    condition match {
      case Always => true
      case Never => false
      case And(conditions) =>
        conditions.forall(evaluate(_))
      case Or(conditions) =>
        conditions.exists(evaluate(_))
      case Exists(_, _, true) => false
      case condition: Exists =>
        val milestone = condition.milestone
        !visiting.contains(milestone) && {
          val progressCondition = observedProgressConditionOf(genActions, seenProgressedRootMilestones, milestone)
          evaluate(progressCondition, visiting + milestone)
        }
      case NotExists(_, _, true) => true
      case condition: NotExists =>
        val milestone = condition.milestone
        visiting.contains(milestone) || {
          val progressCondition = observedProgressConditionOf(genActions, seenProgressedRootMilestones, milestone)
          !evaluate(progressCondition, visiting + milestone)
        }
      case condition: OnlyIf =>
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        evaluate(progressCondition)
      case condition: Unless =>
        val progressCondition = getProgressConditionOf(genActions, condition.milestone)
        !evaluate(progressCondition)
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
      val pendedCollection = mutable.Map[KernelTemplate, (Set[AppendingMilestoneGroup], Option[AcceptConditionTemplate])]()

      val termActionApplied = ctx.paths.flatMap { path =>
        val termAction = parserData.termActions(path.tip.groupId)
          .find(_._1.contains(input))
        termAction match {
          case Some((_, action)) =>
            actionsCollector.termActions += (path.tip -> action)
            // pendedCollection ++= action.pendedAcceptConditionKernels
            action.pendedAcceptConditionKernels.foreach { case (first, (appendings, progressCondition)) =>
              pendedCollection.get(first) match {
                case Some((existingAppendings, existingProgressCondition)) =>
                  val mergedAppendings = existingAppendings ++ appendings
                  val mergedProgressCondition = (existingProgressCondition, progressCondition) match {
                    case (None, None) => None
                    case (Some(condition), None) => Some(condition)
                    case (None, Some(condition)) => Some(condition)
                    case (Some(cond1), Some(cond2)) => Some(AcceptConditionTemplate.disjunct(Set(cond1, cond2)))
                  }
                  pendedCollection += first -> (mergedAppendings, mergedProgressCondition)
                case None =>
                  pendedCollection += first -> (appendings.toSet, progressCondition)
              }
            }
            applyTermAction(path, gen, action, actionsCollector)
          case None => List()
        }
      }
      val pended = pendedCollection.flatMap { case (first, (appendings, progressCondition)) =>
        val firstMilestone = Milestone(first, ctx.gen)
        progressCondition match {
          case Some(progressCondition) =>
            actionsCollector.addProgressedRootMilestone(
              firstMilestone,
              MilestoneAcceptCondition.reify(progressCondition, ctx.gen, gen))
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
        genActions.progressedKernels.foreach(println)
        genActions.progressedKgroups.foreach(println)
      }

      val newConditions = (newPaths.map(_.acceptCondition) ++ genActions.progressedKernels.values).distinct
      val newConditionUpdates = newConditions
        .map(cond => cond -> evolveAcceptCondition(newPaths, genActions, ctx.seenProgressedRootMilestones, cond)).toMap

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
        .filter(path => path.first == initialMilestone || trackings.contains(path.first) ||
          isLookaheadWatcherRoot(path.first))

      if (verbose) {
        println(s"  ===== filtered (trackings=$trackings)")
        newPathsFiltered.foreach(path => println(path.prettyString))
      }

      // 이번 세대의 관찰은 위의 evolve(per-generation 채널)가 이미 처리했으므로, 누적 기록에
      // 접어 넣는 것은 evolve 이후 — 이 기록은 *다음* 세대부터 유효하다.
      val newSeen = updatedSeenProgressedRootMilestones(ctx.seenProgressedRootMilestones, newPaths, genActions)

      Right(ParsingContext(gen, newPathsFiltered, HistoryEntry(newPaths, genActions) +: ctx.history, newSeen))
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

    // TODO initialHistoryEntry의 progressedMilestones와 progressedMilestoneParentGens 추가
    val initialHistoryEntry = HistoryEntry(initialCtx.paths, GenActions(List(), List(), List(), Map(), Map(), Map(), Map()))
    val history = (initialHistoryEntry +: parsingContext.history.reverse).toVector

    // seenHistory(g) = 세대 g의 evolve/evaluate에서 참조할 누적 기록. parseStep과 동일한
    // 재귀를 history prefix fold로 재구성한다 (milestone2와 동일).
    val seenHistory: Vector[Map[Milestone, MilestoneAcceptCondition]] =
      history.scanLeft(Map[Milestone, MilestoneAcceptCondition]()) { (seen, entry) =>
        updatedSeenProgressedRootMilestones(seen, entry.untrimmedPaths, entry.genActions)
      }

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
            evaluateAcceptCondition(entry.genActions, seenHistory(gen), condition)
          } else {
            val evolved = evolveAcceptCondition(entry.untrimmedPaths, entry.genActions, seenHistory(gen), condition)
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
        if (isEventuallyAccepted(history, gen, genActions.progressedKgroups(end -> start.gen), conditionMemos)) {
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
        if (isEventuallyAccepted(history, gen, genActions.progressedKernels(end -> start.gen), conditionMemos)) {
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
      genActions.progressedKernels.foreach {
        case ((milestone, parentGen), condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          kernels += Kernel(milestone.symbolId, milestone.pointer, parentGen, milestone.gen)
          kernels += Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
        case _ => // do nothing
      }
      genActions.progressedRootMilestones.foreach {
        case (milestone, condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          assert(milestone.pointer == 0)
          kernels += Kernel(milestone.symbolId, milestone.pointer, milestone.gen, milestone.gen)
          kernels += Kernel(milestone.symbolId, milestone.pointer + 1, milestone.gen, gen)
        case _ => // do nothing
      }
      genActions.progressedKgroups.foreach {
        case ((mgroup, parentGen), condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          parserData.milestoneGroups(mgroup.groupId).foreach { milestone =>
            kernels += Kernel(milestone.symbolId, milestone.pointer, parentGen, mgroup.gen)
            kernels += Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
          }
        case _ => // do nothing
      }
      genActions.progressedRootMgroups.foreach {
        case (mgroup, condition) if isEventuallyAccepted(history, gen, condition, conditionMemos) =>
          parserData.milestoneGroups(mgroup.groupId).foreach { milestone =>
            kernels += Kernel(milestone.symbolId, milestone.pointer, mgroup.gen, mgroup.gen)
            kernels += Kernel(milestone.symbolId, milestone.pointer + 1, mgroup.gen, gen)
          }
        case _ => // do nothing
      }
      kernels.toSet
    }
    (initialKernels +: kernelsHistory)
  }
}
