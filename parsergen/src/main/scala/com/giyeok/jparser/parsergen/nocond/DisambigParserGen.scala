package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

class DisambigParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    private var nodes = Map[AKernelSetPathSet, Int]()
    private var nodesById = Map[Int, AKernelSetPathSet]()
    // termAction에서 append되는 노드를 같은 AKernelSetPathSet의 노드가 있어도 새 id를 부여하는 노드
    private var heavyBranchNodes = Set[AKernelSetPathSet]()
    private var termActions = Map[(AKernelSetPathSet, CharacterTermGroupDesc), DisambigParser.TermAction]()
    private var idTermActions = Map[(Int, CharacterTermGroupDesc), DisambigParser.TermAction]()
    private var edgeActions = Map[(AKernelSetPathSet, AKernelSetPathSet), DisambigParser.TermAction]()
    private var idEdgeActions = Map[(Int, Int), DisambigParser.EdgeAction]()

    private var nodeRelInferer = DisambigNodeRelInferer.emptyInferer

    private var newToppableNodes = Set[AKernelSetPathSet]()
    private var newFinishableEdges = Set[(AKernelSetPathSet, AKernelSetPathSet)]()

    private def addNode(pathSet: AKernelSetPathSet): Int = {
        val newId = nodes.size
        nodes += pathSet -> newId
        nodesById += newId -> pathSet
        newId
    }

    private def nodeIdOf(pathSet: AKernelSetPathSet): Int = nodes get pathSet match {
        case Some(existingNodeId) => existingNodeId
        case None => addNode(pathSet)
    }

    private def calculateTermActions(base: AKernelSetPathSet): Unit = {
        val terms = analyzer.acceptableTerms(AKernelSet(base.lasts flatMap (_.items)))
        terms foreach { term =>
            // base.lasts 중에 term을 받을 수 있는 것들만 추림.
            // term을 받을 수 있는 base.paths 각각에 대해 last에서 analyzer.termChanges를 해보고,
            // 그 결과를 바탕으로 액션을 만듦
            // heavyBranchNodes인 경우 단순히 nodeIdOf로 하지 말고 addNode 사용.

            // 이 때 만들어진 액션의 pendingFinish가 None이 아닌 경우,
            // finish simulation 해서 액션을 바꾸고 heavyBranchNodes를 추가해야할 수 있음
        }
    }

    private def calculateEdgeActions(edge: (AKernelSetPathSet, AKernelSetPathSet)): Unit = {
        // edge._1.lasts와 edge._2.heads가 연결되어 있다고 보고 edge action 계산
        // 이 때 만들어진 액션의 pendingFinish가 None이 아닌 경우,
        // finish simulation 해서 액션을 바꾸고 heavyBranchNodes를 추가해야할 수 있음
    }

    def generateParser(): DisambigParser = {
        val start = AKernelSetPathSet(Set(AKernelSetPath(List(AKernelSet(Set(AKernel(grammar.startSymbol, 0)))))))
        val startId = nodeIdOf(start)

        newToppableNodes += start
        while (newToppableNodes.nonEmpty || newFinishableEdges.nonEmpty) {
            val processingNodes = newToppableNodes
            val processingAdjs = newFinishableEdges

            newToppableNodes = Set()
            newFinishableEdges = Set()

            processingNodes foreach calculateTermActions
            processingAdjs foreach calculateEdgeActions
        }
        new DisambigParser(grammar, nodesById, ???, startId, idTermActions, idEdgeActions)
    }
}
