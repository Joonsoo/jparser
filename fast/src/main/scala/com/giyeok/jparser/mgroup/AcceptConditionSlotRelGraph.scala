package com.giyeok.jparser.mgroup

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

case class AcceptConditionSlotRelGraph(nodes: Set[AcceptConditionRelEntity], edges: Set[AcceptConditionRelEdge],
                                       edgesByStart: Map[AcceptConditionRelEntity, Set[AcceptConditionRelEdge]],
                                       edgesByEnd: Map[AcceptConditionRelEntity, Set[AcceptConditionRelEdge]]) extends AbstractGraph[AcceptConditionRelEntity, AcceptConditionRelEdge, AcceptConditionSlotRelGraph] {
  override def createGraph(nodes: Set[AcceptConditionRelEntity], edges: Set[AcceptConditionRelEdge], edgesByStart: Map[AcceptConditionRelEntity, Set[AcceptConditionRelEdge]], edgesByEnd: Map[AcceptConditionRelEntity, Set[AcceptConditionRelEdge]]): AcceptConditionSlotRelGraph =
    AcceptConditionSlotRelGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object AcceptConditionSlotRelGraph {
  val empty: AcceptConditionSlotRelGraph = AcceptConditionSlotRelGraph(Set(), Set(), Map(), Map())
}

sealed trait AcceptConditionRelEntity

case class AcceptConditionSlot(mgroupId: Int, slotIdx: Int) extends AcceptConditionRelEntity

case class AcceptConditionAtom(acceptCondition: AcceptCondition) extends AcceptConditionRelEntity

sealed trait AcceptConditionRelEdge extends AbstractEdge[AcceptConditionRelEntity]
