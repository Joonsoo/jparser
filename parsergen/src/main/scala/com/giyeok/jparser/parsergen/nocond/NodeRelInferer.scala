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

case class NodeRels(nodesOnTop: Set[Int], finishableEdges: Set[(Int, Int)])

class NodeRelInferer(private val termActions: Map[Int, Set[SimpleParser.TermAction]],
                     private val edgeActions: Map[(Int, Int), SimpleParser.EdgeAction],
                     val adjGraph: NodeAdjacencyGraph) {
    // stack top에 올 수 있는 노드들
    def nodesOnTop: Set[Int] = {
        val topsFromAppends = termActions.values.toSeq flatMap { acts =>
            acts.toSeq flatMap { act =>
                act match {
                    case SimpleParser.Append(_, append, pendingFinish) => Set(append) ++ pendingFinish.toSet
                    case _: SimpleParser.Finish => Set[Int]()
                }
            }
        }
        val topsFromReplaceEdge = edgeActions.values.toSeq flatMap { act: SimpleParser.EdgeAction =>
            act match {
                case SimpleParser.ReplaceEdge(_, replaceLast, pendingFinish) => Set(replaceLast) ++ pendingFinish.toSet
                case _: SimpleParser.DropLast => Set[Int]()
            }
        }
        (topsFromAppends ++ topsFromReplaceEdge).toSet
    }

    // finish될 수 있는 엣지들
    def finishableEdges: Set[(Int, Int)] = {
        val edgesFromFinishes = termActions.toSeq flatMap { case (baseNodeId, acts) =>
            acts.toSeq flatMap {
                case SimpleParser.Finish(replace) => prevOf(baseNodeId) map (_ -> replace)
                case SimpleParser.Append(_, _, Some(pendingFinish)) =>
                    prevOf(baseNodeId) map (_ -> pendingFinish)
                case _: SimpleParser.Append => Set[(Int, Int)]()
            }
        }
        val edgesFromDropLast = edgeActions.toSeq flatMap { kv =>
            kv match {
                case ((prev, _), SimpleParser.DropLast(replace)) => prevOf(prev) map (_ -> replace)
                case ((prev, _), SimpleParser.ReplaceEdge(_, _, Some(pendingFinish))) =>
                    prevOf(prev) map (_ -> pendingFinish)
                case (_, _: SimpleParser.ReplaceEdge) => Set[(Int, Int)]()
            }
        }
        (edgesFromFinishes ++ edgesFromDropLast).toSet
    }

    // node 앞에 올 수 있는 노드들
    def prevOf(nodeId: Int): Set[Int] = adjGraph.adjacencies.adjByFoll.getOrElse(nodeId, Set())

    def addTermAction(baseNodeId: Int, termAction: SimpleParser.TermAction): (NodeRelInferer, NodeRels) = {
        val newTermActions = termActions + (baseNodeId -> (termActions.getOrElse(baseNodeId, Set()) + termAction))
        val newAdjGraph = {
            var n = adjGraph
            termAction match {
                case SimpleParser.Finish(replace) =>
                    if (replace != baseNodeId) n = n.addReplace(baseNodeId, replace)
                case SimpleParser.Append(replace, append, pendingFinish) =>
                    if (replace != baseNodeId) n = n.addReplace(baseNodeId, replace)
                    n = n.addAppend(replace, append)
                    if (pendingFinish.isDefined && pendingFinish.get != replace) {
                        n = n.addReplace(replace, pendingFinish.get)
                    }
            }
            n
        }
        val newInferer = new NodeRelInferer(newTermActions, edgeActions, newAdjGraph)
        assert(nodesOnTop subsetOf newInferer.nodesOnTop)
        assert(finishableEdges subsetOf newInferer.finishableEdges)
        val newNodeRels = NodeRels(newInferer.nodesOnTop -- nodesOnTop, newInferer.finishableEdges -- finishableEdges)
        (newInferer, newNodeRels)
    }

    def addEdgeAction(prevNodeId: Int, lastNodeId: Int, edgeAction: SimpleParser.EdgeAction): (NodeRelInferer, NodeRels) = {
        val newEdgeActions = edgeActions + ((prevNodeId -> lastNodeId) -> edgeAction)
        val newAdjGraph = {
            var n = adjGraph
            edgeAction match {
                case SimpleParser.DropLast(replace) =>
                    if (replace != prevNodeId) n = n.addReplace(prevNodeId, replace)
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    if (replacePrev != prevNodeId) n = n.addReplace(prevNodeId, replacePrev)
                    n = n.addAppend(replacePrev, replaceLast)
                    if (pendingFinish.isDefined && pendingFinish.get != replacePrev) {
                        n = n.addReplace(replacePrev, pendingFinish.get)
                    }
            }
            n
        }
        val newInferer = new NodeRelInferer(termActions, newEdgeActions, newAdjGraph)
        assert(nodesOnTop subsetOf newInferer.nodesOnTop)
        assert(finishableEdges subsetOf newInferer.finishableEdges)
        val newNodeRels = NodeRels(newInferer.nodesOnTop -- nodesOnTop, newInferer.finishableEdges -- finishableEdges)
        (newInferer, newNodeRels)
    }
}

object NodeRelInferer {
    val emptyInferer = new NodeRelInferer(Map(), Map(), NodeAdjacencyGraph(Set(), Set(), Map(), Map()))
}
