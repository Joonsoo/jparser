package com.giyeok.jparser.milestone2

sealed abstract class AcceptConditionTemplate extends Ordered[AcceptConditionTemplate] {
  def symbolIds: Set[Int] = this match {
    case AndTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case OrTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case LookaheadIsTemplate(symbolId, _) => Set(symbolId)
    case LookaheadNotTemplate(symbolId, _) => Set(symbolId)
    case LongestTemplate(symbolId, _) => Set(symbolId)
    case OnlyIfTemplate(symbolId) => Set(symbolId)
    case UnlessTemplate(symbolId) => Set(symbolId)
    case _ => Set()
  }
}

object AcceptConditionTemplate {
  def conjunct(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.contains(NeverTemplate)) {
      NeverTemplate
    } else {
      val filtered = conditions.filter(_ != AlwaysTemplate)
      if (filtered.isEmpty) {
        AlwaysTemplate
      } else {
        if (filtered.size == 1) {
          filtered.head
        } else {
          val elems = filtered.collect {
            case AndTemplate(andElems) => andElems
            case els => List(els)
          }.flatten
          AndTemplate(elems.toList.sorted)
        }
      }
    }
  }

  def disjunct(conditions: Set[AcceptConditionTemplate]): AcceptConditionTemplate = {
    if (conditions.contains(AlwaysTemplate)) {
      AlwaysTemplate
    } else {
      val filtered = conditions.filter(_ != NeverTemplate)
      if (filtered.isEmpty) {
        NeverTemplate
      } else {
        if (filtered.size == 1) {
          filtered.head
        } else {
          val elems = filtered.collect {
            case OrTemplate(orElems) => orElems
            case els => List(els)
          }.flatten
          OrTemplate(elems.toList)
        }
      }
    }
  }
}

case object AlwaysTemplate extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate => 0
    case _ => 1
  }
}

case object NeverTemplate extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate => -1
    case NeverTemplate => 0
    case _ => 1
  }
}

case class AndTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate {
  //  assert(conditions.sorted == conditions)

  override def toString: String = s"AndTemplate(${conditions.map(_.toString).sorted.mkString(",")})"

  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate => -1
    case AndTemplate(otherConditions) =>
      MultiConditions.compareConditionsList(conditions, otherConditions)
    case _ => 1
  }
}

object MultiConditions {
  def compareConditionsList(conditions1: List[AcceptConditionTemplate], conditions2: List[AcceptConditionTemplate]): Int = {
    if (conditions1.size != conditions2.size) {
      conditions1.size - conditions2.size
    } else {
      conditions1.zip(conditions2).find(pair => pair._1.compare(pair._2) != 0) match {
        case Some((diff1, diff2)) => diff1.compare(diff2)
        case None => 0
      }
    }
  }
}

case class OrTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate {
  //  assert(conditions.sorted == conditions)

  override def toString: String = s"OrTemplate(${conditions.map(_.toString).sorted.mkString(",")})"

  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate => -1
    case OrTemplate(otherConditions) =>
      MultiConditions.compareConditionsList(conditions, otherConditions)
    case _ => 1
  }
}

// Exists(currGen, currGen, symbolId) or from nextGen
// Exists(Milestone(symbolId, 0, currGen)) or from nextGen
case class LookaheadIsTemplate(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate | _: OrTemplate => -1
    case LookaheadIsTemplate(otherSymbolId, otherFromNextGen) =>
      if (symbolId != otherSymbolId) {
        symbolId - otherSymbolId
      } else {
        fromNextGen.compareTo(otherFromNextGen)
      }
    case _ => 1
  }
}

// NotExists(currGen, currGen, symbolId) or from nextGen
// NotExists(Milestone(symbolId, 0, currGen), false) or from nextGen
case class LookaheadNotTemplate(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate | _: OrTemplate |
         _: LookaheadIsTemplate => -1
    case LookaheadNotTemplate(otherSymbolId, otherFromNextGen) =>
      if (symbolId != otherSymbolId) {
        symbolId - otherSymbolId
      } else {
        fromNextGen.compareTo(otherFromNextGen)
      }
    case _ => 1
  }
}

// NotExists(parentGen, currGen + 1, symbolId)
// NotExists(Milestone(symbolId, 0, parentGen), true)
case class LongestTemplate(symbolId: Int, beginFromNextGen: Boolean) extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate | _: OrTemplate |
         _: LookaheadIsTemplate | _: LookaheadNotTemplate => -1
    case LongestTemplate(otherSymbolId, otherBeginFromParentGen) =>
      if (symbolId != otherSymbolId) symbolId - otherSymbolId else beginFromNextGen.compare(otherBeginFromParentGen)
    case _ => 1
  }
}

// OnlyIf(parentGen, currGen, symbolId)
// OnlyIf(Milestone(symbolId, 0, parentGen))
case class OnlyIfTemplate(symbolId: Int) extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate | _: OrTemplate |
         _: LookaheadIsTemplate | _: LookaheadNotTemplate | _: LongestTemplate => -1
    case OnlyIfTemplate(otherSymbolId) =>
      symbolId - otherSymbolId
    case _ => 1
  }
}

// Unless(parentGen, currGen, symbolId)
// Unless(Milestone(symbolId, 0, parentGen))
case class UnlessTemplate(symbolId: Int) extends AcceptConditionTemplate {
  override def compare(that: AcceptConditionTemplate): Int = that match {
    case AlwaysTemplate | NeverTemplate | _: AndTemplate | _: OrTemplate |
         _: LookaheadIsTemplate | _: LookaheadNotTemplate |
         _: LongestTemplate | _: OnlyIfTemplate => -1
    case UnlessTemplate(otherSymbolId) =>
      symbolId - otherSymbolId
    case _ => 1
  }
}
