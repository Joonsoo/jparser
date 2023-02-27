package com.giyeok.jparser.milestone2

import com.giyeok.jparser.fast.KernelTemplate

case class ParsingContext(
  gen: Int,
  paths: List[MilestonePath],
  actionsHistory: List[GenActions],
  conditionsUpdates: Map[(Int, MilestoneAcceptCondition), MilestoneAcceptCondition])

// path는 가장 뒤에 것이 가장 앞에 옴. first는 언제나 path.last와 동일
case class MilestonePath(first: Milestone, path: List[Milestone], acceptCondition: MilestoneAcceptCondition) {
  def prettyString: String = {
    val milestones = path.reverse.map(milestone => s"${milestone.symbolId} ${milestone.pointer} ${milestone.gen}")
    s"${milestones.mkString(" -> ")} ($acceptCondition)"
  }

  def tip: Milestone = path.head

  def tipParent: Option[Milestone] = path.drop(1).headOption

  def append(newTip: Milestone, newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, newTip +: path, newAcceptCondition)

  def pop(newAcceptCondition: MilestoneAcceptCondition): MilestonePath =
    MilestonePath(first, path.drop(1), newAcceptCondition)
}

object MilestonePath {
  def apply(milestone: Milestone): MilestonePath =
    MilestonePath(milestone, List(milestone), Always)
}

case class Milestone(symbolId: Int, pointer: Int, gen: Int) {
  def kernelTemplate: KernelTemplate = KernelTemplate(symbolId, pointer)
}

object Milestone {
  def apply(template: KernelTemplate, gen: Int): Milestone =
    Milestone(template.symbolId, template.pointer, gen)
}

// TODO TermAction하고 EdgeAction에 ID를 붙이는게 좋을까?
case class GenActions(
  termActions: List[(Milestone, TermAction)],
  edgeActions: List[((Milestone, Milestone), EdgeAction)],
  progressedMilestones: Map[Milestone, MilestoneAcceptCondition],
  progressedMilestoneParentGens: Map[Milestone, Int],
)
