package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar

// AKernelSet 하나가 한 노드가 되는 parser 생성.
// SimpleGrammars.array0Grammar 같은 문법을 제대로 처리 못함.
class SimpleParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    private var nodes = Map[AKernelSet, Int]()
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
        newAdjs ++= addedAdjs
    }

    private def addAppendRel(prev: AKernelSet, next: AKernelSet): Unit = addNodeRel(AppendRel(prev, next))

    private def addReplaceRel(prev: AKernelSet, next: AKernelSet): Unit = addNodeRel(ReplaceRel(prev, next))

    private def nodeIdOf(kernelSet: AKernelSet): Int = if (nodes contains kernelSet) nodes(kernelSet) else {
        newNodes += kernelSet
        val newId = nodes.size
        nodes += kernelSet -> newId
        nodeRels = nodeRels.addNode(kernelSet)
        newId
    }

    private def calculateTermActions(start: AKernelSet): Unit = {
        val startId = nodeIdOf(start)
        val terms = analyzer.acceptableTerms(start)
        terms foreach { term =>
            val change = analyzer.termChanges(start, term)
            val replace = change.replacePrev
            val replaceId = nodeIdOf(replace)
            if (start != replace) addReplaceRel(start, replace)
            val idTermAction: SimpleParser.TermAction = change.following match {
                case None =>
                    // Finish
                    SimpleParser.Finish(replaceId)
                case Some(Following(following, pendingFinishReplace)) =>
                    val followingId = nodeIdOf(following)
                    addAppendRel(replace, following)
                    val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        None
                    } else {
                        // Append w/ pendingFinish
                        val pendingFinishReplaceId = nodeIdOf(pendingFinishReplace)
                        if (replace != pendingFinishReplace) {
                            addReplaceRel(replace, pendingFinishReplace)
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
        if (start != replace) addReplaceRel(start, replace)
        val idEdgeAction = change.following match {
            case None => SimpleParser.DropLast(replaceId)
            case Some(Following(following, pendingFinishReplace)) =>
                val followingId = nodeIdOf(following)
                addAppendRel(replace, following)
                val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                    // ReplaceEdge
                    None
                } else {
                    // ReplaceEdge w/ pendingFinish
                    if (replace != pendingFinishReplace) {
                        addReplaceRel(replace, pendingFinishReplace)
                    }
                    Some(nodeIdOf(pendingFinishReplace))
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
        val idNodeRels = nodeRels.toIdNodeRelGraph(nodeIdOf)
        new SimpleParser(grammar, nodes map { p => p._2 -> p._1 }, idNodeRels, startId, idTermActions, idEdgeActions)
    }
}

object SimpleParserGen {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(SimpleGrammars.array0Grammar)
        grammar.describe()

        val parser = new SimpleParserGen(grammar).generateParser()
        parser.describe()
        println()
    }
}
