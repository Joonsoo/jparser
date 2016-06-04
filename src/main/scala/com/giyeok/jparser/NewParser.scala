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
                    case _: DGraph.BaseNode => // BaseNode일 수 없음
                        throw new AssertionError("")
                    case AtomicNode(symbol, _) => derivationFunc.deriveAtomic(symbol)
                    case SequenceNode(symbol, pointer, _, _) => derivationFunc.deriveSequence(symbol, pointer)
                }
                val sliceMap = derivationFunc.sliceByTermGroups(dgraph)
                derivationGraphCache(kernel) = (dgraph, sliceMap)
                (dgraph, sliceMap)
        }
    }

    type Graph = CtxGraph[R]

    trait CtxGraphTransition {
        val baseGraph: Graph
        val nextGraph: Graph
    }
    trait ParsingTaskTransition {
        val baseGraph: Graph
        val initialTasks: Seq[ParsingTasks[R, Graph]#Task]
        // TODO
    }
    case class ExpandTransition(baseGraph: Graph, nextGraph: Graph, expandedTermNodes: Set[TermNode], pendedTermNodes: Set[TermNode]) extends CtxGraphTransition
    case class LiftTransition(baseGraph: Graph, nextGraph: Graph, startingNodes: Set[(TermNode, Input)], nextDerivables: Set[Node], liftBlockedNodes: Map[AtomicNode, Set[Trigger]]) extends CtxGraphTransition
    case class TrimmingTransition(baseGraph: Graph, nextGraph: Graph, startNode: Node, endNodes: Set[Node]) extends CtxGraphTransition
    case class RevertTransition(baseGraph: Graph, nextGraph: Graph, firstLiftResult: Graph) extends CtxGraphTransition

    case class ParsingCtxTransition(
        firstStage: Option[(ExpandTransition, LiftTransition)],
        secondStage: Option[(TrimmingTransition, RevertTransition, LiftTransition)],
        finalTrimming: Option[TrimmingTransition])

    object ParsingCtx {
        def shiftNode[T <: Node](node: T, gen: Int): T = node match {
            case EmptyNode => node
            case TermNode(symbol, beginGen) => TermNode(symbol, beginGen + gen).asInstanceOf[T]
            case n: AtomicNode => (AtomicNode(n.symbol, n.beginGen + gen)(n.liftBlockTrigger map { shiftNode(_, gen) }, n.reservedReverterType)).asInstanceOf[T]
            case SequenceNode(symbol, pointer, beginGen, endGen) => SequenceNode(symbol, pointer, beginGen + gen, endGen + gen).asInstanceOf[T]
        }

        // graph를 expand한다
        // - baseAndGraphs에 있는 튜플의 _1를 base로 해서 _2의 내용을 shiftGen해서 그래프에 추가한다
        def expandMulti(graph: Graph, gen: Int, baseAndGraphs: Set[(NontermNode, DGraph[R])]): (Graph, Set[TermNode]) = {
            // baseAndGraphs의 베이스 노드가 모두 graph에 포함되어 있어야 한다
            assert(baseAndGraphs map { _._1.asInstanceOf[Node] } subsetOf graph.nodes)
            // expand 하기 전에는 이전 세대의 TermNode가 그래프에 있으면 안 됨
            assert({
                val invalidTermNodes = (graph.nodes collect { case node @ TermNode(sym, nodeGen) if nodeGen < gen => node })
                if (!invalidTermNodes.isEmpty) {
                    println(invalidTermNodes)
                }
                invalidTermNodes.isEmpty
            })

            def shiftTrigger(trigger: Trigger, gen: Int): Trigger = trigger match {
                case Trigger(node, ttype) => Trigger(shiftNode(node, gen), ttype)
            }

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

                val newProgresses = dgraph.progresses map (node => shiftNode(node, gen), triggers => triggers map { shiftTrigger(_, gen) }, result => result)

                val newNodesSet = newNodes.values.toSet
                val updatedGraph = result._1.withNodesEdgesProgresses(newNodesSet, newEdges, newProgresses).asInstanceOf[Graph]
                val updatedTermNodes = result._2 ++ (newNodesSet collect { case n: TermNode => n })
                (updatedGraph, updatedTermNodes)
            }
        }

        // 이 그래프에서 lift한 그래프와 그 그래프의 derivation tip nodes를 반환한다
        def lift(graph: Graph, nextGen: Int, proceedingTerms: Set[(TermNode, Inputs.Input)], liftBlockedNodes: Map[AtomicNode, Set[Trigger]]): (Graph, Set[SequenceNode]) = {
            // FinishingTask.node가 liftBlockedNodes에 있으면 해당 task는 제외
            // DeriveTask(node)가 나오면 실제 Derive 진행하지 않고 derivation tip nodes로 넣는다 (이 때 node는 항상 SequenceNode임)
            def rec(tasks: List[Task], graphCC: Graph, derivablesCC: Set[SequenceNode]): (Graph, Set[SequenceNode]) =
                tasks match {
                    case task +: rest =>
                        task match {
                            case DeriveTask(_, node: SequenceNode) =>
                                var shiftedTriggerNodes: Set[Node] = Set()

                                val immediateProgresses: Seq[SequenceProgressTask] = (derive(node)._1.baseProgresses map { kv =>
                                    val (triggers, (result, resultSymbol)) = kv
                                    // triggers를 nextGen만큼 shift해주기
                                    val shiftedNodesAndTriggers = triggers map {
                                        case Trigger(node, ttype) =>
                                            val shiftedNode = shiftNode(node, nextGen)
                                            (shiftedNode, Trigger(shiftedNode, ttype))
                                    }
                                    shiftedTriggerNodes ++= shiftedNodesAndTriggers map { _._1 }

                                    val shiftedTriggers: Set[Trigger] = shiftedNodesAndTriggers map { _._2 }

                                    SequenceProgressTask(nextGen, node.asInstanceOf[SequenceNode], result, resultSymbol, shiftedTriggers)
                                }).toSeq

                                rec(rest ++ immediateProgresses, graphCC.withNodes(shiftedTriggerNodes).asInstanceOf[Graph], derivablesCC + node.asInstanceOf[SequenceNode])
                            case DeriveTask(_, _) =>
                                // 이런 상황은 발생할 수 없음
                                throw new AssertionError("")
                            case task: FinishingTask =>
                                val liftBlocking = task.node match {
                                    case node: AtomicNode => liftBlockedNodes get node
                                    case _ => None
                                }
                                liftBlocking match {
                                    case None =>
                                        // liftedBlockedNodes에 있지 않으면 일반 진행
                                        val (newGraphCC, newTasks) = finishingTask(task, graphCC)
                                        rec(rest ++ newTasks, newGraphCC, derivablesCC)
                                    case Some(triggers) if triggers.isEmpty =>
                                        // liftBlockNode이면 task 진행하지 않음
                                        rec(rest, graphCC, derivablesCC)
                                    case Some(triggers) =>
                                        // except 조건부에서 reverter가 올라온 경우 두 조건을 합쳐서 계속 진행
                                        // FinishingTask(nextGen, node, result, revertTriggers)
                                        val (newGraphCC, newTasks) = finishingTask(FinishingTask(task.nextGen, task.node, task.result, task.revertTriggers ++ triggers), graphCC)
                                        rec(rest ++ newTasks, newGraphCC, derivablesCC)
                                }
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
            // Lift 트리거는 대상 노드가 리프트되면 트리거가 붙어있는 엣지/결과를 무효화
            // Alive 트리거는 대상 노드가 살아있으면 트리거가 붙어있는 엣지/결과를 무효화
            // Wait 트리거는 대상 노드가 리프트되지도 않고 살아있지도 않으면 트리거가 붙어 있는 엣지/결과를 무효화하고,
            //   대상 노드가 리프트되면 트리거만 사라짐

            // (activated, survived)
            def triggerActivated(trigger: Trigger): (Boolean, Boolean) = {
                val activatedSurvived = trigger match {
                    case Trigger(node, Trigger.Type.Lift) =>
                        // 노드가 리프트되었으면 트리거되고, 노드가 살아있으면 트리거도 유지된다
                        (prelift.results contains node, prelift.nodes contains node)
                    case Trigger(node, Trigger.Type.Wait) =>
                        // 노드가 죽으면 트리거되고, 노드가 리프트되면 트리거는 제거된다
                        (!(prelift.nodes contains node) && !(prelift.results contains node), !(prelift.results contains node))
                    case Trigger(node, Trigger.Type.Alive) =>
                        // 노드가 살아있으면 트리거되고, 노드가 살아있어야 트리거도 산다
                        (prelift.nodes contains node, prelift.results contains node)
                    case Trigger(node, Trigger.Type.Dead) =>
                        // 노드가 죽으면 트리거되고, 노드가 살아있어야 트리거도 산다
                        (!(prelift.nodes contains node), prelift.nodes contains node)
                }
                val (activated, survived) = activatedSurvived
                // 대상 노드가 사라진 경우, activated되던지 트리거가 죽던지 둘 중 하나는 해야 한다
                assert((prelift.nodes contains trigger.node) || activated || !survived)
                activatedSurvived
            }
            def processTriggers(triggers: Set[Trigger]): (Boolean, Set[Trigger]) =
                triggers.foldLeft((false, Set[Trigger]())) { (cc, trigger) =>
                    val (shouldRemove, filtered) = cc
                    val (activated, survived) = triggerActivated(trigger)
                    (shouldRemove || activated, if (survived) filtered + trigger else filtered)
                }

            val nextEdges = graph.edges flatMap {
                case SimpleEdge(start, end, revertTriggers) =>
                    val (shouldRemove, filteredTriggers) = processTriggers(revertTriggers)
                    if (shouldRemove) None else Some(SimpleEdge(start, end, filteredTriggers map { case Trigger(node, ttype) => Trigger(node, ttype) }))
                case edge => Some(edge)
            }
            val nextProgresses = graph.progresses.entries.foldLeft(Results[SequenceNode, R]()) { (cc, entry) =>
                val (node, triggers, result) = entry
                val (shouldRemove, filteredTriggers) = processTriggers(triggers)
                if (shouldRemove) cc else {
                    cc.of(node, filteredTriggers) match {
                        case Some(existingResult) =>
                            cc.update(node, filteredTriggers, resultFunc.merge(existingResult, result))
                        case None =>
                            cc.update(node, filteredTriggers, result)
                    }
                }
            }

            // result는 이후에 lift할 때 없애고 시작하기 때문에 의미 없어서 따로 필터링할 필요 없음
            graph.create(graph.nodes, nextEdges, graph.results, nextProgresses)
        }
    }

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

        def proceedDetail(input: ConcreteInput): (ParsingCtxTransition, Either[ParsingCtx, ParsingError]) = {
            val nextGen = gen + 1

            val baseAndGraphs = derivables flatMap { dnode =>
                val coll = derive(dnode)._2 collect { case (termGroup, dgraph) if termGroup contains input => dnode -> dgraph }
                coll ensuring (coll.size <= 1)
            }
            // 1. expand
            val (expandedGraph, termNodes0All) = ParsingCtx.expandMulti(graph, gen, baseAndGraphs)
            // (원래는) slice된 그래프에서 골라서 추가했으므로 termNodes는 모두 input을 받을 수 있어야 한다
            // assert(termNodes0 forall { _.symbol accept input })
            val termNodes0 = termNodes0All filter { _.symbol accept input }
            if (termNodes0.isEmpty) {
                (ParsingCtxTransition(None, None, None), Right(UnexpectedInput(input)))
            } else {
                // Pended terminal nodes
                val pendedTermNodes = graph.nodes collect { case node: TermNode if node.symbol accept input => node ensuring (node.beginGen == gen) }
                val termNodes = pendedTermNodes ++ termNodes0
                val termNodesInput: Set[(TermNode, Input)] = termNodes map { (_, input) }

                val expandTransition = ExpandTransition(graph, expandedGraph, termNodes0, pendedTermNodes)

                // 2. 1차 lift
                val (liftedGraph0pre, nextDerivables0) = ParsingCtx.lift(expandedGraph, nextGen, termNodesInput, Map())

                val firstLiftTransition = LiftTransition(expandedGraph, liftedGraph0pre, termNodesInput, nextDerivables0.asInstanceOf[Set[Node]], Map())
                (liftedGraph0pre.subgraphIn(gen, startNode, nextDerivables0.asInstanceOf[Set[Node]], resultFunc)) match {
                    case Some(liftedGraph0: Graph) =>
                        val firstLiftTrimmingTransition = TrimmingTransition(liftedGraph0pre, liftedGraph0, startNode, nextDerivables0.asInstanceOf[Set[Node]])

                        // expandedGraph0에서 liftedGraph0의 results를 보고 조건이 만족된 엣지들/result들 제거 - unreachable 노드들은 밑에 liftedGraphPre->liftedGraph 에서 처리되므로 여기서는 무시해도 됨
                        val revertedGraph: Graph = ParsingCtx.revert(expandedGraph, liftedGraph0)
                        val revertTransition = RevertTransition(expandedGraph, revertedGraph, liftedGraph0)

                        // TODO lift 막을 노드 정보 수집해서 ParsingCtx.lift에 넘겨주어야 함
                        val liftBlockedNodes: Map[AtomicNode, Set[Trigger]] = {
                            val d = revertedGraph.nodes collect {
                                // liftBlockTrigger가 정의되어 있는 AtomicNode 중 results가 있는 것들을 추려서
                                case node: AtomicNode if (node.liftBlockTrigger flatMap { liftedGraph0.results.of(_) }).isDefined =>
                                    val results = liftedGraph0.results.of(node.liftBlockTrigger.get).get
                                    // 이 지점에서 results에 들어있는 실제 파싱 결과는 관심이 없음
                                    // TODO 여기서 results.keys를 flatten해버리면 되나? 고민해보기
                                    val reverseTriggers = results.keys.flatten map {
                                        case Trigger(node, ttype) =>
                                            val reverseType = ttype match {
                                                case Trigger.Type.Lift => Trigger.Type.Wait
                                                case Trigger.Type.Wait => Trigger.Type.Lift
                                                case Trigger.Type.Alive => Trigger.Type.Dead
                                                case Trigger.Type.Dead => Trigger.Type.Alive
                                            }
                                            Trigger(node, reverseType)
                                    }
                                    node -> reverseTriggers.toSet
                            }
                            d.toMap
                        }

                        val (liftedGraphPre, nextDerivables) = ParsingCtx.lift(revertedGraph, nextGen, termNodesInput, liftBlockedNodes)
                        val secondLiftTransition = LiftTransition(revertedGraph, liftedGraphPre, termNodesInput, nextDerivables.asInstanceOf[Set[Node]], liftBlockedNodes)

                        val liftedGraphOpt = liftedGraphPre.subgraphIn(gen, startNode, nextDerivables.asInstanceOf[Set[Node]], resultFunc) map { _.asInstanceOf[Graph] }
                        liftedGraphOpt match {
                            case Some(liftedGraph) =>
                                val secondLiftTrimmingTransition = TrimmingTransition(liftedGraphPre, liftedGraph, startNode, nextDerivables.asInstanceOf[Set[Node]])
                                val nextContext: ParsingCtx = ParsingCtx(nextGen, liftedGraph, startNode, (nextDerivables.asInstanceOf[Set[Node]] intersect liftedGraph.nodes).asInstanceOf[Set[NontermNode]])
                                val transition = ParsingCtxTransition(
                                    Some(expandTransition, firstLiftTransition),
                                    Some(firstLiftTrimmingTransition, revertTransition, secondLiftTransition),
                                    Some(secondLiftTrimmingTransition))
                                (transition, Left(nextContext))
                            case None =>
                                val transition = ParsingCtxTransition(
                                    Some(expandTransition, firstLiftTransition),
                                    Some(firstLiftTrimmingTransition, revertTransition, secondLiftTransition),
                                    None)
                                // TODO unexpected input 맞나?
                                (transition, Right(UnexpectedInput(input)))
                        }
                    case Some(_) => throw new AssertionError("")
                    case None =>
                        // 파싱 결과는 있는데 더이상 진행할 수 없는 경우
                        val transition = ParsingCtxTransition(Some(expandTransition, firstLiftTransition), None, None)
                        (transition, Left(ParsingCtx(
                            nextGen,
                            CtxGraph(Set(), Set(), Results((liftedGraph0pre.results.of(startNode) map { startNode -> _ }).toSeq: _*), Results()),
                            startNode,
                            Set())))
                }
            }
        }

        def proceed(input: ConcreteInput): Either[ParsingCtx, ParsingError] = proceedDetail(input)._2
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
