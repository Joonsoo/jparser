package com.giyeok.jparser.milestone2

import com.giyeok.jparser.{Inputs, NGrammar}
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInputByTermGroups}
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.utils.Memoize

import scala.collection.mutable

class MilestoneParser(val parserData: MilestoneParserData) {
  private var verbose = false

  def setVerbose(): MilestoneParser = {
    verbose = true
    this
  }

  val initialMilestone: Milestone = Milestone(parserData.grammar.startSymbol, 0, 0)

  // 초기 컨텍스트의 조건 템플릿들이 감시하는 심볼들의 루트 경로.
  // gen 0에서 zero-width로 progress된 lookahead/longest가 만든 조건은 이후
  // 세대에서 해당 심볼의 progress를 추적해야 하는데, 그 루트 경로가 초기
  // 컨텍스트부터 존재해야 gen 0 시점의 조건 평가(evolve)가 moreTrackingNeeded를
  // 올바르게 판단한다. (없으면 다세대 lookahead 조건이 Never로 오판됨)
  private val initialConditionMilestones: List[Milestone] =
    parserData.initialTasksSummary.addedKernels.keySet
      .map(MilestoneAcceptCondition.reify(_, 0, 0))
      .flatMap(_.milestones)
      .filter(_ != initialMilestone)
      .toList

  def initialCtx: ParsingContext =
    ParsingContext(0, MilestonePath(initialMilestone) +: initialConditionMilestones.map(MilestonePath(_)), List())

