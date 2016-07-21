package com.giyeok.jparser.deprecated

import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.deprecated.ParsingGraph.TermNode
import com.giyeok.jparser.deprecated.ParsingGraph.Condition
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.deprecated.ParsingGraph.Node
import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultFunc
import com.giyeok.jparser.Grammar

class NaiveParser[R <: ParseResult](grammar: Grammar, resultFunc: ParseResultFunc[R], derivationFunc: DerivationSliceFunc[R]) extends NewParser(grammar, resultFunc, derivationFunc) with DeriveTasks[R, CtxGraph[R]] {
    class NaiveParsingCtx(gen: Int, graph: Graph) extends ParsingCtx(gen, graph, Set()) {
        override def proceedDetail(input: ConcreteInput): (ParsingCtxTransition, Either[NaiveParsingCtx, ParsingError]) = {
            val nextGen = gen + 1

            // 1. No expansion
            val termFinishingTasks = graph.nodes collect {
                case node @ TermNode(term, _) if term accept input => FinishingTask(nextGen, node, TermResult(resultFunc.terminal(gen, input)), Condition.True)
            }

            if (termFinishingTasks.isEmpty) {
                (ParsingCtxTransition(None, None), Right(UnexpectedInput(input)))
            } else {
                val expandTransition = ExpandTransition(s"Gen $gen > (1) - No Expansion", graph, graph, Set())

                // 2. Lift
                val liftedGraph = rec(termFinishingTasks.toList, graph.withNoResults.asInstanceOf[Graph])
                val liftTransition = LiftTransition(s"Gen $gen > (2) Lift", graph, liftedGraph, termFinishingTasks.asInstanceOf[Set[NewParser[R]#Task]], Set())

                // 3. Trimming
                val newTermNodes: Set[Node] = liftedGraph.nodes collect { case node @ TermNode(_, `nextGen`) => node }
                val trimmingStartNodes = Set(startNode) ++ liftedGraph.nodesInResultsAndProgresses
                val trimmedGraph = liftedGraph.subgraphIn(trimmingStartNodes, newTermNodes, resultFunc).asInstanceOf[Graph]
                val firstTrimmingTransition = TrimmingTransition(s"Gen $gen > (3) First Trimming", liftedGraph, trimmedGraph, trimmingStartNodes, newTermNodes)

                // 4. Revert
                val revertedGraph = ParsingCtx.revert(nextGen, trimmedGraph, trimmedGraph.results, trimmedGraph.nodes)
                val revertTransition = RevertTransition(s"Gen $gen > (4) Revert", trimmedGraph, revertedGraph, trimmedGraph, trimmedGraph.results)

                val nextContext = new NaiveParsingCtx(nextGen, revertedGraph)
                val transition = ParsingCtxTransition(
                    Some(expandTransition, liftTransition),
                    Some(firstTrimmingTransition, revertTransition))
                (transition, Left(nextContext))
            }
        }

        override def proceed(input: ConcreteInput): Either[ParsingCtx, ParsingError] = proceedDetail(input)._2
    }

    def process(task: Task, cc: CtxGraph[R]): (CtxGraph[R], Seq[Task]) = {
        task match {
            case task: DeriveTask => deriveTask(task, cc)
            case task: FinishingTask => finishingTask(task, cc)
            case task: SequenceProgressTask => sequenceProgressTask(task, cc)
        }
    }

    def rec(tasks: List[Task], cc: CtxGraph[R]): CtxGraph[R] =
        tasks match {
            case task +: rest =>
                val (newCC, newTasks) = process(task, cc)
                rec((newTasks.toList ++ rest).distinct, newCC)
            case List() => cc
        }

    override val initialContext = {
        val initialGraph = rec(List(DeriveTask(0, startNode)), CtxGraph(Set(startNode), Set(), Results(), Results()))
        new NaiveParsingCtx(0, initialGraph)
    }
}