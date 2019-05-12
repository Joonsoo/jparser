package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.parsergen.utils.BiDirectionalMap

class DisambigParserGen(val grammar: NGrammar) {
    val analyzer = new GrammarAnalyzer(grammar)

    // DisambigParserGen에서는 SimpleParserGen과 달리 `nodes`와 `nodesById`가 항상 같은 내용을 갖지는 않음.
    // 같은 pathSet을 갖는 두 개 이상의 노드ID가 있을 수 있기 때문
    private var nodes = BiDirectionalMap[Int, AKernelSetPathSet]()

    // termAction에서 append되는 노드를 같은 AKernelSetPathSet의 노드가 있어도 새 id를 부여하는 노드
    private var heavyBranchNodes = Set[AKernelSetPathSet]()
    // termGraphChanges, edgeGraphChanges는 parse tree reconstruction을 위해서 필요할 것 같아서 마련해둠.
    private var termGraphChanges = Map[(AKernelSet, CharacterTermGroupDesc), GraphChange]()
    private var edgeGraphChanges = Map[(AKernelSet, AKernelSet), GraphChange]()

    sealed trait GenTermAction

    case class FixedTermAction(termAction: DisambigParser.TermAction) extends GenTermAction

    case class FinishableTermAction(finSimReqId: Int) extends GenTermAction

    sealed trait GenEdgeAction

    case class FixedEdgeAction(edgeAction: DisambigParser.EdgeAction) extends GenEdgeAction

    case class FinishableEdgeAction(finSimReqId: Int) extends GenEdgeAction

    private var termActions = Map[(Int, CharacterTermGroupDesc), GenTermAction]()
    private var edgeActions = Map[(Int, Int), GenEdgeAction]()

    case class FinishSimulationReq(baseId: Int, newPathsAndPendingFins: Seq[(AKernelSetPath, Option[AKernelSet])], followings: Set[AKernel])

    private var finishSimulationReqs = Map[Int, FinishSimulationReq]()

    private var nodeRelInferer = DisambigNodeRelInferer.emptyInferer

    private var newToppableNodes = Set[Int]()
    private var newFinishableEdges = Set[(Int, Int)]()

    private def addNode(pathSet: AKernelSetPathSet): Int = {
        val newId = nodes.size
        nodes += newId -> pathSet
        newId
    }

    private def nodeIdOf(pathSet: AKernelSetPathSet): Int =
        nodes.byVal.getOrElse(pathSet, addNode(pathSet))

    private def createFinSimReq(baseId: Int, newPathsAndPendingFins: Seq[(AKernelSetPath, Option[AKernelSet])], followings: Set[AKernel]): Int = {
        val newReqId = finishSimulationReqs.size
        finishSimulationReqs += newReqId -> FinishSimulationReq(baseId, newPathsAndPendingFins, followings)
        newReqId
    }

    private def calculateTermActions(baseId: Int): Unit = {
        // TODO heavy=true이면 생성되는 TermAction에 등장하는 append 노드를 항상 새로 만든다(nodeIdOf 대신 addNode 사용)
        val base = nodes.byKey(baseId)
        val isHeavy = heavyBranchNodes contains base
        val terms = analyzer.acceptableTerms(AKernelSet((base.lasts flatMap (_.items)).toSet))
        terms foreach { term =>
            // base.lasts 중에 term을 받을 수 있는 것들만 추림.
            val effectivePaths = base.paths filter { path =>
                analyzer.termSymbolsFrom(path.path.last) exists (_.symbol.acceptTermGroup(term))
            }
            val (newPathsAndPendingFins: Seq[(AKernelSetPath, Option[AKernelSet])], followings: Set[AKernel]) =
                effectivePaths.foldLeft((Seq[(AKernelSetPath, Option[AKernelSet])](), Set[AKernel]())) { (m, path) =>
                    val change = analyzer.termChanges(path.path.last, term)
                    val newPath = path.replaceLast(change.replacePrev)
                    val pair: (AKernelSetPath, Option[AKernelSet]) =
                        (newPath, change.following map (_.pendingFinishReplace))
                    val following = (change.following map (_.following.items)).getOrElse(Set())
                    (m._1 :+ pair, following)
                }
            val replace = AKernelSetPathSet(newPathsAndPendingFins map (_._1))
            val replaceId = nodeIdOf(replace)
            val genTermAction: GenTermAction = if (followings.isEmpty) {
                // Finish
                FinishableTermAction(createFinSimReq(baseId, newPathsAndPendingFins, followings))
            } else {
                // pendingFinish가 필요한 path가 있으면 pending
                if (newPathsAndPendingFins exists (_._2.isDefined)) {
                    FinishableTermAction(createFinSimReq(baseId, newPathsAndPendingFins, followings))
                } else {
                    val followingNode = AKernelSetPathSet(Seq(AKernelSetPath(List(AKernelSet(followings)))))
                    val followingId = nodeIdOf(followingNode)
                    FixedTermAction(DisambigParser.Append(replaceId, followingId, None))
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
        ???
    }

    def generateParser(): DisambigParser = {
        val start = AKernelSetPathSet(Seq(AKernelSetPath(List(AKernelSet(Set(AKernel(grammar.startSymbol, 0)))))))
        val startId = nodeIdOf(start)

        newToppableNodes += startId

        // Returns true if anything has been changed, false otherwise
        def kernelSetActions(): Boolean = {
            if (newToppableNodes.isEmpty || newFinishableEdges.isEmpty) false else {
                while (newToppableNodes.nonEmpty || newFinishableEdges.nonEmpty) {
                    val processingNodes = newToppableNodes
                    val processingAdjs = newFinishableEdges

                    newToppableNodes = Set()
                    newFinishableEdges = Set()

                    processingNodes foreach calculateTermActions
                    processingAdjs foreach calculateEdgeActions
                }
                true
            }
        }

        // Returns true if anything has been changed, false otherwise
        def finishSimulationReqs(): Boolean = {
            ???
        }

        var converged: Boolean = false
        while (!converged) {
            if (kernelSetActions()) {
                converged = false
                finishSimulationReqs()
            } else {
                converged = !finishSimulationReqs()
            }
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
