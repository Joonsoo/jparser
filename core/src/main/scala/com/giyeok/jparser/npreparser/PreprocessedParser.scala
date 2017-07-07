package com.giyeok.jparser.npreparser

import scala.annotation.tailrec
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate
import com.giyeok.jparser.nparser.Parser.Context
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.Parser.Transition
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.ParsingTasks

class DeriveTipsContext(gen: Int, nextGraph: Graph, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Graph], conditionAccumulate: ConditionAccumulate)
        extends Context(gen, nextGraph, _inputs, _history, conditionAccumulate) {
    // assert(deriveTips subsetOf nextGraph.nodes)
    def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, deriveTips: Set[Node], newInput: Input, newConditionAccumulate: ConditionAccumulate): DeriveTipsContext = {
        new DeriveTipsContext(nextGen, nextGraph, deriveTips, newInput +: _inputs, resultGraph +: _history, newConditionAccumulate)
    }
}

class PreprocessedParser(val grammar: NGrammar) extends Parser[DeriveTipsContext] with ParsingTasks with DerivationPreprocessor {
    // assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsContext = {
        val initial = sliceOf(grammar.startSymbol, 0)._1
        assert(initial.lifted.graph.nodes contains startNode)
        val conditionAccumulate = ConditionAccumulate((initial.lifted.graph.nodes map { _.condition } map { x => x -> x }).toMap)
        val initialGraph = Graph(Set(startNode), Set())
        new DeriveTipsContext(0, initialGraph, Set(startNode), List(), List(initial.lifted.graph), conditionAccumulate)
    }

    @tailrec private def recNoDerive(nextGen: Int, tasks: List[Task], cc: Cont, deriveTips: Set[Node]): (Cont, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip) +: rest if deriveTip.kernel.pointer > 0 =>
                recNoDerive(nextGen, rest, cc, deriveTips + deriveTip)
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                recNoDerive(nextGen, newTasks ++: rest, ncc, deriveTips)
            case List() => (cc, deriveTips)
        }

    def proceedDetail(ctx: DeriveTipsContext, input: Input): Either[(ProceedDetail, DeriveTipsContext), ParsingError] = {
        val (graph, gen, nextGen, deriveTips) = (ctx.nextGraph, ctx.gen, ctx.nextGen, ctx.deriveTips)
        // finishable term node를 포함한 deriveTip -> term node set
        val expandings = deriveTips flatMap { sliceOf(_, input) }
        if (expandings.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 1. graph에 expanding의 그래프들 추가, updatedNodes 병합, task들 병합
            val expandedGraph: Graph = {
                val (nodes, edges) = expandings.foldLeft((graph.nodes, graph.edges)) { (cc, preprocessed) =>
                    val (nodes, edges) = cc
                    (nodes ++ preprocessed.lifted.graph.nodes, edges ++ preprocessed.lifted.graph.edges)
                }
                Graph(nodes, edges)
            }
            val expandedTasks: List[ProgressTask] = expandings.foldLeft(List[ProgressTask]()) { (cc, preprocessed) =>
                cc ++ preprocessed.baseTasks
            }
            val expandedUpdatedNodes: Map[Node, Set[Node]] = expandings.foldLeft(Map[Node, Set[Node]]()) { (cc, preprocessed) =>
                preprocessed.lifted.updatedNodesMap.foldLeft(cc) { (cc, kv) =>
                    ??? // cc + (kv._1 -> (cc.getOrElse(kv._1, Set()) ++ kv._2))
                }
            }
            val expandedDeriveTips = expandings flatMap { _.nextDeriveTips }

            // 2. lift - expand의 결과로 나온 graph, updatedNodes, task로 lift - result graph
            val (Cont(liftedGraph, updatedNodes), deriveTips) =
                recNoDerive(nextGen, expandedTasks, Cont(expandedGraph, ??? /*expandedUpdatedNodes*/ ), expandedDeriveTips)

            println(s"nextGen=$nextGen ${deriveTips.size}")
            deriveTips foreach { println }

            // 3. accept condition 처리
            val (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph) =
                processAcceptCondition(nextGen, liftedGraph, ctx.conditionAccumulate)

            // 4. trimming
            val trimmedGraph: Graph = trimUnreachables(conditionFilteredGraph, startNode, deriveTips intersect conditionFilteredGraph.nodes)

            val nextContext = ctx.proceed(
                nextGen,
                resultGraph = liftedGraph, nextGraph = trimmedGraph,
                deriveTips = deriveTips,
                input, nextConditionAccumulate
            )

            Left((ProceedDetail(
                graph,
                Transition("expanded", expandedGraph),
                Transition("lifted(result)", liftedGraph, isResult = true),
                Transition("conditionUpdated", conditionUpdatedGraph),
                Transition("conditionFiltered", conditionFilteredGraph),
                Transition("trimmed(next)", trimmedGraph, isNext = true)
            ), nextContext))
        }
    }
}
