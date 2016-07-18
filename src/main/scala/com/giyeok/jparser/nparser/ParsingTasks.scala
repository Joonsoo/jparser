package com.giyeok.jparser.nparser

import ParsingContext._
import EligCondition._
import NGrammar._

trait ParsingTasks {
    val grammar: NGrammar

    sealed trait Task
    case class DeriveTask(node: Node) extends Task
    case class FinishTask(node: Node, condition: Condition, lastSymbol: Option[Int]) extends Task
    case class ProgressTask(node: SequenceNode, condition: Condition) extends Task

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Context): (Context, Seq[Task]) = {
        val DeriveTask(startNode) = task

        def nodeOf(symbolId: Int): Node =
            grammar.symbolOf(symbolId) match {
                case _: NAtomicSymbol => SymbolNode(symbolId, nextGen)
                case _: Sequence => SequenceNode(symbolId, 0, nextGen, nextGen)
            }
        def derive(symbolIds: Set[Int]): (Context, Seq[Task]) = {
            // (symbolIds에 해당하는 노드) + (startNode에서 symbolIds의 노드로 가는 엣지) 추가
            val destNodes = symbolIds map { nodeOf _ }
            // 새로 추가된 노드에 대한 DeriveTask 추가
            val newDeriveTasks = (destNodes -- cc.graph.nodes).toSeq map { DeriveTask(_) }
            // destNodes 중 cc.finishes에 있으면 finish 추가
            val immediateFinishTasks = destNodes.toSeq flatMap { destNode =>
                val destSymbolIdOpt = destNode match {
                    case SymbolNode(symbolId, _) => Some(symbolId)
                    case _: SequenceNode => None
                }
                cc.finishes.of(destNode).getOrElse(Set()) map { condition =>
                    FinishTask(startNode, condition, destSymbolIdOpt)
                }
            }

            val newGraph = destNodes.foldLeft(cc.graph) { (graph, node) => graph.addNode(node).addEdge(SimpleEdge(startNode, node)) }
            (cc.updateGraph(newGraph), newDeriveTasks ++ immediateFinishTasks)
        }
        startNode match {
            case SymbolNode(symbolId, _) =>
                grammar.nsymbols(symbolId) match {
                    case _: Terminal => (cc, Seq()) // nothing to do
                    case Start(produces) => derive(produces)
                    case Nonterminal(_, produces) => derive(produces)
                    case OneOf(_, produces) => derive(produces)
                    case Proxy(_, produce) => derive(Set(produce))
                    case Repeat(_, produces) => derive(produces)
                    case Join(_, body, join) =>
                        val bodyNode = nodeOf(body)
                        val joinNode = nodeOf(join)
                        val newDeriveTasks = (Set(bodyNode, joinNode) -- cc.graph.nodes).toSeq map { DeriveTask(_) }
                        val immediateFinishTasks = (cc.finishes.of(bodyNode), cc.finishes.of(joinNode)) match {
                            case (Some(bodyConditions), Some(joinConditions)) =>
                                bodyConditions.toSeq flatMap { b =>
                                    joinConditions map { j =>
                                        FinishTask(startNode, conjunct(b, j), None)
                                    }
                                }
                            case _ => Seq()
                        }
                        (cc.updateGraph(_.addNode(bodyNode).addNode(joinNode).addEdge(JoinEdge(startNode, bodyNode, joinNode))), immediateFinishTasks ++ newDeriveTasks)
                    case Except(_, body, except) => derive(Set(body, except))
                    case Longest(_, body) => derive(Set(body))
                    case EagerLongest(_, body) => derive(Set(body))
                    case lookahead: NLookaheadSymbol =>
                        val lookaheadNode = nodeOf(lookahead.lookahead)
                        val newDeriveTask = if (!(cc.graph.nodes contains lookaheadNode)) { Seq(DeriveTask(lookaheadNode)) } else Seq()
                        val emptyFinishTask = {
                            val condition: Condition = lookahead match {
                                case _: LookaheadIs => After(lookaheadNode, nextGen)
                                case _: LookaheadExcept => Until(lookaheadNode, nextGen)
                            }
                            FinishTask(startNode, condition, None)
                        }
                        (cc.updateGraph(_.addNode(lookaheadNode)), emptyFinishTask +: newDeriveTask)
                }
            case SequenceNode(symbolId, pointer, _, _) =>
                grammar.nsequences(symbolId) match {
                    case Sequence(_, Seq()) =>
                        assert(pointer == 0)
                        // 빈 sequence node는 즉각 finish task 만들기
                        (cc, Seq(FinishTask(startNode, True, None)))
                    case Sequence(_, sequence) =>
                        assert(pointer < sequence.length)
                        // sequence(pointer)로 symbol node 만들기
                        derive(Set(sequence(pointer)))
                }
        }
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Context): (Context, Seq[Task]) = ???
    def progressTask(nextGen: Int, task: FinishTask, cc: Context): (Context, Seq[Task]) = ???
}
