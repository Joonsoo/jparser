package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, Always, GenActionsBuilder, Milestone, MilestoneAcceptCondition, MilestonePath, Never}

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
    val reduced = action.startNodeProgress.flatMap { startNodeProgressCondition =>
      path.tipParent match {
        case Some(tipParent) =>
          val newCondition = MilestoneAcceptCondition.reify(startNodeProgressCondition, tip.gen, gen)
          val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))

          val edgeAction = parserData.midEdgeProgressActions(tipParent.kernelTemplate -> tip.kernelTemplate)
          progressTip(path.pop(condition), gen, edgeAction, actionsCollector)
        case None => List()
      }
    }
    appended ++ reduced
  }

  def applyTermAction(path: MilestoneGroupPath, gen: Int, action: TermAction, actionsCollector: GenActionsBuilder): List[MilestoneGroupPath] = {
    val tip = path.tip

    val appended = action.appendingMilestoneGroups.map { appending =>
      val newCondition = MilestoneAcceptCondition.reify(appending._2.acceptCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))
      path.replaceAndAppend(appending._1, MilestoneGroup(appending._2.groupId, gen), condition)
    }
    val reduced: List[MilestoneGroupPath] = action.startNodeProgress.flatMap { startNodeProgress =>
      val (replaceGroupId, startNodeProgressCondition) = startNodeProgress
      val newCondition = MilestoneAcceptCondition.reify(startNodeProgressCondition, tip.gen, gen)
      val condition = MilestoneAcceptCondition.conjunct(Set(path.acceptCondition, newCondition))

      path.tipParent match {
        case Some(tipParent) =>
          val edgeAction = parserData.tipEdgeProgressActions(tipParent.kernelTemplate -> replaceGroupId)
          progressTip(MilestonePath(path.first, path.path, condition), gen, edgeAction, actionsCollector)
        case None => List()
      }
    }
    // TODO lookahead paths
    appended ++ reduced
  }

  def parseStep(ctx: ParsingContext, input: Inputs.Input): Either[ParsingError, ParsingContext] = {
    val gen = ctx.gen + 1
    if (verbose) {
      println(s"  === $gen $input ${ctx.paths.size}")
    }
    val actionsCollector = new GenActionsBuilder()
    val pendedCollection = mutable.Map[KernelTemplate, (List[AppendingMilestoneGroup], Option[AcceptConditionTemplate])]()

    val termActionApplied = ctx.paths.flatMap { path =>
      val termAction = parserData.termActions(path.tip.groupId)
        .find(_._1.contains(input))
      termAction match {
        case Some((_, action)) =>
          pendedCollection ++= action.pendedAcceptConditionKernels
          applyTermAction(path, gen, action, actionsCollector)
        case None => List()
      }
    }
    // TODO pended

    val newPaths = termActionApplied
    if (verbose) {
      newPaths.foreach(path => println(path.prettyString))
      newPaths.map(_.tip.groupId).distinct.sorted.foreach { groupId =>
        println(s"$groupId => ${parserData.milestoneGroups(groupId)}")
      }
    }

    // TODO
    //    val newConditions = (newPaths.map(_.acceptCondition) ++ genActions.progressedMilestones.values).distinct
    //    val newConditionUpdates = newConditions
    //      .map(cond => cond -> evolveAcceptCondition(newPaths, genActions, cond)).toMap

    // newPaths와 수행된 액션을 바탕으로 condition evaluate
    val newPathsUpdated = newPaths
      //      .map(path => path.copy(acceptCondition = newConditionUpdates(path.acceptCondition)))
      .filter(_.acceptCondition != Never)
    if (verbose) {
      println("  ===== condition updated")
      newPathsUpdated.foreach(path => println(path.prettyString))
    }

    // first가 (start symbol, 0, 0)이거나 현재 존재하는 엣지의 trackingMilestones인 경우만 제외하고 모두 제거
    //    val trackings = collectTrackings(newPaths)
    val newPathsFiltered = newPathsUpdated
    //      .filter(path => path.first == initialMilestone || trackings.contains(path.first))
    //    if (verbose) {
    //      println(s"  ===== filtered, trackings: $trackings")
    //      newPathsFiltered.foreach(path => println(path.prettyString))
    //      //        println(s"  ===== conditions:")
    //      //        nextConditionUpdates.toList.sortBy(_._1._1).foreach(pair => println(s"${pair._1} -> ${pair._2}"))
    //    }

    Right(ParsingContext(gen, newPathsFiltered, HistoryEntry(newPaths, GenActions(List(), List(), Map(), Map())) +: ctx.history))
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
}
