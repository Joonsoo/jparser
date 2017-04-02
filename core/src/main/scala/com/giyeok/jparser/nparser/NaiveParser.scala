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
        val conditionsEvaluations = evaluateAcceptConditions(0, cc.graph.nodes map { _.condition }, cc.graph, cc.updatedNodes)
        new NaiveContext(0, cc.graph, List(), List(cc.graph), ConditionFate(conditionsEvaluations))
    }

    def proceedDetail(ctx: NaiveContext, input: Input): Either[(ProceedDetail, NaiveContext), ParsingError] = {
        val (graph, gen, nextGen) = (ctx.nextGraph, ctx.gen, ctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // No Expansion

            // 2. 1차 lift
            val Cont(liftedGraph, updatedNodes) = rec(nextGen, termFinishes, graph)

            // 3. acceptable한 노드들만 필터
            val acceptableOnlyGraph: Graph = liftedGraph filterNode { node =>
                node.condition.acceptable(nextGen, liftedGraph, updatedNodes)
            }

            // 4. Evaluate accept conditions
            val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] =
                evaluateAcceptConditions(nextGen, liftedGraph.nodes map { _.condition }, liftedGraph, updatedNodes)

            // 4. accept condition update
            // 4a. ConditionFate update
            val nextConditionFate: ConditionFate = {
                //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
                //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
                //                evaluated ++ newConditions // filter { _._2 != False }
                ctx.conditionFate.update(conditionsEvaluations)
            }
            // 4b. Update accept conditions in graph
            val acceptConditionUpdatedGraph = acceptableOnlyGraph mapNode { node =>
                Node(node.kernel, conditionsEvaluations(node.condition))
            }

            // 5. 2차 lift
            val Cont(liftedGraph2, updatedNodes2) = rec(nextGen, termFinishes, acceptConditionUpdatedGraph)

            // TODO 2차 리프트가 필요한가?
            assert(acceptConditionUpdatedGraph == liftedGraph2)
            println(s"gen $gen - ${updatedNodes.size} - ${updatedNodes2.size}")
            updatedNodes foreach { kv =>
                println(s"${kv._1} -> ${kv._2}")
            }
            println("-----")
            updatedNodes2 foreach { kv =>
                println(s"${kv._1} -> ${kv._2}")
            }

            // 6. Trimming
            // 6a. 1차 트리밍 - 사용이 완료된 터미널 노드/acceptCondition이 never인 지우기
            val trimmed1 = acceptConditionUpdatedGraph filterNode { node =>
                // TODO node.kernel.isFinished 인 노드도 지워도 될까?
                (node.condition != Never) && (node.kernel.symbol match {
                    case Terminal(_) => node.kernel.beginGen == nextGen
                    case _ => true
                })
            }
            // 6b. 2차 트리밍 - startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
            val trimmedGraph: Graph = trim(trimmed1, startNode, termNodes(trimmed1, nextGen))

            // trimmedGraph와 별개로 finish된 노드 정보를 전달해야 함
            //   - parse tree reconstruction할 때는 acceptConditionUpdatedGraph 그래프를 사용하고
            //   - 다음 generation 시작할 때는 trimmedGraph 사용
            val nextContext = ctx.proceed(nextGen, acceptConditionUpdatedGraph, trimmedGraph, input, nextConditionFate)
            Left((ProceedDetail(
                graph,
                Transition("lift", liftedGraph),
                Transition("acceptableOnly", acceptableOnlyGraph),
                Transition("conditionUpdated", acceptConditionUpdatedGraph),
                Transition("trimmed", trimmedGraph)
            ), nextContext))
        }
    }
}
