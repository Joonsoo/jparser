package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.{KernelTemplate, TasksSummary2}
import com.giyeok.jparser.nparser2.KernelGraph

case class MilestoneParserData(
  grammar: NGrammar,
  initialTasksSummary: TasksSummary2,
  termActions: Map[KernelTemplate, List[(TermGroupDesc, ParsingAction)]],
  edgeProgressActions: Map[(KernelTemplate, KernelTemplate), ParsingAction],
  // 엣지 사이에서 필요할 수도 있는 심볼 ID. except, join, (lookahead?)
  // termAction에서는 forAcceptConditions가 이 정보를 대체한다고 볼 수 있음
  // edgeProgressActions에서도 2 이상 떨어진 gen에 대해서 forAcceptConditions가 생기면 안 될 것 같은데.. 맞나?
  // -> 이런 경우엔 미리 이전에 path가 만들어지고 edgeMayRequire 정보로 인해서 유지되고 있었어야 할것 같은데..
  edgeRequire: Map[(KernelTemplate, KernelTemplate), Set[Int]],
  kernelDeriveGraphs: Map[KernelTemplate, KernelGraph]
)

case class ParsingAction(
  appendingMilestones: List[AppendingMilestone],
  startNodeProgressCondition: Option[AcceptConditionTemplate],
  forAcceptConditions: Map[KernelTemplate, List[AppendingMilestone]],
  tasksSummary: TasksSummary2,
  graphBetween: KernelGraph,
)

case class AppendingMilestone(
  milestone: KernelTemplate,
  acceptCondition: AcceptConditionTemplate,
) extends Ordered[AppendingMilestone] {
  override def compare(that: AppendingMilestone): Int = milestone.compare(that.milestone)
}

sealed class AcceptConditionTemplate {
  def symbolIds: Set[Int] = this match {
    case AndTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case OrTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case ExistsTemplate(symbolId) => Set(symbolId)
    case NotExistsTemplate(symbolId) => Set(symbolId)
    case LongestTemplate(symbolId) => Set(symbolId)
    case OnlyIfTemplate(symbolId) => Set(symbolId)
    case UnlessTemplate(symbolId) => Set(symbolId)
    case _ => Set()
  }
}

case object AlwaysTemplate extends AcceptConditionTemplate

case object NeverTemplate extends AcceptConditionTemplate

case class AndTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class OrTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class ExistsTemplate(symbolId: Int) extends AcceptConditionTemplate

case class NotExistsTemplate(symbolId: Int) extends AcceptConditionTemplate

case class LongestTemplate(symbolId: Int) extends AcceptConditionTemplate

case class OnlyIfTemplate(symbolId: Int) extends AcceptConditionTemplate

case class UnlessTemplate(symbolId: Int) extends AcceptConditionTemplate
