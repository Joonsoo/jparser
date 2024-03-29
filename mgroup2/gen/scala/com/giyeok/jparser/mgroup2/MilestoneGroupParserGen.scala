package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.{Inputs, NGrammar, Symbols}
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AlwaysTemplate, AppendingMilestone, CtxWithTasks, KernelTemplate, MilestoneParserGen, NeverTemplate, ParserGenBase2, TasksSummary2, TermAction => MilestoneTermAction, EdgeAction => MilestoneEdgeAction}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.opt.OptNaiveParser2
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2, ProgressTask}
import com.giyeok.jparser.utils.TermGrouper

import scala.collection.mutable

class MilestoneGroupParserGen(val grammar: NGrammar) {
  private val optParser = new OptNaiveParser2(grammar)
  private val base = new ParserGenBase2(optParser)
  private val milestoneGen = new MilestoneParserGen(grammar)

  val start: KernelTemplate = KernelTemplate(grammar.startSymbol, 0)
  val startingCtx: (Kernel, CtxWithTasks) = base.startingCtxFrom(start, 0)

  val builder = new MilestoneGroupParserDataBuilder(grammar, startingCtx._2.tasksSummary(0))

  class TermActionBuilder(
    val appendingMilestones: mutable.ListBuffer[(KernelTemplate, AppendingMilestone)],
    val startNodeProgress: mutable.ListBuffer[(KernelTemplate, AcceptConditionTemplate)],
    val lookaheadRequiringSymbols: mutable.Set[Int],
    val addedKernels: mutable.Map[AcceptConditionTemplate, mutable.Set[Kernel]],
    val progressedKernels: mutable.Set[Kernel],
    val pendedAcceptConditionAppendings: mutable.Map[KernelTemplate, mutable.ListBuffer[AppendingMilestone]],
    val pendedAcceptConditionProgresses: mutable.Map[KernelTemplate, Option[AcceptConditionTemplate]],
  ) {
    def build(): TermAction = {
      TermAction(
        appendingMilestoneGroups = appendingMilestones.groupBy(_._1).toList.map { case (replace, appendings) =>
          appendings.groupBy(_._2.acceptCondition).toList.map { case (condition, appendings) =>
            val appendingKernels = appendings.map(_._2.milestone).toSet
            replace -> AppendingMilestoneGroup(builder.milestoneGroupId(appendingKernels), condition)
          }
        }.flatten,
        startNodeProgress = startNodeProgress.groupBy(_._2).map { pair =>
          builder.milestoneGroupId(pair._2.map(_._1).toSet) -> pair._1
        }.toList,
        lookaheadRequiringSymbols = lookaheadRequiringSymbols.map { symbolId =>
          LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
        }.toSet,
        TasksSummary2(
          addedKernels.view.mapValues(_.toSet).toMap,
          progressedKernels.toSet,
        ),
        pendedAcceptConditionKernels = pendedAcceptConditionAppendings.map { case (first, appendings) =>
          val appendingGroups = appendings.groupBy(_.acceptCondition).map { case (condition, appendings) =>
            AppendingMilestoneGroup(builder.milestoneGroupId(appendings.map(_.milestone).toSet), condition)
          }.toList
          first -> (appendingGroups, pendedAcceptConditionProgresses(first))
        }.toMap
      )
    }
  }

