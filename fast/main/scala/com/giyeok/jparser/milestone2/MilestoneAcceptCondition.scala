package com.giyeok.jparser.milestone2

object MilestoneAcceptCondition {
  def conjunct(conditions: Set[MilestoneAcceptCondition]): MilestoneAcceptCondition = {
    if (conditions.contains(Never)) {
      Never
    } else {
      val filtered = conditions.filter(_ != Always)
      if (filtered.isEmpty) {
        Always
      } else {
        if (filtered.size == 1) {
          filtered.head
        } else {
          val elems = filtered.collect {
            case And(andElems) => andElems
            case els => List(els)
          }.flatten
          And(elems.toList)
        }
      }
    }
  }

  def disjunct(conditions: Set[MilestoneAcceptCondition]): MilestoneAcceptCondition = {
    if (conditions.contains(Always)) {
      Always
    } else {
      val filtered = conditions.filter(_ != Never)
      if (filtered.isEmpty) {
        Never
      } else {
        if (filtered.size == 1) {
          filtered.head
        } else {
          val elems = filtered.collect {
            case Or(orElems) => orElems
            case els => List(els)
          }.flatten
          Or(elems.toList)
        }
      }
    }
  }
}

sealed abstract class MilestoneAcceptCondition {
  def negation: MilestoneAcceptCondition

  def milestones: Set[Milestone]
}

case object Always extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Never

  def milestones: Set[Milestone] = Set()
}

case object Never extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Always

  def milestones: Set[Milestone] = Set()
}

case class And(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition =
    MilestoneAcceptCondition.disjunct(conditions.map(_.negation).toSet)

  def milestones: Set[Milestone] = conditions.flatMap(_.milestones).toSet
}

case class Or(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition =
    MilestoneAcceptCondition.conjunct(conditions.map(_.negation).toSet)

  def milestones: Set[Milestone] = conditions.flatMap(_.milestones).toSet
}

case class Exists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = NotExists(milestone, checkFromNextGen)

  def milestones: Set[Milestone] = Set(milestone)
}

case class NotExists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Exists(milestone, checkFromNextGen)

  def milestones: Set[Milestone] = Set(milestone)
}

case class OnlyIf(milestone: Milestone) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Unless(milestone)

  def milestones: Set[Milestone] = Set(milestone)
}

case class Unless(milestone: Milestone) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = OnlyIf(milestone)

  def milestones: Set[Milestone] = Set(milestone)
}
