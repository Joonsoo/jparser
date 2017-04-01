package com.giyeok.jparser.nparser

import ParsingContext._
import AcceptCondition._
import NGrammar._
import com.giyeok.jparser.Inputs
import scala.annotation.tailrec

trait ParsingTasks {
    val grammar: NGrammar

    case class Cont(graph: Graph, updatedNodes: Map[Node, Set[Node]])

    sealed trait Task
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node) extends Task
    case class ProgressTask(node: Node, condition: AcceptCondition) extends Task

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Cont): (Cont, Seq[Task]) = {
        val DeriveTask(startNode) = task

        def nodeOf(symbolId: Int): Node = {
            val symbol = grammar.symbolOf(symbolId)
            val condition = symbol match {
                case Except(_, _, except) => Unless(nodeOf(except))
                case _ => Always
            }
            Node(Kernel(symbolId, 0, nextGen, nextGen)(symbol), conjunct(condition))
        }

        def derive(symbolIds: Set[Int]): (Cont, Seq[Task]) = {
            // (symbolIds에 해당하는 노드) + (startNode에서 symbolIds의 노드로 가는 엣지) 추가
            val destNodes: Set[Node] = symbolIds map nodeOf

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
                    case _: Terminal => (cc, Seq()) // nothing to do

                    case simpleDerivable: NSimpleDerivable =>
                        derive(simpleDerivable.produces)

                    case Join(_, body, join) =>
                        val bodyNode = nodeOf(body)
                        val joinNode = nodeOf(join)
                        val bodyNodeTask = if (bodyNode.kernel.isFinished) FinishTask(bodyNode) else DeriveTask(bodyNode)
                        val joinNodeTask = if (joinNode.kernel.isFinished) FinishTask(joinNode) else DeriveTask(joinNode)
                        // TODO cc.updatedNodes 보고 추가 처리
                        val newGraph = cc.graph.addNode(bodyNode).addNode(joinNode).addEdge(JoinEdge(startNode, bodyNode, joinNode))
                        (Cont(newGraph, cc.updatedNodes), Seq(bodyNodeTask, joinNodeTask))

                    case lookaheadSymbol: NLookaheadSymbol =>
                        val lookaheadNode = nodeOf(lookaheadSymbol.lookahead)
                        // lookahead에 들어있는 내용은 emptyable일 수 없으므로 lookaheadNode는 isFinished이면 안됨
                        assert(!lookaheadNode.kernel.isFinished)
                        val newDeriveTask = if (!(cc.graph.nodes contains lookaheadNode)) { Seq(DeriveTask(lookaheadNode)) } else Seq()
                        val condition: AcceptCondition = lookaheadSymbol match {
                            case _: LookaheadIs => After(lookaheadNode, nextGen)
                            case _: LookaheadExcept => Until(lookaheadNode, nextGen)
                        }
                        val newGraph = cc.graph.addNode(lookaheadNode)
                        (Cont(newGraph, cc.updatedNodes), ProgressTask(lookaheadNode, condition) +: newDeriveTask)
                }
            case Sequence(_, seq) =>
                assert(seq.nonEmpty) // empty인 sequence는 derive시점에 모두 처리되어야 함
                assert(startNode.kernel.pointer < seq.length) // node의 pointer는 sequence의 length보다 작아야 함
                derive(Set(seq(startNode.kernel.pointer)))
        }
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Cont): (Cont, Seq[Task]) = {
        val FinishTask(node) = task

        assert(node.kernel.isFinished)

        // nodeSymbolOpt에서 opt를 사용하는 것은 finish는 SequenceNode에 대해서도 실행되기 때문
        val nodeSymbolOpt = grammar.nsymbols get node.kernel.symbolId
        val chainCondition = nodeSymbolOpt match {
            case Some(_: Longest) => conjunct(node.condition, Until(node, nextGen))
            case Some(_: EagerLongest) => conjunct(node.condition, Alive(node, nextGen))
            case _ => node.condition
        }
        val incomingEdges = cc.graph.edgesByDest(node)
        val chainTasks: Seq[Task] = incomingEdges.toSeq flatMap {
            case SimpleEdge(incoming, _) =>
                incoming.kernel.symbol match {
                    case Except(_, _, except) if except == node.kernel.symbolId => None
                    case _ => Some(ProgressTask(incoming, chainCondition))
                }
            case JoinEdge(start, `node`, other) if other.kernel.isFinished =>
                Some(ProgressTask(start, conjunct(chainCondition, other.condition)))
            case JoinEdge(start, other, `node`) if other.kernel.isFinished =>
                Some(ProgressTask(start, conjunct(chainCondition, other.condition)))
        }
        (cc, chainTasks)
    }

    def progressTask(nextGen: Int, task: ProgressTask, cc: Cont): (Cont, Seq[Task]) = {
        val ProgressTask(node, condition) = task
        // assert(cc.graph.nodes contains node)

        val updatedCondition = conjunct(node.condition, condition)
        val updatedNode = Node(node.kernel.proceed(nextGen), updatedCondition)
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

    def rec(nextGen: Int, tasks: List[Task], graph: Graph): Cont =
        rec(nextGen, tasks, Cont(graph, Map()))

    def finishableTermNodes(graph: Graph, nextGen: Int, input: Inputs.Input): Set[Node] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(Terminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }
    def finishableTermNodes(graph: Graph, nextGen: Int, input: Inputs.TermGroupDesc): Set[Node] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(Terminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.nodes collect {
            case node @ Node(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }
    def termNodes(graph: Graph, nextGen: Int): Set[Node] = {
        graph.nodes filter { node =>
            val isTerminalSymbol = grammar.nsymbols get node.kernel.symbolId match {
                case Some(Terminal(_)) => true
                case _ => false
            }
            (node.kernel.beginGen == nextGen) && isTerminalSymbol
        }
    }
    def trim(graph: Graph, starts: Set[Node], ends: Set[Node]): Graph = {
        // TODO finish된 노드는 기본적으로 제거하기
        assert((starts subsetOf graph.nodes) && (ends subsetOf graph.nodes))
        val reachableFromStart = {
            def visit(queue: List[Node], cc: Set[Node]): Set[Node] =
                queue match {
                    case node +: rest =>
                        val reachables = graph.edgesByStart(node) flatMap {
                            case SimpleEdge(_, end) => Set(end)
                            case JoinEdge(_, end, join) => Set(end, join)
                        }
                        val newReachables = reachables -- cc
                        visit(newReachables.toSeq ++: rest, cc ++ newReachables)
                    case List() => cc
                }
            visit(starts.toList, starts)
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
