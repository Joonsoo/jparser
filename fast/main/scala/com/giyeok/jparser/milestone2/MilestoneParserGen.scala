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

  // beginGen은 assertion용.
  // beginGen이 1이면 term action 계산중, 0이면 edge action 계산중
  def parsingActionFrom(
    ctx: NaiveParsingContext,
    beginGen: Int,
    startKernel: Kernel,
    progressTasks: List[ProgressTask],
  ): ParsingAction = {
    val result = base.runTasksWithProgressBarrier(2, progressTasks, startKernel, CtxWithTasks(ctx, List()))

    parsingActionFromProgressResult(result, beginGen, startKernel)
  }

  // beginGen은 assertion용
  def parsingActionFromProgressResult(result: CtxWithTasks, beginGen: Int, start: Kernel): ParsingAction = {
    val CtxWithTasks(ctx, _) = result
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    assert(ctx.graph.nodes.contains(start))
    val appendingMilestoneCandidates = result.deriveTasks.map(_.kernel)
      .filter(kernel => parser.grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
      .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen)
      .filter(ctx.graph.reachableBetween(start, _))
    assert(appendingMilestoneCandidates.forall(kernel => kernel.beginGen == beginGen && kernel.endGen == 2))
    val forAcceptConditions = mutable.Map[KernelTemplate, List[AppendingMilestone]]()
    val appendingMilestones = appendingMilestoneCandidates.map { kernel =>
      val condition = conditionToTemplate(ctx, beginGen, forAcceptConditions, ctx.acceptConditions(kernel))
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }
    val startNodeProgressTasks = result.progressTasks.filter(_.kernel == start)
    val startNodeProgressCondition = startNodeProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition).map(conditionToTemplate(ctx, beginGen, forAcceptConditions, _))
        Some(disjunctConditions(conditions.toSet))
    }
    ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      forAcceptConditions = forAcceptConditions.toMap,
      // TODO tasksSummary and graphBetween
      tasksSummary = result.tasksSummary,
      graphBetween = KernelGraph(Set(), Set())
    )
  }

  private def conjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.size == 1) conditions.head else OrTemplate(conditions.toList)
  }

  private def disjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.size == 1) conditions.head else OrTemplate(conditions.toList)
  }

  // beginGen은 assertion용
  private def conditionToTemplate(
    ctx: NaiveParsingContext,
    beginGen: Int,
    forAcceptConditions: mutable.Map[KernelTemplate, List[AppendingMilestone]],
    condition: AcceptCondition
  ): AcceptConditionTemplate = condition match {
    case AcceptCondition.Always => AlwaysTemplate
    case AcceptCondition.Never => NeverTemplate
    case AcceptCondition.And(conditions) =>
      conjunctConditions(conditions.map(conditionToTemplate(ctx, beginGen, forAcceptConditions, _)))
    case AcceptCondition.Or(conditions) =>
      disjunctConditions(conditions.map(conditionToTemplate(ctx, beginGen, forAcceptConditions, _)))
    case AcceptCondition.NotExists(`beginGen`, 3, symbolId) =>
      // longest
      // longest는 일단 다음 gen부터 체크되므로 반환값이 달라질 일은 없음
      // TODO 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
      LongestTemplate(symbolId)
    case AcceptCondition.Unless(`beginGen`, 2, symbolId) =>
      // except
      // TODO ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
      // TODO 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
      UnlessTemplate(symbolId)
    case AcceptCondition.OnlyIf(`beginGen`, 2, symbolId) =>
      // join
      // TODO ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
      // TODO 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
      OnlyIfTemplate(symbolId)
    case AcceptCondition.NotExists(2, 2, symbolId) =>
      // lookahead except
      // TODO ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
      // - lookahead 심볼은 이미 가망이 없어진 경우가 아니라면 앞으로의 상황만 보니까.. start만 있고 appending milestone은 없는(길이가 1인) path가 추가돼야하나?
      ???
      NotExistsTemplate(symbolId)
    case AcceptCondition.Exists(2, 2, symbolId) =>
      // lookahead is
      // TODO ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
      ???
      ExistsTemplate(symbolId)
  }

  def termActionsFrom(start: KernelTemplate): List[(TermGroupDesc, ParsingAction)] = {
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
      }
      val termProgressTasks = termNodes.toList.map(ProgressTask(_, AcceptCondition.Always))

      termGroup -> parsingActionFrom(startingCtx.ctx, 1, startKernel, termProgressTasks)
    }
  }

  def edgeProgressActionsBetween(start: KernelTemplate, end: KernelTemplate): ParsingAction = {
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
    val afterDerive = parser.recursivelyRunTasks(1,
      fakeEnds.values.map(DeriveTask).toList,
      NaiveParsingContext(derivedWithEnds, startingCtx.ctx.acceptConditions))
    val afterTrimming = parser.trimParsingContext(startKernel, 1, afterDerive)

    val progressTasks = fakeEnds.values.map(ProgressTask(_, AcceptCondition.Always)).toList
    parsingActionFrom(afterTrimming, 0, startKernel, progressTasks)
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  private def createParserData(jobs: Jobs, cc: MilestoneParserData): MilestoneParserData = {
    ???
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)

    val termActions = termActionsFrom(start)
    println(termActions)

    val edgeActions = edgeProgressActionsBetween(KernelTemplate(1, 0), KernelTemplate(14, 1))
    println(edgeActions)
    ???
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser2(grammar)).parserData()
}
