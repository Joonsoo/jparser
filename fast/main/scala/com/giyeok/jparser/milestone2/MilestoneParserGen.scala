package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NSequence, NTerminal}
import com.giyeok.jparser.fast.{CtxWithTasks, KernelTemplate, ParserGenBase2}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, conjunct, disjunct}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.utils.Utils
import com.giyeok.jparser.nparser2.{DeriveTask, Edge, KernelGraph, NaiveParser2, ProgressTask, ParsingContext => NaiveParsingContext}
import com.giyeok.jparser.utils.TermGrouper

import scala.collection.mutable

// naive parser 1과 2의 차이점은 accept condition이 그래프 안에 있냐 밖에 있냐의 차이
// milestone parser 1과 2의 차이점도 비슷
class MilestoneParserGen(val parser: NaiveParser2) {
  val base = ParserGenBase2(parser)

  def applyProgressTasks(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): CtxWithTasks = {
    val result = base.runTasksWithProgressBarrier(2,
      tasks = progressTasks,
      barrierNode = startKernel,
      cc = CtxWithTasks(ctx, List(), List()))
    val trimmedCtx = parser.trimParsingContext(startKernel, 2, result.ctx)

    CtxWithTasks(trimmedCtx, result.tasks, result.startKernelProgressTasks)
  }

  // beginGen은 assertion용
  // beginGen이 1이면 term action 계산중, 0이면 edge action 계산중
  def appendingMilestoneCandidatesOf(
    result: CtxWithTasks,
    beginGen: Int,
    start: Kernel,
  ): List[Kernel] = {
    if (!result.ctx.graph.nodes.contains(start)) {
      // start가 progress되면서 종료되는 경우엔 그래프에 start가 없을 수 있음
      List()
    } else {
      val appendingMilestoneCandidates = result.deriveTasks
        .map(_.kernel)
        .filter(result.ctx.graph.nodes.contains)
        .filter(kernel => parser.grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
        .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen)
        .filter(kernel => result.ctx.graph.reachableBetween(start, kernel))
      assert(appendingMilestoneCandidates.forall(kernel => kernel.beginGen == beginGen && kernel.endGen == 2))
      appendingMilestoneCandidates
    }
  }

  def termAction(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): TermAction =
    parsingActionFromProgressResultForTermAction(
      applyProgressTasks(ctx, startKernel, progressTasks), startKernel)

