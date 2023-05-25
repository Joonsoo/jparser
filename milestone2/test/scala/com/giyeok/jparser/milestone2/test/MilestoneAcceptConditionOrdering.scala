package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.milestone2._

import scala.math.Ordering.comparatorToOrdering

object MilestoneAcceptConditionOrdering {
  // condition 정렬 기준 구현. assertEqualCondition에서 비교하기 위한 것
  implicit val milestoneAcceptConditionOrdering: Ordering[MilestoneAcceptCondition] = comparatorToOrdering {
    (o1: MilestoneAcceptCondition, o2: MilestoneAcceptCondition) => compareAcceptCondition(o1, o2)
  }

  private def compareAcceptCondition(o1: MilestoneAcceptCondition, o2: MilestoneAcceptCondition): Int =
    (o1, o2) match {
      case (Always, Always) => 0
      case (Always, _) => 1

      case (Never, Always) => -1
      case (Never, Never) => 0
      case (Never, _) => 1

      case (_: And, Always | Never) => -1
      case (And(conds1), And(conds2)) =>
        compareMACList(conds1.sorted, conds2.sorted)
      case (_: And, _) => 1

      case (_: Or, Always | Never | _: And) => -1
      case (Or(conds1), Or(conds2)) =>
        compareMACList(conds1.sorted, conds2.sorted)
      case (_: Or, _) => 1

      case (_: NotExists, Always | Never | _: And | _: Or) => -1
      case (NotExists(symbolId1, gen1, check1), NotExists(symbolId2, gen2, check2)) =>
        if (symbolId1 != symbolId2) symbolId1 - symbolId2 else {
          if (gen1 != gen2) gen1 - gen2 else check1.compare(check2)
        }
      case (_: NotExists, _) => 1

      case (_: Exists, Always | Never | _: And | _: Or | _: NotExists) => -1
      case (Exists(symbolId1, gen1, check1), Exists(symbolId2, gen2, check2)) =>
        if (symbolId1 != symbolId2) symbolId1 - symbolId2 else {
          if (gen1 != gen2) gen1 - gen2 else check1.compare(check2)
        }
      case (_: Exists, _) => 1


      case (_: Unless, Always | Never | _: And | _: Or | _: NotExists | _: Exists) => -1
      case (Unless(symbolId1, gen1), Unless(symbolId2, gen2)) =>
        if (symbolId1 != symbolId2) symbolId1 - symbolId2 else gen1 - gen2
      case (_: Unless, _) => 1

      case (OnlyIf(symbolId1, gen1), OnlyIf(symbolId2, gen2)) =>
        if (symbolId1 != symbolId2) symbolId1 - symbolId2 else gen1 - gen2
      case (_: OnlyIf, _) => 1
    }

  private def compareMACList(conds1: List[MilestoneAcceptCondition], conds2: List[MilestoneAcceptCondition]): Int =
    if (conds1.size != conds2.size) conds1.size - conds2.size else {
      conds1.zip(conds2).map(pair => compareAcceptCondition(pair._1, pair._2)).find(_ != 0).getOrElse(0)
    }

  private def compareMilestones(m1: Milestone, m2: Milestone): Int = {
    if (m1.symbolId != m2.symbolId) m1.symbolId - m2.symbolId
    else if (m1.pointer != m2.pointer) m1.pointer - m2.pointer
    else m1.gen - m2.gen
  }
}
