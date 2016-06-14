package com.giyeok.jparser

import com.giyeok.jparser.Inputs.ConcreteInput
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingGraph.TermNode
import com.giyeok.jparser.ParsingGraph.Condition
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.ParsingGraph.Node

class NaiveParser[R <: ParseResult](grammar: Grammar, resultFunc: ParseResultFunc[R], derivationFunc: DerivationSliceFunc[R]) extends NewParser(grammar, resultFunc, derivationFunc) with DeriveTasks[R, CtxGraph[R]] {
    class NaiveParsingCtx(gen: Int, graph: Graph) extends ParsingCtx(gen, graph, Set()) {
        override def proceedDetail(input: ConcreteInput): (ParsingCtxTransition, Either[NaiveParsingCtx, ParsingError]) = {
            val nextGen = gen + 1

            // 1. No expansion
            val termFinishingTasks = graph.nodes collect {
                case node @ TermNode(term, _) if term accept input => FinishingTask(nextGen, node, TermResult(resultFunc.terminal(input)), Condition.True)
            }

            if (termFinishingTasks.isEmpty) {
                (ParsingCtxTransition(None, None, None), Right(UnexpectedInput(input)))
            } else {
                val expandTransition = ExpandTransition(s"Gen $gen > (1) - No Expansion", graph, graph, Set())

                // 2. Lift
                val liftedGraph = rec(termFinishingTasks.toList, graph.withNoResults.asInstanceOf[Graph])
                val liftTransition = LiftTransition(s"Gen $gen > (2) Lift", graph, liftedGraph, termFinishingTasks.asInstanceOf[Set[NewParser[R]#Task]], Set())

                // 3. Trimming
                val newTermNodes: Set[Node] = liftedGraph.nodes collect { case node @ TermNode(_, `nextGen`) => node }
                val trimmedGraph = liftedGraph.subgraphIn(Set(startNode) ++ liftedGraph.nodesInResultsAndProgresses, newTermNodes, resultFunc).asInstanceOf[Graph]
                val firstTrimmingTransition = TrimmingTransition(s"Gen $gen > (3) First Trimming", liftedGraph, trimmedGraph, startNode, newTermNodes)

                // 4. Revert
                val revertedGraph = ParsingCtx.revert(nextGen, trimmedGraph, trimmedGraph.results, trimmedGraph.nodes)
                val revertTransition = RevertTransition(s"Gen $gen > (4) Revert", trimmedGraph, revertedGraph, trimmedGraph, trimmedGraph.results)

                // 5. Second trimming
                val finalGraph = revertedGraph.subgraphIn(Set(startNode) ++ revertedGraph.nodesInResultsAndProgresses, newTermNodes, resultFunc).asInstanceOf[Graph]
                val secondTrimmingTransition = TrimmingTransition(s"Gen $gen > (5) Second Trimming", revertedGraph, finalGraph, startNode, newTermNodes)
                val nextContext = new NaiveParsingCtx(nextGen, finalGraph)
                val transition = ParsingCtxTransition(
                    Some(expandTransition, liftTransition),
                    Some(firstTrimmingTransition, revertTransition),
                    Some(secondTrimmingTransition))
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
