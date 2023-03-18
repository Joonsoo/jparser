package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AppendingMilestone, CtxWithTasks, MilestoneParserGen, ParserGenBase2, TasksSummary2}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.opt.OptNaiveParser2
import com.giyeok.jparser.nparser2.{NaiveParser2, ProgressTask}
import com.giyeok.jparser.utils.TermGrouper

import scala.collection.mutable

class MilestoneGroupParserGen(val grammar: NGrammar) {
  private val parser = new NaiveParser2(grammar)
  private val optParser = new OptNaiveParser2(grammar)
  private val base = new ParserGenBase2(optParser)
  private val milestoneGen = new MilestoneParserGen(grammar)

  val start: KernelTemplate = KernelTemplate(grammar.startSymbol, 0)
  val startingCtx: (Kernel, CtxWithTasks) = base.startingCtxFrom(start, 0)

  val builder = new MilestoneGroupParserDataBuilder(grammar, startingCtx._2.tasksSummary)

  def termActionsFor(groupId: Int): List[(TermGroupDesc, TermAction)] = {
    val starts = builder.milestonesOfGroup(groupId)
    val (startKernelsMap, startingCtx) = base.startingCtxFrom(starts, 0)
    val startKernels = startKernelsMap.values.toSet

    val derived = startingCtx.ctx.graph.nodes
    val termNodes = derived.filter { node => grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal] }
      .filter { node => node.pointer == 0 }
    assert(termNodes.forall { kernel => kernel.beginGen == 1 })
    val terms = termNodes.map { node => grammar.symbolOf(node.symbolId) }
      .map { case terminal: NTerminal => terminal.symbol }
    val termGroups: List[TermGroupDesc] = TermGrouper.termGroupsOf(terms)

