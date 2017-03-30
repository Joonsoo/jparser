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

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Graph): (Graph, Seq[Task]) = {
        val DeriveTask(startNode) = task

        def nodeOf(symbolId: Int): Node =
            Node(Kernel(symbolId, 0, nextGen, nextGen)(grammar.symbolOf(symbolId)), Always)

        def derive(symbolIds: Set[Int]): (Graph, Seq[Task]) = {
            // (symbolIds에 해당하는 노드) + (startNode에서 symbolIds의 노드로 가는 엣지) 추가
            val destNodes: Set[Node] = symbolIds map nodeOf
            val existedNodes: Set[Node] = destNodes intersect cc.nodes
            val newNodes: Set[Node] = destNodes -- cc.nodes

            val newGraph = destNodes.foldLeft(cc) { (graph, node) =>
                graph.addNode(node).addEdge(SimpleEdge(startNode, node))
            }
            // empty여서 isFinished인 것은 바로 FinishTask, 아니면 DeriveTask
            val newTasks: Seq[Task] = newNodes.toSeq map {
                case node if node.kernel.isFinished => FinishTask(node)
                case node => DeriveTask(node)
            }
            // TODO existedNodes 들에 대해서는 cc.updatedNodes보고 추가 처리
            (newGraph, newTasks)
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
                        // TODO cc.updatedNodes보고 추가 처리
                        (cc.addNode(bodyNode).addNode(joinNode).addEdge(JoinEdge(startNode, bodyNode, joinNode)), Seq(bodyNodeTask, joinNodeTask))

                    case lookaheadSymbol: NLookaheadSymbol =>
                        val lookaheadNode = nodeOf(lookaheadSymbol.lookahead)
                        // lookahead에 들어있는 내용은 emptyable일 수 없으므로 lookaheadNode는 isFinished이면 안됨
                        assert(!lookaheadNode.kernel.isFinished)
                        val newDeriveTask = if (!(cc.nodes contains lookaheadNode)) { Seq(DeriveTask(lookaheadNode)) } else Seq()
                        val condition: AcceptCondition = lookaheadSymbol match {
                            case _: LookaheadIs => After(lookaheadNode, nextGen)
                            case _: LookaheadExcept => Until(lookaheadNode, nextGen)
                        }
                        ProgressTask(lookaheadNode, condition)
                        // TODO finishedLookaheadNode 추가하는 부분은 이제 progress task로 하면 되는 거 아닌가?
                        val finishedLookaheadNode = Node(lookaheadNode.kernel.proceed(nextGen), condition)
                        assert(finishedLookaheadNode.kernel.isFinished)
                        (updateNode(cc.addNode(lookaheadNode), lookaheadNode, finishedLookaheadNode), FinishTask(finishedLookaheadNode) +: newDeriveTask)
                }
            case Sequence(_, seq) =>
                if (seq.isEmpty) {
                    println(startNode)
                    println()
                }
                assert(seq.nonEmpty) // empty인 sequence는 derive시점에 모두 처리되어야 함
                assert(startNode.kernel.pointer < seq.length) // node의 pointer는 sequence의 length보다 작아야 함
                derive(Set(seq(startNode.kernel.pointer)))
        }
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Graph): (Graph, Seq[Task]) = {
        val FinishTask(node) = task

        assert(node.kernel.isFinished)

        // nodeSymbolOpt에서 opt를 사용하는 것은 finish는 SequenceNode에 대해서도 실행되기 때문
        val nodeSymbolOpt = grammar.nsymbols get node.kernel.symbolId
        val chainCondition = nodeSymbolOpt match {
            case Some(_: Longest) => conjunct(node.condition, Until(node, nextGen))
            case Some(_: EagerLongest) => conjunct(node.condition, Alive(node, nextGen))
            case _ => node.condition
        }
        val incomingEdges = cc.edgesByDest(node)
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

    def progressTask(nextGen: Int, task: ProgressTask, cc: Graph): (Graph, Seq[Task]) = {
        val ProgressTask(node, condition) = task
        // assert(cc.graph.nodes contains node)

        val updatedCondition = conjunct(node.condition, condition)
        val updatedNode = Node(node.kernel.proceed(nextGen), updatedCondition)
        if (cc.nodes contains updatedNode) {
            // 할 일 없을듯?
            //                if (cc.progresses contains (updatedNode, updatedConditions)) {
            //                    (cc.updateFinishes(newFinishes), Seq())
            //                } else {
            //                    (cc.updateProgresses(newProgresses).updateFinishes(newFinishes), Seq(DeriveTask(updatedNode)))
            //                }
            ???
        } else {
            // TODO cc에 updatedNodes에 node -> updatedNode 추가
            val newGraph = updateNode(cc, node, updatedNode)
            val newTasks = if (updatedNode.kernel.isFinished) {
                // TODO finish task 만들기
                //            val updatedConditions = cc.progresses.of(node).get map { conjunct(_, condition) }
                //            (cc, updatedConditions.toSeq map { FinishTask(node, _, None) })
                Seq(FinishTask(updatedNode))
            } else {
                Seq(DeriveTask(updatedNode))
            }
            (newGraph, newTasks)
        }
    }

    // node로 들어오는 incoming edge 각각에 대해 newNode를 향하는 엣지를 추가한다
    def updateNode(graph: Graph, node: Node, newNode: Node): Graph = {
        val incomingEdges = graph.edgesByDest(node)
        incomingEdges.foldLeft(graph.addNode(newNode)) { (graph, edge) =>
            val newEdge = edge match {
                case SimpleEdge(start, _) => SimpleEdge(start, newNode)
                case JoinEdge(start, `node`, join) => JoinEdge(start, newNode, join)
                case JoinEdge(start, end, `node`) => JoinEdge(start, end, newNode)
                case _ => ??? // should not happen
            }
            graph.addEdge(newEdge)
        }
    }

    def process(nextGen: Int, task: Task, cc: Graph): (Graph, Seq[Task]) =
        task match {
            case task: DeriveTask => deriveTask(nextGen, task, cc)
            case task: ProgressTask => progressTask(nextGen, task, cc)
            case task: FinishTask => finishTask(nextGen, task, cc)
        }

    @tailrec final def rec(nextGen: Int, tasks: List[Task], cc: Graph): Graph =
        tasks match {
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                rec(nextGen, newTasks ++: rest, ncc)
            case List() => cc
        }

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
    def updateAcceptableCondition(nextGen: Int, graph: Graph): Graph = {
        // TODO graph에서 각 node별로 acceptable condition을 업데이트한다
        graph
        //        val newFinishes = context.finishes.mapCondition(_.evaluate(nextGen, finishes, survived)).trimFalse._1
        //        val (newProgresses, falseNodes) = context.progresses.mapCondition(_.evaluate(nextGen, finishes, survived)).trimFalse
        //        context.updateFinishes(newFinishes).updateProgresses(newProgresses).updateGraph(_.removeNodes(falseNodes.asInstanceOf[Set[Node]]))
    }
}
