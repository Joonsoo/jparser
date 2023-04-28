package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NSequence, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
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

    assert(result.startKernelProgressConditions.isEmpty || result.startKernelProgressConditions.keySet == Set(startKernel))

    result.copy(ctx = trimmedCtx)
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
    val startNodeProgressCondition = if (result.startKernelProgressConditions.isEmpty) None else {
      assert(result.startKernelProgressConditions.keySet == Set(start))
      val condition = result.startKernelProgressConditions(start)
      Some(conditionToTemplateForTermAction(result, condition, pendedCollector, lookaheadCollector))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      lookaheadRequiringSymbols = lookaheadCollector.toSet,
      tasksSummary = result.tasksSummary(1),
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
    val startTemplate = KernelTemplate(symbolId, 0)
    if (!pendedCollector.contains(startTemplate)) {
      val symbolStart = Kernel(symbolId, 0, 1, 1)
      val appendingMilestones = appendingMilestonesForTermAction(result, symbolStart, pendedCollector, lookaheadCollector)
      val progressCondition = result.progressConditionsFor(symbolStart)
      val progressConditionTemplate = progressCondition match {
        case List() => None
        case progressConditions =>
          val progressCondition = AcceptCondition.disjunct(progressConditions: _*)
          Some(conditionToTemplateForTermAction(result, progressCondition, pendedCollector, lookaheadCollector))
      }
      pendedCollector(startTemplate) = (appendingMilestones, progressConditionTemplate)
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
      /* 여기서부터 전 세대에서 nullable 심볼에 의해 만들어져서 evolve 이후에도 살아남은 컨디션들 */
      case AcceptCondition.NotExists(1, 2, symbolId) =>
        ???
        // longest
        addPended(symbolId)
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.Exists(1, 1, symbolId) =>
        // copied from old version
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          addPended(symbolId)
          LookaheadIsTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.NotExists(1, 1, symbolId) =>
        // copied from old version
        // lookahead not
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          addPended(symbolId)
          LookaheadNotTemplate(symbolId, fromNextGen = false)
        }
      // Unless와 OnlyIf는 이전 세대에 만들어진 것은 이미 휘발되고 없어야 함
      // 이런 케이스가 있을 수 있는지 모르겠지만 만약 있다면 그냥 Always를 줘서 없는 것처럼 취급하면 되지 않을까?
      //      case AcceptCondition.Unless(1, 1, symbolId) =>
      //        ???
      //      case AcceptCondition.OnlyIf(1, 1, symbolId) =>
      //        ???
      /* 여기서부터 progress task에 의해 새로 추가될 수 있는 컨디션들 */
      case AcceptCondition.NotExists(1, 3, symbolId) =>
        // copied from old version
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        // forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
        // TODO result.progressTasks.filter(_.kernel == Kernel(symbolId, 0, 1, 1)) 에 대한 정보 추가
        addPended(symbolId)
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.NotExists(2, 3, symbolId) =>
        ???
        addPended(symbolId)
        LongestTemplate(symbolId, beginFromNextGen = true)
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // copied from old version
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // NotExists와 같은 이유로 forAcceptConditions는 건드릴 필요 없음
          lookaheadCollector += symbolId
          LookaheadIsTemplate(symbolId, fromNextGen = true)
        }
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // copied from old version
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
      case AcceptCondition.Unless(1, 2, symbolId) =>
        // copied from old version
        // except
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
          AlwaysTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addPended(symbolId)
          UnlessTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.OnlyIf(1, 2, symbolId) =>
        // copied from old version
        // join
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          addPended(symbolId)
          OnlyIfTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.Unless(2, 2, symbolId) =>
        ???
        // TODO addPended나 lookaheadCollector += symbolId가 필요할까?
        UnlessTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.OnlyIf(2, 2, symbolId) =>
        ???
        // TODO addPended나 lookaheadCollector += symbolId가 필요할까?
        OnlyIfTemplate(symbolId, fromNextGen = true)
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
    val startNodeProgressCondition = if (result.startKernelProgressConditions.isEmpty) None else {
      assert(result.startKernelProgressConditions.keySet == Set(start))
      val condition = result.startKernelProgressConditions(start)
      Some(conditionToTemplateForEdgeAction(result, condition, edgeRequires, lookaheadCollector))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      lookaheadRequiringSymbols = lookaheadCollector.toSet,
      tasksSummary = result.tasksSummary(0),
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
      /* 여기서부터 전 세대에서 nullable 심볼에 의해 만들어져서 evolve 이후에도 살아남은 컨디션들 */
      case AcceptCondition.NotExists(0, 1, symbolId) =>
        // new
        ???
        needsToKeep += symbolId
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.Exists(0, 0, symbolId) =>
        // copied from old version
        // TODO verify
        needsToKeep += symbolId
        LookaheadIsTemplate(symbolId, fromNextGen = false)
      case AcceptCondition.NotExists(0, 0, symbolId) =>
        // copied from old version
        // TODO verify
        needsToKeep += symbolId
        LookaheadNotTemplate(symbolId, fromNextGen = false)
      // conditionToTemplateForTermAction 주석 참고
      //      case AcceptCondition.Unless(0, 0, symbolId) =>
      //        ???
      //      case AcceptCondition.OnlyIf(0, 0, symbolId) =>
      //        ???
      /* 여기서부터 progress task에 의해 새로 추가될 수 있는 컨디션들 */
      case AcceptCondition.NotExists(0, 3, symbolId) =>
        // copied from old version
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        needsToKeep += symbolId
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.NotExists(2, 3, symbolId) =>
        // new
        ???
        // longest with nullable symbol
        needsToKeep += symbolId
        LongestTemplate(symbolId, beginFromNextGen = true)
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // copied from old version
        // TODO verify
        // lookahead is
        lookaheadCollector += symbolId
        LookaheadIsTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // copied from old version
        // TODO verify
        // lookahead except
        lookaheadCollector += symbolId
        LookaheadNotTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.Unless(0, 2, symbolId) =>
        // copied from old version
        // except
        needsToKeep += symbolId
        UnlessTemplate(symbolId, fromNextGen = false)
      case AcceptCondition.OnlyIf(0, 2, symbolId) =>
        // copied from old version
        // join
        needsToKeep += symbolId
        OnlyIfTemplate(symbolId, fromNextGen = false)
      case AcceptCondition.Unless(2, 2, symbolId) =>
        // new
        ???
        needsToKeep += symbolId
        UnlessTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.OnlyIf(2, 2, symbolId) =>
        // new
        ???
        needsToKeep += symbolId
        OnlyIfTemplate(symbolId, fromNextGen = true)
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

    val builder = new MilestoneParserDataBuilder(grammar, startingCtx._2.tasksSummary(0))
    createParserData(Jobs(Set(start), Set()), builder)
    builder.build()
  }
}