  def appendingMilestonesForTermAction(
    result: CtxWithTasks,
    start: Kernel,
    forAcceptConditions: mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])]
  ): List[AppendingMilestone] = {
    appendingMilestoneCandidatesOf(result, 1, start).map { kernel =>
      val condition = conditionToTemplateForTermAction(result, start, forAcceptConditions, result.ctx.acceptConditions(kernel))
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }
  }

  // beginGen은 assertion용
  def parsingActionFromProgressResultForTermAction(result: CtxWithTasks, start: Kernel): TermAction = {
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    val forAcceptConditions = mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])]()
    val appendingMilestones = appendingMilestonesForTermAction(result, start, forAcceptConditions)
    val startNodeProgressTasks = result.progressTasks.filter(_.kernel == start)
    val startNodeProgressCondition = startNodeProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition)
          .map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _))
        Some(AcceptConditionTemplate.disjunct(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      tasksSummary = result.tasksSummary,
    )
    TermAction(parsingAction, forAcceptConditions.toMap)
  }

  // beginGen은 assertion용
  private def conditionToTemplateForTermAction(
    result: CtxWithTasks,
    start: Kernel,
    forAcceptConditions: mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])],
    condition: AcceptCondition
  ): AcceptConditionTemplate = {
    def addForAcceptConditionTemplate(symbolId: Int): Unit = {
      val symbolStart = Kernel(symbolId, 0, 1, 1)
      val appendingMilestones = appendingMilestonesForTermAction(result, symbolStart, forAcceptConditions)
      val progresses = result.progressTasks.filter(_.kernel == symbolStart)
      val progressCondition = disjunct(progresses.map(_.condition): _*)
      val progressConditionTemplate = if (progressCondition == Never) None else {
        Some(conditionToTemplateForTermAction(result, symbolStart, forAcceptConditions, progressCondition))
      }
      forAcceptConditions(KernelTemplate(symbolId, 0)) = (appendingMilestones, progressConditionTemplate)
    }

    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _)))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _)))
      case AcceptCondition.NotExists(1, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        // TODO forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
        // TODO result.progressTasks.filter(_.kernel == Kernel(symbolId, 0, 1, 1)) 에 대한 정보 추가
        addForAcceptConditionTemplate(symbolId)
        LongestTemplate(symbolId)
      case AcceptCondition.Unless(1, 2, symbolId) =>
        // except
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
          AlwaysTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addForAcceptConditionTemplate(symbolId)
          UnlessTemplate(symbolId)
        }
      case AcceptCondition.OnlyIf(1, 2, symbolId) =>
        // join
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addForAcceptConditionTemplate(symbolId)
          OnlyIfTemplate(symbolId)
        }
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead except
        // - lookahead 심볼은 이미 가망이 없어진 경우가 아니라면 앞으로의 상황만 보기 때문에
        //   start만 있고 appending milestone은 없는(길이가 1인) path를 추가해야 할듯?
        //   즉, forAcceptConditions는 건드릴 필요 없을듯
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          NotExistsTemplate(symbolId)
        }
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // NotExists와 같은 이유로 forAcceptConditions는 건드릴 필요 없음
          ExistsTemplate(symbolId)
        }
    }
  }

  def termActionsFrom(start: KernelTemplate): List[(TermGroupDesc, TermAction)] = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, 0)

    // new DotGraphGenerator(parser.grammar).addGraph(derived).printDotGraph()

    val derived = startingCtx.ctx.graph.nodes
    val terms = derived.map { node => parser.grammar.symbolOf(node.symbolId) }
      .collect { case terminal: NTerminal => terminal.symbol }
    val termGroups = TermGrouper.termGroupsOf(terms)

    termGroups.map { termGroup =>
      val termNodes = startingCtx.ctx.graph.nodes.filter { kernel =>
        val symbol = parser.grammar.symbolOf(kernel.symbolId)
        symbol.isInstanceOf[NTerminal] && kernel.pointer == 0 && kernel.beginGen == 1
      }.filter { kernel =>
        val symbol = parser.grammar.symbolOf(kernel.symbolId)
        symbol.asInstanceOf[NTerminal].symbol.acceptTermGroup(termGroup)
      }
      val termProgressTasks = termNodes.toList.map(ProgressTask(_, AcceptCondition.Always))

      termGroup -> termAction(startingCtx.ctx, startKernel, termProgressTasks)
    }
  }


  def parsingActionForEdgeAction(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): EdgeAction =
    edgeActionFrom(
      applyProgressTasks(ctx, startKernel, progressTasks),
      startKernel)

  def appendingMilestonesForEdgeAction(
    result: CtxWithTasks,
    start: Kernel,
    edgeRequires: mutable.Set[Int]
  ): List[AppendingMilestone] = {
    appendingMilestoneCandidatesOf(result, 0, start).map { kernel =>
      val condition = conditionToTemplateForEdgeAction(result, start, edgeRequires, result.ctx.acceptConditions(kernel))
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }
  }

  def edgeActionFrom(result: CtxWithTasks, start: Kernel): EdgeAction = {
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    val edgeRequires = mutable.Set[Int]()
    val appendingMilestones = appendingMilestonesForEdgeAction(result, start, edgeRequires)
    val startNodeProgressTasks = result.progressTasks.filter(_.kernel == start)
    val startNodeProgressCondition = startNodeProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition)
          .map(conditionToTemplateForEdgeAction(result, start, edgeRequires, _))
        Some(AcceptConditionTemplate.disjunct(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      tasksSummary = result.tasksSummary,
    )
    EdgeAction(parsingAction, edgeRequires.toSet)
  }

  // beginGen은 assertion용
  private def conditionToTemplateForEdgeAction(
    result: CtxWithTasks,
    start: Kernel,
    needsToKeep: mutable.Set[Int],
    condition: AcceptCondition
  ): AcceptConditionTemplate = {
    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplateForEdgeAction(result, start, needsToKeep, _)))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplateForEdgeAction(result, start, needsToKeep, _)))
      case AcceptCondition.NotExists(0, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        // TODO forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
        needsToKeep += symbolId
        LongestTemplate(symbolId)
      case AcceptCondition.Unless(0, 2, symbolId) =>
        // except
        needsToKeep += symbolId
        UnlessTemplate(symbolId)
      case AcceptCondition.OnlyIf(0, 2, symbolId) =>
        // join
        needsToKeep += symbolId
        OnlyIfTemplate(symbolId)
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead except
        needsToKeep += symbolId
        NotExistsTemplate(symbolId)
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        needsToKeep += symbolId
        ExistsTemplate(symbolId)
    }
  }

  def edgeProgressActionBetween(start: KernelTemplate, end: KernelTemplate): EdgeAction = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)

    val derived = startingCtx.ctx.graph
    val endKernelInitials = derived.nodes.filter { kernel =>
      kernel.symbolId == end.symbolId && kernel.pointer < end.pointer
    }
    val fakeEnds = endKernelInitials.map(_ -> Kernel(end.symbolId, end.pointer, 0, 1)).toMap
    val derivedWithEnds = fakeEnds.foldLeft(derived) { (graph, end) =>
      graph.edgesByEnd(end._1).foldLeft(graph.addNode(end._2)) { (ngraph, start) =>
        ngraph.addEdge(Edge(start.start, end._2))
      }
    }
    val acceptConditionsWithEnds = startingCtx.ctx.acceptConditions ++ fakeEnds.values.map(_ -> AcceptCondition.Always)
    val afterDerive = parser.recursivelyRunTasks(1,
      fakeEnds.values.map(DeriveTask).toList,
      NaiveParsingContext(derivedWithEnds, acceptConditionsWithEnds))
    val afterTrimming = parser.trimParsingContext(startKernel, 1, afterDerive)

    val progressTasks = fakeEnds.values.map(ProgressTask(_, AcceptCondition.Always)).toList
    parsingActionForEdgeAction(afterTrimming, startKernel, progressTasks)
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  private def createParserData(jobs: Jobs, builder: MilestoneParserDataBuilder): Unit = {
    val milestones = mutable.Set[KernelTemplate]()
    val edges = mutable.Set[(KernelTemplate, KernelTemplate)]()

    def tippableSymbolsOf(condition: AcceptConditionTemplate): Set[Int] =
      condition match {
        case ExistsTemplate(symbolId) => Set(symbolId)
        case NotExistsTemplate(symbolId) => Set(symbolId)
        case _ => Set()
      }

    def addAppendingMilestones(parent: KernelTemplate, appendings: List[AppendingMilestone]): Unit = {
      appendings.foreach { appending =>
        milestones.add(appending.milestone)
        edges.add(parent -> appending.milestone)
        milestones.addAll(tippableSymbolsOf(appending.acceptCondition).map(KernelTemplate(_, 0)))
        // TODO action.parsingAction.startNodeProgressCondition??
      }
    }

    jobs.milestones.foreach { milestone =>
      val (_, CtxWithTasks(derived, _, _)) = base.startingCtxFrom(milestone, 0)
      builder.kernelDerives(milestone) = derived.graph.nodes.map(k => KernelTemplate(k.symbolId, k.pointer))

      val termActions = termActionsFrom(milestone)
      builder.termActions(milestone) = termActions

      termActions.foreach { case (_, action) =>
        addAppendingMilestones(milestone, action.parsingAction.appendingMilestones)
        action.pendedAcceptConditionKernels.foreach { fac =>
          addAppendingMilestones(fac._1, fac._2._1)
        }
      }
    }
    jobs.edges.foreach { edge =>
      val edgeAction = edgeProgressActionBetween(edge._1, edge._2)
      builder.edgeProgressActions(edge) = edgeAction

      addAppendingMilestones(edge._1, edgeAction.parsingAction.appendingMilestones)
    }
    val remainingJobs = Jobs(
      (milestones -- builder.termActions.keySet).toSet,
      (edges -- builder.edgeProgressActions.keySet).toSet)
    if (remainingJobs.milestones.nonEmpty || remainingJobs.edges.nonEmpty) {
      println(s"Remaining jobs: milestones=${remainingJobs.milestones.size}, edges=${remainingJobs.edges.size}")
      createParserData(remainingJobs, builder)
    }
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    val startingCtx = base.startingCtxFrom(start, 0)

    val builder = new MilestoneParserDataBuilder(parser.grammar, startingCtx._2.tasksSummary)
    createParserData(Jobs(Set(start), Set()), builder)
    builder.build()
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser2(grammar)).parserData()
}
