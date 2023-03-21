package com.giyeok.jparser.mgroup

import com.giyeok.jparser.graph.{AbstractEdge, AbstractGraph}
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Node}

case class MilestoneGroupPositionGraph(nodes: Set[Int], edges: Set[PositionEdge], edgesByStart: Map[Int, Set[PositionEdge]], edgesByEnd: Map[Int, Set[PositionEdge]]) extends AbstractGraph[Int, PositionEdge, MilestoneGroupPositionGraph] {
  override def createGraph(nodes: Set[Int], edges: Set[PositionEdge], edgesByStart: Map[Int, Set[PositionEdge]], edgesByEnd: Map[Int, Set[PositionEdge]]): MilestoneGroupPositionGraph =
    MilestoneGroupPositionGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object MilestoneGroupPositionGraph {
  val empty: MilestoneGroupPositionGraph = MilestoneGroupPositionGraph(Set(), Set(), Map(), Map())
}

sealed trait PositionEdge extends AbstractEdge[Int]

// start가 end로 치환될 수 있음
case class ReplacedBy(start: Int, end: Int) extends PositionEdge

// start 뒤에 end가 올 수 있음
case class FollowedBy(start: Int, end: Int) extends PositionEdge
