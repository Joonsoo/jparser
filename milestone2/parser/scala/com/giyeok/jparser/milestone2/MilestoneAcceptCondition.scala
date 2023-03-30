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

  def reify(template: AcceptConditionTemplate, beginGen: Int, gen: Int): MilestoneAcceptCondition =
    template match {
      case AlwaysTemplate => Always
      case NeverTemplate => Never
      case AndTemplate(conditions) =>
        And(conditions.map(reify(_, beginGen, gen)).distinct)
      case OrTemplate(conditions) =>
        Or(conditions.map(reify(_, beginGen, gen)).distinct)
      case LookaheadIsTemplate(symbolId, fromNextGen) =>
        Exists(Milestone(symbolId, 0, gen), checkFromNextGen = fromNextGen)
      case LookaheadNotTemplate(symbolId, fromNextGen) =>
        NotExists(Milestone(symbolId, 0, gen), checkFromNextGen = fromNextGen)
      case LongestTemplate(symbolId, beginFromNextGen) =>
        NotExists(Milestone(symbolId, 0, if (beginFromNextGen) gen else beginGen), checkFromNextGen = true)
      case OnlyIfTemplate(symbolId) =>
        OnlyIf(Milestone(symbolId, 0, beginGen))
      case UnlessTemplate(symbolId) =>
        Unless(Milestone(symbolId, 0, beginGen))
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

  override def toString: String = s"And(${conditions.map(_.toString).sorted.mkString(", ")})"
}

case class Or(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition =
    MilestoneAcceptCondition.conjunct(conditions.map(_.negation).toSet)

  def milestones: Set[Milestone] = conditions.flatMap(_.milestones).toSet

  override def toString: String = s"Or(${conditions.map(_.toString).sorted.mkString(", ")})"
}

case class Exists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = NotExists(milestone, checkFromNextGen)

  def milestones: Set[Milestone] = Set(milestone)

  override def toString: String = s"Exists(${milestone.symbolId} ${milestone.pointer} ${milestone.gen}, ${checkFromNextGen})"
}

case class NotExists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Exists(milestone, checkFromNextGen)

  def milestones: Set[Milestone] = Set(milestone)

  override def toString: String = s"NotExists(${milestone.symbolId} ${milestone.pointer} ${milestone.gen}, ${checkFromNextGen})"
}

case class OnlyIf(milestone: Milestone) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Unless(milestone)

  def milestones: Set[Milestone] = Set(milestone)

  override def toString: String = s"OnlyIf(${milestone.symbolId} ${milestone.pointer} ${milestone.gen})"
}

case class Unless(milestone: Milestone) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = OnlyIf(milestone)

  def milestones: Set[Milestone] = Set(milestone)

  override def toString: String = s"Unless(${milestone.symbolId} ${milestone.pointer} ${milestone.gen})"
}
