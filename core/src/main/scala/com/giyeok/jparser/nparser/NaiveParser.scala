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
        val cc = rec(0, List(DeriveTask(startNode)), Graph(Set(startNode), Set()), Set[Node]())
        val conditionsEvaluations = evaluateAcceptConditions(0, cc.graph.nodes map { _.condition }, cc.graph, cc.updatedNodes)
        new NaiveContext(0, cc.graph, List(), List(), ConditionFate(conditionsEvaluations))
    }

    def proceedDetail(ctx: NaiveContext, input: Input): Either[(ProceedDetail, NaiveContext), ParsingError] = {
        val (graph, gen, nextGen) = (ctx.graph, ctx.gen, ctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // No Expansion

            // 2. 1차 lift
            val Cont(liftedGraph0, updatedNodes0) = rec(nextGen, termFinishes, graph, Set[Node]())

            // 3. acceptable한 노드들만 필터
            val acceptableOnlyGraph: Graph = liftedGraph0 filterNode { node =>
                node.condition.acceptable(nextGen, liftedGraph0, updatedNodes0)
            }

            // 4. Evaluate accept conditions
            val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] =
                evaluateAcceptConditions(nextGen, liftedGraph0.nodes map { _.condition }, liftedGraph0, updatedNodes0)

            // 4. accept condition update
            // 5. Accept condition 처리
            // 5a. ConditionFate update
            val nextConditionFate: ConditionFate = {
                //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
                //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
                //                evaluated ++ newConditions // filter { _._2 != False }
                ctx.conditionFate.update(conditionsEvaluations)
            }
            // 5b. Update accept conditions in graph
            val acceptConditionUpdatedGraph = acceptableOnlyGraph mapNode { node =>
                Node(node.kernel, conditionsEvaluations(node.condition))
            }

            val Cont(liftedGraph, updatedNodes) = rec(nextGen, termFinishes, acceptConditionUpdatedGraph, Set[Node]())

            println(s"gen $gen")
            updatedNodes foreach { kv =>
                println(s"${kv._1} -> ${kv._2}")
            }

            // TODO 4, 5 단계가 3단계보다 먼저 돼야하나?

            // 6. Trimming
            // 6a. 사용이 완료된 터미널 노드/acceptCondition이 never인 지우기
            val trimmed1 = acceptConditionUpdatedGraph filterNode { node =>
                (node.condition != Never) && (node.kernel.symbol match {
                    case Terminal(_) => node.kernel.beginGen == nextGen
                    case _ => true
                })
            }
            // 6b. startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
            val trimmedGraph: Graph = trim(trimmed1, startNode, termNodes(trimmed1, nextGen))

            // TODO trimmedGraph와 별개로 finish된 노드 정보를 전달해야 함
            // - acceptConditionUpdatedGraph와 trimmedGraph를 둘 다 받아서 해결 가능
            //   - parse tree reconstruction에는 acceptConditionUpdatedGraph 사용
            //   - 다음 generation 시작할 때는 trimmedGraph 사용
            val nextContext = ctx.proceed(nextGen, trimmedGraph, input, nextConditionFate)
            Left((ProceedDetail(graph, graph, liftedGraph0, acceptableOnlyGraph, acceptConditionUpdatedGraph, liftedGraph, trimmed1, trimmedGraph), nextContext))
        }
    }
}
