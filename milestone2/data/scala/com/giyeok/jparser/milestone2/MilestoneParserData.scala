package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar

import scala.collection.mutable

case class MilestoneParserData(
  grammar: NGrammar,
  initialTasksSummary: TasksSummary2,
  termActions: Map[KernelTemplate, List[(TermGroupDesc, TermAction)]],
  edgeProgressActions: Map[(KernelTemplate, KernelTemplate), EdgeAction],
) {
  def trimTasksSummariesForSymbols(symbolsOfInterest: Set[Int]): MilestoneParserData = {
    MilestoneParserData(
      grammar,
      initialTasksSummary.trimForSymbols(symbolsOfInterest),
      termActions.view.mapValues { termActions =>
        termActions.map { case (termGroup, termAction) =>
          termGroup -> termAction.copy(
            parsingAction = termAction.parsingAction.trimForSymbols(symbolsOfInterest)
          )
        }
      }.toMap,
      edgeProgressActions.view.mapValues { edgeAction =>
        edgeAction.copy(parsingAction = edgeAction.parsingAction.trimForSymbols(symbolsOfInterest))
      }.toMap,
    )
  }
}

class MilestoneParserDataBuilder(val grammar: NGrammar, val initialTasksSummary: TasksSummary2) {
  val termActions: mutable.Map[KernelTemplate, List[(TermGroupDesc, TermAction)]] = mutable.Map()
  val edgeProgressActions: mutable.Map[(KernelTemplate, KernelTemplate), EdgeAction] = mutable.Map()

  def build(): MilestoneParserData = MilestoneParserData(
    grammar,
    initialTasksSummary,
    termActions.toMap,
    edgeProgressActions.toMap,
  )
}

case class ParsingAction(
  appendingMilestones: List[AppendingMilestone],
  startNodeProgressCondition: Option[AcceptConditionTemplate],
  lookaheadRequiringSymbols: Set[Int],
  tasksSummary: TasksSummary2,
) {
  def trimForSymbols(symbolIds: Set[Int]): ParsingAction =
    copy(tasksSummary = tasksSummary.trimForSymbols(symbolIds))
}

case class TermAction(
  parsingAction: ParsingAction,
  pendedAcceptConditionKernels: Map[KernelTemplate, (List[AppendingMilestone], Option[AcceptConditionTemplate])],
)

case class EdgeAction(
  parsingAction: ParsingAction,
  // 엣지 사이에서 필요할 수도 있는 심볼 ID. except, join, (lookahead?)
  // termAction에서는 forAcceptConditions가 이 정보를 대체한다고 볼 수 있음
  // edgeProgressActions에서도 2 이상 떨어진 gen에 대해서 forAcceptConditions가 생기면 안 될 것 같은데.. 맞나?
  // -> 이런 경우엔 미리 이전에 path가 만들어지고 edgeMayRequire 정보로 인해서 유지되고 있었어야 할것 같은데..
  requiredSymbols: Set[Int]
)

case class AppendingMilestone(
  milestone: KernelTemplate,
  acceptCondition: AcceptConditionTemplate,
) extends Ordered[AppendingMilestone] {
  override def compare(that: AppendingMilestone): Int = milestone.compare(that.milestone)
}
