package com.giyeok.jparser

import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.Inputs.Input
import ParsingGraph._

case class CtxGraph[R <: ParseResult](
        nodes: Set[Node],
        edges: Set[Edge],
        results: Map[Node, Map[Set[Trigger], R]],
        progresses: Map[SequenceNode, Map[Set[Trigger], R]]) extends ParsingGraph[R] with TerminalInfo[R] {

    // results를 이용해서 revertTrigger 조건을 비교해서 지워야 할 edge/results를 제거한 그래프를 반환한다
    def revert: CtxGraph[R] = {
        ???
    }

    // baseNode에서 도달 가능한 node와 edge로만 구성된 subgraph를 반환
    def reachableFrom(baseNode: Node): CtxGraph[R] = {
        ???
    }

    def updateResultOf(node: Node, triggers: Set[Trigger], newResult: R): CtxGraph[R] = ???
    def updateProgressOf(node: SequenceNode, triggers: Set[Trigger], newProgress: R): CtxGraph[R] = ???
    def withNodeEdgesProgresses(newNode: SequenceNode, newEdges: Set[Edge], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): CtxGraph[R] = ???
    def withNodesEdgesResultsProgresses(newNodes: Set[Node], newEdges: Set[Edge], newResults: Map[Node, Map[Set[Trigger], R]], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): CtxGraph[R] = ???
}

class NewParser[R <: ParseResult](val grammar: Grammar, val resultFunc: ParseResultFunc[R]) extends LiftTasks[R, CtxGraph[R]] {
    val derivationFunc = new DerivationFunc(grammar, resultFunc)

    private val derivationGraphCache = scala.collection.mutable.Map[Nonterm, DGraph[R]]()
    def deriveAtomic(base: AtomicNonterm): DGraph[R] = {
        derivationGraphCache get base match {
            case Some(graph) => graph
            case None =>
                val graph = derivationFunc.deriveAtomic(base)
                derivationGraphCache(base) = graph
                graph
        }
    }
    def deriveSequence(base: Sequence, pointer: Int): DGraph[R] = {
        derivationGraphCache get base match {
            case Some(graph) => graph
            case None =>
                val graph = derivationFunc.deriveSequence(base, pointer)
                derivationGraphCache(base) = graph
                graph
        }
    }
    def derive(node: NontermNode): DGraph[R] = node match {
        case AtomicNode(symbol, _) => deriveAtomic(symbol)
        case SequenceNode(symbol, pointer, _, _) => deriveSequence(symbol, pointer)
        case _: DGraph.BaseNode => ??? // BaseNode일 수 없음
    }

