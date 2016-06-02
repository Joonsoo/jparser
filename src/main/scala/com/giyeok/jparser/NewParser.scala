package com.giyeok.jparser

import com.giyeok.jparser.Symbols._
import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.Inputs.Input
import ParsingGraph._
import com.giyeok.jparser.Inputs.TermGroupDesc

import com.giyeok.jparser.DGraph.BaseNode

case class CtxGraph[R <: ParseResult](
        nodes: Set[Node],
        edges: Set[Edge],
        results: Results[Node, R],
        progresses: Results[SequenceNode, R]) extends ParsingGraph[R] {
    def create(nodes: Set[Node], edges: Set[Edge], results: Results[Node, R], progresses: Results[SequenceNode, R]): CtxGraph[R] = {
        CtxGraph(nodes, edges, results, progresses)
    }
}

class NewParser[R <: ParseResult](val grammar: Grammar, val resultFunc: ParseResultFunc[R]) extends LiftTasks[R, CtxGraph[R]] {
    val derivationFunc = new DerivationFunc(grammar, resultFunc)

    private val derivationGraphCache = scala.collection.mutable.Map[(Nonterm, Int), (DGraph[R], Map[TermGroupDesc, DGraph[R]])]()
    def derive(baseNode: NontermNode): (DGraph[R], Map[TermGroupDesc, DGraph[R]]) = {
        val kernel = baseNode match {
            case AtomicNode(symbol, _) => (symbol, 0)
            case SequenceNode(symbol, pointer, _, _) => (symbol, pointer)
        }
        derivationGraphCache get kernel match {
            case Some(dgraph) => dgraph
            case None =>
                val dgraph = baseNode match {
                    case _: DGraph.BaseNode => ??? // BaseNode일 수 없음
                    case AtomicNode(symbol, _) => derivationFunc.deriveAtomic(symbol)
                    case SequenceNode(symbol, pointer, _, _) => derivationFunc.deriveSequence(symbol, pointer)
                }
                val sliceMap = dgraph.sliceByTermGroups(resultFunc) collect {
                    case (termGroupDesc, Some(sliced)) => termGroupDesc -> sliced
                }
                derivationGraphCache(kernel) = (dgraph, sliceMap)
                (dgraph, sliceMap)
        }
    }

    case class ProceedDetail(
            expandedGraph: Graph,
            eligibleTermNodes: Set[TermNode],
            liftedGraph0pre: Graph,
            liftedGraph0: Graph,
            nextDerivables0: Set[SequenceNode],
            revertedGraph: Graph,
            // tempLiftBlocking
            liftedGraphPre: Graph,
            liftedGraph: Graph,
            nextDerivables: Set[SequenceNode],
            nextContext: ParsingCtx) {
        val firstStage = (liftedGraph0, nextDerivables0)
        val finalStage = (liftedGraph, nextDerivables)
    }

    type Graph = CtxGraph[R]

    case class ParsingCtx(gen: Int, graph: Graph, startNode: NontermNode, derivables: Set[NontermNode]) {
        assert(startNode.symbol == Start)

        // startNode에서 derivables에 도달 가능한 경로 내의 노드들만 포함되어야 하는데..?
        // assert(graph.subgraphIn(startNode, derivables.asInstanceOf[Set[Node]], resultFunc) == Some(graph))

        def result: Option[R] = {
            // startNode의 result들 중 Wait 타입의 Trigger가 없는 것들만 추려서 merge해서 반환한다
            graph.results.of(startNode) flatMap { results =>
                resultFunc.merge(
                    results collect {
                        case (triggers, result) if triggers forall { _.triggerType != Trigger.Type.Wait } => result
                    })
            }
        }

        def proceedDetail(input: ConcreteInput): Either[ProceedDetail, ParsingError] = {
            val baseAndGraphs = derivables flatMap { dnode =>
                val coll = derive(dnode)._2 collect { case (termGroup, dgraph) if termGroup contains input => dnode -> dgraph }
                coll ensuring (coll.size <= 1)
            }
            // 1. expand
            val (expandedGraph, termNodes) = ParsingCtx.expandMulti(graph, gen, baseAndGraphs)
            // slice된 그래프에서 골라서 추가했으므로 termNodes는 모두 input을 받을 수 있어야 한다
            assert(termNodes forall { _.symbol accept input })
            if (termNodes.isEmpty) {
                Right(UnexpectedInput(input))
            } else {
                val nextGen = gen + 1
                // 2. 1차 lift
                val (liftedGraph0pre, nextDerivables0) = ParsingCtx.lift(expandedGraph, nextGen, termNodes map { (_, input) }, Results())
                val liftedGraph0opt = liftedGraph0pre.subgraphIn(startNode, nextDerivables0.asInstanceOf[Set[Node]], resultFunc) map { _.asInstanceOf[Graph] }
                liftedGraph0opt match {
                    case Some(liftedGraph0) =>
                        // TODO expandedGraph0에서 liftedGraph0의 results를 보고 조건이 만족된 엣지들/result들 제거 - unreachable 노드들은 밑에 liftedGraphPre->liftedGraph 에서 처리되므로 여기서는 무시해도 됨
                        val revertedGraph: Graph = ParsingCtx.revert(expandedGraph, liftedGraph0)
                        val (liftedGraphPre, nextDerivables) = ParsingCtx.lift(revertedGraph, nextGen, termNodes map { (_, input) }, Results()) // TODO
                        val liftedGraphOpt = liftedGraphPre.subgraphIn(startNode, nextDerivables.asInstanceOf[Set[Node]], resultFunc) map { _.asInstanceOf[Graph] }
                        liftedGraphOpt match {
                            case Some(liftedGraph) =>
                                val nextContext: ParsingCtx = ParsingCtx(nextGen, liftedGraph, startNode, (nextDerivables.asInstanceOf[Set[Node]] intersect liftedGraph.nodes).asInstanceOf[Set[NontermNode]])
                                Left(ProceedDetail(expandedGraph, termNodes, liftedGraph0pre, liftedGraph0, nextDerivables0, revertedGraph, liftedGraphPre, liftedGraph, nextDerivables, nextContext))
                            case None =>
                                ???
                        }
                    case None =>
                        // 파싱 결과는 있는데 더이상 진행할 수 없는 경우
                        ???
                }
            }
        }

        def proceed(input: ConcreteInput): Either[ParsingCtx, ParsingError] =
            proceedDetail(input) match {
                case Left(detail) => Left(detail.nextContext)
                case Right(error) => Right(error)
            }
    }

