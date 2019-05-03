package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

trait NodeRelAdjacents[N, E <: AbstractEdge[N], +Self <: AbstractGraph[N, E, Self]] extends AbstractGraph[N, E, Self] {
    def isReplaceEdge(edge: E): Boolean

    def isAppendEdge(edge: E): Boolean

    lazy val adjacents: Set[(N, N)] = {
        var builder = Set[(N, N)]()
        builder ++= edges collect { case e if isAppendEdge(e) => e.start -> e.end }

        def traverse(queue: List[N]): Unit = queue match {
            case head +: rest =>
                val repls = edgesByStart(head) collect { case e if isReplaceEdge(e) && e.start == head => e.end }
                val prevs = builder collect { case (prev, `head`) => prev }
                // head adjacent next 이면 replaceable adjacent next도 추가
                val replPrevs = repls flatMap { repl => prevs map { prev => (prev, repl) } }
                val newAdjs = replPrevs -- builder
                if (newAdjs.nonEmpty) {
                    builder ++= newAdjs
                    // TODO rest에 repls와 adjs를 다 추가하지 않을 순 없을까?
                    traverse((rest ++ repls ++ prevs).distinct)
                } else {
                    traverse(rest)
                }
            case List() => // do nothing
        }

        traverse(nodes.toList)
        builder
    }
}

sealed trait NodeRelEdge extends AbstractEdge[AKernelSet]

case class AppendRel(start: AKernelSet, end: AKernelSet) extends NodeRelEdge

case class ReplaceRel(start: AKernelSet, end: AKernelSet) extends NodeRelEdge

case class NodeRelGraph(nodes: Set[AKernelSet], edges: Set[NodeRelEdge], edgesByStart: Map[AKernelSet, Set[NodeRelEdge]], edgesByEnd: Map[AKernelSet, Set[NodeRelEdge]])
    extends AbstractGraph[AKernelSet, NodeRelEdge, NodeRelGraph] with NodeRelAdjacents[AKernelSet, NodeRelEdge, NodeRelGraph] {

    override def createGraph(nodes: Set[AKernelSet], edges: Set[NodeRelEdge], edgesByStart: Map[AKernelSet, Set[NodeRelEdge]], edgesByEnd: Map[AKernelSet, Set[NodeRelEdge]]): NodeRelGraph =
        NodeRelGraph(nodes, edges, edgesByStart, edgesByEnd)

    override def isAppendEdge(edge: NodeRelEdge): Boolean = edge.isInstanceOf[AppendRel]

    override def isReplaceEdge(edge: NodeRelEdge): Boolean = edge.isInstanceOf[ReplaceRel]

    def newAdjacentsByNewRel(newEdge: NodeRelEdge): (NodeRelGraph, Set[(AKernelSet, AKernelSet)]) = {
        // 일단 무식하게 해놓음.
        val oldAdjs = adjacents
        val newGraph = addEdge(newEdge)
        val newAdjs = newGraph.adjacents
        (newGraph, newAdjs -- oldAdjs)
    }

    def toIdNodeRelGraph(nodeIdOf: AKernelSet => Int): IdNodeRelGraph = {
        val idNodeRels0 = nodes.foldLeft(IdNodeRelGraph(Set(), Set(), Map(), Map())) { (m, i) =>
            m.addNode(nodeIdOf(i))
        }
        edges.foldLeft(idNodeRels0) { (m, i) =>
            val idEdge = i match {
                case AppendRel(start, end) => IdAppendRel(nodeIdOf(start), nodeIdOf(end))
                case ReplaceRel(start, end) => IdReplaceRel(nodeIdOf(start), nodeIdOf(end))
            }
            m.addEdge(idEdge)
        }
    }
}

object NodeRelGraph {
    val emptyGraph = NodeRelGraph(Set(), Set(), Map(), Map())
}


sealed trait IdNodeRelEdge extends AbstractEdge[Int]

case class IdAppendRel(start: Int, end: Int) extends IdNodeRelEdge

case class IdReplaceRel(start: Int, end: Int) extends IdNodeRelEdge

case class IdNodeRelGraph(nodes: Set[Int], edges: Set[IdNodeRelEdge], edgesByStart: Map[Int, Set[IdNodeRelEdge]], edgesByEnd: Map[Int, Set[IdNodeRelEdge]])
    extends AbstractGraph[Int, IdNodeRelEdge, IdNodeRelGraph] with NodeRelAdjacents[Int, IdNodeRelEdge, IdNodeRelGraph] {

    override def createGraph(nodes: Set[Int], edges: Set[IdNodeRelEdge], edgesByStart: Map[Int, Set[IdNodeRelEdge]], edgesByEnd: Map[Int, Set[IdNodeRelEdge]]): IdNodeRelGraph =
        IdNodeRelGraph(nodes, edges, edgesByStart, edgesByEnd)

    override def isAppendEdge(edge: IdNodeRelEdge): Boolean = edge.isInstanceOf[IdAppendRel]

    override def isReplaceEdge(edge: IdNodeRelEdge): Boolean = edge.isInstanceOf[IdReplaceRel]
}
