package com.giyeok.jparser.milestone2

sealed class AcceptConditionTemplate {
  def symbolIds: Set[Int] = this match {
    case AndTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case OrTemplate(conditions) => conditions.flatMap(_.symbolIds).toSet
    case LookaheadIsTemplate(symbolId, _) => Set(symbolId)
    case LookaheadNotTemplate(symbolId, _) => Set(symbolId)
    case LongestTemplate(symbolId) => Set(symbolId)
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
          AndTemplate(elems.toList)
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

case object AlwaysTemplate extends AcceptConditionTemplate

case object NeverTemplate extends AcceptConditionTemplate

case class AndTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class OrTemplate(conditions: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

// Exists(currGen, currGen, symbolId) or from nextGen
// Exists(Milestone(symbolId, 0, currGen)) or from nextGen
case class LookaheadIsTemplate(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

// NotExists(currGen, currGen, symbolId) or from nextGen
// NotExists(Milestone(symbolId, 0, currGen), false) or from nextGen
case class LookaheadNotTemplate(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

// NotExists(parentGen, currGen + 1, symbolId)
// NotExists(Milestone(symbolId, 0, parentGen), true)
case class LongestTemplate(symbolId: Int) extends AcceptConditionTemplate

// OnlyIf(parentGen, currGen, symbolId)
// OnlyIf(Milestone(symbolId, 0, parentGen))
case class OnlyIfTemplate(symbolId: Int) extends AcceptConditionTemplate

// Unless(parentGen, currGen, symbolId)
// Unless(Milestone(symbolId, 0, parentGen))
case class UnlessTemplate(symbolId: Int) extends AcceptConditionTemplate
