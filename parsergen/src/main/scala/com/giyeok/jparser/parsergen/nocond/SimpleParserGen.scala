package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.ExpressionGrammars
import com.giyeok.jparser.nparser.NGrammar

object SimpleParser {

    sealed trait TermAction

    case class Finish(replace: Option[Int]) extends TermAction

    case class Append(replace: Int, append: Int, pendingFinish: Option[Int]) extends TermAction

    sealed trait EdgeAction

}

// grammar, prevNodes는 디버깅을 위한 목적
class SimpleParser(val grammar: NGrammar,
                   val nodes: Map[Int, AKernelSet],
                   val prevNodes: Map[Int, Set[Int]],
                   val startNodeId: Int,
                   val termActions: Map[(Int, CharacterTermGroupDesc), SimpleParser.TermAction],
                   val edgeActions: Map[(Int, Int), SimpleParser.EdgeAction])

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
    private var prevNodes = Map[AKernelSet, Set[AKernelSet]]()

    private var newNodes = Set[AKernelSet]()
    private var newEdges = Set[(AKernelSet, AKernelSet)]()

    private def nodeIdOf(kernelSet: AKernelSet): Int = if (nodes contains kernelSet) nodes(kernelSet) else {
        newNodes += kernelSet
        val newId = nodes.size
        nodes += kernelSet -> newId
        newId
    }

    private def addEdge(start: AKernelSet, end: AKernelSet): Unit = {
        if (prevNodes contains end) {
            if (!(prevNodes(end) contains start)) {
                prevNodes += end -> (prevNodes(end) + start)
                newEdges += start -> end
            }
        } else {
            prevNodes += end -> Set(start)
            newEdges += start -> end
        }
    }

    private def copyPrev(original: AKernelSet, replaced: AKernelSet): Unit = {
        // original의 prev들을 replaced의 prev로 모두 복사
        prevNodes.getOrElse(original, Set()) foreach { prev =>
            addEdge(prev, replaced)
        }
    }

    private def calculateTermActions(start: AKernelSet): Unit = {
        val startId = nodeIdOf(start)
        val terms = analyzer.acceptableTerms(start)
        terms foreach { term =>
            val change = analyzer.termChanges(start, term)
            val replace = change.replacePrev
            copyPrev(start, replace)
            val replaceId = nodeIdOf(replace)
            val idTermAction: SimpleParser.TermAction = change.following match {
                case None =>
                    if (replaceId == startId) {
                        // Finish
                        SimpleParser.Finish(None)
                    } else {
                        // ReplaceAndFinish
                        SimpleParser.Finish(Some(replaceId))
                    }
                case Some(Following(following, pendingFinishReplace)) =>
                    val pfIdOpt = if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        None
                    } else {
                        // Append w/ pendingFinish
                        copyPrev(replace, pendingFinishReplace)
                        Some(nodeIdOf(pendingFinishReplace))
                    }
                    addEdge(replace, following)
                    SimpleParser.Append(replaceId, nodeIdOf(following), pfIdOpt)
            }
            termActions += (start, term) -> change
            idTermActions += (startId, term) -> idTermAction
        }
    }

    private def calculateEdgeActions(edge: (AKernelSet, AKernelSet)): Unit = {
        // TODO
    }

    def generateParser(): SimpleParser = {
        val startId = nodeIdOf(AKernelSet(Set(AKernel(grammar.startSymbol, 0))))

        while (newNodes.nonEmpty || newEdges.nonEmpty) {
            val processingNodes = newNodes
            newNodes = Set()
            processingNodes foreach calculateTermActions

            val processingEdges = newEdges
            newEdges = Set()
            processingEdges foreach calculateEdgeActions
        }
        val idPrevNodes = prevNodes map { kv => nodeIdOf(kv._1) -> (kv._2 map nodeIdOf) }
        new SimpleParser(grammar, nodes map { p => p._2 -> p._1 }, idPrevNodes, startId, idTermActions, idEdgeActions)
    }
}

object SimpleParserGen {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(ExpressionGrammars.simple)

        (grammar.nsymbols ++ grammar.nsequences).toList.sortBy(_._1) foreach { s =>
            println(s"${s._1} -> ${s._2.symbol.toShortString}")
        }

        val parser = new SimpleParserGen(grammar).generateParser()
        parser.nodes.toList.sortBy(_._1).foreach { kv =>
            println(s"${kv._1} -> ${kv._2.items map (_.toReadableString(grammar)) mkString " | "}")
        }
        parser.termActions.toList.sortBy(_._1._1).foreach { act =>
            println(s"${act._1._1}, ${act._1._2.toShortString} -> ${act._2}")
        }
        parser.prevNodes.toList.sortBy(_._1).foreach { kv =>
            println(s"${kv._1} -> ${kv._2}")
        }
        println()
    }
}
