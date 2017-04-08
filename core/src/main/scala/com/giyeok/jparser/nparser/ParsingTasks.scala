package com.giyeok.jparser.nparser

import ParsingContext._
import AcceptCondition._
import NGrammar._
import com.giyeok.jparser.Inputs
import scala.annotation.tailrec
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate

trait ParsingTasks {
    val grammar: NGrammar

    case class Cont(graph: Graph, updatedNodes: Map[Node, Set[Node]])

    sealed trait Task { val node: Node }
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node) extends Task
    case class ProgressTask(node: Node, condition: AcceptCondition) extends Task

    private def nodeOf(symbolId: Int, beginGen: Int): Node =
        Node(Kernel(symbolId, 0, beginGen, beginGen)(grammar.symbolOf(symbolId)), Always)

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Cont): (Cont, Seq[Task]) = {
        val DeriveTask(startNode) = task

        def derive(symbolIds: Set[Int]): (Cont, Seq[Task]) = {
            // (symbolIds에 해당하는 노드) + (startNode에서 symbolIds의 노드로 가는 엣지) 추가
            val destNodes: Set[Node] = symbolIds map { nodeOf(_, nextGen) }

            // empty여서 isFinished인 것은 바로 FinishTask, 아니면 DeriveTask
            val newNodes: Set[Node] = destNodes -- cc.graph.nodes
            val newNodeTasks: Seq[Task] = newNodes.toSeq map {
                case node if node.kernel.isFinished => FinishTask(node)
                case node => DeriveTask(node)
            }

            // existedNodes 들에 대해서는 cc.updatedNodes보고 추가 처리
            def addUpdatedNodes(queue: List[Node], graph: Graph, tasks: Seq[Task]): (Graph, Seq[Task]) =
                queue match {
                    case destNode +: rest if destNode.kernel.isFinished =>
                        assert(!(cc.updatedNodes contains destNode))
                        val newTask = ProgressTask(startNode, destNode.condition)
                        addUpdatedNodes(rest, graph, newTask +: tasks)

                    case destNode +: rest =>
                        cc.updatedNodes get destNode match {
                            case Some(updatedNodes) =>
                                // println(s"updatedNodes: $startNode, $destNode -> $updatedNodes")
                                assert((updatedNodes map { _.kernel }).size == 1)
                                assert((updatedNodes map { _.kernel }).head.symbolId == destNode.kernel.symbolId)
                                val updatedGraph = updatedNodes.foldLeft(graph) { (graph, updatedNode) =>
                                    graph.addEdge(SimpleEdge(startNode, updatedNode))
                                }
                                addUpdatedNodes(updatedNodes.toList ++ rest, updatedGraph, tasks)
                            case None =>
                                addUpdatedNodes(rest, graph, tasks)
                        }

                    case List() => (graph, tasks)
                }

            val (newGraph, newTasks) = destNodes.foldLeft((cc.graph, newNodeTasks)) { (cc, destNode) =>
                val (graph, tasks) = cc
                addUpdatedNodes(List(destNode), graph.addNode(destNode).addEdge(SimpleEdge(startNode, destNode)), tasks)
            }

            (Cont(newGraph, cc.updatedNodes), newTasks)
        }

        startNode.kernel.symbol match {
            case symbol: NAtomicSymbol =>
                symbol match {
                    case _: NTerminal => (cc, Seq()) // nothing to do

                    case simpleDerivable: NSimpleDerivable =>
                        derive(simpleDerivable.produces)

                    case NJoin(_, body, join) =>
                        val bodyNode = nodeOf(body, nextGen)
                        val joinNode = nodeOf(join, nextGen)
                        val bodyNodeTask = if (!(cc.graph.nodes contains bodyNode)) Some(if (bodyNode.kernel.isFinished) FinishTask(bodyNode) else DeriveTask(bodyNode)) else None
                        val joinNodeTask = if (!(cc.graph.nodes contains joinNode)) Some(if (joinNode.kernel.isFinished) FinishTask(joinNode) else DeriveTask(joinNode)) else None
                        // TODO cc.updatedNodes 보고 추가 처리
                        val newGraph = cc.graph.addNode(bodyNode).addNode(joinNode).addEdge(JoinEdge(startNode, bodyNode, joinNode))
                        (Cont(newGraph, cc.updatedNodes), Seq(bodyNodeTask, joinNodeTask).flatten)

                    case lookaheadSymbol: NLookaheadSymbol =>
                        val lookaheadNode = nodeOf(lookaheadSymbol.lookahead, nextGen)

                        val (Cont(graph0, updatedNodes), tasks0) = derive(Set(lookaheadSymbol.emptySeqId))

                        val tasks = if (!(graph0.nodes contains lookaheadNode)) DeriveTask(lookaheadNode) +: tasks0 else tasks0
                        val graph = graph0.addNode(lookaheadNode)

                        (Cont(graph, updatedNodes), tasks)
                }
            case NSequence(_, seq) =>
                assert(seq.nonEmpty) // empty인 sequence는 derive시점에 모두 처리되어야 함
                assert(startNode.kernel.pointer < seq.length) // node의 pointer는 sequence의 length보다 작아야 함
                derive(Set(seq(startNode.kernel.pointer)))
        }
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Cont): (Cont, Seq[Task]) = {
        val FinishTask(node) = task

        assert(node.kernel.isFinished)

        val incomingEdges = cc.graph.edgesByDest(node)
        val chainTasks: Seq[Task] = incomingEdges.toSeq flatMap {
            case SimpleEdge(incoming, _) =>
                incoming.kernel.symbol match {
                    case NExcept(_, _, except) if except == node.kernel.symbolId => None
                    case _ => Some(ProgressTask(incoming, node.condition))
                }
            case JoinEdge(start, `node`, other) if other.kernel.isFinished =>
                Some(ProgressTask(start, conjunct(node.condition, other.condition)))
            case JoinEdge(start, other, `node`) if other.kernel.isFinished =>
                Some(ProgressTask(start, conjunct(node.condition, other.condition)))
            case _: JoinEdge => None
        }
        (cc, chainTasks)
    }

    def progressTask(nextGen: Int, task: ProgressTask, cc: Cont): (Cont, Seq[Task]) = {
        val ProgressTask(node, taskCondition) = task
        // assert(cc.graph.nodes contains node)

        // nodeSymbolOpt에서 opt를 사용하는 것은 finish는 SequenceNode에 대해서도 실행되기 때문
        val nodeSymbolOpt = grammar.nsymbols get node.kernel.symbolId
        val chainCondition = nodeSymbolOpt match {
            case Some(NLongest(_, longest)) =>
                val longestNode = nodeOf(longest, node.kernel.beginGen)
                conjunct(node.condition, Until(longestNode, nextGen))
            case Some(NExcept(_, _, except)) =>
                val exceptNode = nodeOf(except, node.kernel.beginGen)
                conjunct(node.condition, Unless(exceptNode, nextGen))
            case Some(NLookaheadIs(_, _, lookahaed)) =>
                conjunct(node.condition, After(nodeOf(lookahaed, nextGen), nextGen))
            case Some(NLookaheadExcept(_, _, lookahaed)) =>
                conjunct(node.condition, Until(nodeOf(lookahaed, nextGen), nextGen))
            case _ => node.condition
        }
        val newKernel = {
            val kernel = node.kernel
            Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, nextGen)(kernel.symbol)
        }
        val updatedNode = Node(newKernel, conjunct(node.condition, taskCondition, chainCondition))
        if (!(cc.graph.nodes contains updatedNode)) {
            // node로 들어오는 incoming edge 각각에 대해 newNode를 향하는 엣지를 추가한다
            val incomingEdges = cc.graph.edgesByDest(node)
            val newGraph = incomingEdges.foldLeft(cc.graph.addNode(updatedNode)) { (graph, edge) =>
                val newEdge = edge match {
                    case SimpleEdge(start, _) => SimpleEdge(start, updatedNode)
                    case JoinEdge(start, `node`, join) => JoinEdge(start, updatedNode, join)
                    case JoinEdge(start, end, `node`) => JoinEdge(start, end, updatedNode)
                    case _ => ??? // should not happen
                }
                graph.addEdge(newEdge)
            }

            // cc에 updatedNodes에 node -> updatedNode 추가
            val newUpdatedNodes = cc.updatedNodes + (node -> (cc.updatedNodes.getOrElse(node, Set()) + updatedNode))

            val newTasks = if (updatedNode.kernel.isFinished) {
                Seq(FinishTask(updatedNode))
            } else {
                Seq(DeriveTask(updatedNode))
            }
            (Cont(newGraph, newUpdatedNodes), newTasks)
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
                case Some(NTerminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def finishableTermNodes(graph: Graph, nextGen: Int, input: Inputs.TermGroupDesc): Set[Node] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(NTerminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def termNodes(graph: Graph, nextGen: Int): Set[Node] = {
        graph.nodes filter { node =>
            val isTerminal = node.kernel.symbol.isInstanceOf[NTerminal]
            (node.kernel.beginGen == nextGen) && isTerminal
        }
    }

    def processAcceptCondition(nextGen: Int, liftedGraph: Graph, updatedNodes: Map[Node, Set[Node]], conditionAccumulate: ConditionAccumulate): (ConditionAccumulate, Graph, Graph) = {
        // 2a. Evaluate accept conditions
        val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
            val conditions = (liftedGraph.nodes map { _.condition }) ++ conditionAccumulate.unfixed.values.toSet
            (conditions map { condition =>
                condition -> condition.evaluate(nextGen, liftedGraph, updatedNodes)
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
                            val reachables = (graph.edgesByStart(node) flatMap {
                                case SimpleEdge(_, end) => Set(end)
                                case JoinEdge(_, end, join) => Set(end, join)
                            }) ++ (node.condition.nodes intersect graph.nodes)
                            val newReachables = reachables -- cc
                            visit(newReachables.toSeq ++: rest, cc ++ newReachables)
                        case List() => cc
                    }
                visit(List(start), Set(start))
            }
            val reachableToEnds = {
                def visit(queue: List[Node], cc: Set[Node]): Set[Node] =
                    queue match {
                        case node +: rest =>
                            val reachables = graph.edgesByDest(node) collect {
                                case SimpleEdge(start, _) => start
                                case JoinEdge(start, end, join) if (cc contains end) && (cc contains join) => start
                            }
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
            (node.condition != Never) && (!node.kernel.isFinished) && (node.kernel.symbol match {
                case NTerminal(_) => node.kernel.beginGen == nextGen
                case _ => true
            })
        }
        // 2차 트리밍 - startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
        trimUnreachables(trimmed1, startNode, termNodes(trimmed1, nextGen))
    }
}
