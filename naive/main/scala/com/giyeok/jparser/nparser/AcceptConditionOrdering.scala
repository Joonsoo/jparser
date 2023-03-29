package com.giyeok.jparser.nparser

import scala.math.Ordering.comparatorToOrdering

object AcceptConditionOrdering {
  implicit val acceptConditionOrdering: Ordering[AcceptCondition.AcceptCondition] = comparatorToOrdering {
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
      case (_: AcceptCondition.NotExists, _) => 1

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
}
