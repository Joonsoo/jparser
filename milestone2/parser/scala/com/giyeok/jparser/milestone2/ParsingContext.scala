package com.giyeok.jparser.milestone2

import scala.collection.MapView

case class ParsingContext(
  gen: Int,
  paths: List[MilestonePath],
  history: List[HistoryEntry])

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
  // (milestone, parentGen) -> parent gen
  // milestone과 parentGen을 조합하면 커널이 되기 때문에 이름이 progressedKernels
  progressedKernels: Map[(Milestone, Int), MilestoneAcceptCondition],
) {
  // TODO disjunct 맞겠지?
  // MilestoneAcceptCondition.disjunct(progressedKernels.filter(_._1._1 == milestone).values.toSet)
  val progressedMilestones: MapView[Milestone, MilestoneAcceptCondition] = progressedKernels.toList.groupBy(_._1._1)
    .view.mapValues(pairs => MilestoneAcceptCondition.disjunct(pairs.map(_._2).toSet))

  def milestoneProgressConditions(milestone: Milestone): MilestoneAcceptCondition =
    progressedMilestones.getOrElse(milestone, Never)
}

case class HistoryEntry(untrimmedPaths: List[MilestonePath], genActions: GenActions)
