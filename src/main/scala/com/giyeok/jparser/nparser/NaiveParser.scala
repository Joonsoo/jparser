package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.nparser.Parser._
import com.giyeok.jparser.ParsingErrors._
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.nparser.Parser.NaiveWrappedContext

class NaiveParser(val grammar: NGrammar) extends Parser[NaiveWrappedContext] with ParsingTasks {
    // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

    val initialContext = {
        val ctx0 = rec(0, List(DeriveTask(startNode)), Context(Graph(Set(startNode), Set()), Results[SequenceNode](), Results[Node]()))
        val ctx = revert(0, ctx0, ctx0.finishes, ctx0.graph.nodes)
        new NaiveWrappedContext(0, ctx, List(), List(), (ctx.finishes.conditions map { c => (c -> c) }).toMap)
    }

    def proceedDetail(wctx: NaiveWrappedContext, input: Input): Either[(ProceedDetail, NaiveWrappedContext), ParsingError] = {
        val (ctx, gen, nextGen) = (wctx.ctx, wctx.gen, wctx.nextGen)
        val termFinishes = finishableTermNodes(wctx.ctx, wctx.gen, input).toList map { FinishTask(_, True, None) }
        if (termFinishes.isEmpty) {
            Right(UnexpectedInput(input))
        } else {
            // No Expansion
            // 2. Lift
            val liftedCtx: Context = rec(nextGen, termFinishes, ctx.emptyFinishes)
            // 3. Trimming
            val trimStarts = (Set(startNode) ++ (liftedCtx.finishes.conditionNodes) ++ (liftedCtx.progresses.conditionNodes)) intersect liftedCtx.graph.nodes
            val newTermNodes = termNodes(liftedCtx, nextGen)
            val trimmedCtx: Context = trim(liftedCtx, trimStarts, newTermNodes)
            // 4. Revert
            val revertedCtx: Context = revert(nextGen, trimmedCtx, trimmedCtx.finishes, trimmedCtx.graph.nodes)
            // 5. Condition Fate
            val conditionFateNext = {
                val evaluated = wctx.conditionFate mapValues { _.evaluate(nextGen, trimmedCtx.finishes, trimmedCtx.graph.nodes) }
                val newConditions = (revertedCtx.finishes.conditions map { c => (c -> c) }).toMap
                (evaluated ++ newConditions) filter { _._2 != False }
            }
            val nextCtx = wctx.proceed(nextGen, revertedCtx, input, conditionFateNext)
            Left((ProceedDetail(ctx, ctx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
        }
    }
}
