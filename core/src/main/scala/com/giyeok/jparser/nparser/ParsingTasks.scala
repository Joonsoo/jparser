package com.giyeok.jparser.nparser

import ParsingContext._
import AcceptCondition._
import NGrammar._
import com.giyeok.jparser.Inputs
import scala.annotation.tailrec

trait ParsingTasks {
    val grammar: NGrammar

    case class Cont(graph: Graph, updatedNodes: Map[Node, Set[Node]])

    sealed trait Task { val node: Node }
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node) extends Task
    case class ProgressTask(node: Node, condition: AcceptCondition) extends Task

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Cont): (Cont, Seq[Task]) = {
        val DeriveTask(startNode) = task

        def nodeOf(symbolId: Int): Node =
            Node(Kernel(symbolId, 0, nextGen, nextGen)(grammar.symbolOf(symbolId)), Always)

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
                        val bodyNodeTask = if (!(cc.graph.nodes contains bodyNode)) Some(if (bodyNode.kernel.isFinished) FinishTask(bodyNode) else DeriveTask(bodyNode)) else None
                        val joinNodeTask = if (!(cc.graph.nodes contains joinNode)) Some(if (joinNode.kernel.isFinished) FinishTask(joinNode) else DeriveTask(joinNode)) else None
                        // TODO cc.updatedNodes 보고 추가 처리
                        val newGraph = cc.graph.addNode(bodyNode).addNode(joinNode).addEdge(JoinEdge(startNode, bodyNode, joinNode))
                        (Cont(newGraph, cc.updatedNodes), Seq(bodyNodeTask, joinNodeTask).flatten)

                    case lookaheadSymbol: NLookaheadSymbol =>
                        val emptySeqNode = nodeOf(lookaheadSymbol.emptySeqId)
                        val lookaheadNode = nodeOf(lookaheadSymbol.lookahead)

                        // lookahead에 들어있는 내용은 emptyable일 수 없으므로 lookaheadNode는 isFinished이면 안됨
                        assert(!lookaheadNode.kernel.isFinished)

                        // TODO empty sequence 노드를 child로 만들고 이 조건 추가 코드는 progressTask로 옮기면 좋을까?

                        val newDeriveTask = if (!(cc.graph.nodes contains lookaheadNode)) { Seq(DeriveTask(lookaheadNode)) } else Seq()
                        val newGraph = cc.graph.addNode(lookaheadNode).addNode(emptySeqNode).addEdge(SimpleEdge(startNode, emptySeqNode))
                        val condition = lookaheadSymbol match {
                            case _: LookaheadIs => After(lookaheadNode, nextGen)
                            case _: LookaheadExcept => Until(lookaheadNode, nextGen)
                        }
                        (Cont(newGraph, cc.updatedNodes), ProgressTask(startNode, condition) +: newDeriveTask)
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

        val incomingEdges = cc.graph.edgesByDest(node)
        val chainTasks: Seq[Task] = incomingEdges.toSeq flatMap {
            case SimpleEdge(incoming, _) =>
                incoming.kernel.symbol match {
                    case Except(_, _, except) if except == node.kernel.symbolId => None
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
            case Some(_: Longest) => conjunct(node.condition, Until(node, nextGen))
            case Some(Except(_, _, except)) =>
                val exceptNode = Node(Kernel(except, 0, node.kernel.beginGen, node.kernel.beginGen)(grammar.symbolOf(except)), Always)
                conjunct(node.condition, Unless(exceptNode, nextGen))
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
                case Terminal(_) => node.kernel.beginGen == nextGen
                case _ => true
            })
        }
        // 2차 트리밍 - startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
        trimUnreachables(trimmed1, startNode, termNodes(trimmed1, nextGen))
    }
}