  def applyParsingAction(path: MilestonePath, gen: Int, action: ParsingAction, actionsCollector: GenActionsBuilder): List[MilestonePath] = {
    val tip = path.tip
    val appended = action.appendingMilestones.map { appending =>
      val newCondition = MilestoneAcceptCondition.reify(appending.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      path.append(Milestone(appending.milestone, gen), condition)
    }
    // apply edge actions to path
    val reduced: List[MilestonePath] = action.startNodeProgressCondition match {
      case Some(startNodeProgressCondition) =>
        val newCondition = MilestoneAcceptCondition.reify(startNodeProgressCondition, tip.gen, gen)
        val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))

        path.tipParent match {
          case Some(tipParent) =>
            actionsCollector.addProgressedKernel(tip, tipParent.gen, condition)

            val edgeAction = parserData.edgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            // record parse action
            actionsCollector.edgeActions += ((tipParent -> tip, edgeAction))
            applyParsingAction(path.pop(condition), gen, edgeAction.parsingAction, actionsCollector)
          case None =>
            actionsCollector.addProgressedRootMilestone(tip, condition)

            List()
        }
      case None => List()
    }
    val lookaheadPaths = action.lookaheadRequiringSymbols.map(symbolId => MilestonePath(Milestone(symbolId, 0, gen)))
    appended ++ reduced ++ lookaheadPaths
  }

  def collectTrackings(paths: List[MilestonePath]): Set[Milestone] = {
    // TODO first가 initialMilestone인 경로들에서 필요한 tracking들을 추린 다음, trackings에 의해 필요한 다른 path들을 추가하기
    def collectForPath(path: MilestonePath): Set[Milestone] = {
      def traverse(tip: Milestone, rest: List[Milestone]): Set[Milestone] =
        rest match {
          case Nil => Set()
          case parent +: next =>
            val action = parserData.edgeProgressActions(parent.kernelTemplate -> tip.kernelTemplate)
            action.requiredSymbols.map(Milestone(_, 0, parent.gen)) ++ traverse(parent, next)
        }

      // TODO path.acceptCondition.milestones가 정말로 필요한가?
      path.acceptCondition.milestones ++ traverse(path.path.head, path.path.tail)
    }

    paths.flatMap { path =>
      collectForPath(path)
    }.toSet
  }

  def expectedInputsOf(ctx: ParsingContext): Set[Inputs.TermGroupDesc] =
    ctx.paths.filter(_.first == initialMilestone)
      .flatMap(path => parserData.termActions(path.tip.kernelTemplate).map(_._1)).toSet

  // 누적 기록(seen)을 참조해도 되는 감시 심볼들 = lookahead(!A/^A)의 body에서
  // longest(<A>)의 body를 뺀 것.
  //
  // 왜 longest를 빼야 하나: naive2에서 longest 조건은 NotExists(beginGen, endGen + 1, body)
  // — "이번에 취한 매치보다 *더 긴* (end >= endGen+1) 매치가 없다" — 인데, milestone 계열은
  // 이 창 하한을 `checkFromNextGen` 플래그 한 비트로만 인코딩하고 그 비트는 첫 evolve에서
  // 소진된다. 그 뒤로는 순수 lookahead leaf(NotExists(sym, beginGen, false))와 구별되지
  // 않으므로, 누적 기록을 보게 하면 *이미 취한 매치*가 다시 witness로 잡혀 longest 조건이
  // Never로 무너진다 (metalang3 자신의 .cdg 파싱이 즉시 깨짐: `<...>` 토큰이 전부 죽는다).
  // lookahead 계열은 anchor == 창 하한이라 그 세대 이전의 관찰이 존재할 수 없거나
  // (fromNextGen) 전부 정당한 witness이므로(anchor 기준 lookahead) 안전하다.
  //
  // 한계: 어떤 심볼이 longest body이면서 동시에 lookahead body인 문법에서는 그 심볼의
  // lookahead가 이 수정의 혜택을 받지 못한다(기존 동작 유지 — 회귀는 없음). 정확히 고치려면
  // 조건에 창 하한(minEndGen)을 실어야 하는데 그건 condition algebra 변경이다.
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
  //
  // 왜: 조건은 dot이 조건부 kernel을 지나는 세대에 물질화되는데, 생성기의 edge
  // requiredSymbols는 그 조건이 *그 엣지에서* 물질화될 때만 감시 심볼을 요구한다.
  // 그래서 body가 lookahead operand보다 길게 뻗는 문법(`Program = !"fn" Expression ';'`,
  // `Expression = "fn" "()"`)에서는 watcher가 완성되기 *전에* trimming으로 죽어 버리고
  // (gen 1의 trackings가 비어 있음), 관찰 자체가 생기지 않아 gen 4에 물질화된
  // NotExists가 Always로 오해소된다. watcher를 살려 두면 관찰이 생기고, 그 관찰은
  // seenProgressedRootMilestones(누적 기록)를 통해 나중에 태어난 leaf에 전달된다.
  // 두 수정은 짝이다 — 어느 하나만으로는 이 형태를 고칠 수 없다.
  //
  // longest body는 제외(cumulativeRecordSymbols가 이미 뺀다): longest watcher를 더 오래
  // 살려 두면 longest 조건의 pending 구간이 달라져 기존 동작이 바뀐다.
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

  // Exists/NotExists(unbounded lookahead)가 볼 수 있는 관찰:
  // 이번 세대의 progress + 이전 세대들의 누적 기록(seen). 기록 자체가
  // cumulativeRecordSymbols로 걸러져 있으므로(updatedSeenProgressedRootMilestones) 여기서
  // 다시 걸러낼 필요는 없다. bounded(OnlyIf/Unless)는 이 함수를 쓰지 않는다.
  //
  // 반환 형태(단일 조건의 disjunct)와 호출부의 conjunct/disjunct 인자 순서는 mgroup2의
  // MilestoneGroupParser.observedProgressConditionOf와 *구조까지* 같아야 한다:
  // And/Or는 List를 담는 case class라 원소 순서가 다르면 동등하지 않고,
  // Mgroup2ToMilestone2Tests가 두 계통의 path accept condition을 구조 비교한다.
  private def observedProgressConditionOf(
    genActions: GenActions,
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    milestone: Milestone,
  ): MilestoneAcceptCondition =
    MilestoneAcceptCondition.disjunct(Set(
      genActions.progressedRootMilestones.getOrElse(milestone, Never),
      seenProgressedRootMilestones.getOrElse(milestone, Never)))

  // 이번 세대의 관찰을 누적 기록에 접어 넣은 새 기록. 순서가 load-bearing:
  //  1) 이번 세대의 progressedRootMilestones를 기존 엔트리와 disjoin해서 병합
  //  2) 그 다음 *모든* non-constant 엔트리를 이번 세대에서 evolve (병합 직후 상태만 읽도록
  //     업데이트를 모아 두었다가 일괄 적용). 저장된 조건은 다른 root를 참조할 수 있고,
  //     특히 *관찰 세대 자신의* evolve를 건너뛰면 안 된다 — OnlyIf(sym, g) 형태의 progress
  //     조건은 그 세대의 evolve에서만 discharge되기 때문. Always/Never는 고정점이라 skip.
  //  결과 Never는 "그 관찰은 불가능했다"이므로 엔트리를 제거한다.
  def updatedSeenProgressedRootMilestones(
    seenProgressedRootMilestones: Map[Milestone, MilestoneAcceptCondition],
    paths: List[MilestonePath],
    genActions: GenActions,
  ): Map[Milestone, MilestoneAcceptCondition] = {
    // 문법에 (longest body가 아닌) lookahead 감시 심볼이 없으면 기록은 언제나 비어 있다.
    if (cumulativeRecordSymbols.isEmpty) seenProgressedRootMilestones else {
    val merged = genActions.progressedRootMilestones.foldLeft(seenProgressedRootMilestones) {
      case (acc, (milestone, condition)) =>
        if (condition == Never || !cumulativeRecordSymbols.contains(milestone.symbolId)) acc
        else acc + (milestone -> (acc.get(milestone) match {
          case Some(existing) => MilestoneAcceptCondition.disjunct(Set(existing, condition))
          case None => condition
        }))
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

  // visiting: 재귀 도중 이미 진행 중인 root milestone들. 누적 기록의 조건이 자기 자신을
  // (직·간접적으로) 참조하는 문법에서 무한 재귀를 끊는다. 순환은 witness가 아니라고 보아
  // 해당 leaf를 그대로 남긴다(= 이번 세대 소비 보류).
  def evolveAcceptCondition(
    paths: List[MilestonePath],
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
        genActions.progressedRootMilestones.get(condition.milestone) match {
          case Some(progressCondition) => evolve(progressCondition)
          case None => Never
        }
      case condition: Unless =>
        genActions.progressedRootMilestones.get(condition.milestone) match {
          case Some(progressCondition) => evolve(progressCondition).negation
          case None => Always
        }
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
        genActions.progressedRootMilestones.get(condition.milestone) match {
          case Some(progressCondition) => evaluate(progressCondition)
          case None => false
        }
      case condition: Unless =>
        genActions.progressedRootMilestones.get(condition.milestone) match {
          case Some(progressCondition) => !evaluate(progressCondition)
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
      val pendedCollection = mutable.Map[KernelTemplate, (Set[AppendingMilestone], Option[AcceptConditionTemplate])]()
      val termActionApplied = ctx.paths.flatMap { path =>
        val termAction = parserData.termActions(KernelTemplate(path.tip.symbolId, path.tip.pointer))
          .find(_._1.contains(input))
        termAction match {
          case Some((_, action)) =>
            // record parse action
            actionsCollector.termActions += ((path.tip, action))
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
            applyParsingAction(path, gen, action.parsingAction, actionsCollector)
          case None => List()
        }
      }
      val pended = pendedCollection.flatMap { case (first, (appendings, progressCondition)) =>
        val firstMilestone = Milestone(first, ctx.gen)
        progressCondition match {
          case Some(progressCondition) =>
            actionsCollector.addProgressedRootMilestone(firstMilestone,
              MilestoneAcceptCondition.reify(progressCondition, ctx.gen, gen))
          case None =>
        }
        appendings.map { appending =>
          val condition = MilestoneAcceptCondition.reify(appending.acceptCondition, ctx.gen, gen)
          MilestonePath(firstMilestone).append(Milestone(appending.milestone, gen), condition)
        }
      }
      val newPaths: List[MilestonePath] = termActionApplied ++ pended
      if (verbose) {
        newPaths.foreach(path => println(path.prettyString))
      }
      val genActions = actionsCollector.build()

      if (verbose) {
        println("  ===== genActions")
        genActions.edgeActions.map(_._1).foreach(println)
        genActions.progressedKernels.foreach(println)
      }

      val newConditions = (newPaths.map(_.acceptCondition) ++ genActions.progressedKernels.values).distinct
      val newConditionUpdates = newConditions
        .map(cond => cond -> evolveAcceptCondition(newPaths, genActions, ctx.seenProgressedRootMilestones, cond)).toMap

      if (verbose) {
        println("  ==== condition updates")
        newConditionUpdates.foreach(println(_))
      }

      // newPaths와 수행된 액션을 바탕으로 condition evaluate
      val newPathsUpdated = newPaths
        .map(path => path.copy(acceptCondition = newConditionUpdates(path.acceptCondition)))
        .filter(_.acceptCondition != Never)
      if (verbose) {
        println("  ===== condition updated")
        newPathsUpdated.foreach(path => println(path.prettyString))
      }

      // first가 (start symbol, 0, 0)이거나 현재 존재하는 엣지의 trackingMilestones인 경우만 제외하고 모두 제거
      // 경우에 따라선 필터링을 함에 따라서 trackings가 달라져서 불필요한 경로가 남는 경우가 있을 수 있는데, 다음 gen에 어차피 제거될 것이므로 큰 문제 없을듯
      val trackings = collectTrackings(newPaths)
      val newPathsFiltered = newPathsUpdated
        .filter(path => path.first == initialMilestone || trackings.contains(path.first) ||
          isLookaheadWatcherRoot(path.first))
      if (verbose) {
        println(s"  ===== filtered, trackings: $trackings")
        newPathsFiltered.foreach(path => println(path.prettyString))
        //        println(s"  ===== conditions:")
        //        nextConditionUpdates.toList.sortBy(_._1._1).foreach(pair => println(s"${pair._1} -> ${pair._2}"))
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

  def parseOrThrow(source: String): ParsingContext = parseOrThrow(Inputs.fromString(source))

  // progress되면서 추가된 커널들을 반환한다. finish는 progress 되면서 자연스럽게 따라오는 것이기 때문에 처리할 필요 없음
  def kernelsHistory(parsingContext: ParsingContext): Vector[Set[Kernel]] = {
    //    def progressAndMapGen(kernel: Kernel, gen: Int, genMap: Map[Int, Int]): Kernel =
    //      Kernel(kernel.symbolId, kernel.pointer + 1, genMap(kernel.beginGen), gen)

    def mapGen(kernel: Kernel, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer, genMap(kernel.beginGen), genMap(kernel.endGen))

    val initialHistoryEntry = HistoryEntry(initialCtx.paths, GenActions(List(), List(), Map(), Map()))
    val history = (initialHistoryEntry +: parsingContext.history.reverse).toVector

    // seenHistory(g) = 세대 g의 evolve/evaluate에서 참조할 누적 기록.
    // parseStep과 동일한 재귀 — history prefix를 fold해서 재구성한다:
    // seenHistory(0) = 빈 기록, seenHistory(g+1) = updated(seenHistory(g), history(g)).
    // (scanLeft라 길이가 history.length + 1이고 마지막 원소는 쓰이지 않는다)
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
        // termAction.parsingAction.tasksSummary.progressedStartKernel은 progressedStartKernel에서 처리
        addKernelsFrom(
          kernels,
          history,
          gen - 1,
          gen,
          termAction.parsingAction.tasksSummary,
          Map(0 -> milestone.gen, 1 -> (gen - 1), 2 -> gen),
          conditionMemos)
      }
      genActions.edgeActions.foreach { case ((start, end), edgeAction) =>
        if (isEventuallyAccepted(history, gen, genActions.progressedKernels(end -> start.gen), conditionMemos)) {
          // edgeAction.parsingAction.tasksSummary.progressedStartKernel은 progressedMilestones에서 처리
          addKernelsFrom(
            kernels,
            history,
            start.gen,
            gen,
            edgeAction.parsingAction.tasksSummary,
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
      kernels.toSet
    }
    (initialKernels +: kernelsHistory)
  }
}

class GenActionsBuilder {
  val termActions: mutable.ListBuffer[(Milestone, TermAction)] = mutable.ListBuffer()
  val edgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  // milestone과 parent gen을 조합하면 kernel이 되기 때문
  private val progressedKernels: mutable.Map[(Milestone, Int), MilestoneAcceptCondition] = mutable.Map()
  private val progressedRootMilestones: mutable.Map[Milestone, MilestoneAcceptCondition] = mutable.Map()

  def addProgressedKernel(milestone: Milestone, parentGen: Int, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedKernels.get((milestone, parentGen)) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedKernels += ((milestone -> parentGen) -> newCondition)
  }

  def addProgressedRootMilestone(milestone: Milestone, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedRootMilestones.get(milestone) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedRootMilestones += (milestone -> newCondition)
  }

  def build(): GenActions =
    GenActions(termActions.toList, edgeActions.toList, progressedKernels.toMap, progressedRootMilestones.toMap)
}
