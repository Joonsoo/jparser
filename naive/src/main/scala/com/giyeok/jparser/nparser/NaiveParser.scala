package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NTerminal
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.Parser.{ConditionAccumulate, NaiveContext, _}
import com.giyeok.jparser.nparser.ParsingContext._

class NaiveParser(val grammar: NGrammar, val trim: Boolean = true) extends Parser[NaiveContext] with ParsingTasks {
    // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

    val initialContext: NaiveContext = {
        // TODO lift 제외하고 proceed할 때랑 동일하게 해야 하나?
        val Cont(graph, _) = rec(0, List(DeriveTask(startNode)), Graph(Set(startNode), Set()))
        val conditionsMap = (graph.nodes map { n => n.condition -> n.condition }).toMap

        val initialConditionAccumulate = ConditionAccumulate(conditionsMap)

        // 2. Accept condition 처리
        // 2a. Evaluate accept conditions
        val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
            (graph.nodes map { _.condition } map { condition =>
                condition -> condition.evaluate(0, graph)
            }).toMap
        }
        // 2b. ConditionAccumulate update
        val nextConditionAccumulate: ConditionAccumulate = {
            //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
            //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
            //                evaluated ++ newConditions // filter { _._2 != False }
            initialConditionAccumulate.update(conditionsEvaluations)
        }
        // 2c. Update accept conditions in graph
        val acceptConditionUpdatedGraph = graph mapNode { node =>
            Node(node.kernel, conditionsEvaluations(node.condition))
        } filterNode { _.condition != Never }

        // 3. Trimming
        val trimmedGraph: Graph =
            if (trim) trimGraph(acceptConditionUpdatedGraph, startNode, 0)
            else acceptConditionUpdatedGraph

        new NaiveContext(0, trimmedGraph, List(), List(graph), nextConditionAccumulate)
    }

    def proceedDetail(ctx: NaiveContext, input: Input): Either[(ProceedDetail, NaiveContext), ParsingError] = {
        val (graph, gen, nextGen) = (ctx.nextGraph, ctx.gen, ctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            val terms = graph.nodes flatMap {
                case Node(Kernel(symbolId, 0, `gen`, `gen`), _) =>
                    grammar.symbolOf(symbolId) match {
                        case nterm: NTerminal => Some(nterm.symbol)
                        case _ => None
                    }
                case _ => None
            }
            Right(UnexpectedInput(input, terms, nextGen))
        } else {
            // 1. 1차 lift
            val liftedGraph = rec(nextGen, termFinishes, graph).graph

            if (trim) {
                // 2. Accept condition 처리
                val (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph) =
                    processAcceptCondition(nextGen, liftedGraph, ctx.conditionAccumulate)

                // 3. Trimming
                val trimmedGraph: Graph = trimGraph(conditionFilteredGraph, startNode, nextGen)

                // trimmedGraph와 별개로 finish된 노드 정보를 전달해야 함
                //   - parse tree reconstruction할 때는 liftedGraph를 사용하고
                //   - 다음 generation 시작할 때는 trimmedGraph 사용
                val nextContext = ctx.proceed(
                    nextGen,
                    resultGraph = liftedGraph, nextGraph = trimmedGraph,
                    input, nextConditionAccumulate
                )

                Left((ProceedDetail(
                    graph,
                    Transition("lifted(result)", liftedGraph, isResult = true),
                    Transition("conditionUpdated", conditionUpdatedGraph),
                    Transition("conditionFiltered", conditionFilteredGraph),
                    Transition("trimmed(next)", trimmedGraph, isNext = true)
                ), nextContext))
            } else {
                val nextContext = ctx.proceed(
                    nextGen,
                    resultGraph = liftedGraph, nextGraph = liftedGraph,
                    input, ctx.conditionAccumulate
                )

                Left((ProceedDetail(
                    graph,
                    Transition("lifted(result)", liftedGraph, isResult = true, isNext = true)
                ), nextContext))
            }
        }
    }
}