    object ParsingCtx {
        def shiftNode[T <: Node](node: T, gen: Int): T = node match {
            case EmptyNode => node
            case TermNode(symbol, beginGen) => TermNode(symbol, beginGen + gen).asInstanceOf[T]
            case n: AtomicNode => (AtomicNode(n.symbol, n.beginGen + gen)(n.liftBlockTrigger map { shiftNode(_, gen) }, n.reservedReverterType)).asInstanceOf[T]
            case SequenceNode(symbol, pointer, beginGen, endGen) => SequenceNode(symbol, pointer, beginGen + gen, endGen + gen).asInstanceOf[T]
        }
        def shiftTrigger(trigger: Trigger, gen: Int): Trigger = trigger match {
            case Trigger(node, ttype) => Trigger(shiftNode(node, gen), ttype)
        }

        // graph를 expand한다
        // - baseAndGraphs에 있는 튜플의 _1를 base로 해서 _2의 내용을 shiftGen해서 그래프에 추가한다
        def expandMulti(graph: Graph, gen: Int, baseAndGraphs: Set[(NontermNode, DGraph[R])]): (Graph, Set[TermNode]) = {
            // baseAndGraphs의 베이스 노드가 모두 graph에 포함되어 있어야 한다
            assert(baseAndGraphs map { _._1.asInstanceOf[Node] } subsetOf graph.nodes)
            // 현재 파서 구조에서 expand 하기 전에는 TermNode가 그래프에 있으면 안 됨
            println(graph.nodes filter { _.isInstanceOf[TermNode] })
            assert(!(graph.nodes exists { _.isInstanceOf[TermNode] }))

            // graph에 DerivationGraph.shiftGen해서 추가해준다
            // - edgesFromBaseNode/edgesNotFromBaseNode 구분해서 잘 처리
            baseAndGraphs.foldLeft((graph, Set[TermNode]())) { (result, pair) =>
                val (baseNode, dgraph) = pair

                val newNodes = (dgraph.nodes map { node =>
                    val newNode = node match {
                        case _: BaseNode => baseNode
                        case n => shiftNode(n, gen)
                    }
                    node -> newNode
                }).toMap
                val newEdges: Set[Edge] = dgraph.edges map {
                    case SimpleEdge(start, end, revertTriggers) =>
                        SimpleEdge(
                            newNodes(start).asInstanceOf[NontermNode],
                            newNodes(end),
                            revertTriggers map { shiftTrigger(_, gen) })
                    case JoinEdge(start, end, join) =>
                        JoinEdge(
                            newNodes(start).asInstanceOf[NontermNode],
                            newNodes(end),
                            newNodes(join))
                }
                // dgraph.results.of(dgraph.baseNode)와 dgraph.progresses.of(dgraph.baseNode)는 항상 비어 있으므로 여기서 신경쓰지 않아도 됨
                // - 이 내용들은 dgraph.baseResults와 dgraph.baseProgresses로 들어는데 이 부분은 이전 세대의 lift에서 이미 처리되었을 것임

                val newProgresses = dgraph.progresses mapNodesTriggers (shiftNode(_, gen), _ map { shiftTrigger(_, gen) })

                val newNodesSet = newNodes.values.toSet
                val updatedGraph = result._1.withNodesEdgesProgresses(newNodesSet, newEdges, newProgresses).asInstanceOf[Graph]
                val updatedTermNodes = result._2 ++ (newNodesSet collect { case n: TermNode => n })
                (updatedGraph, updatedTermNodes)
            }
        }