  def termGroupsOf(graph: KernelGraph): List[TermGroupDesc] = {
    val termNodes = graph.nodes
      .filter { node => grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal] }
      .filter { node => node.pointer == 0 }
    assert(termNodes.forall { kernel => kernel.beginGen == 1 })
    val terms = termNodes.map { node => grammar.symbolOf(node.symbolId) }
      .map { case terminal: NTerminal => terminal.symbol }
    TermGrouper.termGroupsOf(terms)
  }

  def hasIntersection(tg1: TermGroupDesc, tg2: TermGroupDesc): Boolean = {
    tg1 match {
      case tg1: Inputs.CharacterTermGroupDesc =>
        tg2 match {
          case tg2: Inputs.CharacterTermGroupDesc =>
            !tg1.intersect(tg2).isEmpty
          case _: Inputs.VirtualTermGroupDesc => false
        }
      case tg1: Inputs.VirtualTermGroupDesc =>
        tg2 match {
          case _: Inputs.CharacterTermGroupDesc => false
          case tg2: Inputs.VirtualTermGroupDesc =>
            !tg1.intersect(tg2).isEmpty
        }
    }
  }

  private val milestoneTermActionsCache = mutable.Map[KernelTemplate, List[(TermGroupDesc, MilestoneTermAction)]]()
  private val milestoneEdgeActionsCache = mutable.Map[(KernelTemplate, KernelTemplate), MilestoneEdgeAction]()

  def getMilestoneTermActions(startKernel: KernelTemplate): List[(TermGroupDesc, MilestoneTermAction)] = {
    milestoneTermActionsCache.get(startKernel) match {
      case Some(cached) => cached
      case None =>
        val calced = milestoneGen.termActionsFor(startKernel)
        milestoneTermActionsCache(startKernel) = calced
        calced
    }
  }

  def getMilestoneEdgeAction(start: Kernel, end: KernelTemplate, startingCtx: CtxWithTasks): MilestoneEdgeAction = {
    val edge = KernelTemplate(start.symbolId, start.pointer) -> end
    milestoneEdgeActionsCache.get(edge) match {
      case Some(cached) => cached
      case None =>
        val calced = milestoneGen.edgeProgressActionBetween(start, end, startingCtx)
        milestoneEdgeActionsCache(edge) = calced
        calced
    }
  }

  def getMilestoneEdgeAction(startKernel: KernelTemplate, endKernel: KernelTemplate): MilestoneEdgeAction = {
    val edge = startKernel -> endKernel
    milestoneEdgeActionsCache.get(edge) match {
      case Some(cached) => cached
      case None =>
        val calced = milestoneGen.edgeProgressActionBetween(startKernel, endKernel)
        milestoneEdgeActionsCache(edge) = calced
        calced
    }
  }

  def termActionsFor(groupId: Int): List[(TermGroupDesc, TermAction)] = {
    val starts = builder.milestonesOfGroup(groupId)
    // lookahead requiring때문에 들어오게 된 경우 start가 딱 한 개 있고, 터미널인 경우가 있을 수 있음. 그 외의 경우엔 터미널이 들어오면 안됨
    if (starts.size == 1 && grammar.symbolOf(starts.head.symbolId).isInstanceOf[NTerminal]) {
      val termId = starts.head.symbolId
      val termGroup = grammar.symbolOf(termId).symbol match {
        case term: Symbols.Terminals.CharacterTerminal => TermGroupDesc.descOf(term)
        case term: Symbols.Terminals.VirtualTerminal => TermGroupDesc.descOf(term)
      }
      List(termGroup -> TermAction(
        appendingMilestoneGroups = List(),
        startNodeProgress = List(groupId -> AlwaysTemplate),
        lookaheadRequiringSymbols = Set(),
        tasksSummary = TasksSummary2(
          addedKernels = Map(AlwaysTemplate -> Set(Kernel(termId, 1, 0, 2))),
          progressedKernels = Set(Kernel(termId, 0, 0, 1))
        ),
        pendedAcceptConditionKernels = Map(),
      ))
    } else {
      assert(starts.forall(start => !grammar.symbolOf(start.symbolId).isInstanceOf[NTerminal]))

      val (startKernelsMap, startingCtx) = base.startingCtxFrom(starts, 0)

      val termActionsBuilder = termGroupsOf(startingCtx.ctx.graph).map(_ -> new TermActionBuilder(
        mutable.ListBuffer(),
        mutable.ListBuffer(),
        mutable.Set(),
        mutable.Map(),
        mutable.Set(),
        mutable.Map(),
        mutable.Map(),
      ))

      startKernelsMap.keySet.foreach { startKernel =>
        val milestoneTermActions = getMilestoneTermActions(startKernel)

        milestoneTermActions.foreach { case (milestoneTermGroup, milestoneAction) =>
          termActionsBuilder.foreach { case (mgroupTermGroup, builder) =>
            if (hasIntersection(mgroupTermGroup, milestoneTermGroup)) {
              builder.appendingMilestones ++= milestoneAction.parsingAction.appendingMilestones.map(startKernel -> _)
              builder.startNodeProgress ++= milestoneAction.parsingAction.startNodeProgressCondition.map(startKernel -> _)
              builder.lookaheadRequiringSymbols ++= milestoneAction.parsingAction.lookaheadRequiringSymbols
              milestoneAction.parsingAction.tasksSummary.addedKernels.foreach { pair =>
                builder.addedKernels.getOrElseUpdate(pair._1, mutable.Set()) ++= pair._2
              }
              builder.progressedKernels ++= milestoneAction.parsingAction.tasksSummary.progressedKernels
              milestoneAction.pendedAcceptConditionKernels.foreach { pended =>
                if (builder.pendedAcceptConditionAppendings.contains(pended._1)) {
                  builder.pendedAcceptConditionAppendings(pended._1) ++= pended._2._1
                  builder.pendedAcceptConditionProgresses(pended._1) match {
                    case Some(existingCondition) =>
                      pended._2._2 match {
                        case Some(newCondition) =>
                          builder.pendedAcceptConditionProgresses(pended._1) =
                            Some(AcceptConditionTemplate.disjunct(Set(existingCondition, newCondition)))
                        case None => // do nothing
                      }
                    case None => builder.pendedAcceptConditionProgresses(pended._1) = pended._2._2
                  }
                } else {
                  builder.pendedAcceptConditionAppendings(pended._1) = mutable.ListBuffer(pended._2._1: _*)
                  builder.pendedAcceptConditionProgresses(pended._1) = pended._2._2
                }
              }
            }
          }
        }
      }

      termActionsBuilder.map { pair =>
        val termAction = pair._2.build()
        pair._1 -> termAction
      }
    }
  }

  // TODO termActionsFor(위 함수)와 비교 - 중요하진 않지만 그냥 궁금해서..
  def termActionsFor_(groupId: Int): List[(TermGroupDesc, TermAction)] = {
    val starts = builder.milestonesOfGroup(groupId)
    val (startKernelsMap, startingCtx) = base.startingCtxFrom(starts, 0)

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
    //    val edgeRequiresCollector = mutable.Set[Int]()

    val addedKernels = mutable.Map[AcceptConditionTemplate, mutable.Set[Kernel]]()
    val progressedKernels = mutable.Set[Kernel]()

    endKernels.foreach { endKernel =>
      val edgeAction = getMilestoneEdgeAction(startKernel, endKernel, startingCtx)

      appendings ++= edgeAction.parsingAction.appendingMilestones

      edgeAction.parsingAction.startNodeProgressCondition match {
        case Some(condition) =>
          startNodeProgresses += condition
        case None => // do nothing
      }

      lookaheadCollector ++= edgeAction.parsingAction.lookaheadRequiringSymbols
      //      edgeRequiresCollector ++= edgeAction.requiredSymbols

      edgeAction.parsingAction.tasksSummary.addedKernels.foreach { case (condition, added) =>
        addedKernels.getOrElseUpdate(condition, mutable.Set()) ++= added
      }
      progressedKernels ++= edgeAction.parsingAction.tasksSummary.progressedKernels
    }

    val appendingMilestoneGroups = appendings.groupBy(_.acceptCondition).toList.map { pair =>
      AppendingMilestoneGroup(builder.milestoneGroupId(pair._2.map(_.milestone).toSet), pair._1)
    }

    val startNodeProgresses1 = startNodeProgresses.toSet.filter(_ != NeverTemplate)
    val startNodeProgress = if (startNodeProgresses1.isEmpty) None else {
      Some(AcceptConditionTemplate.disjunct(startNodeProgresses1))
    }

    EdgeAction(
      appendingMilestoneGroups = appendingMilestoneGroups,
      startNodeProgress = startNodeProgress,
      lookaheadRequiringSymbols = lookaheadCollector.toSet.map { symbolId =>
        LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
      },
      tasksSummary = TasksSummary2(addedKernels.view.mapValues(_.toSet).toMap, progressedKernels.toSet),
    )
  }

  def tipEdgeRequiredSymbolsBetween(start: KernelTemplate, endGroupId: Int): Set[Int] = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)
    val endKernels = builder.milestonesOfGroup(endGroupId)

    val edgeRequiresCollector = mutable.Set[Int]()

    endKernels.foreach { endKernel =>
      // TODO 버려지는 정보가 많으니 최적화할 수 있을 것
      val edgeAction = getMilestoneEdgeAction(startKernel, endKernel, startingCtx)

      edgeRequiresCollector ++= edgeAction.requiredSymbols
    }

    edgeRequiresCollector.toSet
  }

  def midEdgeProgressActionBetween(start: KernelTemplate, end: KernelTemplate): (EdgeAction, Set[Int]) = {
    val edgeAction = getMilestoneEdgeAction(start, end)

    val appendingMilestoneGroups = edgeAction.parsingAction.appendingMilestones.groupBy(_.acceptCondition).toList.map { pair =>
      AppendingMilestoneGroup(builder.milestoneGroupId(pair._2.map(_.milestone).toSet), pair._1)
    }

    val mgroupEdgeAction = EdgeAction(
      appendingMilestoneGroups = appendingMilestoneGroups,
      startNodeProgress = edgeAction.parsingAction.startNodeProgressCondition,
      lookaheadRequiringSymbols = edgeAction.parsingAction.lookaheadRequiringSymbols.map { symbolId =>
        LookaheadRequires(symbolId, builder.milestoneGroupId(Set(KernelTemplate(symbolId, 0))))
      },
      tasksSummary = edgeAction.parsingAction.tasksSummary,
    )
    (mgroupEdgeAction, edgeAction.requiredSymbols)
  }

  case class Jobs(
    milestoneGroupIds: Set[Int],
    progressableTipEdges: Set[(KernelTemplate, Int)],
    existableTipEdges: Set[(KernelTemplate, Int)],
    midEdges: Set[(KernelTemplate, KernelTemplate)]) {
    def nonEmpty: Boolean = milestoneGroupIds.nonEmpty || progressableTipEdges.nonEmpty || existableTipEdges.nonEmpty || midEdges.nonEmpty
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
    precedingOfGroups.keySet.toSet ++ lookaheadRequires
  }

  private def precedingMilestonesOf(groupId: Int): Set[KernelTemplate] = {
    precedingOfGroups.get(groupId) match {
      case Some(value) => value.toSet
      case None => Set()
    }
  }

  // progress될 수 있는 tip edge 조합, 존재할 수 있는 tip edge 조합
  private def possibleTipEdges(): (Set[(KernelTemplate, Int)], Set[(KernelTemplate, Int)]) = {
    val progressable = mutable.Set[(KernelTemplate, Int)]()
    val existable = mutable.Set[(KernelTemplate, Int)]()
    edgeActionTriggers.foreach { pair =>
      // pair._1 앞에 올 수 있는 milestone들 -> pair._2
      precedingMilestonesOf(pair._1).foreach { preceding =>
        pair._2.foreach { following =>
          progressable += preceding -> following
        }
      }
    }
    precedingOfGroups.foreach { pair =>
      pair._2.foreach { preceding =>
        existable += preceding -> pair._1
      }
    }
    (progressable.toSet, existable.toSet)
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

    jobs.progressableTipEdges.foreach { edge =>
      val edgeAction = tipEdgeProgressActionBetween(edge._1, edge._2)
      builder.tipEdgeProgressActions(edge) = edgeAction

      addEdgeAction(edge._1, edgeAction)
    }

    jobs.existableTipEdges.foreach { edge =>
      val requiredSymbols = tipEdgeRequiredSymbolsBetween(edge._1, edge._2)
      builder.tipEdgeRequiredSymbols(edge) = requiredSymbols
    }

    jobs.midEdges.foreach { edge =>
      val (edgeAction, requiredSymbols) = midEdgeProgressActionBetween(edge._1, edge._2)
      builder.midEdgeProgressActions(edge) = edgeAction
      builder.midEdgeRequiredSymbols(edge) = requiredSymbols

      addEdgeAction(edge._1, edgeAction)
    }

    val allTips = possibleTips()
    val (allProgressableTipEdges, allExistableTipEdges) = possibleTipEdges()
    val allMidEdges = possibleMidEdges()
    val remainingJobs = Jobs(
      allTips -- builder.termActions.keySet,
      allProgressableTipEdges -- builder.tipEdgeProgressActions.keySet,
      allExistableTipEdges -- builder.tipEdgeRequiredSymbols.keySet,
      allMidEdges -- builder.midEdgeProgressActions.keySet,
    )
    if (remainingJobs.nonEmpty) {
      println(s"Remaining jobs: mgroups=${remainingJobs.milestoneGroupIds.size}, prTipEdges=${remainingJobs.progressableTipEdges.size}, exTipEdges=${remainingJobs.existableTipEdges.size}, midEdges=${remainingJobs.midEdges.size}")
      createParserData(remainingJobs)
    }
  }

  def parserData(): MilestoneGroupParserData = {
    val startGroupId = builder.milestoneGroupId(Set(start))
    createParserData(Jobs(Set(startGroupId), Set(), Set(), Set()))
    builder.build(startGroupId)
  }
}
