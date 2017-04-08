package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate
import com.giyeok.jparser.nparser.Parser.NaiveContext
import com.giyeok.jparser.nparser.Parser._
import com.giyeok.jparser.nparser.ParsingContext._

class NaiveParser(val grammar: NGrammar) extends Parser[NaiveContext] with ParsingTasks {
    // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

    val initialContext: NaiveContext = {
        // TODO lift 제외하고 proceed할 때랑 동일하게 해야 하나?
        val Cont(graph, updatedNodes) = rec(0, List(DeriveTask(startNode)), Graph(Set(startNode), Set()))
        val conditionsMap = (graph.nodes map { n => n.condition -> n.condition }).toMap

        val initialConditionAccumulate = ConditionAccumulate(conditionsMap)

        // 2. Accept condition 처리
        // 2a. Evaluate accept conditions
        val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
            (graph.nodes map { _.condition } map { condition =>
                condition -> condition.evaluate(0, graph, updatedNodes)
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
        val trimmedGraph: Graph = trimGraph(acceptConditionUpdatedGraph, startNode, 0)

        new NaiveContext(0, trimmedGraph, List(), List(graph), updatedNodes, nextConditionAccumulate)
    }

    def proceedDetail(ctx: NaiveContext, input: Input): Either[(ProceedDetail, NaiveContext), ParsingError] = {
        val (graph, gen, nextGen) = (ctx.nextGraph, ctx.gen, ctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 1. 1차 lift
            val Cont(liftedGraph, updatedNodes) = rec(nextGen, termFinishes, graph)

            // 2. Accept condition 처리
            // 2a. Evaluate accept conditions
            val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
                val conditions = (liftedGraph.nodes map { _.condition }) ++ ctx.conditionAccumulate.unfixed.values.toSet
                (conditions map { condition =>
                    condition -> condition.evaluate(nextGen, liftedGraph, updatedNodes)
                }).toMap
            }
            // 2b. ConditionAccumulate update
            val nextConditionAccumulate: ConditionAccumulate = {
                //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
                //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
                //                evaluated ++ newConditions // filter { _._2 != False }
                ctx.conditionAccumulate.update(conditionsEvaluations)
            }
            // 2c. Update accept conditions in graph
            val acceptConditionUpdatedGraph = liftedGraph mapNode { node =>
                Node(node.kernel, conditionsEvaluations(node.condition))
            }
            // 2d. Remove never condition nodes
            val conditionFilteredGraph = acceptConditionUpdatedGraph filterNode { _.condition != Never }

            // 3. Trimming
            val trimmedGraph: Graph = trimGraph(conditionFilteredGraph, startNode, nextGen)

            // trimmedGraph와 별개로 finish된 노드 정보를 전달해야 함
            //   - parse tree reconstruction할 때는 liftedGraph를 사용하고
            //   - 다음 generation 시작할 때는 trimmedGraph 사용
            val nextContext = ctx.proceed(nextGen, resultGraph = liftedGraph, nextGraph = trimmedGraph, input, updatedNodes, nextConditionAccumulate)
            Left((ProceedDetail(
                graph,
                Transition("lifted", liftedGraph),
                Transition("conditionsUpdated", acceptConditionUpdatedGraph),
                Transition("conditionFiltered", conditionFilteredGraph),
                Transition("trimmed", trimmedGraph)
            ), nextContext))
        }
    }
}
