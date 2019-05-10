package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

sealed trait NodeAdjEdge extends AbstractEdge[Int]

case class Append(start: Int, end: Int) extends NodeAdjEdge

case class Replace(start: Int, end: Int) extends NodeAdjEdge

case class Adjacencies(adjs: Set[(Int, Int)], adjByPrev: Map[Int, Set[Int]], adjByFoll: Map[Int, Set[Int]]) {
    def addAdj(prev: Int, foll: Int): Adjacencies = {
        val newAdjByPrev = adjByPrev + (prev -> (adjByPrev.getOrElse(prev, Set()) + foll))
        val newAdjByFoll = adjByFoll + (foll -> (adjByFoll.getOrElse(foll, Set()) + prev))
        Adjacencies(adjs + (prev -> foll), newAdjByPrev, newAdjByFoll)
    }

    def ++(adjs: Iterable[(Int, Int)]): Adjacencies =
        adjs.foldLeft(this) { (g, p) => g.addAdj(p._1, p._2) }
}

case class NodeAdjacencyGraph(nodes: Set[Int], edges: Set[NodeAdjEdge], edgesByStart: Map[Int, Set[NodeAdjEdge]], edgesByEnd: Map[Int, Set[NodeAdjEdge]])
    extends AbstractGraph[Int, NodeAdjEdge, NodeAdjacencyGraph] {

    override def createGraph(nodes: Set[Int], edges: Set[NodeAdjEdge], edgesByStart: Map[Int, Set[NodeAdjEdge]], edgesByEnd: Map[Int, Set[NodeAdjEdge]]): NodeAdjacencyGraph =
        NodeAdjacencyGraph(nodes, edges, edgesByStart, edgesByEnd)

    def addAppend(start: Int, end: Int): NodeAdjacencyGraph = addEdgeSafe(Append(start, end))

    def addReplace(start: Int, end: Int): NodeAdjacencyGraph = addEdgeSafe(Replace(start, end))

    lazy val adjacencies: Adjacencies = {
        var builder = Adjacencies(Set(), Map(), Map())
        builder ++= edges collect { case Append(start, end) => start -> end }

        def traverse(queue: List[Int]): Unit = queue match {
            case head +: rest =>
                val repls = edgesByStart(head) collect { case Replace(`head`, end) => end }
                val prevs = builder.adjByFoll.getOrElse(head, Set())
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
}

object NodeAdjacencyGraph {
    def emptyGraph = NodeAdjacencyGraph(Set(), Set(), Map(), Map())
}
