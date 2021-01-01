package com.giyeok.jparser.nparser

import ParsingContext._
import AcceptCondition._
import com.giyeok.jparser.NGrammar.{NAtomicSymbol, NExcept, NJoin, NLongest, NLookaheadExcept, NLookaheadIs, NLookaheadSymbol, NSequence, NSimpleDerive, NSymbol, NTerminal}
import com.giyeok.jparser.{Inputs, NGrammar}

import scala.annotation.tailrec
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate

trait ParsingTasks {
    val grammar: NGrammar

    case class Cont(graph: Graph, updatedNodesMap: Map[Node, Set[Node]]) {
        // assert((updatedNodesMap flatMap { kv => kv._2 + kv._1 }).toSet subsetOf graph.nodes)
        // assert(updatedNodesMap forall { kv => kv._2 forall { _.initial == kv._1 } })
        // assert(updatedFinalNodesMap forall { _._2 forall { _.kernel.isFinal } })
    }

    sealed trait Task { val node: Node }
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node) extends Task
    case class ProgressTask(node: Node, condition: AcceptCondition) extends Task

    private def symbolOf(symbolId: Int): NSymbol =
        grammar.symbolOf(symbolId)
    private def atomicSymbolOf(symbolId: Int): NAtomicSymbol =
        symbolOf(symbolId).asInstanceOf[NAtomicSymbol]
    private def newNodeOf(symbolId: Int, beginGen: Int): Node =
        Node(Kernel(symbolId, 0, beginGen, beginGen), Always)

    private case class GraphTasksCont(graph: Graph, newTasks: List[Task])
    private def addNode(cc: GraphTasksCont, newNode: Node): GraphTasksCont = {
        if (!(cc.graph.nodes contains newNode)) {
            // 새로운 노드이면 그래프에 추가하고 task도 추가
            val newNodeTask: Task = if (newNode.kernel.isFinal(grammar)) FinishTask(newNode) else DeriveTask(newNode)
            GraphTasksCont(cc.graph.addNode(newNode), newNodeTask +: cc.newTasks)
        } else {
            // 이미 있는 노드이면 아무 일도 하지 않음
            cc
        }
    }

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Cont): (Cont, Seq[Task]) = {
        val DeriveTask(startNode) = task

        assert(cc.graph.nodes contains startNode)
        assert(!startNode.kernel.isFinal(grammar))
        assert(startNode.kernel.endGen == nextGen)

        def nodeOf(symbolId: Int): Node = newNodeOf(symbolId, nextGen)

        // cc.updatedNodesMap는 참조만 하고 변경하지 않는다
        val updatedNodesMap = cc.updatedNodesMap

        def derive0(cc: GraphTasksCont, symbolId: Int): GraphTasksCont = {
            val newNode = nodeOf(symbolId)
            if (!(cc.graph.nodes contains newNode)) {
                val ncc = addNode(cc, newNode)
                GraphTasksCont(ncc.graph.addEdge(Edge(startNode, newNode)), ncc.newTasks)
            } else if (newNode.kernel.isFinal(grammar)) {
                GraphTasksCont(cc.graph.addEdge(Edge(startNode, newNode)), ProgressTask(startNode, Always) +: cc.newTasks)
            } else {
                assert(newNode.isInitial)
                val updatedNodes = updatedNodesMap.getOrElse(newNode, Set())
                val updatedFinalNodes = updatedNodes filter { _.kernel.isFinal(grammar) }
                // TODO: final node만 저장하도록 수정, assert(updatedFinalNodes forall { _.kernel.isFinal })
                val newTasks = updatedFinalNodes map { finalNode => ProgressTask(startNode, finalNode.condition) }

                assert(!(updatedNodes contains newNode))
                val graph1 = cc.graph.addEdge(Edge(startNode, newNode))
                val newGraph = updatedNodes.foldLeft(graph1) { (graph, updatedNode) =>
                    graph.addEdge(Edge(startNode, updatedNode))
                }
                GraphTasksCont(newGraph, newTasks ++: cc.newTasks)
            }
        }

        val gtc0 = GraphTasksCont(cc.graph, List())
        val GraphTasksCont(newGraph, newTasks) = grammar.symbolOf(startNode.kernel.symbolId) match {
            case symbol: NAtomicSymbol =>
                symbol match {
                    case _: NTerminal =>
                        gtc0 // nothing to do
                    case simpleDerives: NSimpleDerive =>
                        simpleDerives.produces.foldLeft(gtc0) { (cc, deriveSymbolId) => derive0(cc, deriveSymbolId) }
                    case NExcept(_, _, body, except) =>
                        addNode(derive0(gtc0, body), nodeOf(except))
                    case NJoin(_, _, body, join) =>
                        addNode(derive0(gtc0, body), nodeOf(join))
                    case NLongest(_, _, body) =>
                        derive0(gtc0, body)
                    case lookaheadSymbol: NLookaheadSymbol =>
                        addNode(derive0(gtc0, lookaheadSymbol.emptySeqId), nodeOf(lookaheadSymbol.lookahead))
                }
            case NSequence(_, _, seq) =>
                assert(seq.nonEmpty) // empty인 sequence는 derive시점에 모두 처리되어야 함
                assert(startNode.kernel.pointer < seq.length) // node의 pointer는 sequence의 length보다 작아야 함
                derive0(gtc0, seq(startNode.kernel.pointer))
        }
        (Cont(newGraph, cc.updatedNodesMap), newTasks)
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Cont): (Cont, Seq[Task]) = {
        val FinishTask(node) = task

        assert(cc.graph.nodes contains node)
        assert(node.kernel.isFinal(grammar))
        assert(node.kernel.endGen == nextGen)

        // 원래는 cc.graph.edgesByEnd(node.initial) 를 사용해야 하는데, trimming돼서 다 없어져버려서 그냥 둠
        // 사실 trimming 없으면 언제나 cc.graph.edgesByEnd(node.initial) == cc.graph.edgesByEnd(node)
        val incomingEdges = cc.graph.edgesByEnd(node)
        val chainTasks: Seq[Task] = incomingEdges.toSeq map { edge =>
            ProgressTask(edge.start, node.condition)
        }
        (cc, chainTasks)
    }

    def progressTask(nextGen: Int, task: ProgressTask, cc: Cont): (Cont, Seq[Task]) = {
        val ProgressTask(node, incomingCondition) = task

        assert(!node.kernel.isFinal(grammar))
        assert(cc.graph.nodes contains node)

        // nodeSymbolOpt에서 opt를 사용하는 것은 finish는 SequenceNode에 대해서도 실행되기 때문
        val newCondition = grammar.symbolOf(node.kernel.symbolId) match {
            case NLongest(_, _, longest) =>
                NotExists(node.kernel.beginGen, nextGen + 1, longest)
            case NExcept(_, _, _, except) =>
                Unless(node.kernel.beginGen, nextGen, except)
            case NJoin(_, _, _, join) =>
                OnlyIf(node.kernel.beginGen, nextGen, join)
            case NLookaheadIs(_, _, _, lookahead) =>
                Exists(nextGen, nextGen, lookahead)
            case NLookaheadExcept(_, _, _, lookahead) =>
                NotExists(nextGen, nextGen, lookahead)
            case _ => Always
        }
        val newKernel = {
            val kernel = node.kernel
            Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, nextGen)
        }
        val updatedNode = Node(newKernel, conjunct(node.condition, incomingCondition, newCondition))
        if (!(cc.graph.nodes contains updatedNode)) {
            val GraphTasksCont(graph1, newTasks) = addNode(GraphTasksCont(cc.graph, List()), updatedNode)

            // node로 들어오는 incoming edge 각각에 대해 newNode를 향하는 엣지를 추가한다
            val incomingEdges = cc.graph.edgesByEnd(node)
            val newGraph = incomingEdges.foldLeft(graph1) { (graph, edge) =>
                val newEdge = Edge(edge.start, updatedNode)
                graph.addEdge(newEdge)
            }

            // cc에 updatedNodes에 node -> updatedNode 추가
            val newUpdatedNodesMap = cc.updatedNodesMap + (node -> (cc.updatedNodesMap.getOrElse(node, Set()) + updatedNode))

            (Cont(newGraph, newUpdatedNodesMap), newTasks)
        } else {
            // 할 일 없음
            // 그런데 이런 상황 자체가 나오면 안되는건 아닐까?
            (cc, Seq())
        }
    }

    def process(nextGen: Int, task: Task, cc: Cont): (Cont, Seq[Task]) =
        task match {
            case task: DeriveTask => deriveTask(nextGen, task, cc)
            case task: ProgressTask => progressTask(nextGen, task, cc)
            case task: FinishTask => finishTask(nextGen, task, cc)
        }

    @tailrec final def rec(nextGen: Int, tasks: List[Task], cc: Cont): Cont =
        tasks match {
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                rec(nextGen, newTasks ++: rest, ncc)
            case List() => cc
        }

    // acceptable하지 않은 조건을 가진 기존의 노드에 대해서는 task를 진행하지 않는다
    def rec(nextGen: Int, tasks: List[Task], graph: Graph): Cont =
        rec(nextGen, tasks, Cont(graph, Map()))

    def finishableTermNodes(graph: Graph, nextGen: Int, input: Inputs.Input): Set[Node] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(NTerminal(_, terminal)) => terminal accept input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def finishableTermNodes(graph: Graph, nextGen: Int, input: Inputs.TermGroupDesc): Set[Node] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(NTerminal(_, terminal)) => terminal acceptTermGroup input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def termNodes(graph: Graph, nextGen: Int): Set[Node] = {
        graph.nodes filter { node =>
            val isTerminal = grammar.symbolOf(node.kernel.symbolId).isInstanceOf[NTerminal]
            (node.kernel.beginGen == nextGen) && isTerminal
        }
    }

    def processAcceptCondition(nextGen: Int, liftedGraph: Graph, conditionAccumulate: ConditionAccumulate): (ConditionAccumulate, Graph, Graph) = {
        // 2a. Evaluate accept conditions
        val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
            val conditions = (liftedGraph.nodes map { _.condition }) ++ conditionAccumulate.unfixed.values.toSet
            (conditions map { condition =>
                condition -> condition.evaluate(nextGen, liftedGraph)
            }).toMap
        }
        // 2b. ConditionAccumulate update
        val nextConditionAccumulate: ConditionAccumulate = {
            //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
            //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
            //                evaluated ++ newConditions // filter { _._2 != False }
            conditionAccumulate.update(conditionsEvaluations)
        }
        // 2c. Update accept conditions in graph
        val conditionUpdatedGraph = liftedGraph mapNode { node =>
            Node(node.kernel, conditionsEvaluations(node.condition))
        }
        // 2d. Remove never condition nodes
        val conditionFilteredGraph = conditionUpdatedGraph filterNode { _.condition != Never }

        (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph)
    }

    def trimUnreachables(graph: Graph, start: Node, ends: Set[Node]): Graph = {
        if (!(graph.nodes contains start)) {
            Graph(Set(), Set())
        } else {
            assert(ends subsetOf graph.nodes)
            val reachableFromStart = {
                def visit(queue: List[Node], cc: Set[Node]): Set[Node] =
                    queue match {
                        case node +: rest =>
                            val edgeReachables: Set[Node] = graph.edgesByStart(node) map { _.end }
                            val conditionReachables: Set[Node] = {
                                val potential = if (node.kernel.pointer != 0) Set() else
                                    grammar.symbolOf(node.kernel.symbolId) match {
                                        case NExcept(_, _, _, except) => Set(newNodeOf(except, node.kernel.beginGen))
                                        case NJoin(_, _, _, join) => Set(newNodeOf(join, node.kernel.beginGen))
                                        // lookahead는 항상 바로 progress되므로 conditionReachables에서 처리됨
                                        case _ => Set()
                                    }
                                (node.condition.nodes ++ potential) intersect graph.nodes
                            }
                            val reachables: Set[Node] = edgeReachables ++ conditionReachables
                            val newReachables = reachables -- cc
                            assert(newReachables subsetOf graph.nodes)
                            visit(newReachables.toSeq ++: rest, cc ++ newReachables)
                        case List() => cc
                    }
                visit(List(start), Set(start))
            }
            val reachableToEnds = {
                def visit(queue: List[Node], cc: Set[Node]): Set[Node] =
                    queue match {
                        case node +: rest =>
                            val reachables = graph.edgesByEnd(node) map { _.start }
                            val newReachables = reachables -- cc
                            visit(newReachables.toSeq ++: rest, cc ++ newReachables)
                        case List() => cc
                    }
                visit(ends.toList, ends)
            }
            val removing = graph.nodes -- (reachableFromStart intersect reachableToEnds)
            graph.removeNodes(removing)
        }
    }

    def trimGraph(graph: Graph, startNode: Node, nextGen: Int): Graph = {
        // 트리밍 - 사용이 완료된 터미널 노드/acceptCondition이 never인 지우기
        val trimmed1 = graph filterNode { node =>
            // TODO node.kernel.isFinished 인 노드도 지워도 될까?
            (node.condition != Never) && (!node.kernel.isFinal(grammar)) && (grammar.symbolOf(node.kernel.symbolId) match {
                case NTerminal(_, _) => node.kernel.beginGen == nextGen
                case _ => true
            })
        }
        // 2차 트리밍 - startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
        trimUnreachables(trimmed1, startNode, termNodes(trimmed1, nextGen))
    }
}
