package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.nparser.Parser._
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.nparser.Parser.NaiveWrappedContext
import com.giyeok.jparser.nparser.Parser.ConditionFate

class NaiveParser(val grammar: NGrammar) extends Parser[NaiveWrappedContext] with ParsingTasks {
    // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

    val initialContext: NaiveWrappedContext = {
        val cc0 = rec(0, List(DeriveTask(startNode)), Graph(Set(startNode), Set()))
        val ctx = updateAcceptableCondition(0, cc0)
        new NaiveWrappedContext(0, ctx, List(), List(), ConditionFate(
            (ctx.finishedNodes map { _.condition } map { c => c -> c }).toMap
        ))
    }

    def proceedDetail(wctx: NaiveWrappedContext, input: Input): Either[(ProceedDetail, NaiveWrappedContext), ParsingError] = {
        val (graph, gen, nextGen) = (wctx.graph, wctx.gen, wctx.nextGen)
        val termFinishes = finishableTermNodes(graph, gen, input).toList map { ProgressTask(_, Always) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // No Expansion
            // 2. Lift
            val liftedGraph: Graph = rec(nextGen, termFinishes, graph)
            // 3. Trimming
            // TODO trimming은 accept condition evaluateion과 history를 위한 노드 기록 등의 모든 과정이 끝나고 다음 단계로 넘어가기 직전에 해야함
            // TODO 사용이 완료된 터미널 노드 지우기 추가
            val trimStarts: Set[Node] = Set(startNode) // (Set(startNode) ++ (liftedGraph.finishedNodes.conditionNodes) ++ (liftedGraph.progresses.conditionNodes)) intersect liftedGraph.graph.nodes
            val newTermNodes: Set[Node] = termNodes(liftedGraph, nextGen)
            val trimmedGraph: Graph = trim(liftedGraph, trimStarts, newTermNodes)
            // 4. Revert
            val revertedGraph: Graph = updateAcceptableCondition(nextGen, trimmedGraph)
            // 5. Condition Fate
            val conditionFateNext = {
                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
                evaluated ++ newConditions // filter { _._2 != False }
            }
            val nextGraph = wctx.proceed(nextGen, revertedGraph, input, conditionFateNext)
            Left((ProceedDetail(graph, graph, liftedGraph, trimmedGraph, revertedGraph), nextGraph))
        }
    }
}
