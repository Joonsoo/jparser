package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.milestone2.{Milestone, MilestoneAcceptCondition}

case class ParsingContext(
  gen: Int,
  paths: List[MilestoneGroupPath],
  history: List[HistoryEntry])

case class MilestoneGroupPath(
  first: Milestone,
  path: List[Milestone],
  tip: MilestoneGroup,
  acceptCondition: MilestoneAcceptCondition) {

  def prettyString: String = {
    val milestones = path.reverse.map(milestone => s"${milestone.symbolId} ${milestone.pointer} ${milestone.gen}")
    s"${milestones.mkString(" -> ")} -> [${tip.groupId}] ${tip.gen} ($acceptCondition)"
  }

  def replaceAndAppend(replace: KernelTemplate, append: MilestoneGroup, condition: MilestoneAcceptCondition): MilestoneGroupPath =
    MilestoneGroupPath(first, Milestone(replace, tip.gen) +: path, append, condition)

  def tipParent: Option[Milestone] = path.headOption
}

case class MilestoneGroup(groupId: Int, gen: Int)

case class GenActions(
  termActions: List[(MilestoneGroup, TermAction)],
  edgeActions: List[((Milestone, MilestoneGroup), EdgeAction)],
  progressedMilestones: Map[Milestone, MilestoneAcceptCondition],
  progressedMilestoneParentGens: Map[Milestone, Set[Int]],
)

case class HistoryEntry(untrimmedPaths: List[MilestoneGroupPath], genActions: GenActions)
