package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.milestone2.{Always, KernelTemplate, Milestone, MilestoneAcceptCondition}

import scala.collection.mutable

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

object MilestoneGroupPath {
  def apply(milestone: Milestone, group: MilestoneGroup) =
    new MilestoneGroupPath(milestone, List(), group, Always)
}

case class MilestoneGroup(groupId: Int, gen: Int)

case class GenActions(
  termActions: List[(MilestoneGroup, TermAction)],
  tipEdgeActions: List[((Milestone, MilestoneGroup), EdgeAction)],
  midEdgeActions: List[((Milestone, Milestone), EdgeAction)],
  progressedMilestones: Map[Milestone, MilestoneAcceptCondition],
  progressedMilestoneParentGens: Map[Milestone, Set[Int]],
  progressedMgroups: Map[MilestoneGroup, MilestoneAcceptCondition],
  progressedMgroupParentGens: Map[MilestoneGroup, Set[Int]],
)

class GenActionsBuilder() {
  val termActions: mutable.ListBuffer[(MilestoneGroup, TermAction)] = mutable.ListBuffer()
  val tipEdgeActions: mutable.ListBuffer[((Milestone, MilestoneGroup), EdgeAction)] = mutable.ListBuffer()
  val midEdgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  private val progressedMilestones: mutable.Map[Milestone, MilestoneAcceptCondition] = mutable.Map()
  private val progressedMilestoneParentGens: mutable.Map[Milestone, mutable.Set[Int]] = mutable.Map()

  private val progressedMgroups: mutable.Map[MilestoneGroup, MilestoneAcceptCondition] = mutable.Map()
  private val progressedMgroupParentGens: mutable.Map[MilestoneGroup, mutable.Set[Int]] = mutable.Map()

  def addProgressedMilestone(milestone: Milestone, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedMilestones.get(milestone) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedMilestones += (milestone -> newCondition)
  }

  def addProgressedMilestoneParentGen(milestone: Milestone, parentGen: Int): Unit = {
    progressedMilestoneParentGens.getOrElseUpdate(milestone, mutable.Set()).add(parentGen)
  }

  def addProgressedMilestoneGroup(mgroup: MilestoneGroup, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedMgroups.get(mgroup) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedMgroups += (mgroup -> newCondition)
  }

  def addProgressedMilestoneGroupParentGen(mgroup: MilestoneGroup, parentGen: Int): Unit = {
    progressedMgroupParentGens.getOrElseUpdate(mgroup, mutable.Set()).add(parentGen)
  }

  def build(): GenActions = {
    GenActions(
      termActions.toList,
      tipEdgeActions.toList,
      midEdgeActions.toList,
      progressedMilestones.toMap,
      progressedMilestoneParentGens.view.mapValues(_.toSet).toMap,
      progressedMgroups.toMap,
      progressedMgroupParentGens.view.mapValues(_.toSet).toMap,
    )
  }
}

case class HistoryEntry(untrimmedPaths: List[MilestoneGroupPath], genActions: GenActions)
