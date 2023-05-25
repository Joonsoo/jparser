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
        Exists(symbolId, gen, checkFromNextGen = fromNextGen)
      case LookaheadNotTemplate(symbolId, fromNextGen) =>
        NotExists(symbolId, gen, checkFromNextGen = fromNextGen)
      case LongestTemplate(symbolId, beginFromNextGen) =>
        NotExists(symbolId, if (beginFromNextGen) gen else beginGen, checkFromNextGen = true)
      case OnlyIfTemplate(symbolId, fromNextGen) =>
        OnlyIf(symbolId, if (fromNextGen) gen else beginGen)
      case UnlessTemplate(symbolId, fromNextGen) =>
        Unless(symbolId, if (fromNextGen) gen else beginGen)
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

case class Exists(symbolId: Int, gen: Int, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = NotExists(symbolId, gen, checkFromNextGen)

  val milestone = Milestone(symbolId, 0, gen)

  def milestones: Set[Milestone] = Set(milestone)

  override def toString: String = s"Exists($symbolId, $gen, $checkFromNextGen)"
}

case class NotExists(symbolId: Int, gen: Int, checkFromNextGen: Boolean) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Exists(symbolId, gen, checkFromNextGen)

  val milestone = Milestone(symbolId, 0, gen)

  def milestones: Set[Milestone] = Set(Milestone(symbolId, 0, gen))

  override def toString: String = s"NotExists($symbolId, $gen, $checkFromNextGen)"
}

case class OnlyIf(symbolId: Int, gen: Int) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = Unless(symbolId, gen)

  val milestone = Milestone(symbolId, 0, gen)

  def milestones: Set[Milestone] = Set(Milestone(symbolId, 0, gen))

  override def toString: String = s"OnlyIf($symbolId, $gen)"
}

case class Unless(symbolId: Int, gen: Int) extends MilestoneAcceptCondition {
  override def negation: MilestoneAcceptCondition = OnlyIf(symbolId, gen)

  val milestone = Milestone(symbolId, 0, gen)

  def milestones: Set[Milestone] = Set(Milestone(symbolId, 0, gen))

  override def toString: String = s"Unless($symbolId, $gen)"
}
