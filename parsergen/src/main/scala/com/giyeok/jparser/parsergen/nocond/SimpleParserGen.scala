package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.NGrammar

// AKernelSet 하나가 한 노드가 되는 parser 생성.
// SimpleGrammars.array0Grammar 같은 문법을 제대로 처리 못함.
class SimpleParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    private var nodes = Map[AKernelSet, Int]()
    private var nodesById = Map[Int, AKernelSet]()
    private var termActions = Map[(Int, CharacterTermGroupDesc), SimpleParser.TermAction]()
    private var edgeActions = Map[(Int, Int), SimpleParser.EdgeAction]()

    private var nodeRelInferer = SimpleNodeRelInferer.emptyInferer

    private def updateNodeRelInferer(result: (SimpleNodeRelInferer, NodeRels)): Unit = {
        val (newInferer, newRels) = result
        nodeRelInferer = newInferer
        newToppableNodes ++= (newRels.nodesOnTop map nodesById)
        newFinishableEdges ++= (newRels.finishableEdges map { e => nodesById(e._1) -> nodesById(e._2) })
    }

    private def addTermAction(baseNodeId: Int, termAction: SimpleParser.TermAction): Unit =
        updateNodeRelInferer(nodeRelInferer.addTermAction(baseNodeId, termAction))

    private def addEdgeAction(prev: Int, last: Int, edgeAction: SimpleParser.EdgeAction): Unit =
        updateNodeRelInferer(nodeRelInferer.addEdgeAction(prev, last, edgeAction))

    private var newToppableNodes = Set[AKernelSet]()
    private var newFinishableEdges = Set[(AKernelSet, AKernelSet)]()

    private def nodeIdOf(kernelSet: AKernelSet): Int = nodes get kernelSet match {
        case Some(existingNodeId) => existingNodeId
        case None =>
            val newId = nodes.size
            nodes += kernelSet -> newId
            nodesById += newId -> kernelSet
            newId
    }

    private def calculateTermActions(base: AKernelSet): Unit = {
        val baseId = nodeIdOf(base)
        val terms = analyzer.acceptableTerms(base)
        terms foreach { term =>
            val change = analyzer.termChanges(base, term)
            val replace = change.replacePrev
            val replaceId = nodeIdOf(replace)
            val termAction: SimpleParser.TermAction = change.following match {
                case None =>
                    // Finish
                    SimpleParser.Finish(replaceId)
                case Some(Following(following, pendingFinishReplace)) =>
                    val followingId = nodeIdOf(following)
                    val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        None
                    } else {
                        // Append w/ pendingFinish
                        val pendingFinishReplaceId = nodeIdOf(pendingFinishReplace)
                        Some(pendingFinishReplaceId)
                    }
                    SimpleParser.Append(replaceId, followingId, pfIdOpt)
            }
            addTermAction(baseId, termAction)
            termActions += (baseId, term) -> termAction
        }
    }

    private def calculateEdgeActions(edge: (AKernelSet, AKernelSet)): Unit = {
        val (start, next) = edge
        val startId = nodeIdOf(start)
        val change = analyzer.edgeChanges(start, next)
        val replace = change.replacePrev
        val replaceId = nodeIdOf(replace)
        val idEdgeAction = change.following match {
            case None =>
                SimpleParser.DropLast(replaceId)
            case Some(Following(following, pendingFinishReplace)) =>
                val followingId = nodeIdOf(following)
                val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                    // ReplaceEdge
                    None
                } else {
                    // ReplaceEdge w/ pendingFinish
                    val pendingFinishReplaceId = nodeIdOf(pendingFinishReplace)
                    Some(pendingFinishReplaceId)
                }
                SimpleParser.ReplaceEdge(replaceId, followingId, pfIdOpt)
        }
        val nextId = nodeIdOf(next)
        addEdgeAction(startId, nextId, idEdgeAction)
        edgeActions += (startId, nodeIdOf(edge._2)) -> idEdgeAction
    }

    def generateParser(): SimpleParser = {
        val start = AKernelSet(Set(AKernel(grammar.startSymbol, 0)))
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
        new SimpleParser(grammar, nodesById, nodeRelInferer, startId, termActions, edgeActions)
    }
}
