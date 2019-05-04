package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

// AKernelSet 하나가 한 노드가 되는 parser 생성.
// SimpleGrammars.array0Grammar 같은 문법을 제대로 처리 못함.
class SimpleParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    private var nodes = Map[AKernelSet, Int]()
    private var nodesById = Map[Int, AKernelSet]()
    private var termActions = Map[(AKernelSet, CharacterTermGroupDesc), GraphChange]()
    private var idTermActions = Map[(Int, CharacterTermGroupDesc), SimpleParser.TermAction]()
    private var edgeActions = Map[(AKernelSet, AKernelSet), GraphChange]()
    private var idEdgeActions = Map[(Int, Int), SimpleParser.EdgeAction]()

    // key 앞에 올 수 있는 AKernelSet의 집합
    private var nodeRels = NodeRelGraph.emptyGraph

    private var newNodes = Set[AKernelSet]()
    private var newAdjs = Set[(AKernelSet, AKernelSet)]()

    private def addNodeRel(nodeRel: NodeRelEdge): Unit = {
        val (newNodeRels, addedAdjs) = nodeRels.newAdjacentsByNewRel(nodeRel)
        nodeRels = newNodeRels
        newAdjs ++= (addedAdjs map { p => nodesById(p._1) -> nodesById(p._2) })
    }

    private def addAppendRel(prev: Int, next: Int): Unit = addNodeRel(AppendRel(prev, next))

    private def addReplaceRel(prev: Int, next: Int): Unit = addNodeRel(ReplaceRel(prev, next))

    private def nodeIdOf(kernelSet: AKernelSet): Int = if (nodes contains kernelSet) nodes(kernelSet) else {
        newNodes += kernelSet
        val newId = nodes.size
        nodes += kernelSet -> newId
        nodesById += newId -> kernelSet
        nodeRels = nodeRels.addNode(newId)
        newId
    }

    private def calculateTermActions(start: AKernelSet): Unit = {
        val startId = nodeIdOf(start)
        val terms = analyzer.acceptableTerms(start)
        terms foreach { term =>
            val change = analyzer.termChanges(start, term)
            val replace = change.replacePrev
            val replaceId = nodeIdOf(replace)
            if (start != replace) addReplaceRel(startId, replaceId)
            val idTermAction: SimpleParser.TermAction = change.following match {
                case None =>
                    // Finish
                    SimpleParser.Finish(replaceId)
                case Some(Following(following, pendingFinishReplace)) =>
                    val followingId = nodeIdOf(following)
                    addAppendRel(replaceId, followingId)
                    val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        None
                    } else {
                        // Append w/ pendingFinish
                        val pendingFinishReplaceId = nodeIdOf(pendingFinishReplace)
                        if (replace != pendingFinishReplace) {
                            addReplaceRel(replaceId, pendingFinishReplaceId)
                        }
                        Some(pendingFinishReplaceId)
                    }
                    SimpleParser.Append(replaceId, followingId, pfIdOpt)
            }
            termActions += (start, term) -> change
            idTermActions += (startId, term) -> idTermAction
        }
    }

    private def calculateEdgeActions(edge: (AKernelSet, AKernelSet)): Unit = {
        val (start, next) = edge
        val startId = nodeIdOf(start)
        val change = analyzer.edgeChanges(start, next)
        val replace = change.replacePrev
        val replaceId = nodeIdOf(replace)
        if (start != replace) addReplaceRel(startId, replaceId)
        val idEdgeAction = change.following match {
            case None => SimpleParser.DropLast(replaceId)
            case Some(Following(following, pendingFinishReplace)) =>
                val followingId = nodeIdOf(following)
                addAppendRel(replaceId, followingId)
                val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                    // ReplaceEdge
                    None
                } else {
                    // ReplaceEdge w/ pendingFinish
                    val pendingFinishReplaceId = nodeIdOf(pendingFinishReplace)
                    if (replace != pendingFinishReplace) {
                        addReplaceRel(replaceId, pendingFinishReplaceId)
                    }
                    Some(pendingFinishReplaceId)
                }
                SimpleParser.ReplaceEdge(replaceId, followingId, pfIdOpt)
        }
        edgeActions += edge -> change
        idEdgeActions += (startId, nodeIdOf(edge._2)) -> idEdgeAction
    }

    def generateParser(): SimpleParser = {
        val startId = nodeIdOf(AKernelSet(Set(AKernel(grammar.startSymbol, 0))))

        while (newNodes.nonEmpty || newAdjs.nonEmpty) {
            val processingNodes = newNodes
            val processingAdjs = newAdjs

            newNodes = Set()
            newAdjs = Set()

            processingNodes foreach calculateTermActions
            processingAdjs foreach calculateEdgeActions
        }
        new SimpleParser(grammar, nodes map { p => p._2 -> p._1 }, nodeRels, startId, idTermActions, idEdgeActions)
    }
}
