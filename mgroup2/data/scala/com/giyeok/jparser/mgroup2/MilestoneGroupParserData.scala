package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, TasksSummary2}

import scala.collection.mutable

case class MilestoneGroupParserData(
  grammar: NGrammar,
  startGroupId: Int,
  initialTasksSummary: TasksSummary2,
  milestoneGroups: Map[Int, Set[KernelTemplate]],
  // group id -> actions
  termActions: Map[Int, List[(TermGroupDesc, TermAction)]],
  // (milestone -> group id) -> actions
  tipEdgeProgressActions: Map[(KernelTemplate, Int), EdgeAction],
  // (milestone -> milestone) -> actions
  midEdgeProgressActions: Map[(KernelTemplate, KernelTemplate), EdgeAction],
)

class MilestoneGroupParserDataBuilder(val grammar: NGrammar, val initialTasksSummary: TasksSummary2) {
  private val milestoneGroups = mutable.Map[Int, Set[KernelTemplate]]()
  private val milestoneGroupsInverse = mutable.Map[Set[KernelTemplate], Int]()

  val termActions: mutable.Map[Int, List[(TermGroupDesc, TermAction)]] = mutable.Map()
  val tipEdgeProgressActions: mutable.Map[(KernelTemplate, Int), EdgeAction] = mutable.Map()
  val midEdgeProgressActions: mutable.Map[(KernelTemplate, KernelTemplate), EdgeAction] = mutable.Map()

  def milestoneGroupId(milestones: Set[KernelTemplate]): Int = {
    milestoneGroupsInverse.get(milestones) match {
      case Some(existingId) => existingId
      case None =>
        val newId = milestoneGroups.size + 1
        assert(!milestoneGroups.contains(newId))
        milestoneGroups += newId -> milestones
        milestoneGroupsInverse += milestones -> newId
        newId
    }
  }

  def milestonesOfGroup(groupId: Int): Set[KernelTemplate] = milestoneGroups(groupId)

  def milestoneGroupIds: collection.Set[Int] = milestoneGroups.keySet

  def build(startGroupId: Int): MilestoneGroupParserData = {
    MilestoneGroupParserData(
      grammar,
      startGroupId,
      initialTasksSummary,
      milestoneGroups.toMap,
      termActions.toMap,
      tipEdgeProgressActions.toMap,
      midEdgeProgressActions.toMap
    )
  }
}

case class TermAction(
  // 현재 path는 appendingMilestoneGroups만큼 분화되고, 현재 group은 _1로 치환되고 뒤에 _2(appendingMilestones)가 붙음
  appendingMilestoneGroups: List[(KernelTemplate, AppendingMilestoneGroup)],
  // 현재 group 중 _1(groupId)가 _2의 조건을 갖고 progress된다
  startNodeProgress: List[(Int, AcceptConditionTemplate)],
  lookaheadRequiringSymbols: Set[LookaheadRequires],
  tasksSummary2: TasksSummary2,
  pendedAcceptConditionKernels: Map[KernelTemplate, (List[AppendingMilestoneGroup], Option[AcceptConditionTemplate])],
)

case class EdgeAction(
  // 현재 path는 appendingMilestoneGroups만큼 분화되고, 현재 group은 _1로 치환되고 뒤에 _2(appendingMilestones)가 붙음
  appendingMilestoneGroups: List[AppendingMilestoneGroup],
  // 현재 group 중 _1(groupId)가 _2의 조건을 갖고 progress된다
  startNodeProgress: List[AcceptConditionTemplate],
  lookaheadRequiringSymbols: Set[LookaheadRequires],
  tasksSummary2: TasksSummary2,
  requiredSymbols: Set[Int],
)

case class LookaheadRequires(symbolId: Int, groupId: Int)

case class AppendingMilestoneGroup(
  groupId: Int,
  acceptCondition: AcceptConditionTemplate,
) extends Ordered[AppendingMilestoneGroup] {
  override def compare(that: AppendingMilestoneGroup): Int = groupId - that.groupId
}
