package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NSequence, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, disjunct}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.opt.OptNaiveParser2
import com.giyeok.jparser.nparser2.{DeriveTask, Edge, KernelGraph, NaiveParser2, ProgressTask, ParsingContext => NaiveParsingContext}
import com.giyeok.jparser.utils.TermGrouper

import scala.collection.mutable

// naive parser 1과 2의 차이점은 accept condition이 그래프 안에 있냐 밖에 있냐의 차이
// milestone parser 1과 2의 차이점도 비슷
class MilestoneParserGen(val grammar: NGrammar) {
  private val parser = new NaiveParser2(grammar)
  private val optParser = new OptNaiveParser2(grammar)
  private val base = new ParserGenBase2(optParser)

  def applyProgressTasks(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): CtxWithTasks = {
    val result = base.runTasksWithProgressBarrier(2, progressTasks, startKernel, ctx)
    val trimmedCtx = parser.trimParsingContext(startKernel, 2, result.ctx)
    val evolvedAcceptConditions = trimmedCtx.acceptConditions.view.mapValues { cond =>
      parser.evolveAcceptCondition(cond, 2, result.ctx)
    }.toMap

    CtxWithTasks(trimmedCtx.copy(acceptConditions = evolvedAcceptConditions), result.tasks, result.startKernelProgressTasks)
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
      val reachableFromStart = result.ctx.graph.reachableNodesFrom(start)
      val appendingMilestoneCandidates = result.deriveTasks
        .map(_.kernel)
        .filter(result.ctx.graph.nodes.contains)
        .filter(kernel => grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
        .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen)
        .filter(kernel => reachableFromStart.contains(kernel))
      assert(appendingMilestoneCandidates.forall(kernel => kernel.beginGen == beginGen && kernel.endGen == 2))
      appendingMilestoneCandidates
    }
  }

  def termActionFor(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): TermAction =
    parsingActionFromProgressResultForTermAction(
      applyProgressTasks(ctx, startKernel, progressTasks), startKernel)

  // beginGen은 assertion용
  def parsingActionFromProgressResultForTermAction(result: CtxWithTasks, start: Kernel): TermAction = {
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    val pendedCollector = mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])]()
    val lookaheadCollector = mutable.Set[Int]()
    val appendingMilestones = appendingMilestonesForTermAction(result, start, pendedCollector, lookaheadCollector)
    val startNodeProgressCondition = result.startKernelProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition)
          .map(conditionToTemplateForTermAction(result, _, pendedCollector, lookaheadCollector))
        Some(AcceptConditionTemplate.disjunct(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      lookaheadRequiringSymbols = lookaheadCollector.toSet,
      tasksSummary = result.tasksSummary(0, 1),
    )
    TermAction(parsingAction, pendedCollector.toMap)
  }

  def appendingMilestonesForTermAction(
    result: CtxWithTasks,
    start: Kernel,
    pendedCollector: mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])],
    lookaheadCollector: mutable.Set[Int],
  ): List[AppendingMilestone] = {
    val conditionSymbolIds = reachableConditionSymbols(result.ctx.graph, start)
    if (conditionSymbolIds.nonEmpty) {
      conditionSymbolIds.foreach(addPendedForTermAction(result, _, pendedCollector, lookaheadCollector))
    }
    appendingMilestoneCandidatesOf(result, 1, start).map { kernel =>
      val condition = conditionToTemplateForTermAction(result, result.ctx.acceptConditions(kernel), pendedCollector, lookaheadCollector)
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }
  }

  def reachableConditionSymbols(graph: KernelGraph, start: Kernel): Set[Int] = {
    val reachables = graph.reachableNodesFrom(start)
    val conditionSymbolIds = reachables.map(_.symbolId).flatMap { symbolId =>
      grammar.symbolOf(symbolId) match {
        case NGrammar.NExcept(_, _, _, except) => Some(except)
        case NGrammar.NJoin(_, _, _, join) => Some(join)
        case NGrammar.NLongest(_, _, body) => Some(body)
        case _ => None
      }
    }
    // TODO 이 아래 조건이 필요한게 맞나..?
    // assert(!conditionSymbolIds.contains(start.symbolId))
    conditionSymbolIds
  }

  private def addPendedForTermAction(
    result: CtxWithTasks,
    symbolId: Int,
    pendedCollector: mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])],
    lookaheadCollector: mutable.Set[Int],
  ): Unit = {
    if (!pendedCollector.contains(KernelTemplate(symbolId, 0))) {
      val symbolStart = Kernel(symbolId, 0, 1, 1)
      val appendingMilestones = appendingMilestonesForTermAction(result, symbolStart, pendedCollector, lookaheadCollector)
      val progresses = result.progressTasks.filter(_.kernel == symbolStart)
      val progressCondition = disjunct(progresses.map(_.condition): _*)
      val progressConditionTemplate = if (progressCondition == AcceptCondition.Never) None else {
        Some(conditionToTemplateForTermAction(result, progressCondition, pendedCollector, lookaheadCollector))
      }
      pendedCollector(KernelTemplate(symbolId, 0)) = (appendingMilestones, progressConditionTemplate)
    }
  }

  def conditionToTemplateForTermAction(
    result: CtxWithTasks,
    condition: AcceptCondition,
    pendedCollector: mutable.Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])],
    lookaheadCollector: mutable.Set[Int],
  ): AcceptConditionTemplate = {
    def cannotExist(kernel: Kernel): Boolean =
      result.progressConditionsFor(kernel).isEmpty && !result.ctx.graph.nodes.contains(kernel)

    def addPended(symbolId: Int): Unit =
      addPendedForTermAction(result, symbolId, pendedCollector, lookaheadCollector)

    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplateForTermAction(result, _, pendedCollector, lookaheadCollector)))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplateForTermAction(result, _, pendedCollector, lookaheadCollector)))
      case AcceptCondition.NotExists(1, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        // forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
        // TODO result.progressTasks.filter(_.kernel == Kernel(symbolId, 0, 1, 1)) 에 대한 정보 추가
        addPended(symbolId)
        LongestTemplate(symbolId, beginFromNextGen = false)
      // TODO 여기서 NotExists(2, 3, symbolId) 가 나올 수 있을까? 그러면 Longest(symbolId, true)가 되면 될 듯 한데..
      case AcceptCondition.Unless(1, 2, symbolId) =>
        // except
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
          AlwaysTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addPended(symbolId)
          UnlessTemplate(symbolId)
        }
      case AcceptCondition.OnlyIf(1, 2, symbolId) =>
        // join
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addPended(symbolId)
          OnlyIfTemplate(symbolId)
        }
      case AcceptCondition.NotExists(1, 1, symbolId) =>
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          addPended(symbolId)
          LookaheadNotTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead except
        // - lookahead 심볼은 이미 가망이 없어진 경우가 아니라면 앞으로의 상황만 보기 때문에
        //   start만 있고 appending milestone은 없는(길이가 1인) path를 추가해야 할듯?
        //   즉, forAcceptConditions는 건드릴 필요 없을듯
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          lookaheadCollector += symbolId
          LookaheadNotTemplate(symbolId, fromNextGen = true)
        }
      case AcceptCondition.Exists(1, 1, symbolId) =>
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          addPended(symbolId)
          LookaheadIsTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // NotExists와 같은 이유로 forAcceptConditions는 건드릴 필요 없음
          lookaheadCollector += symbolId
          LookaheadIsTemplate(symbolId, fromNextGen = true)
        }
    }
  }

  def termActionsFor(start: KernelTemplate): List[(TermGroupDesc, TermAction)] = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, 0)

    // new DotGraphGenerator(parser.grammar).addGraph(derived).printDotGraph()

    val derived = startingCtx.ctx.graph.nodes
    val termNodes = derived.filter { node => grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal] }
      .filter { node => node.pointer == 0 }
    // TODO kernel.symbolId == start.symbolId 가 무슨 의미지..?
    assert(termNodes.forall { kernel => kernel.symbolId == start.symbolId || kernel.beginGen == 1 })
    val terms = termNodes.map { node => grammar.symbolOf(node.symbolId) }
      .map { case terminal: NTerminal => terminal.symbol }
    val termGroups: List[TermGroupDesc] = TermGrouper.termGroupsOf(terms)

    termGroups.flatMap { termGroup =>
      val applicableTermNodes = termNodes.filter { kernel =>
        val symbol = grammar.symbolOf(kernel.symbolId)
        symbol.asInstanceOf[NTerminal].symbol.acceptTermGroup(termGroup)
      }
      val termProgressTasks = applicableTermNodes.toList.map(ProgressTask(_, AcceptCondition.Always))
      val termAction = termActionFor(startingCtx.ctx, startKernel, termProgressTasks)

      if (termAction.parsingAction.appendingMilestones.isEmpty && termAction.parsingAction.startNodeProgressCondition.isEmpty) {
        None
      } else {
        Some(termGroup -> termAction)
      }
    }
  }

  def parsingActionForEdgeAction(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): EdgeAction = {
    val nextCtx = applyProgressTasks(ctx, startKernel, progressTasks)
    edgeActionFor(nextCtx, startKernel)
  }

  def appendingMilestonesForEdgeAction(
    result: CtxWithTasks,
    start: Kernel,
    edgeRequires: mutable.Set[Int],
    lookaheadCollector: mutable.Set[Int],
  ): List[AppendingMilestone] = {
    val conditionSymbolIds = reachableConditionSymbols(result.ctx.graph, start)
    if (conditionSymbolIds.nonEmpty) {
      edgeRequires.addAll(conditionSymbolIds)
    }
    appendingMilestoneCandidatesOf(result, 0, start).map { kernel =>
      val condition = conditionToTemplateForEdgeAction(result, result.ctx.acceptConditions(kernel), edgeRequires, lookaheadCollector)
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }
  }

  def edgeActionFor(result: CtxWithTasks, start: Kernel): EdgeAction = {
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    val edgeRequires = mutable.Set[Int]()
    val lookaheadCollector = mutable.Set[Int]()
    val appendingMilestones = appendingMilestonesForEdgeAction(result, start, edgeRequires, lookaheadCollector)
    val startNodeProgressCondition = result.startKernelProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition)
          .map(conditionToTemplateForEdgeAction(result, _, edgeRequires, lookaheadCollector))
        Some(AcceptConditionTemplate.disjunct(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      lookaheadRequiringSymbols = lookaheadCollector.toSet,
      tasksSummary = result.tasksSummary(0, 1),
    )
    EdgeAction(parsingAction, edgeRequires.toSet)
  }

  // beginGen은 assertion용
  private def conditionToTemplateForEdgeAction(
    result: CtxWithTasks,
    condition: AcceptCondition,
    needsToKeep: mutable.Set[Int],
    lookaheadCollector: mutable.Set[Int],
  ): AcceptConditionTemplate = {
    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplateForEdgeAction(result, _, needsToKeep, lookaheadCollector)))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplateForEdgeAction(result, _, needsToKeep, lookaheadCollector)))
      case AcceptCondition.NotExists(0, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        needsToKeep += symbolId
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.Unless(0, 2, symbolId) =>
        // except
        needsToKeep += symbolId
        UnlessTemplate(symbolId)
      case AcceptCondition.OnlyIf(0, 2, symbolId) =>
        // join
        needsToKeep += symbolId
        OnlyIfTemplate(symbolId)
      case AcceptCondition.NotExists(0, 0, symbolId) =>
        // TODO
        needsToKeep += symbolId
        LookaheadNotTemplate(symbolId, fromNextGen = false)
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // TODO
        // lookahead except
        lookaheadCollector += symbolId
        LookaheadNotTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.Exists(0, 0, symbolId) =>
        // TODO
        needsToKeep += symbolId
        LookaheadIsTemplate(symbolId, fromNextGen = false)
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // TODO
        // lookahead is
        lookaheadCollector += symbolId
        LookaheadIsTemplate(symbolId, fromNextGen = true)
    }
  }

  def edgeProgressActionBetween(start: KernelTemplate, end: KernelTemplate): EdgeAction = {
    val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)
    edgeProgressActionBetween(startKernel, end, startingCtx)
  }

  def edgeProgressActionBetween(startKernel: Kernel, end: KernelTemplate, startingCtx: CtxWithTasks): EdgeAction = {
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
    val afterDerive = optParser.runTasks(1,
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
        case LookaheadIsTemplate(symbolId, _) => Set(symbolId)
        case LookaheadNotTemplate(symbolId, _) => Set(symbolId)
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
      // val (_, CtxWithTasks(derived, _, _)) = base.startingCtxFrom(milestone, 0)
      // builder.kernelDerives(milestone) = derived.graph.nodes.map(k => KernelTemplate(k.symbolId, k.pointer))

      val termActions = termActionsFor(milestone)
      builder.termActions(milestone) = termActions

      termActions.foreach { case (_, action) =>
        addAppendingMilestones(milestone, action.parsingAction.appendingMilestones)
        action.pendedAcceptConditionKernels.foreach { fac =>
          addAppendingMilestones(fac._1, fac._2._1)
        }
        action.parsingAction.lookaheadRequiringSymbols.foreach { symbolId =>
          milestones.add(KernelTemplate(symbolId, 0))
        }
      }
    }
    jobs.edges.groupBy(_._1).foreach { edgeGroup =>
      val (start, edges) = edgeGroup
      val (startKernel, startingCtx) = base.startingCtxFrom(start, -1)
      edges.foreach { edge =>
        val edgeAction = edgeProgressActionBetween(startKernel, edge._2, startingCtx)
        builder.edgeProgressActions(edge) = edgeAction

        addAppendingMilestones(start, edgeAction.parsingAction.appendingMilestones)
        edgeAction.parsingAction.lookaheadRequiringSymbols.foreach { symbolId =>
          milestones.add(KernelTemplate(symbolId, 0))
        }
      }
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
    val start = KernelTemplate(grammar.startSymbol, 0)
    val startingCtx = base.startingCtxFrom(start, 0)

    val builder = new MilestoneParserDataBuilder(grammar, startingCtx._2.tasksSummary(0, 0))
    createParserData(Jobs(Set(start), Set()), builder)
    builder.build()
  }
}
