package com.giyeok.jparser.milestone2

sealed class MilestoneAcceptCondition

object MilestoneAcceptCondition {
  def conjunct(conditions: Set[MilestoneAcceptCondition]): MilestoneAcceptCondition = {
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

  def disjunct(conditions: Set[MilestoneAcceptCondition]): MilestoneAcceptCondition = {
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

case object Always extends MilestoneAcceptCondition

case object Never extends MilestoneAcceptCondition

case class And(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition

case class Or(conditions: List[MilestoneAcceptCondition]) extends MilestoneAcceptCondition

case class Exists(milestone: Milestone) extends MilestoneAcceptCondition

case class NotExists(milestone: Milestone, checkFromNextGen: Boolean) extends MilestoneAcceptCondition

case class OnlyIf(milestone: Milestone) extends MilestoneAcceptCondition

case class Unless(milestone: Milestone) extends MilestoneAcceptCondition
