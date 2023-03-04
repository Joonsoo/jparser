package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInputByTermGroups}
import com.giyeok.jparser.fast.KernelTemplate
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

  def initialCtx: ParsingContext =
    ParsingContext(0, List(MilestonePath(initialMilestone)), List())

  def reifyCondition(template: AcceptConditionTemplate, beginGen: Int, gen: Int): MilestoneAcceptCondition =
    template match {
      case AlwaysTemplate => Always
      case NeverTemplate => Never
      case AndTemplate(conditions) =>
        And(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case OrTemplate(conditions) =>
        Or(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case LookaheadIsTemplate(symbolId, fromNextGen) =>
        Exists(Milestone(symbolId, 0, gen), fromNextGen)
      case LookaheadNotTemplate(symbolId, fromNextGen) =>
        NotExists(Milestone(symbolId, 0, gen), checkFromNextGen = fromNextGen)
      case LongestTemplate(symbolId) =>
        NotExists(Milestone(symbolId, 0, beginGen), checkFromNextGen = true)
      case OnlyIfTemplate(symbolId) =>
        OnlyIf(Milestone(symbolId, 0, beginGen))
      case UnlessTemplate(symbolId) =>
        Unless(Milestone(symbolId, 0, beginGen))
    }

  def applyParsingAction(path: MilestonePath, gen: Int, action: ParsingAction, actionsCollector: GenActionsBuilder): List[MilestonePath] = {
    val tip = path.tip
    val appended = action.appendingMilestones.map { appending =>
      val newCondition = reifyCondition(appending.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      path.append(Milestone(appending.milestone, gen), condition)
    }
    // apply edge actions to path
    val reduced: List[MilestonePath] = action.startNodeProgressCondition match {
      case Some(startNodeProgressCondition) =>
        val newCondition = reifyCondition(startNodeProgressCondition, tip.gen, gen)
        val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
        actionsCollector.addProgressedMilestone(tip, condition)
        path.tipParent match {
          case Some(tipParent) =>
            val edgeAction = parserData.edgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            // record parse action
            actionsCollector.edgeActions += ((tipParent -> tip, edgeAction))
            actionsCollector.addProgressedMilestoneParentGen(tip, tipParent.gen)
            applyParsingAction(path.pop(condition), gen, edgeAction.parsingAction, actionsCollector)
          case None =>
            List()
        }
      case None => List()
    }
    val lookaheadPaths = action.lookaheadRequiringSymbols.map(symbolId => MilestonePath(Milestone(symbolId, 0, gen)))
    appended ++ reduced ++ lookaheadPaths
  }

  def collectTrackings(paths: List[MilestonePath]): Set[Milestone] =
    paths.flatMap { path =>
      def traverse(tip: Milestone, rest: List[Milestone]): Set[Milestone] =
        rest match {
          case Nil => Set()
          case parent +: next =>
            val action = parserData.edgeProgressActions(parent.kernelTemplate -> tip.kernelTemplate)
            action.requiredSymbols.map(Milestone(_, 0, parent.gen)) ++ traverse(parent, next)
        }

      path.acceptCondition.milestones ++ traverse(path.path.head, path.path.tail)
    }.toSet

  def expectedInputsOf(ctx: ParsingContext): Set[Inputs.TermGroupDesc] =
    ctx.paths.filter(_.first == initialMilestone)
      .flatMap(path => parserData.termActions(path.tip.kernelTemplate).map(_._1)).toSet

  def evolveAcceptCondition(
    paths: List[MilestonePath],
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
        Exists(milestone, false)
      case Exists(milestone, false) =>
        val moreTrackingNeeded = paths.exists(_.first == milestone)
        genActions.progressedMilestones.get(milestone) match {
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
        NotExists(milestone, false)
      case NotExists(milestone, false) =>
        val moreTrackingNeeded = paths.exists(_.first == milestone)
        val progressCondition = genActions.progressedMilestones.get(milestone)
        progressCondition match {
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
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            evolveAcceptCondition(paths, genActions, progressCondition)
          case None => Never
        }
      case Unless(milestone) =>
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            evolveAcceptCondition(paths, genActions, progressCondition).negation
          case None => Always
        }
    }
  }

  def evaluateAcceptCondition(
    paths: List[MilestonePath],
    genActions: GenActions,
    condition: MilestoneAcceptCondition
  ): Boolean = {
    condition match {
      case Always => true
      case Never => false
      case And(conditions) =>
        conditions.forall(evaluateAcceptCondition(paths, genActions, _))
      case Or(conditions) =>
        conditions.exists(evaluateAcceptCondition(paths, genActions, _))
      case Exists(milestone, true) => false
      case Exists(milestone, false) =>
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            evaluateAcceptCondition(paths, genActions, progressCondition)
          case None => false
        }
      case NotExists(milestone, true) => true
      case NotExists(milestone, false) =>
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            !evaluateAcceptCondition(paths, genActions, progressCondition)
          case None => true
        }
      case OnlyIf(milestone) =>
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            evaluateAcceptCondition(paths, genActions, progressCondition)
          case None => false
        }
      case Unless(milestone) =>
        genActions.progressedMilestones.get(milestone) match {
          case Some(progressCondition) =>
            !evaluateAcceptCondition(paths, genActions, progressCondition)
          case None => true
        }
    }
  }

  def parseStep(ctx: ParsingContext, input: Inputs.Input): Either[ParsingError, ParsingContext] = {
    val gen = ctx.gen + 1
    val actionsCollector = new GenActionsBuilder()
    val pendedCollection = mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])]()
    val termActionApplied = ctx.paths.flatMap { path =>
      val termAction = parserData.termActions(KernelTemplate(path.tip.symbolId, path.tip.pointer))
        .find(_._1.contains(input))
      termAction match {
        case Some((_, action)) =>
          // record parse action
          actionsCollector.termActions += ((path.tip, action))
          pendedCollection ++= action.pendedAcceptConditionKernels
          applyParsingAction(path, gen, action.parsingAction, actionsCollector)
        case None => List()
      }
    }
    val pended = pendedCollection.flatMap { case (first, (appendings, progressCondition)) =>
      val firstMilestone = Milestone(first, ctx.gen)
      progressCondition match {
        case Some(progressCondition) =>
          actionsCollector.addProgressedMilestone(firstMilestone, reifyCondition(progressCondition, ctx.gen, gen))
        case None =>
      }
      appendings.map { appending =>
        val condition = reifyCondition(appending.acceptCondition, ctx.gen, gen)
        MilestonePath(firstMilestone).append(Milestone(appending.milestone, gen), condition)
      }
    }
    val newPaths: List[MilestonePath] = termActionApplied ++ pended
    if (verbose) {
      newPaths.foreach(path => println(path.prettyString))
    }
    if (!newPaths.exists(_.first == initialMilestone)) {
      // start symbol에 의한 path가 없으면 해당 input이 invalid하다는 뜻
      Left(UnexpectedInputByTermGroups(input, expectedInputsOf(ctx), gen))
    } else {
      val genActions = actionsCollector.build()

      if (verbose) {
        println("  ===== genActions")
        genActions.edgeActions.map(_._1).foreach(println)
        genActions.progressedMilestones.foreach(println)
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
      val trackings = collectTrackings(newPaths)
      val newPathsFiltered = newPathsUpdated
        .filter(path => path.first == initialMilestone || trackings.contains(path.first))
      if (verbose) {
        println(s"  ===== filtered, trackings: $trackings")
        newPathsFiltered.foreach(path => println(path.prettyString))
        //        println(s"  ===== conditions:")
        //        nextConditionUpdates.toList.sortBy(_._1._1).foreach(pair => println(s"${pair._1} -> ${pair._2}"))
      }

      Right(ParsingContext(gen, newPathsFiltered, HistoryEntry(newPaths, genActions) +: ctx.history))
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
          if (verbose) {
            println(s"  === ${currCtx.gen + 1} $nextInput ${currCtx.paths.size}")
          }
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
  def kernelsHistory(parsingContext: ParsingContext): List[Set[Kernel]] = {
    def progressAndMapGen(kernel: Kernel, gen: Int, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer + 1, genMap(kernel.beginGen), gen)

    def mapGen(kernel: Kernel, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer, genMap(kernel.beginGen), genMap(kernel.endGen))

    def isFinallyAccepted(
      history: Seq[HistoryEntry],
      gen: Int,
      condition: MilestoneAcceptCondition,
      conditionMemos: Memoize[MilestoneAcceptCondition, Boolean]
    ): Boolean = conditionMemos(condition) {
      val entry = history(gen)
      condition match {
        case Always => true
        case Never => false
        case _ =>
          if (gen + 1 == history.length) {
            evaluateAcceptCondition(entry.untrimmedPaths, entry.genActions, condition)
          } else {
            val evolved = evolveAcceptCondition(entry.untrimmedPaths, entry.genActions, condition)
            isFinallyAccepted(history, gen + 1, evolved, conditionMemos)
          }
      }
    }

    def kernelsFrom(
      history: Seq[HistoryEntry],
      beginGen: Int,
      gen: Int,
      tasksSummary: TasksSummary2,
      genMap: Map[Int, Int],
      conditionMemos: Memoize[MilestoneAcceptCondition, Boolean],
    ): Set[Kernel] = {
      val collector = mutable.Set[Kernel]()
      addKernelsFrom(collector, history, beginGen, gen, tasksSummary, genMap, conditionMemos)
      collector.toSet
    }

    def addKernelsFrom(
      kernelsCollector: mutable.Set[Kernel],
      history: Seq[HistoryEntry],
      beginGen: Int,
      gen: Int,
      tasksSummary: TasksSummary2,
      genMap: Map[Int, Int],
      conditionMemos: Memoize[MilestoneAcceptCondition, Boolean],
    ): Unit = {
      tasksSummary.addedKernels.foreach { pair =>
        val condition = reifyCondition(pair._1, beginGen, gen)
        if (isFinallyAccepted(history, gen, condition, conditionMemos)) {
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
    val initialHistoryEntry = HistoryEntry(List(MilestonePath(initialMilestone)), GenActions(List(), List(), Map(), Map()))
    val history = (initialHistoryEntry +: parsingContext.history.reverse).toVector

    val initialKernels = kernelsFrom(history, 0, 0, parserData.initialTasksSummary, Map(-1 -> 0, 0 -> 0, 1 -> 0, 2 -> 0), Memoize())
    val kernelsHistory = history.zipWithIndex.drop(1).map { case (entry, gen) =>
      val genActions = entry.genActions
      // TODO conditionMemos를 generation마다 초기화하지 않게 할 수 있을 것 같은데..
      // - conditionMemos는 반드시 앞쪽 generation에서 뒤쪽 generation으로 진행하면서 사용하도록 하고
      // - Unless나 OnlyIf가 포함되어 있으면 generation마다 삭제하는 식으로?
      val conditionMemos = Memoize[MilestoneAcceptCondition, Boolean]()

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
        if (isFinallyAccepted(history, gen, genActions.progressedMilestones(end), conditionMemos)) {
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
      genActions.progressedMilestones.foreach {
        case (milestone, condition) if isFinallyAccepted(history, gen, condition, conditionMemos) =>
          val parentGens = genActions.progressedMilestoneParentGens.getOrElse(milestone, Set(milestone.gen))
          parentGens.foreach { parentGen =>
            kernels += Kernel(milestone.symbolId, milestone.pointer, parentGen, milestone.gen)
            kernels += Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
          }
        case _ => // do nothing
      }
      kernels.toSet
    }
    (initialKernels +: kernelsHistory).toList
  }
}

class GenActionsBuilder {
  val termActions: mutable.ListBuffer[(Milestone, TermAction)] = mutable.ListBuffer()
  val edgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  private val progressedMilestones: mutable.Map[Milestone, MilestoneAcceptCondition] = mutable.Map()
  private val progressedMilestoneParentGens: mutable.Map[Milestone, mutable.Set[Int]] = mutable.Map()

  def addProgressedMilestone(milestone: Milestone, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedMilestones.get(milestone) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedMilestones += (milestone -> newCondition)
  }

  def addProgressedMilestoneParentGen(milestone: Milestone, parentGen: Int): Unit = {
    progressedMilestoneParentGens.getOrElseUpdate(milestone, mutable.Set()).add(parentGen)
  }

  def build(): GenActions =
    GenActions(termActions.toList, edgeActions.toList, progressedMilestones.toMap, progressedMilestoneParentGens.view.mapValues(_.toSet).toMap)
}
