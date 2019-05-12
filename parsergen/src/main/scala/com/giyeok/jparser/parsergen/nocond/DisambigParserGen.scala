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

    private var termFinishSimulationReqs = Map[Int, TermFinishSimulationReq]()
    private var edgeFinishSimulationReqs = Map[Int, EdgeFinishSimulationReq]()

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
        val newReqId = termFinishSimulationReqs.size
        val finSimReq = TermFinishSimulationReq(baseId, change)
        termFinishSimulationReqs += newReqId -> finSimReq
        updateNodeRelInferer(nodeRelInferer.addTermFinishable(baseId, newReqId, finSimReq))
        newReqId
    }

    private def createEdgeFinSimReq(edge: (Int, Int), change: GraphChange): Int = {
        val newReqId = edgeFinishSimulationReqs.size
        val finSimReq = EdgeFinishSimulationReq(edge._1, edge._2, change)
        edgeFinishSimulationReqs += newReqId -> finSimReq
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
                    FinishableTermAction(createTermFinSimReq(baseId, change))
                case Some(Following(following, pendingFinishReplace)) =>
                    if (pendingFinishReplace.items.isEmpty) {
                        // pendingFinish가 필요한 path가 전혀 없으면 바로 append로 확정
                        val followingNode = AKernelSetPathSet(Seq(AKernelSetPath(List(following))))
                        val followingId = nodeIdOf(followingNode)
                        val effectivePaths = effectivePathsFrom(base.paths, change)
                        val replaceId = nodeIdOf(AKernelSetPathSet(effectivePaths))
                        val appendAction = DisambigParser.Append(replaceId, List(followingId), None)
                        addTermActionAppend(baseId, appendAction)
                        FixedTermAction(appendAction)
                    } else {
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
                FinishableEdgeAction(createEdgeFinSimReq(edge, change))
            case Some(Following(following, pendingFinishReplace)) =>
                if (pendingFinishReplace.items.nonEmpty) {
                    FinishableEdgeAction(createEdgeFinSimReq(edge, change))
                } else {
                    val effectivePaths = effectivePathsFrom(start.paths, change)
                    val replaceId = nodeIdOf(AKernelSetPathSet(effectivePaths))
                    val followingId = nodeIdOf(AKernelSetPathSet(Seq(AKernelSetPath(List(following)))))
                    val replaceAction = DisambigParser.ReplaceEdge(replaceId, followingId, None)
                    addEdgeActionReplace(edge, replaceAction)
                    FixedEdgeAction(replaceAction)
                }
        }
        edgeActions += edge -> genEdgeAction
    }

    private def applyGraphChangeToPath(path: AKernelSetPath, graphChange: GraphChange): (AKernelSetPath, Boolean) = {
        val init = path.path.init
        graphChange.following match {
            case None =>
                val newPath = init :+ graphChange.replacePrev
                (AKernelSetPath(newPath), true)
            case Some(Following(_, pendingFinishReplace)) =>
                if (pendingFinishReplace.items.isEmpty) {
                    val newPath = init.init :+ pendingFinishReplace
                    (AKernelSetPath(newPath), false)
                } else {
                    val newPath = init :+ pendingFinishReplace
                    (AKernelSetPath(newPath), true)
                }
        }
    }

    private def calculateTermFinishable(finSimReqId: Int): Unit = {
        val finSimReq = termFinishSimulationReqs(finSimReqId)
        val base = nodes.byKey(finSimReq.baseId)

        // base에서 finSimReq의 change 더이상 pendingFinish가 없을 때까지 extend하면서 모든 가능한 path를 계산해봄
        // 그 중에 stack top에 오는 AKernelSet 이 받을 수 있는 term끼리 겹치는 path가 있으면 그 path들을 묶어서 노드로 만듦
    }

    private def calculateEdgeFinishable(finSimReqId: Int): Unit = {
        val finSimReq = edgeFinishSimulationReqs(finSimReqId)
        val prev = nodes.byKey(finSimReq.prevId)
        println("edgeFin", finSimReq)

        // calculateTermFinishable 하고 거의 같음.
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

            termFinishSimulationReqs.keys foreach calculateTermFinishable
            edgeFinishSimulationReqs.keys foreach calculateEdgeFinishable
        }

        new DisambigParser(grammar, nodes.byKey, ???, startId, ???, ???)
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