    /*
    case class Graph(nodes: Set[Node], edges: Set[Edge], progresses: Map[Node, Map[Set[Trigger], R]]) {
        assert(allNodesAppearedInEdges subsetOf nodes)

        // lifts에 의해 trigger되는 node/edge를 제거한 그래프와 temporarily lift block되는 노드들의 집합을 반환
        def revert(liftedGraph: Graph, lifts: Set[Lift]): (Graph, Set[AtomicNode[_]]) = {
            val liftedNodes = lifts map { _.before }
            // assert(liftedNodes subsetOf nodes)

            val tempLiftBlockNodes: Set[AtomicNode[_]] = nodes collect {
                case node @ AtomicNode(_, _, Some(liftBlockTrigger), _) if liftedNodes contains liftBlockTrigger => node
            }
            // DeadUntilLift/WaitUntilLift reverter는 만약 대상 노드가 survive하지 못했으면(alive와 반대 조건) 엣지가 revert되고, 만약 대상 노드가 lift되었으면 엣지에서 그 reverter만 제거된다
            // DeadUntilLift와 WaitUntilLift의 차이점은 WaitUntilLift는 revertTrigger에 WaitUntilLift reverter가 섞여있는 lift는 최종 결과로 인정되지 않는다는 점이다
            // DeadUntilLift는 그런 제약이 없음 - 현재는 쓰이지도 않고 있는데 필요한 부분이 있을지도 몰라서 일단 만들어 놓음
            val survivedEdges: Set[Edge] = edges flatMap {
                case edge @ SimpleEdge(start, end, revertTriggers) =>
                    val triggered = revertTriggers exists {
                        case NodeTrigger(node, Trigger.Type.Lift) => liftedNodes contains node
                        case NodeTrigger(node, Trigger.Type.Alive) => liftedGraph.nodes contains node
                        case NodeTrigger(node, Trigger.Type.DeadUntilLift | Trigger.Type.WaitUntilLift) =>
                            !(liftedNodes contains node) && !(liftedGraph.nodes contains node)
                        case pended: PendedNodeTrigger =>
                            // TODO 이 시점에 Pended가 있는건 어떤건지 고민
                            false
                    }
                    if (!triggered) {
                        val newRevertTriggers = revertTriggers filterNot {
                            case NodeTrigger(node, Trigger.Type.DeadUntilLift | Trigger.Type.WaitUntilLift) =>
                                liftedNodes contains node
                            case trigger => false
                        }
                        Some(SimpleEdge(start, end, newRevertTriggers))
                    } else None
                case edge: JoinEdge => Some(edge)
            }
            (Graph(nodes, survivedEdges), tempLiftBlockNodes)
        }

        // startNode에서 도달 가능한 node와 edge로만 구성된 subgraph를 반환
        def reachableFrom(baseNode: Node): Graph = {
            def traverse(queue: List[Node], cc: Graph): Graph =
                queue match {
                    case node +: rest =>
                        val outgoingEdges = outgoingEdgesFrom(node)
                        val liftBlockTrigger = node match {
                            case AtomicNode(_, _, Some(liftBlockTrigger), _) => Set(liftBlockTrigger)
                            case _ => Set()
                        }
                        val newNodes = liftBlockTrigger ++ (outgoingEdges flatMap {
                            case SimpleEdge(_, end, revertTriggers) =>
                                Set(end) ++ (revertTriggers collect { case NodeTrigger(node, _) => node })
                            case JoinEdge(_, end, join) =>
                                Set(end, join)
                        })
                        traverse(rest ++ (newNodes -- cc.nodes).toList, Graph(cc.nodes ++ newNodes, cc.edges ++ outgoingEdges))
                    case List() => cc
                }
            traverse(List(baseNode), Graph(Set(baseNode), Set()))
        }

        // nodes로 reachable한 노드들로만 구성된 subgraph를 반환한다
        // - 이 때 reachability는 node에 붙은 liftBlockTrigger나 simple edge에 붙은 edgeRevertTriggers와는 무관하게 계산되고
        // - reachability 계산이 끝난 뒤에 subgraph를 만들 때 subgraph에 포함되지 못한 node가 trigger가 되는 경우 해당 trigger들은 제외된다
        def reachableTo(nodes: Set[Node]): Graph = {
            object Reachability extends Enumeration {
                val True, False, Unknown = Value
            }
            val cache = scala.collection.mutable.Map[Node, Boolean]()
            def reachable(node: Node): Boolean = {
                def _reachable(node: Node, path: Seq[Node]): Reachability.Value =
                    cache get node match {
                        case Some(v) => if (v) Reachability.True else Reachability.False
                        case None =>
                            if (nodes contains node) {
                                cache(node) = true
                                Reachability.True
                            } else if (path contains node) {
                                Reachability.Unknown
                            } else {
                                val outgoingEdges = outgoingEdgesFrom(node)
                                val unknown = outgoingEdges forall {
                                    case SimpleEdge(_, end, _) => (_reachable(end, node +: path) == Reachability.Unknown)
                                    case JoinEdge(_, end, join) => (_reachable(end, node +: path) == Reachability.Unknown) || (_reachable(join, node +: path) == Reachability.Unknown)
                                }
                                if (unknown) Reachability.Unknown else {
                                    val r = outgoingEdges exists {
                                        case SimpleEdge(_, end, _) => (_reachable(end, node +: path) == Reachability.True)
                                        case JoinEdge(_, end, join) => (_reachable(end, node +: path) == Reachability.True) && (_reachable(join, node +: path) == Reachability.True)
                                    }
                                    cache(node) = r
                                    if (r) Reachability.True else Reachability.False
                                }
                            }
                    }
                _reachable(node, Seq()) == Reachability.True
            }
            val reachableNodes: Map[Node, Node] = (this.nodes collect {
                case node @ AtomicNode(kernel, beginGen, liftBlockTrigger, reservedReverter) if reachable(node) =>
                    node -> AtomicNode(kernel, beginGen, liftBlockTrigger filter { reachable _ }, reservedReverter)
                case node if reachable(node) =>
                    node -> node
            }).toMap
            val reachableEdges: Set[Edge] = this.edges collect {
                case SimpleEdge(start, end, revertTriggers) if reachable(start) && reachable(end) =>
                    val newRevertTriggers: Set[Trigger] = revertTriggers collect {
                        case NodeTrigger(node, ttype) if reachable(node) => NodeTrigger(reachableNodes(node), ttype)
                        case pended @ PendedNodeTrigger(node, ttype) => pended
                    }
                    SimpleEdge(reachableNodes(start).asInstanceOf[NontermNode[Nonterm]], reachableNodes(end), newRevertTriggers)
                case JoinEdge(start, end, join) if reachable(start) && reachable(end) && reachable(join) =>
                    JoinEdge(reachableNodes(start).asInstanceOf[AtomicNode[Join]], reachableNodes(end), reachableNodes(join))
            }
            Graph(reachableNodes.values.toSet, reachableEdges)
        }

        def incomingEdgesTo(node: Node): Set[Edge] = edges collect {
            case e @ SimpleEdge(_, `node`, _) => e
            case e @ JoinEdge(_, `node`, _) => e
            case e @ JoinEdge(_, _, `node`) => e
        }
        def outgoingEdgesFrom(node: Node): Set[Edge] = edges filter {
            case SimpleEdge(start, _, _) => start == node
            case JoinEdge(start, _, _) => start == node
        }
        def withNodeAndEdges(node: Node, edges: Set[Edge]): Graph = Graph(this.nodes + node, this.edges ++ edges)
    }
*/
    trait ProceedDetail {
        val expandedGraph: Graph
        val eligibleTermNodes0: Set[TermNode]
        val liftedGraph0: Graph
        val nextDerivables0: Set[NontermNode]
        val nextContext: ParsingCtx
    }
    /*
    case class FinishedProceedDetail(
        expandedGraph: Graph,
        eligibleTermNodes0: Set[TermNode],
        liftedGraph0: Graph,
        nextDerivables0: Set[NontermNode[Nonterm]],
        lifts0: Set[Lift],
        nextContext: ParsingContext)
            extends ProceedDetail {
        def expandStage = expandedGraph
        def preLiftStage = (eligibleTermNodes0, liftedGraph0, lifts0)
    }
    case class UnfinishedProceedDetail(
        expandedGraph: Graph,
        eligibleTermNodes0: Set[TermNode],
        liftedGraph0: Graph,
        nextDerivables0: Set[NontermNode[Nonterm]],
        lifts0: Set[Lift],
        revertedGraph: Graph,
        tempLiftBlockNodes: Set[AtomicNode[_]],
        trimmedRevertedGraph: Graph,
        eligibleTermNodes: Set[TermNode],
        liftedGraph: Graph,
        nextDerivables: Set[NontermNode[Nonterm]],
        lifts: Set[Lift],
        nextContext: ParsingContext)
            extends ProceedDetail {
        def expandStage = expandedGraph
        def preLiftStage = (eligibleTermNodes0, liftedGraph0, lifts0)
        def revertStage = (revertedGraph, tempLiftBlockNodes, trimmedRevertedGraph)
        def finalLiftStage = (eligibleTermNodes, liftedGraph, nextDerivables, lifts)
    }

    case class ParsingContext(gen: Int, startNode: NontermNode[Start.type], graph: Graph, derivables: Set[NontermNode[Nonterm]], results: Option[R]) {
        assert(derivables.toSet[Node] subsetOf graph.nodes)
        def proceedDetail(input: ConcreteInput): Either[ProceedDetail, ParsingError] = {
            val baseNodeAndSubgraphs = derivables map { derivable =>
                (derivable, derive(derivable.kernel).subgraphTo(input))
            } collect {
                case (derivable, Some(subgraph)) => (derivable, subgraph)
            }
            val (expandedGraph, eligibleTermNodes0) = graph.expandMulti(gen, baseNodeAndSubgraphs)
            if (eligibleTermNodes0.isEmpty) {
                Right(UnexpectedInput(input))
            } else {
                assert(eligibleTermNodes0 forall { _.kernel.symbol.accept(input) })
                def liftsFromTermNodes(termNodes: Set[TermNode]): Set[(TermNode, Input)] = termNodes map { (_, input) }

                def collectResultsFromLifts(lifts: Set[Lift[R]]): Option[R] = {
                    val resultLifts = lifts collect {
                        case Lift(`startNode`, _, parsed, _, revertTriggers) if revertTriggers forall { _.triggerType != Trigger.Type.WaitUntilLift } => parsed
                    }
                    resultFunc.merge(resultLifts)
                }

                // 1. reverter 무시하고 우선 한번 lift를 진행한다
                val (liftedGraph0, nextDerivables0, lifts0) = expandedGraph.lift(gen, liftsFromTermNodes(eligibleTermNodes0), Set())
                // liftedGraph0에 startNode가 없는 경우 -> 파싱이 종료되었음을 의미
                if (!(liftedGraph0.nodes contains startNode)) {
                    val nextContext = new ParsingContext(gen + 1, startNode, liftedGraph0, Set(), collectResultsFromLifts(lifts0))
                    Left(new FinishedProceedDetail(expandedGraph, eligibleTermNodes0, liftedGraph0, nextDerivables0, lifts0, nextContext))
                } else {
                    // 2. lift가 진행된 뒤 trigger되는 reverter를 적용한 그래프를 만든다
                    val (revertedGraph, tempLiftBlockNodes) = expandedGraph.revert(liftedGraph0, lifts0)
                    // 3. (optional?) baseNode에서 reachable한 node와 edge로만 구성된 그래프를 추린다
                    val trimmedRevertedGraph = revertedGraph.reachableFrom(startNode)
                    // 4. 추려진 그래프에 대해 다시 lift를 진행한다 (사실은 liftedGraph0, lifts0 등을 고쳐서 쓸 수도 있겠지만 귀찮으니..)
                    val eligibleTermNodes = eligibleTermNodes0 intersect trimmedRevertedGraph.termNodes
                    // 5. 그런 뒤에 eligibleTermNodes1이 비어있으면 에러를 반환한다
                    if (eligibleTermNodes.isEmpty) {
                        Right(UnexpectedInput(input))
                    } else {
                        val (liftedGraph, nextDerivables, lifts) = trimmedRevertedGraph.lift(gen, liftsFromTermNodes(eligibleTermNodes), tempLiftBlockNodes)
                        // 6. reverter가 적용되어 계산된 ParsingContext를 반환한다
                        val nextContext = new ParsingContext(gen + 1, startNode, liftedGraph, nextDerivables, collectResultsFromLifts(lifts))
                        Left(UnfinishedProceedDetail(expandedGraph, eligibleTermNodes0, liftedGraph0, nextDerivables0, lifts0, revertedGraph, tempLiftBlockNodes, trimmedRevertedGraph, eligibleTermNodes, liftedGraph, nextDerivables, lifts, nextContext))
                    }
                }
            }
        }
        def proceed(input: ConcreteInput): Either[ParsingContext, ParsingError] = proceedDetail(input) match {
            case Left(detail) => Left(detail.nextContext)
            case Right(error) => Right(error)
        }
    }
    */

