package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.milestone2._
import com.giyeok.jparser.nparser.AcceptCondition

import scala.math.Ordering.comparatorToOrdering

object AcceptConditionOrderings {
  // condition 정렬 기준 구현. assertEqualCondition에서 비교하기 위한 것
  implicit val naiveAcceptConditionOrdering: Ordering[AcceptCondition.AcceptCondition] = comparatorToOrdering {
    (o1: AcceptCondition.AcceptCondition, o2: AcceptCondition.AcceptCondition) =>
      compareAcceptCondition(o1, o2)
  }

  private def compareAcceptCondition(o1: AcceptCondition.AcceptCondition, o2: AcceptCondition.AcceptCondition): Int =
    (o1, o2) match {
      case (AcceptCondition.Always, AcceptCondition.Always) => 0
      case (AcceptCondition.Always, _) => 1

      case (AcceptCondition.Never, AcceptCondition.Always) => -1
      case (AcceptCondition.Never, AcceptCondition.Never) => 0
      case (AcceptCondition.Never, _) => 1

      case (_: AcceptCondition.And, AcceptCondition.Always | AcceptCondition.Never) => -1
      case (AcceptCondition.And(conds1), AcceptCondition.And(conds2)) =>
        compareCondsList(conds1.toList.sorted, conds2.toList.sorted)
      case (_: AcceptCondition.And, _) => 1

      case (_: AcceptCondition.Or, AcceptCondition.Always | AcceptCondition.Never | _: AcceptCondition.And) => -1
      case (AcceptCondition.Or(conds1), AcceptCondition.Or(conds2)) =>
        compareCondsList(conds1.toList.sorted, conds2.toList.sorted)
      case (_: AcceptCondition.Or, _) => 1

      case (_: AcceptCondition.NotExists, AcceptCondition.Always | AcceptCondition.Never |
                                          _: AcceptCondition.And | _: AcceptCondition.Or) => -1
      case (AcceptCondition.NotExists(beginGen1, endGen1, symbol1), AcceptCondition.NotExists(beginGen2, endGen2, symbol2)) =>
        if (symbol1 != symbol2) symbol1 - symbol2 else if (beginGen1 != beginGen2) beginGen1 - beginGen2 else endGen1 - endGen2
      case (_: AcceptCondition.Or, _) => 1

      case (_: AcceptCondition.Exists, AcceptCondition.Always | AcceptCondition.Never |
                                       _: AcceptCondition.And | _: AcceptCondition.Or |
                                       _: AcceptCondition.NotExists) => -1
      case (AcceptCondition.Exists(beginGen1, endGen1, symbol1), AcceptCondition.Exists(beginGen2, endGen2, symbol2)) =>
        if (symbol1 != symbol2) symbol1 - symbol2 else if (beginGen1 != beginGen2) beginGen1 - beginGen2 else endGen1 - endGen2
      case (_: AcceptCondition.Exists, _) => 1


      case (_: AcceptCondition.Unless, AcceptCondition.Always | AcceptCondition.Never |
                                       _: AcceptCondition.And | _: AcceptCondition.Or |
                                       _: AcceptCondition.NotExists | _: AcceptCondition.Exists) => -1
      case (AcceptCondition.Unless(beginGen1, endGen1, symbol1), AcceptCondition.Unless(beginGen2, endGen2, symbol2)) =>
        if (symbol1 != symbol2) symbol1 - symbol2 else if (beginGen1 != beginGen2) beginGen1 - beginGen2 else endGen1 - endGen2
      case (_: AcceptCondition.Unless, _) => 1

      case (AcceptCondition.OnlyIf(beginGen1, endGen1, symbol1), AcceptCondition.OnlyIf(beginGen2, endGen2, symbol2)) =>
        if (symbol1 != symbol2) symbol1 - symbol2 else if (beginGen1 != beginGen2) beginGen1 - beginGen2 else endGen1 - endGen2
      case (_: AcceptCondition.OnlyIf, _) => 1
    }

  private def compareCondsList(conds1: List[AcceptCondition.AcceptCondition], conds2: List[AcceptCondition.AcceptCondition]): Int =
    if (conds1.size != conds2.size) conds1.size - conds2.size else {
      conds1.zip(conds2).map(pair => compareAcceptCondition(pair._1, pair._2)).find(_ != 0).getOrElse(0)
    }

  implicit val mileestoneAcceptConditionOrdering: Ordering[MilestoneAcceptCondition] = comparatorToOrdering {
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
      case (NotExists(milestone1, check1), NotExists(milestone2, check2)) =>
        if (milestone1 != milestone2) compareMilestones(milestone1, milestone2) else check1.compare(check2)
      case (_: Or, _) => 1

      case (_: Exists, Always | Never | _: And | _: Or | _: NotExists) => -1
      case (Exists(milestone1, check1), Exists(milestone2, check2)) =>
        if (milestone1 != milestone2) compareMilestones(milestone1, milestone2) else check1.compare(check2)
      case (_: Exists, _) => 1


      case (_: Unless, Always | Never | _: And | _: Or | _: NotExists | _: Exists) => -1
      case (Unless(milestone1), Unless(milestone2)) =>
        compareMilestones(milestone1, milestone2)
      case (_: Unless, _) => 1

      case (OnlyIf(milestone1), OnlyIf(milestone2)) =>
        compareMilestones(milestone1, milestone2)
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
