package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.{ExpressionGrammars, SimpleGrammars}
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

    private def updateTermFinSimResult(finSimReqId: Int, action: DisambigParser.TermAction): Unit =
        updateNodeRelInferer(nodeRelInferer.updateTermFinSimResult(finSimReqId, action))

    private def addEdgeActionReplace(edge: (Int, Int), replaceAction: DisambigParser.ReplaceEdge): Unit =
        updateNodeRelInferer(nodeRelInferer.addEdgeActionReplace(edge._1, edge._2, replaceAction))

    private def effectivePathsFrom(paths: Seq[AKernelSetPath], changeReplacePrev: Set[AKernel]): Seq[AKernelSetPath] =
        paths flatMap { path =>
            val intersect = path.path.last.items intersect changeReplacePrev
            if (intersect.isEmpty) None else Some(path.replaceLast(AKernelSet(intersect)))
        }

    private def replaceLastOf(paths: Seq[AKernelSetPath], replaceKernels: Set[AKernel]): Seq[AKernelSetPath] = {
        // paths의 last들을 replaceKernels와의 intersect들로 변경한 path seq를 반환
        paths map { path =>
            val newLast = AKernelSet(path.path.last.items intersect replaceKernels)
            assert(newLast.items.nonEmpty)
            AKernelSetPath(path.path.init :+ newLast)
        }
    }

    private def replaceNodeFrom(base: AKernelSetPathSet, change: GraphChange): AKernelSetPathSet = {
        // base 노드의 path들 중 유효한 것들만 남기고, 각 path의 last를 change.replacePrev에 맞춰서 변경한 노드로 replace해야 함
        val effectivePaths = effectivePathsFrom(base.paths, change.replacePrev.items)
        AKernelSetPathSet(replaceLastOf(effectivePaths, change.replacePrev.items))
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
                        val replace = replaceNodeFrom(base, change)
                        val replaceId = nodeIdOf(replace)
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
                    val replace = replaceNodeFrom(start, change)
                    val replaceId = nodeIdOf(replace)
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

    // cycle=true이면 extended.head가 extended.tail의 어딘가에서 반복된다는 의미
    case class SimulatedPath(extended: List[Int], cycle: Boolean, path: AKernelSetPath)

    // nodeRelInferer.prevOf 를 포함해서, 실제로 path(의 last)에서 change가 일어난 경우 발생 가능한 "모든" finish 경우의 수를 반환
    // 반환의 List[Int]는 사용된 node id들. 만약 path 안에서 다 해결 되면 빈 리스트가 반환됨.
    private def simulateFinish(extended: List[Int], path: AKernelSetPath, change: GraphChange): Seq[SimulatedPath] = {
        val nodeId = extended.head
        assert(nodes.byKey(nodeId).paths contains path)

        val (paths, moreFins) = simulateFinishLocally(path, change)
        moreFins match {
            case None => paths map { p => SimulatedPath(extended, cycle = false, p) }
            case Some(lastKernel) =>
                val pathsReform = paths map (SimulatedPath(extended, cycle = false, _))
                val prevIds = nodeRelInferer.prevOf(nodeId)
                // TODO FIX prevIds중에 지금까지 거쳐온 노드가 다시 나오면 싸이클이 생겨서 무한루프에 빠짐.
                // -> 그런 경우엔 싸이클이 생겼다는 것만 저장하고 더이상 prev로 extend하지 말아야 함.
                // -> 밖에서 처리할 때는 aceeptableTerm이 겹치는 것들 중에 싸이클이 있는 경우가 있으면 파서 생성 실패..?
                //   -> 그런데 이런 경우에도, ExpressionGrammars.simple 에서 실행했을떄 생기는 싸이클처럼,
                //      extended List(1)과 List(1, 1)의 acceptableTerm이 겹친다면? List(1, 1)은 그냥 무시해도 될 것 같은데?
                // TODO path에 만들어지는 AKernelSet의 싸이클도 찾아야할듯?
                val pathsMoreFinished = prevIds.toSeq flatMap { prevId =>
                    nodes.byKey(prevId).paths flatMap { path =>
                        val newChange = analyzer.edgeChanges(path.path.last, lastKernel)
                        if (extended contains prevId) {
                            val (paths2, moreFins2) = simulateFinishLocally(path, newChange)
                            val isCycle = moreFins2.isDefined
                            // finish이거나 pendingFinish인 경우
                            paths2 map { p2 => SimulatedPath(prevId +: extended, cycle = isCycle, p2) }
                        } else {
                            simulateFinish(prevId +: extended, path, newChange)
                        }
                    }
                }
                pathsReform ++ pathsMoreFinished
        }
    }

    // 주어진 path(의 last)에서 change가 일어난 경우 만들어질 수 있는 term accept가 가능한 path를 반환.
    // 반환 튜플의 _2는, finish를 계속 진행하다가 마지막 AKernelSet 하나만 남았는데 더 finish를 진행해야 하는 경우 그 AKernelSet 반환.
    private def simulateFinishLocally(path: AKernelSetPath, change: GraphChange): (Seq[AKernelSetPath], Option[AKernelSet]) = {
        val last = path.path.last
        if ((change.replacePrev.items intersect last.items).isEmpty) {
            // change에 기여하지 않은 path라면
            (Seq(), None)
        } else if (path.path.size == 1) {
            change.following match {
                case None => (Seq(), Some(last))
                case Some(Following(following, pendingFinishReplace)) =>
                    val appended = path.path.init :+ change.replacePrev :+ following
                    if (pendingFinishReplace.items.isEmpty) {
                        (Seq(AKernelSetPath(appended)), None)
                    } else {
                        (Seq(AKernelSetPath(appended)), Some(pendingFinishReplace))
                    }
            }
        } else {
            change.following match {
                case None =>
                    // Finish
                    simulateFinishLocally(AKernelSetPath(path.path.init :+ change.replacePrev),
                        analyzer.edgeChanges(path.path.init.last, change.replacePrev))
                case Some(Following(following, pendingFinishReplace)) =>
                    val appended = path.path.init :+ change.replacePrev :+ following
                    if (pendingFinishReplace.items.isEmpty) {
                        // Append
                        (Seq(AKernelSetPath(appended)), None)
                    } else {
                        // Append w/ pendingFinish
                        val pendingFinPath = AKernelSetPath(path.path.init)
                        val pendingFinChange = analyzer.edgeChanges(path.path.init.last, pendingFinishReplace)
                        val finished = simulateFinishLocally(pendingFinPath, pendingFinChange)
                        (AKernelSetPath(appended) +: finished._1, finished._2)
                    }
            }
        }
    }

    private def acceptableTermConflictingPaths(paths: Seq[SimulatedPath]): Map[CharacterTermGroupDesc, Seq[SimulatedPath]] = {
        // TODO
        Map()
    }

    private def calculateTermFinishable(finSimReqId: Int): Unit = {
        val finSimReq = termFinishSimReqs(finSimReqId)
        val base = nodes.byKey(finSimReq.baseId)

        // 1. base에서 finSimReq의 change 더이상 pendingFinish가 없을 때까지 extend하면서 모든 가능한 path를 계산
        val allPaths = base.paths flatMap { p =>
            simulateFinish(List(finSimReq.baseId), p, finSimReq.change)
        }

        if (allPaths exists (_.cycle)) {
            throw new Exception(s"Cycle found during analyzing termFinishables: $allPaths")
        }

        println(s"base: ${finSimReq.baseId}")
        base.paths foreach { p =>
            println(s"base: $p")
        }
        allPaths foreach { p =>
            println(p.extended)
            println(p.cycle)
            println(p.path)
            println(CharacterTermGroupDesc.merge(analyzer.acceptableTerms(p.path.path.last)).toShortString)
        }

        // allPaths의 각 path에 대해 analyzer.acceptableTerms(path.path.last) 한 것 중에 겹치는게 있으면 노드를 합치는 등의 작업이 필요
        val termConflictingPaths = acceptableTermConflictingPaths(allPaths)
        if (termConflictingPaths.isEmpty) {
            // 겹치는 term이 없으면
            //   -> finSimReq.change를 보고 Finish나 Append w/ pendingFinish를 만들어줌
            //   - finish는 각 path의 last를 replace한(뭘로 replace할 지는 좀 더 봐야함) 새 노드로 replace&finish 하면 됨
            val finSimAction = finSimReq.change.following match {
                case None =>
                    val replace = replaceNodeFrom(base, finSimReq.change)
                    val replaceId = nodeIdOf(replace)
                    DisambigParser.Finish(replaceId)
                case Some(Following(following, pendingFinishReplace)) =>
                    assert(pendingFinishReplace.items.nonEmpty)
                    // base 노드의 각 path의 last를 pendingFinishReplace로 바꾼 노드를 pendingFinish를 갖는 append
                    val replace = replaceNodeFrom(base, finSimReq.change)
                    val replaceId = nodeIdOf(replace)
                    val followingNode = AKernelSetPathSet(Seq(AKernelSetPath(List(following))))
                    val followingId = nodeIdOf(followingNode)
                    val pendingFinishNode = AKernelSetPathSet(replaceLastOf(base.paths, pendingFinishReplace.items))
                    val pendingFinishId = nodeIdOf(pendingFinishNode)
                    DisambigParser.Append(replaceId, List(followingId), Some(pendingFinishId))
            }
            // 만들어진 action을 nodeRelInferer와 termFinishSimResults에 추가
            updateTermFinSimResult(finSimReqId, finSimAction)
            termFinishSimResults += finSimReqId -> finSimAction
        } else {
            // 겹치는 term이 있으면 PopAndReplace 를 만듦
            ???
        }
        println()
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
        val grammar1 = ExpressionGrammars.simple
        val grammar2 = SimpleGrammars.array0Grammar

        val ngrammar = NGrammar.fromGrammar(grammar2)
        ngrammar.describe()

        val parser = new DisambigParserGen(ngrammar).generateParser()
        parser.describe()
    }
}