    termGroups.flatMap { termGroup =>
      val applicableTermNodes = termNodes.filter { kernel =>
        val symbol = grammar.symbolOf(kernel.symbolId)
        symbol.asInstanceOf[NTerminal].symbol.acceptTermGroup(termGroup)
      }
      val termProgressTasks = applicableTermNodes.toList.map(ProgressTask(_, AcceptCondition.Always))

      val applied = base.runTasksWithProgressBarriers(2, termProgressTasks, startKernels, startingCtx.ctx)
      val trimmedCtx = parser.trimParsingContext(startKernels, 2, applied.ctx)
      val result = CtxWithTasks(trimmedCtx, applied.tasks, applied.startKernelProgressTasks)

      val appendings = mutable.Buffer[(KernelTemplate, AppendingMilestoneGroup)]()
      val startNodeProgresses = mutable.Set[(KernelTemplate, AcceptConditionTemplate)]()

      val lookaheadCollector = mutable.Set[Int]()
      val pendedCollector = mutable.Map[KernelTemplate, (List[AppendingMilestoneGroup], Option[AcceptConditionTemplate])]()

      startKernelsMap.toList.foreach { startPair =>
        val (start, startKernel) = startPair

        val termAction = milestoneGen.termActionFor(startingCtx.ctx, startKernel, termProgressTasks)

        termAction.parsingAction.appendingMilestones.groupBy(_.acceptCondition).foreach { pair =>
          val condition = pair._1
          val kernelTemplates = pair._2.map(_.milestone).toSet
          appendings += start -> AppendingMilestoneGroup(builder.milestoneGroupId(kernelTemplates), condition)
        }

        termAction.parsingAction.startNodeProgressCondition match {
          case Some(condition) =>
            startNodeProgresses += (start -> condition)
          case None => // do nothing
        }

        lookaheadCollector ++= termAction.parsingAction.lookaheadRequiringSymbols
        termAction.pendedAcceptConditionKernels.foreach { pended =>
          // TODO
          // pendedCollector += pended._1 -> pended._2
        }
      }

      val startProgresses = startNodeProgresses.groupBy(_._2).toList.map { pair =>
        val condition = pair._1
        val kernelTemplates = pair._2.map(_._1)
        builder.milestoneGroupId(kernelTemplates.toSet) -> condition
      }

      val termAction = TermAction(
        ParsingAction(
          appendings.toList,
          startProgresses,
          lookaheadCollector.toSet,
          // TODO
          TasksSummary2(Map(), Set(), None)
        ),
        pendedCollector.toMap,
      )
      Some(termGroup -> termAction)
    }
  }

  def edgeProgressActionBetween(start: KernelTemplate, endGroupId: Int): EdgeAction = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)
    val endKernels = builder.milestonesOfGroup(endGroupId)

    val appendings = mutable.Buffer[AppendingMilestone]()
    val startNodeProgresses = mutable.Set[(KernelTemplate, AcceptConditionTemplate)]()

    val lookaheadCollector = mutable.Set[Int]()
    val edgeRequiresCollector = mutable.Set[Int]()

    endKernels.foreach { endKernel =>
      val edgeAction = milestoneGen.edgeProgressActionBetween(startKernel, endKernel, startingCtx)

      appendings ++= edgeAction.parsingAction.appendingMilestones

      edgeAction.parsingAction.startNodeProgressCondition match {
        case Some(condition) =>
          startNodeProgresses += (endKernel -> condition)
        case None => // do nothing
      }

      lookaheadCollector ++= edgeAction.parsingAction.lookaheadRequiringSymbols
      edgeRequiresCollector ++= edgeAction.requiredSymbols
    }

    val appendingMilestoneGroups = appendings.groupBy(_.acceptCondition).toList.map { pair =>
      start -> AppendingMilestoneGroup(builder.milestoneGroupId(pair._2.map(_.milestone).toSet), pair._1)
    }

    val startProgresses = startNodeProgresses.groupBy(_._2).toList.map { pair =>
      val condition = pair._1
      val kernelTemplates = pair._2.map(_._1)
      builder.milestoneGroupId(kernelTemplates.toSet) -> condition
    }

    EdgeAction(
      ParsingAction(
        appendingMilestoneGroups,
        startProgresses,
        lookaheadCollector.toSet,
        // TODO
        TasksSummary2(Map(), Set(), None)
      ),
      edgeRequiresCollector.toSet
    )
  }

  case class Jobs(milestoneGroupIds: Set[Int], edges: Set[(KernelTemplate, Int)])

  // key groupId 앞에 올 수 있는 milestone들
  private val precedings = mutable.Map[Int, mutable.Set[KernelTemplate]]()
  // key가 value로 replace돼서 reduce 실행 가능
  private val edgeActionTriggers = mutable.Map[Int, mutable.Set[Int]]()
  // key가 value의 milestone으로 치환 가능
  private val mgroupReplaced = mutable.Map[Int, mutable.Set[KernelTemplate]]()

  private def addParsingAction(mgroupId: Int, parsingAction: ParsingAction): Unit = {
    parsingAction.appendingMilestoneGroups.foreach { appending =>
      // milestoneGroupId 앞에 올 수 있는 mileestone들 -> appending._1가 뒤에 올 수 있고
      // milestone -> appending._1 (KernelTemplate)
      mgroupReplaced.getOrElseUpdate(mgroupId, mutable.Set()) += appending._1
      // appending._1 -> appending._2.groupId 뒤에 올 수 있음
      precedings.getOrElseUpdate(appending._2.groupId, mutable.Set()) += appending._1
    }
    parsingAction.startNodeProgress.foreach { progress =>
      // milestoneGroupId 앞에 올 수 있는 milestone들 -> progress._1 사이에 edge action 발생 가능
      // milestone -> mgroupId reduce trigger
      edgeActionTriggers.getOrElseUpdate(mgroupId, mutable.Set()) += progress._1
    }
  }

  private def possibleTips(): Set[Int] = {
    precedings.keySet.toSet
  }

  private def precedingMilestonesOf(groupId: Int): Set[KernelTemplate] = {
    precedings.get(groupId) match {
      case Some(value) => value.toSet
      case None => Set()
    }
  }

  private def possibleEdges(): Set[(KernelTemplate, Int)] = {
    val x = mutable.Set[(KernelTemplate, Int)]()
    edgeActionTriggers.foreach { pair =>
      // pair._1 앞에 올 수 있는 milestone들 -> pair._2
      precedingMilestonesOf(pair._1).foreach { preceding =>
        pair._2.foreach { following =>
          x += preceding -> following
        }
      }
    }
    x.toSet
  }

  private def createParserData(jobs: Jobs): Unit = {
    jobs.milestoneGroupIds.foreach { milestoneGroupId =>
      val termActions = termActionsFor(milestoneGroupId)
      builder.termActions(milestoneGroupId) = termActions

      termActions.foreach { case (_, termAction) =>
        addParsingAction(milestoneGroupId, termAction.parsingAction)
        termAction.pendedAcceptConditionKernels.foreach { pended =>
          pended._2._1.foreach { appending =>
            precedings.getOrElseUpdate(appending.groupId, mutable.Set()) += pended._1
          }
        }
      }
    }

    jobs.edges.foreach { edge =>
      val edgeAction = edgeProgressActionBetween(edge._1, edge._2)
      builder.edgeProgressActions(edge) = edgeAction

      addParsingAction(edge._2, edgeAction.parsingAction)
    }

    val allTips = possibleTips()
    val allEdges = possibleEdges()
    val remainingJobs = Jobs(
      allTips -- builder.termActions.keySet,
      allEdges -- builder.edgeProgressActions.keySet,
    )
    if (remainingJobs.milestoneGroupIds.nonEmpty || remainingJobs.edges.nonEmpty) {
      println(s"Remaining jobs: milestones=${remainingJobs.milestoneGroupIds.size}, edges=${remainingJobs.edges.size}")
      createParserData(remainingJobs)
    }
  }

  def parserData(): MilestoneGroupParserData = {
    val startGroupId = builder.milestoneGroupId(Set(start))
    createParserData(Jobs(Set(startGroupId), Set()))
    builder.build(startGroupId)
  }
}
