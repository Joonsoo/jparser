package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.KernelGraph

case class MilestoneParserData(
  grammar: NGrammar,
  initialTasksSummary: TasksSummary,
  termActions: Map[KernelTemplate, List[(TermGroupDesc, ParsingAction)]],
  edgeProgressActions: Map[(KernelTemplate, KernelTemplate), ParsingAction],
  // 엣지 사이에서 필요할 수도 있는 심볼 ID. except, join, (lookahead?)
  // termAction에서는 forAcceptConditions가 이 정보를 대체한다고 볼 수 있음
  // edgeProgressActions에서도 2 이상 떨어진 gen에 대해서 forAcceptConditions가 생기면 안 될 것 같은데.. 맞나?
  // -> 이런 경우엔 미리 이전에 path가 만들어지고 edgeMayRequire 정보로 인해서 유지되고 있었어야 할것 같은데..
  edgeMayRequire: Map[(KernelTemplate, KernelTemplate), Set[Int]],
  kernelDeriveGraphs: Map[KernelTemplate, KernelGraph]
)

case class ParsingAction(
  appendingMilestones: List[AppendingMilestone],
  forAcceptConditions: Map[KernelTemplate, List[AppendingMilestone]],
  tasksSummary: TasksSummary,
  startNodeProgressCondition: Option[AcceptConditionTemplate],
  graphBetween: KernelGraph,
)

case class AppendingMilestone(
  milestone: KernelTemplate,
  acceptCondition: AcceptConditionTemplate,
)

case class TasksSummary(
  progressedKernels: List[Kernel],
  finishedKernels: List[Kernel],
)

sealed class AcceptConditionTemplate

case object AlwaysTemplate extends AcceptConditionTemplate

case object NeverTemplate extends AcceptConditionTemplate

case class AndTemplate(conditions: Set[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class OrTemplate(conditions: Set[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class ExistsTemplate(symbolId: Int, beginGenFromNow: Boolean) extends AcceptConditionTemplate

case class NotExistsTemplate(symbolId: Int, beginGenFromNow: Boolean, checkFromNextGen: Boolean) extends AcceptConditionTemplate

case class OnlyIfTemplate(symbolId: Int) extends AcceptConditionTemplate

case class UnlessTemplate(symbolId: Int) extends AcceptConditionTemplate
