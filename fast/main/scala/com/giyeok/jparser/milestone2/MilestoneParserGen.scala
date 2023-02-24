package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NSequence, NTerminal}
import com.giyeok.jparser.fast.{CtxWithTasks, KernelTemplate, ParserGenBase2}
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
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
    val result = base.runTasksWithProgressBarrier(2, progressTasks, startKernel, CtxWithTasks(ctx, List()))
    val trimmedCtx = parser.trimParsingContext(startKernel, 2, result.ctx)

    CtxWithTasks(trimmedCtx, result.tasks)
  }

  // beginGen은 assertion용
  // beginGen이 1이면 term action 계산중, 0이면 edge action 계산중
  def appendingMilestoneCandidatesOf(
    result: CtxWithTasks,
    beginGen: Int,
    start: Kernel,
  ): List[Kernel] = {
    assert(result.ctx.graph.nodes.contains(start))
    val appendingMilestoneCandidates = result.deriveTasks.map(_.kernel)
      .filter(kernel => parser.grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
      .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen)
      .filter(result.ctx.graph.reachableBetween(start, _))
    assert(appendingMilestoneCandidates.forall(kernel => kernel.beginGen == beginGen && kernel.endGen == 2))
    appendingMilestoneCandidates
  }

  def termAction(
    ctx: NaiveParsingContext,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): TermAction = parsingActionFromProgressResultForTermAction(
    applyProgressTasks(ctx, startKernel, progressTasks), startKernel)

  def appendingMilestonesForTermAction(
    result: CtxWithTasks,
    start: Kernel,
    forAcceptConditions: mutable.Map[KernelTemplate, List[AppendingMilestone]]
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
    val forAcceptConditions = mutable.Map[KernelTemplate, List[AppendingMilestone]]()
    val appendingMilestones = appendingMilestonesForTermAction(result, start, forAcceptConditions)
    val startNodeProgressTasks = result.progressTasks.filter(_.kernel == start)
    val startNodeProgressCondition = startNodeProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition)
          .map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _))
        Some(disjunctConditions(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      tasksSummary = result.tasksSummary,
      graphBetween = KernelGraph(Set(), Set())
    )
    TermAction(parsingAction, forAcceptConditions.toMap)
  }

  private def conjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    val filtered = conditions.filter(_ != AlwaysTemplate)
    if (filtered.isEmpty) {
      AlwaysTemplate
    } else {
      if (filtered.size == 1) {
        filtered.head
      } else {
        AndTemplate(filtered.toList)
      }
    }
  }

  private def disjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    val filtered = conditions.filter(_ != NeverTemplate)
    if (filtered.isEmpty) {
      NeverTemplate
    } else {
      if (filtered.size == 1) {
        filtered.head
      } else {
        OrTemplate(filtered.toList)
      }
    }
  }

  // beginGen은 assertion용
  private def conditionToTemplateForTermAction(
    result: CtxWithTasks,
    start: Kernel,
    forAcceptConditions: mutable.Map[KernelTemplate, List[AppendingMilestone]],
    condition: AcceptCondition
  ): AcceptConditionTemplate = {
    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        conjunctConditions(conditions.map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _)))
      case AcceptCondition.Or(conditions) =>
        disjunctConditions(conditions.map(conditionToTemplateForTermAction(result, start, forAcceptConditions, _)))
      case AcceptCondition.NotExists(1, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        // TODO forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
        forAcceptConditions(KernelTemplate(symbolId, 0)) =
          appendingMilestonesForTermAction(result, Kernel(symbolId, 0, 1, 1), forAcceptConditions)
        LongestTemplate(symbolId)
      case AcceptCondition.Unless(1, 2, symbolId) =>
        // except
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
          AlwaysTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          forAcceptConditions(KernelTemplate(symbolId, 0)) =
            appendingMilestonesForTermAction(result, Kernel(symbolId, 0, 1, 1), forAcceptConditions)
          UnlessTemplate(symbolId)
        }
      case AcceptCondition.OnlyIf(1, 2, symbolId) =>
        // join
        if (!result.ctx.graph.nodes.contains(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          forAcceptConditions(KernelTemplate(symbolId, 0)) =
            appendingMilestonesForTermAction(result, Kernel(symbolId, 0, 1, 1), forAcceptConditions)
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
        Some(disjunctConditions(conditions.toSet))
    }
    val parsingAction = ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      // TODO tasksSummary and graphBetween
      tasksSummary = result.tasksSummary,
      graphBetween = KernelGraph(Set(), Set())
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
        conjunctConditions(conditions.map(conditionToTemplateForEdgeAction(result, start, needsToKeep, _)))
      case AcceptCondition.Or(conditions) =>
        disjunctConditions(conditions.map(conditionToTemplateForEdgeAction(result, start, needsToKeep, _)))
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

  def edgeProgressActionsBetween(start: KernelTemplate, end: KernelTemplate): EdgeAction = {
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

  private def createParserData(jobs: Jobs, cc: MilestoneParserData): MilestoneParserData = {
    ???
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)

    val termActions = termActionsFrom(start)
    println(termActions)

    val termActions2 = termActionsFrom(KernelTemplate(14, 1))
    println(termActions2)

    val edgeAction = edgeProgressActionsBetween(KernelTemplate(1, 0), KernelTemplate(14, 1))
    println(edgeAction.parsingAction)
    println(edgeAction.requiredSymbols)
    ???
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser2(grammar)).parserData()
}
