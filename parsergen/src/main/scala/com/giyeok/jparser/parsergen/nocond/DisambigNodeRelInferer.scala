package com.giyeok.jparser.parsergen.nocond

// DisambigNodeRelInferer는 SimpleNodeRelInferer에 pop, restore 등의 추가적인 action 종류를 추가로 지원함
class DisambigNodeRelInferer(private val termActionAppends: Map[Int, Set[DisambigParser.Append]],
                             private val edgeActionReplaces: Map[(Int, Int), DisambigParser.ReplaceEdge],
                             private val termFinishSimReqs: Map[Int, TermFinishSimulationReq],
                             private val termFinishSimResults: Map[Int, DisambigParser.TermAction],
                             private val edgeFinishSimReqs: Map[Int, EdgeFinishSimulationReq],
                             private val edgeFinishSimResults: Map[Int, DisambigParser.EdgeAction],
                             val adjGraph: NodeAdjacencyGraph) {
    // stack top에 올 수 있는 노드들
    def nodesOnTop: Set[Int] = {
        val topsFromTermAppends = termActionAppends.toSeq flatMap { acts =>
            acts._2.toSeq flatMap {
                case DisambigParser.Append(_, append, _) => Seq(append.last)
                case _ => Seq[Int]()
            }
        }
        val topsFromTermFinAppends = termFinishSimResults.toSeq flatMap {
            case (_, DisambigParser.Append(_, append, _)) => Seq(append.last)
            case _ => Seq()
        }
        val topsFromPopAndReplaces = termFinishSimResults.toSeq flatMap {
            case (_, DisambigParser.PopAndReplace(_, replace, _)) => Seq(replace)
            case _ => Seq()
        }
        val topsFromEdgeReplaces = edgeActionReplaces.toSeq flatMap {
            case (_, DisambigParser.ReplaceEdge(_, replaceLast, _)) => Seq(replaceLast)
            case _ => Seq()
        }
        val topsFromEdgePopAndReplaces = edgeFinishSimResults.toSeq flatMap {
            case (_, DisambigParser.PopAndReplaceEdge(_, replace, _)) => Seq(replace)
            case _ => Seq()
        }
        (topsFromTermAppends ++ topsFromTermFinAppends ++ topsFromPopAndReplaces ++
            topsFromEdgeReplaces ++ topsFromEdgePopAndReplaces).toSet
    }

    def finishableEdges: Set[(Int, Int)] = {
        val edgesFromTermActions = termFinishSimResults.toSeq flatMap {
            case (finSimReqId, DisambigParser.Finish(replace)) =>
                prevOf(termFinishSimReqs(finSimReqId).baseId) map (_ -> replace)
            case (finSimReqId, DisambigParser.Append(_, _, Some(pendingFinish))) =>
                prevOf(termFinishSimReqs(finSimReqId).baseId) map (_ -> pendingFinish)
            case (finSimReqId, DisambigParser.PopAndReplace(popCount, replace, Some(pendingFinish))) =>
                // prevOf(edgeFinishSimReqs(finSimReqId).prevId) 에서 시작해서 popCount만큼 빼서 나올 수 있는 모든 prev -> pendingFinish
                // popCount가 0이면 그냥 prevOf(edgeFinishSimReqs(finSimReqId).prevId),
                // popCount가 1이면 prevOf(prevOf(edgeFinishSimReqs(finSimReqId).prevId)), etc.
                ???
            case _ => Seq()
        }
        val edgesFromEdgeActions = edgeFinishSimResults.toSeq flatMap {
            case (finSimReqId, DisambigParser.DropLast(replace)) =>
                prevOf(edgeFinishSimReqs(finSimReqId).prevId) map (_ -> replace)
            case (finSimReqId, DisambigParser.ReplaceEdge(_, _, Some(pendingFinish))) =>
                prevOf(edgeFinishSimReqs(finSimReqId).prevId) map (_ -> pendingFinish)
            case (finSimReqId, DisambigParser.PopAndReplaceEdge(popCount, replace, Some(pendingFinish))) =>
                // prevOf(edgeFinishSimReqs(finSimReqId).prevId) 에서 시작해서 popCount만큼 빼서 나올 수 있는 모든 prev -> pendingFinish
                // popCount가 0이면 그냥 prevOf(edgeFinishSimReqs(finSimReqId).prevId),
                // popCount가 1이면 prevOf(prevOf(edgeFinishSimReqs(finSimReqId).prevId)), etc.
                ???
            case _ => Seq()
        }
        (edgesFromTermActions ++ edgesFromEdgeActions).toSet
    }

    def prevOf(nodeId: Int): Set[Int] = adjGraph.adjacencies.adjByFoll.getOrElse(nodeId, Set())

    private def nodeRelDiffs(newInferer: DisambigNodeRelInferer): (DisambigNodeRelInferer, NodeRels) = {
        val newNodesOnTop = newInferer.nodesOnTop
        val newFinishableEdges = newInferer.finishableEdges
        assert(nodesOnTop subsetOf newNodesOnTop)
        assert(finishableEdges subsetOf newFinishableEdges)
        val newNodeRels = NodeRels(newNodesOnTop -- nodesOnTop, newFinishableEdges -- finishableEdges)
        (newInferer, newNodeRels)
    }

    def addTermActionAppend(baseNodeId: Int, append: DisambigParser.Append): (DisambigNodeRelInferer, NodeRels) = {
        val newTermActionAppends = termActionAppends + (baseNodeId -> (termActionAppends.getOrElse(baseNodeId, Set()) + append))
        val newAdjGraph0 = adjGraph.addReplace(baseNodeId, append.replace).addAppend(append.replace, append.append.head)
        val newAdjGraph = append.append.sliding(2).foldLeft(newAdjGraph0) { (m, i) =>
            if (i.size == 2) m.addAppend(i.head, i(1)) else m
        }
        val newInferer = new DisambigNodeRelInferer(newTermActionAppends, edgeActionReplaces, termFinishSimReqs,
            termFinishSimResults, edgeFinishSimReqs, edgeFinishSimResults, newAdjGraph)

        nodeRelDiffs(newInferer)
    }

    def addEdgeActionReplace(prevNodeId: Int, lastNodeId: Int, replace: DisambigParser.ReplaceEdge): (DisambigNodeRelInferer, NodeRels) = {
        val newEdgeActionReplaces = edgeActionReplaces + ((prevNodeId -> lastNodeId) -> replace)
        val newAdjGraph0 = adjGraph.addReplace(prevNodeId, replace.replacePrev).addAppend(replace.replacePrev, replace.replaceLast)
        val newAdjGraph = replace.pendingFinish match {
            case Some(pendingFinish) => newAdjGraph0.addReplace(replace.replacePrev, pendingFinish)
            case None => newAdjGraph0
        }
        val newInferer = new DisambigNodeRelInferer(termActionAppends, newEdgeActionReplaces, termFinishSimReqs,
            termFinishSimResults, edgeFinishSimReqs, edgeFinishSimResults, newAdjGraph)

        nodeRelDiffs(newInferer)
    }

    def addTermFinishable(baseId: Int, finSimReqId: Int, finSimReq: TermFinishSimulationReq): (DisambigNodeRelInferer, NodeRels) = {
        val newTermFinishSimReqs = termFinishSimReqs + (finSimReqId -> finSimReq)
        val newInferer = new DisambigNodeRelInferer(termActionAppends, edgeActionReplaces, newTermFinishSimReqs,
            termFinishSimResults, edgeFinishSimReqs, edgeFinishSimResults, adjGraph)

        nodeRelDiffs(newInferer)
    }

    def updateTermFinSimResult(finSimReqId: Int, action: DisambigParser.TermAction): (DisambigNodeRelInferer, NodeRels) = {
        val finSimReq = termFinishSimReqs(finSimReqId)
        val newTermFinishSimResults = termFinishSimResults + (finSimReqId -> action)
        val newAdjGraph = {
            var n = adjGraph
            action match {
                case DisambigParser.Finish(replace) =>
                    if (replace != finSimReq.baseId) n = n.addReplace(finSimReq.baseId, replace)
                case DisambigParser.Append(replace, append, pendingFinish) =>
                    if (replace != finSimReq.baseId) n = n.addReplace(finSimReq.baseId, replace)
                    n = n.addAppend(replace, append.head)
                    append.sliding(2) foreach { e =>
                        if (e.size == 2) n = n.addAppend(e.head, e.last)
                    }
                    if (pendingFinish.isDefined && pendingFinish.get != replace) {
                        n = n.addReplace(replace, pendingFinish.get)
                    }
                case DisambigParser.PopAndReplace(popCount, replace, pendingFinish) =>
                    ???
            }
            n
        }
        val newInferer = new DisambigNodeRelInferer(termActionAppends, edgeActionReplaces, termFinishSimReqs,
            newTermFinishSimResults, edgeFinishSimReqs, edgeFinishSimResults, newAdjGraph)

        // 여기선 NodeRel이 추가가 아니라 변경될 수도 있지 않을까?
        nodeRelDiffs(newInferer)
    }

    def addEdgeFinishable(edge: (Int, Int), finSimReqId: Int, finSimReq: EdgeFinishSimulationReq): (DisambigNodeRelInferer, NodeRels) = {
        val newEdgeFinishSimReqs = edgeFinishSimReqs + (finSimReqId -> finSimReq)
        val newInferer = new DisambigNodeRelInferer(termActionAppends, edgeActionReplaces, termFinishSimReqs,
            termFinishSimResults, newEdgeFinishSimReqs, edgeFinishSimResults, adjGraph)

        nodeRelDiffs(newInferer)
    }

    def updateEdgeFinSimResult(finSimReqId: Int, action: DisambigParser.EdgeAction): (DisambigNodeRelInferer, NodeRels) = {
        val finSimReq = edgeFinishSimReqs(finSimReqId)
        val newEdgeFinishSimResults = edgeFinishSimResults + (finSimReqId -> action)
        val newAdjGraph = {
            var n = adjGraph
            action match {
                case DisambigParser.DropLast(replace) =>
                    if (replace != finSimReq.prevId) n = n.addReplace(finSimReq.prevId, replace)
                case DisambigParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    if (replacePrev != finSimReq.prevId) n = n.addReplace(finSimReq.prevId, replacePrev)
                    n = n.addAppend(replacePrev, replaceLast)
                    if (pendingFinish.isDefined && pendingFinish.get != replacePrev) {
                        n = n.addReplace(replacePrev, pendingFinish.get)
                    }
                case DisambigParser.PopAndReplaceEdge(popCount, replace, pendingFinish) =>
                    ???
            }
            n
        }
        val newInferer = new DisambigNodeRelInferer(termActionAppends, edgeActionReplaces, termFinishSimReqs,
            termFinishSimResults, edgeFinishSimReqs, newEdgeFinishSimResults, newAdjGraph)

        // 여기선 NodeRel이 추가가 아니라 변경될 수도 있지 않을까?
        nodeRelDiffs(newInferer)
    }
}

object DisambigNodeRelInferer {
    val emptyInferer = new DisambigNodeRelInferer(Map(), Map(), Map(), Map(), Map(), Map(), NodeAdjacencyGraph.emptyGraph)
}
