package com.giyeok.jparser

import Symbols._
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.AbstractInput
import com.giyeok.jparser.Inputs.ConcreteInput
import Symbols.Terminals._
import Inputs._
import ParsingGraph._
import com.giyeok.jparser.DGraph.BaseNode
import com.giyeok.jparser.DGraph.BaseAtomicNode
import com.giyeok.jparser.DGraph.BaseSequenceNode

object DGraph {
    sealed trait BaseNode
    class BaseAtomicNode(_symbol: AtomicNonterm) extends AtomicNode(_symbol, 0)(None, None) with BaseNode
    class BaseSequenceNode(_symbol: Sequence, _pointer: Int) extends SequenceNode(_symbol, _pointer, 0, 0) with BaseNode
}

case class DGraph[R <: ParseResult](
        baseNode: DGraph.BaseNode with Node,
        nodes: Set[Node],
        edges: Set[Edge],
        results: Results[Node, R],
        progresses: Results[SequenceNode, R]) extends ParsingGraph[R] with TerminalInfo[R] {
    // baseNode가 nodes에 포함되어야 함
    assert(nodes contains baseNode.asInstanceOf[Node])
    // baseNode를 제외하고는 전부 BaseNode가 아니어야 함
    assert((nodes - baseNode.asInstanceOf[Node]) forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })

    // DerivationGraph에 등장하는 모든 gen이 0이어야 한다
    assert(nodes forall {
        case EmptyNode | TermNode(_) => true
        case _: DGraph.BaseNode => true
        case AtomicNode(_, beginGen) => beginGen == 0
        case SequenceNode(_, _, beginGen, endGen) => beginGen == 0 && endGen == 0
    })

    // Information Retrieval
    def edgesFromBaseNode = edges filter {
        case SimpleEdge(start, _, _) => start == baseNode
        case JoinEdge(start, _, _) =>
            // 이런 경우는 일반적으로 발생하진 않아야 함(visualize나 test시에만 발생 가능)
            start == baseNode
    }
    def edgesNotFromBaseNode = edges -- edgesFromBaseNode

    // Modification
    def create(nodes: Set[Node], edges: Set[Edge], results: Results[Node, R], progresses: Results[SequenceNode, R]): DGraph[R] =
        DGraph(baseNode, nodes, edges, results, progresses)

    def shiftGen(gen: Int): DGraph[R] = {
        def shiftNode[T <: Node](node: T): T = node match {
            case n: AtomicNode => (AtomicNode(n.symbol, n.beginGen + gen)(n.liftBlockTrigger map { shiftNode _ }, n.reservedReverterType)).asInstanceOf[T]
            case n: SequenceNode => SequenceNode(n.symbol, n.pointer, n.beginGen + gen, n.endGen + gen).asInstanceOf[T]
            case n => n
        }
        def shiftTrigger(trigger: Trigger): Trigger = Trigger(shiftNode(trigger.node), trigger.triggerType)
        val shiftedNodes: Set[Node] = nodes map { shiftNode _ }
        val shiftedEdges: Set[Edge] = edges map {
            case SimpleEdge(start, end, revertTriggers) => SimpleEdge(shiftNode(start), shiftNode(end), revertTriggers map { shiftTrigger _ })
            case JoinEdge(start, end, join) => JoinEdge(shiftNode(start), shiftNode(end), shiftNode(join))
        }
        val shiftedResults = results mapTo { (node, triggers, result) =>
            (shiftNode(node), triggers map { shiftTrigger _ }, result)
        }
        val shiftedProgresses = progresses mapTo { (node, triggers, result) =>
            (shiftNode(node), triggers map { shiftTrigger _ }, result)
        }
        // baseNode는 shiftGen 할 필요 없음
        DGraph(baseNode, shiftedNodes, shiftedEdges, shiftedResults, shiftedProgresses)
    }

    // Misc.
    def sliceByTermGroups(resultFunc: ParseResultFunc[R]): Map[TermGroupDesc, Option[DGraph[R]]] = {
        (termGroups map { termGroup =>
            val eligibleTerminalNodes = (terminalNodes filter { _.symbol.accept(termGroup) }).toSet[Node]
            termGroup -> (subgraphIn(baseNode, eligibleTerminalNodes, resultFunc) map { _.asInstanceOf[DGraph[R]] })
        }).toMap
    }

    //    def subgraphTo(termGroup: TermGroupDesc): Option[DGraph[R]] =
    //        sliceByTermGroups(termGroup) ensuring (termGroups contains termGroup)
    //    def subgraphTo(input: ConcreteInput): Option[DGraph[R]] =
    //        termGroups find { _.contains(input) } flatMap { subgraphTo(_) }

    // TODO def compaction: DGraph[R] = ???
}

class DerivationFunc[R <: ParseResult](val grammar: Grammar, val resultFunc: ParseResultFunc[R])
        extends LiftTasks[R, DGraph[R]] with DeriveTasks[R, DGraph[R]] {
    // cc에서 task를 처리해서
    // (변경된 cc, 새로 추가되어야 하는 task)를 반환한다
    // - 나중에 visualize할 수 있게 하려고 process/rec 분리했음
    def process(task: Task, cc: DGraph[R]): (DGraph[R], Seq[Task]) = {
        task match {
            case task: DeriveTask => deriveTask(task, cc)
            case task: FinishingTask => finishingTask(task, cc)
            case task: SequenceProgressTask => sequenceProgressTask(task, cc)
        }
    }

    def rec(tasks: List[Task], cc: DGraph[R]): DGraph[R] =
        tasks match {
            case task +: rest =>
                println(task)
                val (newCC, newTasks) = process(task, cc)
                rec((newTasks.toList ++ rest).distinct, newCC)
            case List() => cc
        }

    def deriveAtomic(symbol: AtomicNonterm): DGraph[R] = {
        val baseNode = new BaseAtomicNode(symbol)
        rec(List(DeriveTask(0, baseNode)), DGraph(baseNode, Set(baseNode), Set(), Results(), Results()))
    }

    def deriveSequence(symbol: Sequence, pointer: Int): DGraph[R] = {
        val baseNode = new BaseSequenceNode(symbol, pointer)
        rec(List(DeriveTask(0, baseNode)), DGraph(baseNode, Set(baseNode), Set(), Results(), Results(baseNode -> Map(Set[Trigger]() -> resultFunc.sequence()))))
    }
}
