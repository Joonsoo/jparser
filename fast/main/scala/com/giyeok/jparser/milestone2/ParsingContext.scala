package com.giyeok.jparser.milestone2

import com.giyeok.jparser.fast.KernelTemplate

case class ParsingContext(
  gen: Int,
  paths: List[MilestonePath],
  actionsHistory: List[GenActions],
  conditionsUpdates: ConditionUpdates,
  nextConditionsUpdates: ConditionUpdates)

case class ConditionUpdates(
  becameAlways: Set[(Int, MilestoneAcceptCondition)],
  becameNever: Set[(Int, MilestoneAcceptCondition)],
  tracking: Map[(Int, MilestoneAcceptCondition), MilestoneAcceptCondition],
) {
  def apply(gen: Int, condition: MilestoneAcceptCondition): MilestoneAcceptCondition = {
    if (condition == Always) {
      Always
    } else if (condition == Never) {
      Never
    } else {
      val key = gen -> condition
      if (becameAlways.contains(key)) {
        Always
      } else if (becameNever.contains(key)) {
        Never
      } else {
        tracking(key)
      }
    }
  }

  // val currConditionUpdates = ctx.nextConditionsUpdates ++ (newConditions.map(cond => (gen, cond) -> cond))
  def addNewConditions(gen: Int, newConditions: List[MilestoneAcceptCondition]): ConditionUpdates = {
    ConditionUpdates(becameAlways, becameNever, tracking ++ newConditions.filter(c => c != Always && c != Never).map(cond => (gen, cond) -> cond))
  }

  // val nextConditionUpdates = ctx.nextConditionsUpdates.view
  //   .mapValues(evolveAcceptCondition(newPaths, genActions, _)).toMap ++
  //     newConditionUpdates.map(cond => (gen, cond._1) -> cond._2)
  def evolveConditions(evolver: MilestoneAcceptCondition => MilestoneAcceptCondition): ConditionUpdates = {
    val evolved = tracking.view.mapValues(evolver)
    ConditionUpdates(
      becameAlways ++ evolved.filter(_._2 == Always).keySet,
      becameNever ++ evolved.filter(_._2 == Never).keySet,
      tracking ++ evolved.filter(p => p._2 != Always && p._2 != Never)
    )
  }

  def addNewUpdatedConditions(gen: Int, conditionUpdates: Map[MilestoneAcceptCondition, MilestoneAcceptCondition]): ConditionUpdates = {
    val updates = (conditionUpdates - Always - Never).view
    ConditionUpdates(
      becameAlways ++ updates.filter(_._2 == Always).keys.map(gen -> _),
      becameNever ++ updates.filter(_._2 == Never).keys.map(gen -> _),
      tracking ++ updates.filter(p => p._2 != Always && p._2 != Never).map(p => (gen, p._1) -> p._2)
    )
  }
}

object ConditionUpdates {
  def apply(): ConditionUpdates = ConditionUpdates(Set(), Set(), Map())
}

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