    type Graph = CtxGraph[R]

    case class ParsingCtx(gen: Int, graph: Graph, startNode: NontermNode, derivables: Set[NontermNode]) {
        assert(startNode.symbol == Start)

        val nextGen = gen + 1

        def result = {
            // startNode의 result들 중 Wait 타입의 Trigger가 없는 것들만 추려서 merge해서 반환한다
            resultFunc.merge(
                graph.resultOf(startNode) collect {
                    case (triggers, result) if triggers forall { _.triggerType != Trigger.Type.Wait } => result
                })
        }

        def expandMulti(baseAndGraphs: Set[(NontermNode, DGraph[R])]): (Graph, Set[TermNode]) = {
            // graph에 DerivationGraph.shiftGen해서 추가해준다
            // edgesFromBaseNode/edgesNotFromBaseNode 구분해서 잘 처리
            ???
        }

        // 이 그래프에서 lift한 그래프와 그 그래프의 derivation tip nodes를 반환한다
        def lift(graph: Graph, termNodes: Set[TermNode], input: Inputs.Input, liftBlockedNodes: Set[AtomicNode]): (Graph, Set[SequenceNode]) = {
            // FinishingTask.node가 liftBlockedNodes에 있으면 해당 task는 제외
            // DeriveTask(node)가 나오면 실제 Derive 진행하지 않고 derivation tip nodes로 넣는다 (이 때 node는 항상 SequenceNode임)
            def rec(tasks: List[Task], graphCC: Graph, derivablesCC: Set[SequenceNode]): (Graph, Set[SequenceNode]) =
                tasks match {
                    case task +: rest =>
                        task match {
                            case DeriveTask(_, node) =>
                                assert(node.isInstanceOf[SequenceNode])
                                rec(rest, graphCC, derivablesCC + node.asInstanceOf[SequenceNode])
                            case task: FinishingTask =>
                                if (liftBlockedNodes.toSet[Node] contains task.node) {
                                    // liftBlockNode이면 task 진행하지 않음
                                    rec(rest, graphCC, derivablesCC)
                                } else {
                                    val (newGraphCC, newTasks) = finishingTask(task, graphCC)
                                    rec(rest ++ newTasks, newGraphCC, derivablesCC)
                                }
                            case task: SequenceProgressTask =>
                                val (newGraphCC, newTasks) = sequenceProgressTask(task, graphCC)
                                rec(rest ++ newTasks, newGraphCC, derivablesCC)
                        }
                    case List() => (graphCC, derivablesCC)
                }

            val initialTasks = termNodes.toList map {
                FinishingTask(nextGen, _, resultFunc.terminal(input), Set())
            }
            rec(initialTasks, graph, Set())
        }

        def proceedDetail(input: ConcreteInput): Either[ProceedDetail, ParsingError] = {
            ???
        }
        def proceed(input: ConcreteInput): Either[ParsingCtx, ParsingError] = {
            ???
        }
    }

    val initialContext = {
        val startNode = AtomicNode(Start, 0)(None, None)
        ParsingCtx(
            0,
            CtxGraph[R](Set(startNode), Set(), Map(startNode -> derive(startNode).resultOf(startNode)), Map()),
            startNode,
            Set(startNode))
    }

    def parse(source: Inputs.ConcreteSource): Either[ParsingCtx, ParsingError] =
        source.foldLeft[Either[ParsingCtx, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => ctx proceed input
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[ParsingCtx, ParsingError] =
        parse(Inputs.fromString(source))
}
