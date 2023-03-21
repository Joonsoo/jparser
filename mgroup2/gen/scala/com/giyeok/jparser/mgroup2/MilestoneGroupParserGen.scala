package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AppendingMilestone, CtxWithTasks, KernelTemplate, MilestoneParserGen, ParserGenBase2, TasksSummary2}
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

      val appendings = mutable.Buffer[(KernelTemplate, AppendingMilestoneGroup)]()
      val startNodeProgresses = mutable.Set[(KernelTemplate, AcceptConditionTemplate)]()

      val lookaheadCollector = mutable.Set[Int]()
      val pendedCollector = mutable.Map[KernelTemplate, (List[AppendingMilestoneGroup], Option[AcceptConditionTemplate])]()

      val addedKernels = mutable.Map[AcceptConditionTemplate, mutable.Set[Kernel]]()
      val progressedKernels = mutable.Set[Kernel]()

      startKernelsMap.toList.foreach { startPair =>
        val (start, startKernel) = startPair
        assert(starts.contains(start))

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
          val progressCondition = pended._2._2
          val appendings = pended._2._1.groupBy(_.acceptCondition).map { pair =>
            val condition = pair._1
            val kernelTemplates = pair._2.map(_.milestone).toSet
            AppendingMilestoneGroup(builder.milestoneGroupId(kernelTemplates), condition)
          }
          pendedCollector += pended._1 -> (appendings.toList, progressCondition)
        }

        termAction.parsingAction.tasksSummary.addedKernels.foreach { case (condition, added) =>
          addedKernels.getOrElseUpdate(condition, mutable.Set()) ++= added
        }
        progressedKernels ++= termAction.parsingAction.tasksSummary.progressedKernels
      }

      val startProgresses = startNodeProgresses.groupBy(_._2).toList.map { pair =>
        val condition = pair._1
        val kernelTemplates = pair._2.map(_._1)
        builder.milestoneGroupId(kernelTemplates.toSet) -> condition
      }

      val termAction = TermAction(
        appendingMilestoneGroups = appendings.toList,
        startNodeProgress = startProgresses,
        lookaheadRequiringSymbols = lookaheadCollector.toSet.map { symbolId =>
          LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
        },
        tasksSummary = TasksSummary2(addedKernels.view.mapValues(_.toSet).toMap, progressedKernels.toSet),
        pendedAcceptConditionKernels = pendedCollector.toMap,
      )
      Some(termGroup -> termAction)
    }
  }

  def tipEdgeProgressActionBetween(start: KernelTemplate, endGroupId: Int): EdgeAction = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)
    val endKernels = builder.milestonesOfGroup(endGroupId)

    val appendings = mutable.Buffer[AppendingMilestone]()
    val startNodeProgresses = mutable.Set[AcceptConditionTemplate]()

    val lookaheadCollector = mutable.Set[Int]()
    val edgeRequiresCollector = mutable.Set[Int]()

    val addedKernels = mutable.Map[AcceptConditionTemplate, mutable.Set[Kernel]]()
    val progressedKernels = mutable.Set[Kernel]()

    endKernels.foreach { endKernel =>
      val edgeAction = milestoneGen.edgeProgressActionBetween(startKernel, endKernel, startingCtx)

      appendings ++= edgeAction.parsingAction.appendingMilestones

      edgeAction.parsingAction.startNodeProgressCondition match {
        case Some(condition) =>
          startNodeProgresses += condition
        case None => // do nothing
      }

      lookaheadCollector ++= edgeAction.parsingAction.lookaheadRequiringSymbols
      edgeRequiresCollector ++= edgeAction.requiredSymbols

      edgeAction.parsingAction.tasksSummary.addedKernels.foreach { case (condition, added) =>
        addedKernels.getOrElseUpdate(condition, mutable.Set()) ++= added
      }
      progressedKernels ++= edgeAction.parsingAction.tasksSummary.progressedKernels
    }

    val appendingMilestoneGroups = appendings.groupBy(_.acceptCondition).toList.map { pair =>
      AppendingMilestoneGroup(builder.milestoneGroupId(pair._2.map(_.milestone).toSet), pair._1)
    }

    EdgeAction(
      appendingMilestoneGroups = appendingMilestoneGroups,
      startNodeProgress = startNodeProgresses.toList,
      lookaheadRequiringSymbols = lookaheadCollector.toSet.map { symbolId =>
        LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
      },
      tasksSummary = TasksSummary2(addedKernels.view.mapValues(_.toSet).toMap, progressedKernels.toSet),
      requiredSymbols = edgeRequiresCollector.toSet
    )
  }

  def midEdgeProgressActionBetween(start: KernelTemplate, end: KernelTemplate): EdgeAction = {
    val edgeAction = milestoneGen.edgeProgressActionBetween(start, end)

    val appendingMilestoneGroups = edgeAction.parsingAction.appendingMilestones.groupBy(_.acceptCondition).toList.map { pair =>
      AppendingMilestoneGroup(builder.milestoneGroupId(pair._2.map(_.milestone).toSet), pair._1)
    }

    EdgeAction(
      appendingMilestoneGroups = appendingMilestoneGroups,
      startNodeProgress = edgeAction.parsingAction.startNodeProgressCondition.toList,
      lookaheadRequiringSymbols = edgeAction.parsingAction.lookaheadRequiringSymbols.map { symbolId =>
        LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
      },
      tasksSummary = edgeAction.parsingAction.tasksSummary,
      requiredSymbols = edgeAction.requiredSymbols
    )
  }

  case class Jobs(
    milestoneGroupIds: Set[Int],
    tipEdges: Set[(KernelTemplate, Int)],
    midEdges: Set[(KernelTemplate, KernelTemplate)]) {
    def nonEmpty: Boolean = milestoneGroupIds.nonEmpty || tipEdges.nonEmpty || midEdges.nonEmpty
  }

  // key groupId 앞에 올 수 있는 milestone들
  private val precedingOfGroups = mutable.Map[Int, mutable.Set[KernelTemplate]]()
  // lookaheadRequires 때문에 tip에 올 수 있는 mgroup id들
  private val lookaheadRequires = mutable.Set[Int]()
  // key가 value로 replace돼서 reduce 실행 가능
  private val edgeActionTriggers = mutable.Map[Int, mutable.Set[Int]]()
  // key가 value의 milestone으로 치환 가능
  private val mgroupReplaced = mutable.Map[Int, mutable.Set[KernelTemplate]]()

  private def addTermAction(mgroupId: Int, termAction: TermAction): Unit = {
    termAction.appendingMilestoneGroups.foreach { appending =>
      // milestoneGroupId 앞에 올 수 있는 milestone들 -> appending._1가 뒤에 올 수 있고
      // milestone -> appending._1 (KernelTemplate)
      mgroupReplaced.getOrElseUpdate(mgroupId, mutable.Set()) += appending._1
      // appending._1 -> appending._2.groupId 뒤에 올 수 있음
      precedingOfGroups.getOrElseUpdate(appending._2.groupId, mutable.Set()) += appending._1
    }
    termAction.startNodeProgress.foreach { progress =>
      // milestoneGroupId 앞에 올 수 있는 milestone들 -> progress._1 사이에 edge action 발생 가능
      // milestone -> mgroupId reduce trigger
      edgeActionTriggers.getOrElseUpdate(mgroupId, mutable.Set()) += progress._1
    }
    termAction.pendedAcceptConditionKernels.foreach { pended =>
      pended._2._1.foreach { appending =>
        precedingOfGroups.getOrElseUpdate(appending.groupId, mutable.Set()) += pended._1
      }
    }
    lookaheadRequires ++= termAction.lookaheadRequiringSymbols.map(_.groupId)
  }

  private def addEdgeAction(edgeStart: KernelTemplate, edgeAction: EdgeAction): Unit = {
    edgeAction.appendingMilestoneGroups.foreach { appending =>
      precedingOfGroups.getOrElseUpdate(appending.groupId, mutable.Set()) += edgeStart
    }
    lookaheadRequires ++= edgeAction.lookaheadRequiringSymbols.map(_.groupId)
  }

  private def possibleTips(): Set[Int] = {
    precedingOfGroups.keySet.toSet
  }

  private def precedingMilestonesOf(groupId: Int): Set[KernelTemplate] = {
    precedingOfGroups.get(groupId) match {
      case Some(value) => value.toSet
      case None => Set()
    }
  }

  private def possibleTipEdges(): Set[(KernelTemplate, Int)] = {
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

  private def possibleMidEdges(): Set[(KernelTemplate, KernelTemplate)] = {
    val x = mutable.Set[(KernelTemplate, KernelTemplate)]()
    mgroupReplaced.foreach { pair =>
      assert(pair._2.subsetOf(builder.milestonesOfGroup(pair._1)))
      precedingOfGroups.getOrElse(pair._1, mutable.Set()).foreach { preceding =>
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
        addTermAction(milestoneGroupId, termAction)
      }
    }

    jobs.tipEdges.foreach { edge =>
      val edgeAction = tipEdgeProgressActionBetween(edge._1, edge._2)
      builder.tipEdgeProgressActions(edge) = edgeAction

      addEdgeAction(edge._1, edgeAction)
    }

    jobs.midEdges.foreach { edge =>
      val edgeAction = midEdgeProgressActionBetween(edge._1, edge._2)
      builder.midEdgeProgressActions(edge) = edgeAction

      addEdgeAction(edge._1, edgeAction)
    }

    val allTips = possibleTips()
    val allTipEdges = possibleTipEdges()
    val allMidEdges = possibleMidEdges()
    val remainingJobs = Jobs(
      allTips -- builder.termActions.keySet,
      allTipEdges -- builder.tipEdgeProgressActions.keySet,
      allMidEdges -- builder.midEdgeProgressActions.keySet,
    )
    if (remainingJobs.nonEmpty) {
      println(s"Remaining jobs: mgroups=${remainingJobs.milestoneGroupIds.size}, tipEdges=${remainingJobs.tipEdges.size}, midEdges=${remainingJobs.midEdges.size}")
      createParserData(remainingJobs)
    }
  }

  def parserData(): MilestoneGroupParserData = {
    val startGroupId = builder.milestoneGroupId(Set(start))
    createParserData(Jobs(Set(startGroupId), Set(), Set()))
    builder.build(startGroupId)
  }
}
