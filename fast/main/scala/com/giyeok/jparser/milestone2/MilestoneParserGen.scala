package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2, ProgressTask, ParsingContext => NaiveParsingContext}
import com.giyeok.jparser.utils.TermGrouper

import scala.collection.mutable

// naive parser 1과 2의 차이점은 accept condition이 그래프 안에 있냐 밖에 있냐의 차이
// milestone parser 1과 2의 차이점도 비슷
class MilestoneParserGen(val parser: NaiveParser2) {
  def parsingActionFrom(ctx: NaiveParsingContext, start: Kernel, progressTasks: List[ProgressTask]): ParsingAction = {
    // start는 0..1, currGen은 2
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone의 accept condition과 startNodeProgressCondition에 등장하는 accept condition들을 template화
    // -> 여기서 등장하는 심볼들로부터 reachable한 milestone들을 찾아서 forAcceptConditions 만들기
    // -> 반복해서 새로 등장하는 accept condition template이 없을 때까지
    // 추가로 edgeMayRequire 계산
    assert(ctx.graph.nodes.contains(start))
    val reachables = ctx.graph.reachableNodesFrom(start)
      .filter(kernel => kernel.beginGen != kernel.endGen)
    assert(reachables.forall(kernel => kernel.beginGen == 1 && kernel.endGen == 2))
    val forAcceptConditions = mutable.Map[KernelTemplate, List[AppendingMilestone]]()
    val appendingMilestones = reachables.map { kernel =>
      val condition = templatizeCondition(ctx, forAcceptConditions, ctx.acceptConditions.getOrElse(kernel, AlwaysTemplate))
      AppendingMilestone(KernelTemplate(kernel.symbolId, kernel.pointer), condition)
    }.toList.sorted
    val startNodeProgressTasks = progressTasks.filter(_.kernel == start)
    val startNodeProgressCondition = startNodeProgressTasks match {
      case List() => None
      case progressTasks =>
        val conditions = progressTasks.map(_.condition).map(templatizeCondition(ctx, forAcceptConditions, _))
        Some(disjunctConditions(conditions.toSet))
    }
    ParsingAction(
      appendingMilestones = appendingMilestones,
      startNodeProgressCondition = startNodeProgressCondition,
      forAcceptConditions = forAcceptConditions.toMap,
      // TODO tasksSummary and graphBetween
      tasksSummary = TasksSummary(List(), List()),
      graphBetween = KernelGraph(Set(), Set())
    )
  }

  private def pathsForAcceptConditions(ctx: NaiveParsingContext, appendingMilestones: List[AcceptConditionTemplate]): Map[KernelTemplate, List[AppendingMilestone]] = {
    ???
  }

  private def conjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.size == 1) conditions.head else OrTemplate(conditions.toList)
  }

  private def disjunctConditions(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.size == 1) conditions.head else OrTemplate(conditions.toList)
  }

  private def templatizeCondition(
    ctx: NaiveParsingContext,
    forAcceptConditions: mutable.Map[KernelTemplate, List[AppendingMilestone]],
    condition: AcceptCondition
  ): AcceptConditionTemplate = condition match {
    case AcceptCondition.Always => AlwaysTemplate
    case AcceptCondition.Never => NeverTemplate
    case AcceptCondition.And(conditions) =>
      conjunctConditions(conditions.map(templatizeCondition(ctx, forAcceptConditions, _)))
    case AcceptCondition.Or(conditions) =>
      disjunctConditions(conditions.map(templatizeCondition(ctx, forAcceptConditions, _)))
    case AcceptCondition.NotExists(1, 3, symbolId) =>
      // longest
      LongestTemplate(symbolId)
    case AcceptCondition.Unless(1, 2, symbolId) =>
      // except
      UnlessTemplate(symbolId)
    case AcceptCondition.OnlyIf(1, 2, symbolId) =>
      // join
      OnlyIfTemplate(symbolId)
    case AcceptCondition.Exists(2, 2, symbolId) =>
      // lookahead is
      ExistsTemplate(symbolId)
    case AcceptCondition.NotExists(2, 2, symbolId) =>
      // lookahead except
      NotExistsTemplate(symbolId)
  }

  def termActionsFrom(termKernel: KernelTemplate): List[(TermGroupDesc, ParsingAction)] = {
    val (startNode, ContWithTasks(_, parser.Cont(derived, _))) = startingCtxFrom(termKernel)

    // new DotGraphGenerator(parser.grammar).addGraph(derived).printDotGraph()

    val termGroups = TermGrouper.termGroupsOf(parser.grammar, derived)

    termGroups.map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 1, termGroup)
      val termProgressTasks = termNodes.toList.map(parser.ProgressTask(_, AcceptCondition.Always))

      termGroup -> parsingActionFrom(derived, startNode, termProgressTasks, 1)
    }
  }

  def edgeProgressActionsBetween(startKernel: KernelTemplate, endKernel: KernelTemplate): ParsingAction = {
    ???
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  private def createParserData(jobs: Jobs, cc: MilestoneParserData): MilestoneParserData = {
    ???
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    ???
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser2(grammar)).parserData()
}
