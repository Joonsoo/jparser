package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.milestone2.{Always, KernelTemplate, Milestone, MilestoneAcceptCondition, Never}

import scala.collection.{MapView, mutable}

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
  progressedKernels: Map[(Milestone, Int), MilestoneAcceptCondition],
  progressedKgroups: Map[(MilestoneGroup, Int), MilestoneAcceptCondition],
) {
  // TODO progressedMilestones, progressedMgroups
  val progressedMilestones: MapView[Milestone, MilestoneAcceptCondition] = progressedKernels.toList.groupBy(_._1._1)
    .view.mapValues(pairs => MilestoneAcceptCondition.disjunct(pairs.map(_._2).toSet))

  def milestoneProgressConditionOf(milestone: Milestone): MilestoneAcceptCondition =
    progressedMilestones.getOrElse(milestone, Never)
}

class GenActionsBuilder() {
  val termActions: mutable.ListBuffer[(MilestoneGroup, TermAction)] = mutable.ListBuffer()
  val tipEdgeActions: mutable.ListBuffer[((Milestone, MilestoneGroup), EdgeAction)] = mutable.ListBuffer()
  val midEdgeActions: mutable.ListBuffer[((Milestone, Milestone), EdgeAction)] = mutable.ListBuffer()

  private val progressedKernels: mutable.Map[(Milestone, Int), MilestoneAcceptCondition] = mutable.Map()

  // kernel groups
  private val progressedKgroups: mutable.Map[(MilestoneGroup, Int), MilestoneAcceptCondition] = mutable.Map()

  def addProgressedKernel(milestone: Milestone, parentGen: Int, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedKernels.get(milestone -> parentGen) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedKernels += ((milestone -> parentGen) -> newCondition)
  }

  def addProgressedKernelGroup(mgroup: MilestoneGroup, parentGen: Int, condition: MilestoneAcceptCondition): Unit = {
    val newCondition = progressedKgroups.get(mgroup -> parentGen) match {
      case Some(existingCondition) =>
        MilestoneAcceptCondition.disjunct(Set(existingCondition, condition))
      case None => condition
    }
    progressedKgroups += ((mgroup -> parentGen) -> newCondition)
  }

  def build(): GenActions = {
    GenActions(
      termActions.toList,
      tipEdgeActions.toList,
      midEdgeActions.toList,
      progressedKernels.toMap,
      progressedKgroups.toMap,
    )
  }
}

case class HistoryEntry(untrimmedPaths: List[MilestoneGroupPath], genActions: GenActions)