        // 이 그래프에서 lift한 그래프와 그 그래프의 derivation tip nodes를 반환한다
        def lift(graph: Graph, nextGen: Int, proceedingTerms: Set[(TermNode, Inputs.Input)], liftBlockedNodes: Results[AtomicNode, R]): (Graph, Set[SequenceNode]) = {
            // FinishingTask.node가 liftBlockedNodes에 있으면 해당 task는 제외
            // DeriveTask(node)가 나오면 실제 Derive 진행하지 않고 derivation tip nodes로 넣는다 (이 때 node는 항상 SequenceNode임)
            def rec(tasks: List[Task], graphCC: Graph, derivablesCC: Set[SequenceNode]): (Graph, Set[SequenceNode]) =
                tasks match {
                    case task +: rest =>
                        task match {
                            case DeriveTask(_, node) =>
                                assert(node.isInstanceOf[SequenceNode])
                                // TODO derive(node)._1.baseResults 가 있는 경우는 맨 처음에 Start에서만 생길 수 있음 - 별도 처리가 필요한가?

                                // derive(node)._1.baseProgresses 있으면 SequenceProgressTask
                                val seqNode = node.asInstanceOf[SequenceNode]
                                val immediateProgresses: Seq[SequenceProgressTask] = (derive(seqNode)._1.baseProgresses map { kv =>
                                    val (triggers, (result, resultSymbol)) = kv
                                    // triggers를 nextGen만큼 shift해주기
                                    val shiftedTriggers: Set[Trigger] = triggers map { shiftTrigger(_, nextGen) }
                                    SequenceProgressTask(nextGen, node.asInstanceOf[SequenceNode], result, resultSymbol, shiftedTriggers)
                                }).toSeq

                                rec(rest ++ immediateProgresses, graphCC, derivablesCC + node.asInstanceOf[SequenceNode])
                            case task: FinishingTask =>
                                //                                if (liftBlockedNodes.asInstanceOf[Set[Node]] contains task.node) {
                                //                                    // liftBlockNode이면 task 진행하지 않음
                                //                                    rec(rest, graphCC, derivablesCC)
                                //                                }
                                val (newGraphCC, newTasks) = finishingTask(task, graphCC)
                                rec(rest ++ newTasks, newGraphCC, derivablesCC)
                            case task: SequenceProgressTask =>
                                val (newGraphCC, newTasks) = sequenceProgressTask(task, graphCC)
                                rec(rest ++ newTasks, newGraphCC, derivablesCC)
                        }
                    case List() => (graphCC, derivablesCC)
                }

            val initialTasks = proceedingTerms.toList map { p =>
                FinishingTask(nextGen, p._1, resultFunc.terminal(p._2), Set())
            }
            rec(initialTasks, graph.withNoResults.asInstanceOf[Graph], Set())
        }

        // graph.results를 이용해서 revertTrigger 조건을 비교해서 지워야 할 edge/results를 제거한 그래프를 반환한다
        def revert(graph: Graph, prelift: Graph): Graph = {
            def triggerActivated(trigger: Trigger): Boolean = trigger match {
                case Trigger(node, Trigger.Type.Lift) => prelift.results contains node
                case Trigger(node, Trigger.Type.Alive) => prelift.nodes contains node
                case _ => false
            }

            val edgeFiltered = graph filterEdges {
                case SimpleEdge(_, _, revertTriggers) if revertTriggers exists { triggerActivated _ } => false
                case _ => true
            }
            // result는 이후에 lift할 때 없애고 시작하므로 따로 필터링할 필요 없음
            val progressesFiltered = edgeFiltered.updateProgresses(edgeFiltered.progresses filterTo { (_, triggers, _) => !(triggers exists { triggerActivated _ }) })
            progressesFiltered.asInstanceOf[Graph]
        }
    }

    val initialContext = {
        val startNode = AtomicNode(Start, 0)(None, None)
        val emptyResult: Results[Node, R] = derive(startNode)._1.baseResults match {
            case Some(r) => Results[Node, R](startNode -> r)
            case None => Results[Node, R]()
        }
        ParsingCtx(
            0,
            CtxGraph[R](
                Set(startNode),
                Set(),
                emptyResult,
                Results[SequenceNode, R]()),
            startNode,
            Set(startNode))
    }

    def parse(source: Inputs.ConcreteSource): Either[ParsingCtx, ParsingError] =
        source.foldLeft[Either[ParsingCtx, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => ctx.proceed(input)
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[ParsingCtx, ParsingError] =
        parse(Inputs.fromString(source))
}
