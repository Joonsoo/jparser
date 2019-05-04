package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

sealed trait NodeRelEdge extends AbstractEdge[Int]

case class AppendRel(start: Int, end: Int) extends NodeRelEdge

case class ReplaceRel(start: Int, end: Int) extends NodeRelEdge

case class AdjacentNodes(adjs: Set[(Int, Int)], adjByPrev: Map[Int, Set[Int]], adjByFoll: Map[Int, Set[Int]]) {
    def addAdj(prev: Int, foll: Int): AdjacentNodes = {
        val newAdjByPrev = adjByPrev + (prev -> (adjByPrev.getOrElse(prev, Set()) + foll))
        val newAdjByFoll = adjByFoll + (foll -> (adjByFoll.getOrElse(foll, Set()) + prev))
        AdjacentNodes(adjs + (prev -> foll), newAdjByPrev, newAdjByFoll)
    }

    def ++(adjs: Iterable[(Int, Int)]): AdjacentNodes =
        adjs.foldLeft(this) { (g, p) => g.addAdj(p._1, p._2) }
}

case class NodeRelGraph(nodes: Set[Int], edges: Set[NodeRelEdge], edgesByStart: Map[Int, Set[NodeRelEdge]], edgesByEnd: Map[Int, Set[NodeRelEdge]])
    extends AbstractGraph[Int, NodeRelEdge, NodeRelGraph] {

    override def createGraph(nodes: Set[Int], edges: Set[NodeRelEdge], edgesByStart: Map[Int, Set[NodeRelEdge]], edgesByEnd: Map[Int, Set[NodeRelEdge]]): NodeRelGraph =
        NodeRelGraph(nodes, edges, edgesByStart, edgesByEnd)

    lazy val adjacentNodes: AdjacentNodes = {
        var builder = AdjacentNodes(Set(), Map(), Map())
        builder ++= edges collect { case AppendRel(start, end) => start -> end }

        def traverse(queue: List[Int]): Unit = queue match {
            case head +: rest =>
                val repls = edgesByStart(head) collect { case ReplaceRel(`head`, end) => end }
                val prevs = builder.adjByFoll(head)
                // head adjacent next 이면 replaceable adjacent next도 추가
                val replPrevs = repls flatMap { repl => prevs map { prev => (prev, repl) } }
                val newAdjs = replPrevs -- builder.adjs
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

    lazy val adjacents: Set[(Int, Int)] = adjacentNodes.adjs

    def newAdjacentsByNewRel(newEdge: NodeRelEdge): (NodeRelGraph, Set[(Int, Int)]) = {
        // 일단 무식하게 해놓음.
        val oldAdjs = adjacents
        val newGraph = addEdge(newEdge)
        val newAdjs = newGraph.adjacents
        (newGraph, newAdjs -- oldAdjs)
    }
}

object NodeRelGraph {
    val emptyGraph = NodeRelGraph(Set(), Set(), Map(), Map())
}
