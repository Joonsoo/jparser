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

  private val builder = new MilestoneGroupParserDataBuilder(grammar, startingCtx._2.tasksSummary)

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

  private def createParserData(jobs: Jobs): Unit = {
    val tips = mutable.Set[Int]()
    val edges = mutable.Set[(KernelTemplate, Int)]()

    jobs.milestoneGroupIds.foreach { milestoneGroupId =>
      val termActions = termActionsFor(milestoneGroupId)
      builder.termActions(milestoneGroupId) = termActions

      termActions.foreach { case (_, termAction) =>
        termAction.parsingAction.appendingMilestoneGroups.foreach { appending =>
          tips += appending._2.groupId
          edges += appending._1 -> appending._2.groupId
        }
      }
    }

    jobs.edges.foreach { edge =>
      val edgeAction = edgeProgressActionBetween(edge._1, edge._2)
      builder.edgeProgressActions(edge) = edgeAction

      edgeAction.parsingAction.appendingMilestoneGroups.foreach { appending =>
        tips += appending._2.groupId
        edges += appending._1 -> appending._2.groupId
      }
    }

    val remainingJobs = Jobs(
      (tips -- builder.termActions.keySet).toSet,
      (edges -- builder.edgeProgressActions.keySet).toSet,
    )
    if (remainingJobs.milestoneGroupIds.nonEmpty || remainingJobs.edges.nonEmpty) {
      println(s"Remaining jobs: milestones=${remainingJobs.milestoneGroupIds.size}, edges=${remainingJobs.edges.size}")
      createParserData(remainingJobs)
    }
  }

  def parserData(): MilestoneGroupParserData = {
    //    val initGroup = builder.milestoneGroupId(Set(start))
    //    val initTermActions = termActionsFor(builder, initGroup)
    //    println(initTermActions)
    //
    //    val termActions2 = termActionsFor(builder, 2)
    //    println(termActions2)
    //
    //    val edgeAction1 = edgeProgressActionBetween(builder, KernelTemplate(1, 0), 4)
    //    println(edgeAction1)
    val startGroupId = builder.milestoneGroupId(Set(start))
    createParserData(Jobs(Set(startGroupId), Set()))
    builder.build(startGroupId)
  }
}
