package com.giyeok.jparser.parsergen.nocond

case class NodeRels(nodesOnTop: Set[Int], finishableEdges: Set[(Int, Int)])

class SimpleNodeRelInferer(private val termActions: Map[Int, Set[SimpleParser.TermAction]],
                           private val edgeActions: Map[(Int, Int), SimpleParser.EdgeAction],
                           val adjGraph: NodeAdjacencyGraph) {
    // stack top에 올 수 있는 노드들
    def nodesOnTop: Set[Int] = {
        val topsFromAppends = termActions.values.toSeq flatMap { acts =>
            acts.toSeq flatMap { act =>
                act match {
                    case SimpleParser.Append(_, append, _) => Seq(append)
                    case _: SimpleParser.Finish => Seq()
                }
            }
        }
        val topsFromReplaceEdge = edgeActions.values.toSeq flatMap { act: SimpleParser.EdgeAction =>
            act match {
                case SimpleParser.ReplaceEdge(_, replaceLast, _) => Seq(replaceLast)
                case _: SimpleParser.DropLast => Seq()
            }
        }
        (topsFromAppends ++ topsFromReplaceEdge).toSet
    }

    // finish될 수 있는 엣지들
    def finishableEdges: Set[(Int, Int)] = {
        val edgesFromTermActions = termActions.toSeq flatMap { case (baseNodeId, acts) =>
            acts.toSeq flatMap {
                case SimpleParser.Finish(replace) => prevOf(baseNodeId) map (_ -> replace)
                case SimpleParser.Append(_, _, Some(pendingFinish)) =>
                    prevOf(baseNodeId) map (_ -> pendingFinish)
                case _: SimpleParser.Append => Set[(Int, Int)]()
            }
        }
        val edgesFromEdgeActions = edgeActions.toSeq flatMap { kv =>
            kv match {
                case ((prev, _), SimpleParser.DropLast(replace)) => prevOf(prev) map (_ -> replace)
                case ((prev, _), SimpleParser.ReplaceEdge(_, _, Some(pendingFinish))) =>
                    prevOf(prev) map (_ -> pendingFinish)
                case (_, _: SimpleParser.ReplaceEdge) => Set[(Int, Int)]()
            }
        }
        (edgesFromTermActions ++ edgesFromEdgeActions).toSet
    }

    // node 앞에 올 수 있는 노드들
    def prevOf(nodeId: Int): Set[Int] = adjGraph.adjacencies.adjByFoll.getOrElse(nodeId, Set())

    def addTermAction(baseNodeId: Int, termAction: SimpleParser.TermAction): (SimpleNodeRelInferer, NodeRels) = {
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
        val newInferer = new SimpleNodeRelInferer(newTermActions, edgeActions, newAdjGraph)
        assert(nodesOnTop subsetOf newInferer.nodesOnTop)
        assert(finishableEdges subsetOf newInferer.finishableEdges)
        val newNodeRels = NodeRels(newInferer.nodesOnTop -- nodesOnTop, newInferer.finishableEdges -- finishableEdges)
        (newInferer, newNodeRels)
    }

    def addEdgeAction(prevNodeId: Int, lastNodeId: Int, edgeAction: SimpleParser.EdgeAction): (SimpleNodeRelInferer, NodeRels) = {
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
        val newInferer = new SimpleNodeRelInferer(termActions, newEdgeActions, newAdjGraph)
        val newNodesOnTop = newInferer.nodesOnTop
        val newFinishableEdges = newInferer.finishableEdges
        assert(nodesOnTop subsetOf newNodesOnTop)
        assert(finishableEdges subsetOf newFinishableEdges)
        val newNodeRels = NodeRels(newNodesOnTop -- nodesOnTop, newFinishableEdges -- finishableEdges)
        (newInferer, newNodeRels)
    }
}

object SimpleNodeRelInferer {
    val emptyInferer = new SimpleNodeRelInferer(Map(), Map(), NodeAdjacencyGraph.emptyGraph)
}
