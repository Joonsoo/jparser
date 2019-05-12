package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.parsergen.utils.BiDirectionalMap

sealed trait GenTermAction

case class FixedTermAction(termAction: DisambigParser.TermAction) extends GenTermAction

case class FinishableTermAction(finSimReqId: Int) extends GenTermAction

sealed trait GenEdgeAction

case class FixedEdgeAction(edgeAction: DisambigParser.EdgeAction) extends GenEdgeAction

case class FinishableEdgeAction(finSimReqId: Int) extends GenEdgeAction

// TermFinishSimulationReq와 EdgeFinishSimulationReq의 effectivePaths는 baseId/prevId에서 effective path들의 목록
case class TermFinishSimulationReq(baseId: Int, change: GraphChange)

case class EdgeFinishSimulationReq(prevId: Int, lastId: Int, change: GraphChange)

class DisambigParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    // DisambigParserGen에서는 SimpleParserGen과 달리 `nodes`와 `nodesById`가 항상 같은 내용을 갖지는 않음.
    // 같은 pathSet을 갖는 두 개 이상의 노드ID가 있을 수 있기 때문
    private var nodes = BiDirectionalMap[Int, AKernelSetPathSet]()

    // termAction에서 append되는 노드를 같은 AKernelSetPathSet의 노드가 있어도 새 id를 부여하는 노드
    private var heavyBranchNodes = Set[AKernelSetPathSet]()
    private var newHeavyBranchNodes = Set[AKernelSetPathSet]()

    private var termActions = Map[(Int, CharacterTermGroupDesc), GenTermAction]()
    private var edgeActions = Map[(Int, Int), GenEdgeAction]()

    private var termFinishSimReqs = Map[Int, TermFinishSimulationReq]()
    private var edgeFinishSimReqs = Map[Int, EdgeFinishSimulationReq]()

    private var termFinishSimResults = Map[Int, DisambigParser.TermAction]()
    private var edgeFinishSimResults = Map[Int, DisambigParser.EdgeAction]()

    private var nodeRelInferer = DisambigNodeRelInferer.emptyInferer

    private var newToppableNodes = Set[Int]()
    private var newFinishableEdges = Set[(Int, Int)]()

    private def addNode(pathSet: AKernelSetPathSet): Int = {
        val newId = nodes.size
        nodes += newId -> pathSet
        // newToppableNodes += newId 는 node rel inferer 구현되면 지워야함
        newToppableNodes += newId
        newId
    }

    private def nodeIdOf(pathSet: AKernelSetPathSet): Int =
        nodes.byVal.getOrElse(pathSet, addNode(pathSet))

    private def updateNodeRelInferer(result: (DisambigNodeRelInferer, NodeRels)): Unit = {
        val (newInferer, newRels) = result
        nodeRelInferer = newInferer
        newToppableNodes ++= newRels.nodesOnTop
        newFinishableEdges ++= newRels.finishableEdges
    }

    private def createTermFinSimReq(baseId: Int, change: GraphChange): Int = {
        val newReqId = termFinishSimReqs.size
        val finSimReq = TermFinishSimulationReq(baseId, change)
        termFinishSimReqs += newReqId -> finSimReq
        updateNodeRelInferer(nodeRelInferer.addTermFinishable(baseId, newReqId, finSimReq))
        newReqId
    }

    private def createEdgeFinSimReq(edge: (Int, Int), change: GraphChange): Int = {
        val newReqId = edgeFinishSimReqs.size
        val finSimReq = EdgeFinishSimulationReq(edge._1, edge._2, change)
        edgeFinishSimReqs += newReqId -> finSimReq
        updateNodeRelInferer(nodeRelInferer.addEdgeFinishable(edge, newReqId, finSimReq))
        newReqId
    }

    private def addTermActionAppend(baseNodeId: Int, appendAction: DisambigParser.Append): Unit =
        updateNodeRelInferer(nodeRelInferer.addTermActionAppend(baseNodeId, appendAction))

    private def addEdgeActionReplace(edge: (Int, Int), replaceAction: DisambigParser.ReplaceEdge): Unit =
        updateNodeRelInferer(nodeRelInferer.addEdgeActionReplace(edge._1, edge._2, replaceAction))

    private def effectivePathsFrom(paths: Seq[AKernelSetPath], change: GraphChange): Seq[AKernelSetPath] =
        paths flatMap { path =>
            val intersect = path.path.last.items intersect change.replacePrev.items
            if (intersect.isEmpty) None else Some(path.replaceLast(AKernelSet(intersect)))
        }

    private def calculateTermActions(baseId: Int): Unit = {
        // TODO heavy=true이면 생성되는 TermAction에 등장하는 append 노드를 항상 새로 만든다(nodeIdOf 대신 addNode 사용)
        val base = nodes.byKey(baseId)
        val isHeavy = heavyBranchNodes contains base
        val lasts = AKernelSet((base.lasts flatMap (_.items)).toSet)
        val terms = analyzer.acceptableTerms(AKernelSet((base.lasts flatMap (_.items)).toSet))
        terms foreach { term =>
            // base.lasts 중에 term을 받을 수 있는 것들만 추림.
            val change = analyzer.termChanges(lasts, term)

            val genTermAction: GenTermAction = change.following match {
                case None =>
                    // Finish
                    FinishableTermAction(createTermFinSimReq(baseId, change))
                case Some(Following(following, pendingFinishReplace)) =>
                    if (pendingFinishReplace.items.isEmpty) {
                        // pendingFinish가 필요한 path가 전혀 없으면 바로 Append로 확정
                        val followingNode = AKernelSetPathSet(Seq(AKernelSetPath(List(following))))
                        val followingId = nodeIdOf(followingNode)
                        val effectivePaths = effectivePathsFrom(base.paths, change)
                        val replaceId = nodeIdOf(AKernelSetPathSet(effectivePaths))
                        val appendAction = DisambigParser.Append(replaceId, List(followingId), None)
                        addTermActionAppend(baseId, appendAction)
                        FixedTermAction(appendAction)
                    } else {
                        // Append w/ pendingFinish
                        FinishableTermAction(createTermFinSimReq(baseId, change))
                    }
            }
            termActions += (baseId, term) -> genTermAction
        }
    }

    private def calculateEdgeActions(edge: (Int, Int)): Unit = {
        // edge._1.lasts와 edge._2.heads가 연결되어 있다고 보고 edge action 계산
        // 이 때 만들어진 액션의 pendingFinish가 None이 아닌 경우,
        // finish simulation 해서 액션을 바꾸고 heavyBranchNodes를 추가해야할 수 있음
        val (startId, nextId) = edge
        val start = nodes.byKey(startId)
        val next = nodes.byKey(nextId)
        val startLasts = AKernelSet((start.lasts flatMap (_.items)).toSet)
        val nextHeads = AKernelSet((next.heads flatMap (_.items)).toSet)
        val change = analyzer.edgeChanges(startLasts, nextHeads)


        val genEdgeAction: GenEdgeAction = change.following match {
            case None =>
                // Finish
                FinishableEdgeAction(createEdgeFinSimReq(edge, change))
            case Some(Following(following, pendingFinishReplace)) =>
                if (pendingFinishReplace.items.isEmpty) {
                    // Append
                    val effectivePaths = effectivePathsFrom(start.paths, change)
                    val replaceId = nodeIdOf(AKernelSetPathSet(effectivePaths))
                    val followingId = nodeIdOf(AKernelSetPathSet(Seq(AKernelSetPath(List(following)))))
                    val replaceAction = DisambigParser.ReplaceEdge(replaceId, followingId, None)
                    addEdgeActionReplace(edge, replaceAction)
                    FixedEdgeAction(replaceAction)
                } else {
                    // Append w/ pendingFinish
                    FinishableEdgeAction(createEdgeFinSimReq(edge, change))
                }
        }
        edgeActions += edge -> genEdgeAction
    }

    //    private def applyGraphChangeToPath(path: AKernelSetPath, graphChange: GraphChange): (AKernelSetPath, Option[AKernelSetPath]) = {
    //        graphChange.following match {
    //            case None =>
    //                // Finish
    //                val newPath = path.path.init :+ graphChange.replacePrev
    //                (AKernelSetPath(newPath), true)
    //            case Some(Following(_, pendingFinishReplace)) =>
    //                if (pendingFinishReplace.items.isEmpty) {
    //                    // Append
    //                    val newPath = path.path :+ pendingFinishReplace
    //                    (AKernelSetPath(newPath), false)
    //                } else {
    //                    // Append w/ pendingFinish
    //                    val newPath = init :+ pendingFinishReplace
    //                    (AKernelSetPath(newPath), true)
    //                }
    //        }
    //    }

    // nodeRelInferer.prevOf 를 포함해서, 실제로 path(의 last)에서 change가 일어난 경우 발생 가능한 "모든" finish 경우의 수를 반환
    // 반환의 List[Int]는 사용된 node id들. 만약 path 안에서 다 해결 되면 빈 리스트가 반환됨.
    private def simulateFinish(nodeId: Int, path: AKernelSetPath, change: GraphChange): Seq[(List[Int], AKernelSetPath)] = {
        assert(nodes.byKey(nodeId).paths contains path)

        val (paths, moreFins) = simulateFinishLocally(path, change)
        moreFins match {
            case None => paths map { p => (List(), p) }
            case Some((lastKernel, lastChange)) =>
                val x = nodeRelInferer.prevOf(nodeId).toSeq flatMap { prevId =>
                    nodes.byKey(prevId).paths map { path =>
                        simulateFinishLocally(AKernelSetPath(path.path :+ lastKernel), lastChange)
                    }
                }
                ???
        }
    }

    // 주어진 path(의 last)에서 change가 일어난 경우 만들어질 수 있는 term accept가 가능한 path를 반환.
    // 반환 튜플의 _2는, finish를 계속 진행하다가 마지막 AKernelSet 하나만 남았는데 더 finish를 진행해야 하는 경우 그 AKernelSet 반환.
    private def simulateFinishLocally(path: AKernelSetPath, change: GraphChange): (Seq[AKernelSetPath], Option[(AKernelSet, GraphChange)]) = {
        val last = path.path.last
        if ((change.replacePrev.items intersect last.items).isEmpty) {
            // change에 기여하지 않은 path라면
            (Seq(), None)
        } else if (path.path.size == 1) {
            (Seq(), Some(last, change))
        } else {
            change.following match {
                case None =>
                    // Finish
                    simulateFinishLocally(AKernelSetPath(path.path.init :+ change.replacePrev),
                        analyzer.edgeChanges(path.path.init.last, change.replacePrev))
                case Some(Following(following, pendingFinishReplace)) =>
                    if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        val appended = path.path :+ following
                        (Seq(AKernelSetPath(appended)), None)
                    } else {
                        // Append w/ pendingFinish
                        val appended = path.path :+ following
                        val finished = simulateFinishLocally(AKernelSetPath(path.path.init :+ pendingFinishReplace),
                            analyzer.edgeChanges(path.path.init.last, pendingFinishReplace))
                        (AKernelSetPath(appended) +: finished._1, finished._2)
                    }
            }
        }
    }

    private def calculateTermFinishable(finSimReqId: Int): Unit = {
        val finSimReq = termFinishSimReqs(finSimReqId)
        val base = nodes.byKey(finSimReq.baseId)

        // 1. base에서 finSimReq의 change 더이상 pendingFinish가 없을 때까지 extend하면서 모든 가능한 path를 계산
        val allPaths = base.paths flatMap { p =>
            simulateFinish(finSimReq.baseId, p, finSimReq.change)
        }

        // 그 중에 stack top에 오는 AKernelSet 이 받을 수 있는 term끼리 겹치는 path가 있으면 그 path들을 묶어서 노드로 만듦
        // 계산 결과를 termFinishSimResults(finSimReqId) 에 넣음. nodeRelInferer에도 추가.
        ???
    }

    private def calculateEdgeFinishable(finSimReqId: Int): Unit = {
        val finSimReq = edgeFinishSimReqs(finSimReqId)
        val prev = nodes.byKey(finSimReq.prevId)
        println("edgeFin", finSimReq)

        // calculateTermFinishable 하고 거의 같음.
        // 계산 결과를 edgeFinishSimResults(finSimReqId) 에 넣음. nodeRelInferer에도 추가.
        ???
    }

    def generateParser(): DisambigParser = {
        val start = AKernelSetPathSet(Seq(AKernelSetPath(List(AKernelSet(Set(AKernel(grammar.startSymbol, 0)))))))
        val startId = nodeIdOf(start)

        newToppableNodes += startId

        while (newToppableNodes.nonEmpty || newFinishableEdges.nonEmpty || newHeavyBranchNodes.nonEmpty) {
            val processingNodes = newToppableNodes
            val processingAdjs = newFinishableEdges

            newToppableNodes = Set()
            newFinishableEdges = Set()
            newHeavyBranchNodes = Set()

            processingNodes foreach calculateTermActions
            processingAdjs foreach calculateEdgeActions

            termFinishSimReqs.keys foreach calculateTermFinishable
            edgeFinishSimReqs.keys foreach calculateEdgeFinishable
        }

        val finalTermActions = termActions mapValues {
            case FixedTermAction(termAction) => termAction
            case FinishableTermAction(finSimReqId) => termFinishSimResults(finSimReqId)
        }
        val finalEdgeActions = edgeActions mapValues {
            case FixedEdgeAction(edgeAction) => edgeAction
            case FinishableEdgeAction(finSimReqId) => edgeFinishSimResults(finSimReqId)
        }
        new DisambigParser(grammar, nodes.byKey, nodeRelInferer, startId, finalTermActions, finalEdgeActions)
    }
}

object DisambigParserGen {
    def main(args: Array[String]): Unit = {
        val grammar = SimpleGrammars.array0Grammar

        val ngrammar = NGrammar.fromGrammar(grammar)
        ngrammar.describe()

        val parser = new DisambigParserGen(ngrammar).generateParser()
        parser.describe()
    }
}
