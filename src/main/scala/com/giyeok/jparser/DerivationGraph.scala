package com.giyeok.jparser

import Symbols._
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.AbstractInput
import com.giyeok.jparser.Inputs.ConcreteInput
import Symbols.Terminals._
import Inputs._
import ParsingGraph._
import com.giyeok.jparser.DGraph.BaseNode
import com.giyeok.jparser.DGraph.BaseNode

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
                rec((rest ++ newTasks).distinct, newCC)
            case List() => cc
        }

    def deriveAtomic(symbol: AtomicNonterm): DGraph[R] = {
        val baseNode = BaseNode(symbol, 0)
        rec(List(DeriveTask(0, baseNode)), DGraph(baseNode, Set(baseNode), Set(), Results(), Results()))
    }

    def deriveSequence(symbol: Sequence, pointer: Int): DGraph[R] = {
        val baseNode = BaseNode(symbol, pointer)
        rec(List(DeriveTask(0, baseNode)), DGraph(baseNode, Set(baseNode), Set(), Results(), Results()))
    }
}
