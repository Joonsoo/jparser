package com.giyeok.jparser.nparser

import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.Inputs

class NaiveParser(val grammar: NGrammar) extends ParsingTasks {
    case class WrappedContext(gen: Int, ctx: Context, _inputs: List[Input], _history: List[Results[Node]], conditionFate: Map[Condition, Condition]) {
        // TODO PreprocessedParser에서는 0단계로 expand가 붙음
        // TODO Right recursion 최적화를 위해서 progress task를 수정해야할 수도 있음

        def inputs = _inputs.reverse
        def history = (ctx.finishes +: _history).reverse

        def proceedDetail(input: Input): Either[(ProceedDetail, WrappedContext), ParsingError] = {
            val nextGen = gen + 1
            val termFinishes = finishableTermNodes(ctx, gen, input).toList map { FinishTask(_, True, None) }
            if (termFinishes.isEmpty) {
                Right(UnexpectedInput(input))
            } else {
                val lastFinishes = ctx.finishes
                // 1. Lift
                val liftedCtx: Context = rec(nextGen, termFinishes, ctx.emptyFinishes)
                // 2. Trimming
                val trimStarts = (Set(startNode) ++ (liftedCtx.finishes.conditionNodes) ++ (liftedCtx.progresses.conditionNodes)) intersect liftedCtx.graph.nodes
                val newTermNodes = termNodes(liftedCtx, nextGen)
                val trimmedCtx: Context = trim(liftedCtx, trimStarts, newTermNodes)
                // 3. Revert
                val revertedCtx: Context = revert(nextGen, trimmedCtx, trimmedCtx.finishes, trimmedCtx.graph.nodes)
                // 4. Condition Fate
                val conditionFateNext = {
                    val evaluated = conditionFate mapValues { _.evaluate(nextGen, trimmedCtx.finishes, trimmedCtx.graph.nodes) }
                    val newConditions = (revertedCtx.finishes.conditions map { c => (c -> c) }).toMap
                    (evaluated ++ newConditions) filter { _._2 != False }
                }
                val nextCtx = WrappedContext(nextGen, revertedCtx, input +: _inputs, lastFinishes +: _history, conditionFateNext)
                Left((ProceedDetail(ctx, liftedCtx, trimmedCtx, revertedCtx), nextCtx))
            }
        }

        def proceed(input: Input): Either[WrappedContext, ParsingError] =
            proceedDetail(input) match {
                case Left((detail, nextCtx)) => Left(nextCtx)
                case Right(error) => Right(error)
            }
    }

    case class ProceedDetail(baseCtx: Context, liftedCtx: Context, trimmedCtx: Context, revertedCtx: Context)

    def process(nextGen: Int, task: Task, cc: Context): (Context, Seq[Task]) =
        task match {
            case task: DeriveTask => deriveTask(nextGen, task, cc)
            case task: ProgressTask => progressTask(nextGen, task, cc)
            case task: FinishTask => finishTask(nextGen, task, cc)
        }

    def rec(nextGen: Int, tasks: List[Task], cc: Context): Context =
        tasks match {
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                rec(nextGen, newTasks ++: rest, ncc)
            case List() => cc
        }

    val startNode = SymbolNode(grammar.startSymbol, 0)

    val initialContext = {
        val ctx0 = rec(0, List(DeriveTask(startNode)), Context(Graph(Set(startNode), Set()), Results[SequenceNode](), Results[Node]()))
        val ctx = revert(0, ctx0, ctx0.finishes, ctx0.graph.nodes)
        WrappedContext(0, ctx, List(), List(), (ctx.finishes.conditions map { c => (c -> c) }).toMap)
    }

    def parse(source: Inputs.Source): Either[WrappedContext, ParsingError] =
        source.foldLeft[Either[WrappedContext, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => ctx.proceed(input)
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[WrappedContext, ParsingError] =
        parse(Inputs.fromString(source))
}
