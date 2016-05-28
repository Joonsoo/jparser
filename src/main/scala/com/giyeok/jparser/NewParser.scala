package com.giyeok.jparser

import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.ParseTree._
import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors._
import DerivationGraph._
import com.giyeok.jparser.Inputs.Input

class NewParser(val grammar: Grammar) {
    import NewParser._

    private val derivationGraphCache = scala.collection.mutable.Map[NontermKernel[_], DerivationGraph]()
    def derive(kernel: NontermKernel[Nonterm]): DerivationGraph = {
        derivationGraphCache get kernel match {
            case Some(graph) => graph
            case None =>
                val graph = DerivationGraph.deriveFromKernel(grammar, kernel)
                derivationGraphCache(kernel) = graph
                graph
        }
    }

    case class Graph(nodes: Set[Node], edges: Set[Edge]) {
        assert(allNodesAppearedInEdges subsetOf nodes)
        def allNodesAppearedInEdges = edges flatMap {
            case SimpleEdge(start, end, revertTriggers) =>
                Set(start, end) ++ (revertTriggers collect { case NodeTrigger(node, _) => node })
            case JoinEdge(start, end, join) =>
                Set(start, end, join)
        }

        def hasPendedTrigger: Boolean = {
            edges exists {
                case SimpleEdge(_, _, revertTriggers) =>
                    revertTriggers exists { _.isInstanceOf[PendedNodeTrigger] }
                case edge: JoinEdge => false
            }
        }

        def termNodes: Set[TermNode] = nodes collect { case n: TermNode => n }

        def expandMulti(gen: Int, baseAndGraphs: Set[(NontermNode[Nonterm], DerivationGraph)]): (Graph, Set[TermNode]) = {
            assert(baseAndGraphs forall { bg => bg._1.kernel == bg._2.baseNode.kernel })
            val baseNodes: Set[Node] = baseAndGraphs map { _._1 }
            def newNode(dnode: NewNode with NonEmptyNode): Node = dnode match {
                case NewTermNode(kernel) => TermNode(kernel)
                case NewAtomicNode(kernel, liftBlockTrigger, reservedReverter) =>
                    AtomicNode(kernel, gen, liftBlockTrigger map { t => newNode(t.asInstanceOf[NewNode with NonEmptyNode]) }, reservedReverter map { Trigger.Type.of _ })
                case NewNonAtomicNode(kernel, progress) =>
                    NonAtomicNode(kernel, gen, gen, progress)
            }
            val derivationGraphs = baseAndGraphs map { _._2 }
            val nodesMap: Map[DerivationGraph.Node, Node] = {
                val newNodes = derivationGraphs.foldLeft(Set[DerivationGraph.NewNode]()) { _ ++ _.newNodes }
                (newNodes map { n => n -> newNode(n.asInstanceOf[NewNode with NonEmptyNode]) }).toMap
            }
            val newEdges: Set[Edge] = derivationGraphs flatMap { derivationGraph =>
                (derivationGraph.edges -- derivationGraph.edgesFromBaseNode) map {
                    case DerivationGraph.SimpleEdge(start, end, edgeRevertTriggers) =>
                        val derivedRevertTriggers: Set[Trigger] = edgeRevertTriggers map {
                            case DerivationGraph.Trigger(node, triggerType) =>
                                NodeTrigger(nodesMap(node), Trigger.Type.of(triggerType))
                        }
                        val startNode = nodesMap(start).asInstanceOf[NontermNode[Nonterm]]
                        SimpleEdge(startNode, nodesMap(end), derivedRevertTriggers)
                    case DerivationGraph.JoinEdge(start, end, join) =>
                        assert(start.kernel.isInstanceOf[JoinKernel])
                        val startNode = nodesMap(start).asInstanceOf[AtomicNode[Join]]
                        JoinEdge(startNode, nodesMap(end), nodesMap(join))
                }
            }
            val newEdgesFromBase: Set[Edge] = baseAndGraphs flatMap { baseAndGraph =>
                val (baseNode, derivationGraph) = baseAndGraph
                derivationGraph.edgesFromBaseNode map {
                    case DerivationGraph.SimpleEdge(start, end, edgeRevertTriggers) =>
                        assert(start.kernel == baseNode.kernel)
                        val derivedRevertTriggers: Set[Trigger] = edgeRevertTriggers map {
                            case DerivationGraph.Trigger(node, triggerType) =>
                                NodeTrigger(nodesMap(node), Trigger.Type.of(triggerType))
                        }
                        SimpleEdge(baseNode, nodesMap(end), derivedRevertTriggers)
                    case DerivationGraph.JoinEdge(start, end, join) =>
                        throw new AssertionError("")
                }
            }
            val pendedProcessedEdges: Set[Edge] = edges collect {
                case SimpleEdge(start, end, revertTriggers) if baseNodes contains end =>
                    // baseNode로 들어오는 SimpleEdge의 revertTrigger중 PendedNode로 되어 있는 것들 처리
                    // - PendedNode가 실제로 expand되었으면 실제 노드로 바꿔주고
                    // - expand되지 않았으면 해당 트리거는 제거
                    val newRevertTriggers: Set[Trigger] = revertTriggers collect {
                        case trigger: NodeTrigger => trigger
                        case PendedNodeTrigger(dnode, ttype) if nodesMap contains dnode =>
                            NodeTrigger(nodesMap(dnode), ttype)
                    }
                    SimpleEdge(start, end, newRevertTriggers)
                case edge => edge
            }
            val result = Graph(nodes ++ nodesMap.values.toSet, pendedProcessedEdges ++ newEdges ++ newEdgesFromBase)
            (result, nodesMap.values.toSet[Node] collect { case n: TermNode => n })
        }
        def expand(gen: Int, baseNode: NontermNode[Nonterm], derivationGraph: DerivationGraph): (Graph, Set[TermNode]) = {
            assert(derivationGraph.baseNode.kernel == baseNode.kernel)
            def newNode(dnode: NewNode with NonEmptyNode): Node = dnode match {
                case NewTermNode(kernel) => TermNode(kernel)
                case NewAtomicNode(kernel, liftBlockTrigger, reservedReverter) =>
                    AtomicNode(kernel, gen, liftBlockTrigger map { t => newNode(t.asInstanceOf[NewNode with NonEmptyNode]) }, reservedReverter map { Trigger.Type.of _ })
                case NewNonAtomicNode(kernel, progress) =>
                    NonAtomicNode(kernel, gen, gen, progress)
            }
            val nodesMap: Map[DerivationGraph.Node, Node] =
                (((derivationGraph.nodes - derivationGraph.baseNode) map { n =>
                    (n -> newNode(n.asInstanceOf[NewNode with NonEmptyNode]))
                }).toMap) + (derivationGraph.baseNode -> baseNode)
            val newEdges = derivationGraph.edges map {
                case DerivationGraph.SimpleEdge(start, end, edgeRevertTriggers) =>
                    val derivedRevertTriggers: Set[Trigger] = edgeRevertTriggers map {
                        case DerivationGraph.Trigger(node, triggerType) =>
                            NodeTrigger(nodesMap(node), Trigger.Type.of(triggerType))
                    }
                    val startNode = nodesMap(start).asInstanceOf[NontermNode[Nonterm]]
                    SimpleEdge(startNode, nodesMap(end), derivedRevertTriggers)
                case DerivationGraph.JoinEdge(start, end, join) =>
                    assert(start.kernel.isInstanceOf[JoinKernel])
                    val startNode = nodesMap(start).asInstanceOf[AtomicNode[Join]]
                    JoinEdge(startNode, nodesMap(end), nodesMap(join))
            }
            val pendedProcessedEdges: Set[Edge] = edges collect {
                case SimpleEdge(start, end, revertTriggers) if end == baseNode =>
                    // baseNode로 들어오는 SimpleEdge의 revertTrigger중 PendedNode로 되어 있는 것들 처리
                    // - PendedNode가 실제로 expand되었으면 실제 노드로 바꿔주고
                    // - expand되지 않았으면 해당 트리거는 제거
                    val newRevertTriggers: Set[Trigger] = revertTriggers collect {
                        case trigger: NodeTrigger => trigger
                        case PendedNodeTrigger(dnode, ttype) =>
                            NodeTrigger(nodesMap(dnode), ttype)
                    }
                    SimpleEdge(start, end, newRevertTriggers)
                case edge => edge
            }
            val result = Graph(nodes ++ nodesMap.values.toSet, pendedProcessedEdges ++ newEdges)
            (result, nodesMap.values.toSet[Node] collect { case n: TermNode => n })
        }

        def lift(gen: Int, termLifts: Set[(TermNode, Input)], liftBlockedNodes: Set[AtomicNode[_]]): (Graph, Set[NontermNode[Nonterm]], Set[Lift]) = {
            sealed trait LiftTask { val before: Node }
            case class TermLift(before: TermNode, by: Input) extends LiftTask
            case class NontermLift(before: NontermNode[Nonterm], by: ParseNode[Symbol], revertTriggers: Set[Trigger]) extends LiftTask
            case class JoinLift(before: NontermNode[Join], by: ParseNode[Symbol], join: ParseNode[Symbol], revertTriggers: Set[Trigger]) extends LiftTask
            def lift(queue: List[LiftTask], graph: Graph, derivables: Set[NontermNode[Nonterm]], lifts: Set[Lift]): (Graph, Set[NontermNode[Nonterm]], Set[Lift]) = {
                def chainLift(node: Node, parsed: ParseNode[Symbol], revertTriggers: Set[Trigger]): Set[LiftTask] = {
                    val incomingEdges = graph.incomingEdgesTo(node)
                    val chains = incomingEdges flatMap {
                        case SimpleEdge(start, end, edgeRevertTriggers) =>
                            assert(node == end)
                            Set[LiftTask](NontermLift(start, parsed, revertTriggers ++ edgeRevertTriggers))
                        case JoinEdge(start, end, join) =>
                            if (end == node) {
                                (lifts filter { _.before == join } map { l =>
                                    JoinLift(start, parsed, l.parsed, revertTriggers ++ l.revertTriggers)
                                }).toSet[LiftTask]
                            } else {
                                assert(join == node)
                                (lifts filter { _.before == end } map { l =>
                                    JoinLift(start, l.parsed, parsed, revertTriggers ++ l.revertTriggers)
                                }).toSet[LiftTask]
                            }
                    }
                    chains filterNot { liftBlockedNodes.toSet[Node] contains _.before }
                }
                queue match {
                    case TermLift(before, by) +: rest =>
                        val (afterKernel, parsed) = (before.kernel.lifted, ParsedTerminal(before.kernel.symbol, by))
                        // TermNode는 atomic node이므로 한 번 lift하면 finishable && !derivable 해진다
                        assert(afterKernel.finishable && !afterKernel.derivable)
                        // lifts에 Lift 추가하고
                        val newLift = Lift(before, afterKernel, parsed, None, Set())
                        // TermNode는 이번 세대에서 항상 사라지기 때문에 (항상 dangled이므로) graph에서 지우고
                        // before로 incoming node들에 대해 새로운 LiftTask를 추가
                        val newLiftTasks = chainLift(before, parsed, Set())
                        lift(newLiftTasks.toList ++: rest, graph, derivables, lifts + newLift)

                    case NontermLift(before @ AtomicNode(kernel, _, _, reservedReverter), by, revertTriggers) +: rest =>
                        val (afterKernel, parsed) = (kernel.lifted, ParsedSymbol(kernel.symbol, by))
                        // Atomic node는 한 번 lift하면 finishable && !derivable 해진다
                        assert(afterKernel.finishable && !afterKernel.derivable)
                        // lifts에 Lift 추가하고
                        val newRevertTriggers = revertTriggers ++ (reservedReverter map { NodeTrigger(before, _) })
                        val newLift = Lift(before, afterKernel, parsed, None, newRevertTriggers)
                        // dangled == true이면 before를 graph에서 지우고
                        // before로 incoming node들에 대해 새로운 LiftTask를 추가
                        val newLiftTasks = chainLift(before, parsed, newRevertTriggers)
                        lift(newLiftTasks.toList ++: rest, graph, derivables, lifts + newLift)

                    case JoinLift(before @ AtomicNode(kernel, _, _, reservedReverter), by, join, revertTriggers) +: rest =>
                        // AtomicNode NontermLift와 사실상 동일
                        val (afterKernel, parsed) = (kernel.lifted, new ParsedSymbolJoin(kernel.symbol, by, join))
                        assert(afterKernel.finishable && !afterKernel.derivable)
                        val newLift = Lift(before, afterKernel, parsed, None, revertTriggers)
                        val newLiftTasks = chainLift(before, parsed, revertTriggers)
                        lift(newLiftTasks.toList ++: rest, graph, derivables, lifts + newLift)

                    case NontermLift(before @ NonAtomicNode(kernel, _, _, progress), by, revertTriggers) +: rest =>
                        val (afterKernel: NonAtomicNontermKernel[_], parsed: ParsedSymbolsSeq[_]) = kernel.lifted(progress, by)
                        val incomingEdges0 = graph.incomingEdgesTo(before)
                        assert(incomingEdges0 forall { _.isInstanceOf[SimpleEdge] })
                        val incomingEdges = incomingEdges0 map { _.asInstanceOf[SimpleEdge] }

                        var newDerivables = derivables
                        var afterNode: Option[Node] = None
                        // afterKernel.derivable하면 새로 생긴 노드는 roottip이 되고 그 이후로는 root 노드가 되므로 살려야 한다
                        var newGraph = graph
                        var newLiftTasks = List[LiftTask]()

                        if (afterKernel.derivable) {
                            // afterKernel과 newProgress로 새로운 node 만들고 start -> 새 노드로 가는 엣지 추가하고, 이 때 before에 붙어있던 edge revert trigger들은 이 엣지에도 붙여준다
                            val newNode = NonAtomicNode(afterKernel, before.beginGen, gen, parsed)
                            val newEdges: Set[Edge] = incomingEdges map {
                                case SimpleEdge(start, _, edgeRevertTriggers) =>
                                    SimpleEdge(start, newNode, edgeRevertTriggers ++ revertTriggers)
                            }
                            // 새로 생성된 노드는 아래에서 만드는 Lift에 after node로 지정해주고 derviables에 추가한다
                            newDerivables += newNode
                            newGraph = newGraph.withNodeAndEdges(newNode, newEdges)
                            afterNode = Some(newNode)
                            // DerivationGraph of 새로 생성된 노드에 baseNodeLift가 있는 경우(nullable 처리)
                            // - 새로운 lift task를 만들어서 추가해준다
                            val baseNodeLifts = derive(afterKernel).baseLifts
                            newLiftTasks ++:= baseNodeLifts map { lift =>
                                // lift.revertTriggers는 PendedNode에 의해 trigger되는 것으로 처리
                                val pendedTriggers: Set[Trigger] = lift.revertTriggers map {
                                    case DerivationGraph.Trigger(node, triggerType) =>
                                        PendedNodeTrigger(node, Trigger.Type.of(triggerType))
                                }
                                NontermLift(newNode, lift.parsedBy, revertTriggers ++ pendedTriggers)
                            }
                        }

                        val newLift = Lift(before, afterKernel, parsed, afterNode, revertTriggers)
                        if (afterKernel.finishable) {
                            newLiftTasks ++:= chainLift(before, parsed, revertTriggers).toList
                        }
                        lift(newLiftTasks ++: rest, newGraph, newDerivables, lifts + newLift)

                    case List() => (graph, derivables, lifts)
                }
                // dangled이면 graph에서 lift.before 노드와 incomingEdges를 모두 제거한다
            }
            val (graph, derivables, lifts) = lift(termLifts.toList map { p => TermLift(p._1, p._2) }, this, Set(), Set())
            // graph에서 derivables로 reachable한 노드/엣지만 추린다(기존의 rootTip/root와 같은 의미)
            (graph.reachableTo(derivables.toSet[Node]), derivables, lifts)
        }

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

    trait ProceedDetail {
        val expandedGraph: Graph
        val eligibleTermNodes0: Set[TermNode]
        val liftedGraph0: Graph
        val nextDerivables0: Set[NontermNode[Nonterm]]
        val lifts0: Set[Lift]
        val nextContext: ParsingContext
    }
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

    case class ParsingContext(gen: Int, startNode: NontermNode[Start.type], graph: Graph, derivables: Set[NontermNode[Nonterm]], results: Set[ParseNode[Symbol]]) {
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

                def collectResultsFromLifts(lifts: Set[Lift]): Set[ParseNode[Symbol]] =
                    lifts collect {
                        case Lift(`startNode`, _, parsed, _, revertTriggers) if revertTriggers forall { _.triggerType != Trigger.Type.WaitUntilLift } => parsed
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

    val initialContext = {
        val startKernel = StartKernel(0)
        val startNode = AtomicNode(startKernel, 0, None, None)
        new ParsingContext(0, startNode, Graph(Set(startNode), Set()), Set(startNode), derive(startKernel).baseLifts map { l => ParsedSymbol(Start, l.parsedBy) })
    }

    def parse(source: Inputs.ConcreteSource): Either[ParsingContext, ParsingError] =
        source.foldLeft[Either[ParsingContext, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => ctx proceed input
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[ParsingContext, ParsingError] =
        parse(Inputs.fromString(source))
}

object NewParser {
    sealed trait Node {
        val kernel: Kernel
    }
    sealed trait NontermNode[+T <: Nonterm] extends Node {
        val kernel: NontermKernel[T]
        val beginGen: Int
    }
    case class TermNode(kernel: TerminalKernel) extends Node
    case class AtomicNode[+T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T], beginGen: Int, liftBlockTrigger: Option[Node], reservedReverter: Option[Trigger.Type.Value]) extends NontermNode[T]
    case class NonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], beginGen: Int, endGen: Int, progress: ParsedSymbolsSeq[T]) extends NontermNode[T]

    trait Trigger {
        val triggerType: Trigger.Type.Value
    }
    case class NodeTrigger(node: Node, triggerType: Trigger.Type.Value) extends Trigger
    case class PendedNodeTrigger(node: DerivationGraph.Node, triggerType: Trigger.Type.Value) extends Trigger {
        assert(node.isInstanceOf[DerivationGraph.NewNode])
    }
    object Trigger {
        object Type extends Enumeration {
            val Lift, Alive, DeadUntilLift, WaitUntilLift = Value
            def of(t: DerivationGraph.Trigger.Type.Value) = t match {
                case DerivationGraph.Trigger.Type.Lift => Lift
                case DerivationGraph.Trigger.Type.Alive => Alive
                case DerivationGraph.Trigger.Type.DeadUntilLift => DeadUntilLift
                case DerivationGraph.Trigger.Type.WaitUntilLift => WaitUntilLift
            }
        }
    }

    sealed trait Edge
    case class SimpleEdge(start: NontermNode[Nonterm], end: Node, revertTriggers: Set[Trigger]) extends Edge
    case class JoinEdge(start: AtomicNode[Join], end: Node, join: Node) extends Edge

    case class Lift(before: Node, afterKernel: Kernel, parsed: ParseNode[Symbol], after: Option[Node], revertTriggers: Set[Trigger])
}
