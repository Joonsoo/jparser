package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.NGrammar.Terminal
import com.giyeok.jparser.nparser.Parser.ConditionFate
import com.giyeok.jparser.nparser.Parser.NaiveContext
import com.giyeok.jparser.nparser.Parser._
import com.giyeok.jparser.nparser.ParsingContext._

class NaiveParser(val grammar: NGrammar) extends Parser[NaiveContext] with ParsingTasks {
    // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

    val initialContext: NaiveContext = {
        val cc = rec(0, List(DeriveTask(startNode)), Graph(Set(startNode), Set()))
        val acceptConditions = evaluateAcceptConditions(0, cc.graph.nodes map { _.condition }, cc.graph, cc.updatedNodes)
        new NaiveContext(0, cc.graph, cc.graph.nodes, List(), List(), ConditionFate(acceptConditions))
    }

    def proceedDetail(ctx: NaiveContext, input: Input): Either[(ProceedDetail, NaiveContext), ParsingError] = {
        val (graph, gen, nextGen) = (ctx.graph, ctx.gen, ctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // No Expansion
            // 2. Lift
            val Cont(liftedGraph, updatedNodes) = rec(nextGen, termFinishes, graph)

            // 3. Evaluate accept conditions
            val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] =
                evaluateAcceptConditions(nextGen, liftedGraph.nodes map { _.condition }, liftedGraph, updatedNodes)
            val acceptableConditions = liftedGraph.nodes map { _.condition } filter { condition =>
                condition.acceptable(gen, liftedGraph, updatedNodes)
            }

            // 4. Accept condition 처리
            // 4a. Condition Fate
            val nextConditionFate: ConditionFate = {
                //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
                //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
                //                evaluated ++ newConditions // filter { _._2 != False }
                ctx.conditionFate.update(conditionsEvaluations)
            }
            // 4b. Update accept conditions
            val acceptConditionUpdatedGraph = liftedGraph mapNode { node =>
                Node(node.kernel, conditionsEvaluations(node.condition))
            }

            // 5. Trimming
            // 5a. 사용이 완료된 터미널 노드/acceptCondition이 never인 지우기
            val trimmed1 = acceptConditionUpdatedGraph filterNode { node =>
                (node.condition != Never) && (node.kernel.symbol match {
                    case Terminal(_) => node.kernel.beginGen == nextGen
                    case _ => true
                })
            }
            // 5b. startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
            val trimStarts: Set[Node] = Set(startNode) ++ (trimmed1.nodes flatMap { _.condition.nodes } intersect trimmed1.nodes)
            val newTermNodes: Set[Node] = termNodes(trimmed1, nextGen)
            val nextGraph: Graph = trim(trimmed1, trimStarts, newTermNodes)

            val nextContext = ctx.proceed(nextGen, nextGraph, nextGraph.nodes filter { node => acceptableConditions contains node.condition }, input, nextConditionFate)
            Left((ProceedDetail(graph, graph, liftedGraph, acceptConditionUpdatedGraph, nextGraph), nextContext))
        }
    }
}
