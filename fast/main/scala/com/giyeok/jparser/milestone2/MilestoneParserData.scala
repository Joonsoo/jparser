package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.{KernelTemplate, TasksSummary2}

import scala.collection.mutable

case class MilestoneParserData(
  grammar: NGrammar,
  initialTasksSummary: TasksSummary2,
  termActions: Map[KernelTemplate, List[(TermGroupDesc, TermAction)]],
  edgeProgressActions: Map[(KernelTemplate, KernelTemplate), EdgeAction],
  kernelDeriveGraphs: Map[KernelTemplate, Set[KernelTemplate]]
)

class MilestoneParserDataBuilder(val grammar: NGrammar, val initialTasksSummary: TasksSummary2) {
  val termActions: mutable.Map[KernelTemplate, List[(TermGroupDesc, TermAction)]] = mutable.Map()
  val edgeProgressActions: mutable.Map[(KernelTemplate, KernelTemplate), EdgeAction] = mutable.Map()
  val kernelDerives: mutable.Map[KernelTemplate, Set[KernelTemplate]] = mutable.Map()

  def build(): MilestoneParserData = MilestoneParserData(
    grammar,
    initialTasksSummary,
    termActions.toMap,
    edgeProgressActions.toMap,
    kernelDerives.toMap
  )
}

case class ParsingAction(
  appendingMilestones: List[AppendingMilestone],
  startNodeProgressCondition: Option[AcceptConditionTemplate],
  tasksSummary: TasksSummary2,
)

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

object AcceptConditionTemplate {
  def conjunct(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    val filtered = conditions.filter(_ != AlwaysTemplate)
    if (filtered.isEmpty) {
      AlwaysTemplate
    } else {
      if (filtered.size == 1) {
        filtered.head
      } else {
        val elems = filtered.collect {
          case AndTemplate(andElems) => andElems
          case els => List(els)
        }.flatten
        AndTemplate(elems.toList)
      }
    }
  }

  def disjunct(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    val filtered = conditions.filter(_ != NeverTemplate)
    if (filtered.isEmpty) {
      NeverTemplate
    } else {
      if (filtered.size == 1) {
        filtered.head
      } else {
        val elems = filtered.collect {
          case OrTemplate(orElems) => orElems
          case els => List(els)
        }.flatten
        OrTemplate(elems.toList)
      }
    }
  }
}

case object AlwaysTemplate extends AcceptConditionTemplate

case object NeverTemplate extends AcceptConditionTemplate

case class AndTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class OrTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

// Exists(currGen, currGen, symbolId)
// Exists(Milestone(symbolId, 0, currGen))
case class ExistsTemplate(symbolId: Int) extends AcceptConditionTemplate

// NotExists(currGen, currGen, symbolId)
// NotExists(Milestone(symbolId, 0, currGen), false)
case class NotExistsTemplate(symbolId: Int) extends AcceptConditionTemplate

// NotExists(parentGen, currGen + 1, symbolId)
// NotExists(Milestone(symbolId, 0, parentGen), true)
case class LongestTemplate(symbolId: Int) extends AcceptConditionTemplate

// OnlyIf(parentGen, currGen, symbolId)
// OnlyIf(Milestone(symbolId, 0, parentGen))
case class OnlyIfTemplate(symbolId: Int) extends AcceptConditionTemplate

// Unless(parentGen, currGen, symbolId)
// Unless(Milestone(symbolId, 0, parentGen))
case class UnlessTemplate(symbolId: Int) extends AcceptConditionTemplate