package com.giyeok.jparser.milestone2

import com.giyeok.jparser.{Inputs, Symbols}
import com.giyeok.jparser.ParsingErrors.{ParsingError, UnexpectedInput, UnexpectedInputByTermGroups}
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.Kernel

import scala.collection.mutable

class MilestoneParser(val parserData: MilestoneParserData) {
  private var verbose = false

  def setVerbose(): MilestoneParser = {
    verbose = true
    this
  }

  val initialMilestone: Milestone = Milestone(parserData.grammar.startSymbol, 0, 0)

  def initialCtx: ParsingContext =
    ParsingContext(0, List(MilestonePath(initialMilestone)), List(), Map())

  def reifyCondition(template: AcceptConditionTemplate, beginGen: Int, gen: Int): MilestoneAcceptCondition =
    template match {
      case AlwaysTemplate => Always
      case NeverTemplate => Never
      case AndTemplate(conditions) =>
        And(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case OrTemplate(conditions) =>
        Or(conditions.map(reifyCondition(_, beginGen, gen)).distinct)
      case ExistsTemplate(symbolId) =>
        Exists(Milestone(symbolId, 0, gen))
      case NotExistsTemplate(symbolId) =>
        NotExists(Milestone(symbolId, 0, gen), checkFromNextGen = false)
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
        actionsCollector.progressedMilestones += tip -> condition
        path.tipParent match {
          case Some(tipParent) =>
            val edgeAction = parserData.edgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
            // record parse action
            actionsCollector.edgeActions += ((tipParent -> tip, edgeAction))
            actionsCollector.progressedMilestoneParentGens += tip -> tipParent.gen
            applyParsingAction(path.pop(condition), gen, edgeAction.parsingAction, actionsCollector)
          case None =>
            List()
        }
      case None => List()
    }
    appended ++ reduced
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

      traverse(path.path.head, path.path.tail)
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
      case Exists(milestone) =>
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
        genActions.progressedMilestones.get(milestone) match {
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
      case Exists(milestone) =>
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
          actionsCollector.progressedMilestones += (firstMilestone -> reifyCondition(progressCondition, ctx.gen, gen))
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

      val newConditions = (newPaths.map(_.acceptCondition) ++ genActions.progressedMilestones.values).distinct
      val thisGenConditionUpdates = newConditions
        .map(cond => cond -> evolveAcceptCondition(newPaths, genActions, cond))
        .toMap

      // newPaths와 수행된 액션을 바탕으로 condition evaluate
      val newPathsUpdated = newPaths
        .map(path => path.copy(acceptCondition = thisGenConditionUpdates(path.acceptCondition)))
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
      }

      // TODO conditionUpdates는 의도적으로 한 generation 늦게 업데이트됨 - 정확한 타이밍은 아직 모르겠음..
      val oldGenConditionUpdates = ctx.conditionsUpdates.view.mapValues(evolveAcceptCondition(newPaths, genActions, _)).toMap
      val nextConditionUpdates = (oldGenConditionUpdates ++ newConditions.map(cond => (gen, cond) -> thisGenConditionUpdates(cond)))

      Right(ParsingContext(gen, newPathsFiltered, genActions +: ctx.actionsHistory, nextConditionUpdates))
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
            println(s"  === ${currCtx.gen} $nextInput ${currCtx.paths.size}")
          }
          parseStep(currCtx, nextInput)
        case error => error
      }
    }
  }

  // progress되면서 추가된 커널들을 반환한다. finish는 progress 되면서 자연스럽게 따라오는 것이기 때문에 처리할 필요 없음
  def kernelsHistory(parsingContext: ParsingContext): List[Set[Kernel]] = {
    // TODO accept condition 처리
    def progressAndMapGen(kernel: Kernel, gen: Int, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer + 1, genMap(kernel.beginGen), gen)

    def mapGen(kernel: Kernel, genMap: Map[Int, Int]): Kernel =
      Kernel(kernel.symbolId, kernel.pointer, genMap(kernel.beginGen), genMap(kernel.endGen))

    def kernelsFrom(parsingAction: ParsingAction, genMap: Map[Int, Int]): Set[Kernel] = {
      val tasksSummary = parsingAction.tasksSummary
      tasksSummary.addedKernels.map(mapGen(_, genMap)) ++ tasksSummary.progressedKernels.map(mapGen(_, genMap))
    }

    def isFinallyAccepted(gen: Int, condition: MilestoneAcceptCondition): Boolean = {
      val finalCondition = parsingContext.conditionsUpdates(gen, condition)
      evaluateAcceptCondition(parsingContext.paths, parsingContext.actionsHistory.head, finalCondition)
    }

    val initialNodes = parserData.initialTasksSummary.progressedKernels.map(progressAndMapGen(_, 0, Map(-1 -> 0, 0 -> 0))) ++
      parserData.initialTasksSummary.progressedStartKernel.map(progressAndMapGen(_, 0, Map(0 -> 0, 1 -> 0)))

    val actionsHistory = parsingContext.actionsHistory.reverse
    val kernelsHistory = actionsHistory.zipWithIndex.map { case (genActions, gen_) =>
      val gen = gen_ + 1
      val nodesByTermActions = genActions.termActions.flatMap { case (milestone, termAction) =>
        // termAction.parsingAction.tasksSummary.progressedStartKernel은 progressedStartKernel에서 처리
        kernelsFrom(termAction.parsingAction, Map(0 -> milestone.gen, 1 -> gen_, 2 -> gen)) // ++ startKernel
      }
      val nodesByEdgeActions = genActions.edgeActions.flatMap { case ((start, end), edgeAction) =>
        if (isFinallyAccepted(gen, genActions.progressedMilestones(end))) {
          // edgeAction.parsingAction.tasksSummary.progressedStartKernel은 progressedMilestones에서 처리
          kernelsFrom(edgeAction.parsingAction, Map(0 -> start.gen, 1 -> end.gen, 2 -> gen)) // ++ startKernel
        } else {
          List()
        }
      }
      val progressed = genActions.progressedMilestones.collect {
        case (milestone, condition) if isFinallyAccepted(gen, condition) =>
          val parentGen = genActions.progressedMilestoneParentGens.getOrElse(milestone, milestone.gen)
          Kernel(milestone.symbolId, milestone.pointer + 1, parentGen, gen)
      }
      (nodesByTermActions ++ nodesByEdgeActions ++ progressed).toSet
    }
    initialNodes +: kernelsHistory
  }
}

class GenActionsBuilder {
  val termActions: mutable.ListBuffer[(Milestone, TermAction)] = mutable.ListBuffer()
  val edgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  val progressedMilestones: mutable.Map[Milestone, MilestoneAcceptCondition] = mutable.Map()
  val progressedMilestoneParentGens: mutable.Map[Milestone, Int] = mutable.Map()

  def build(): GenActions =
    GenActions(termActions.toList, edgeActions.toList, progressedMilestones.toMap, progressedMilestoneParentGens.toMap)
}
