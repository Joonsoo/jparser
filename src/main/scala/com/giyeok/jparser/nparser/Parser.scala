package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.EligCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs
import Parser._

trait Parser {
    val grammar: NGrammar

    val startNode = SymbolNode(grammar.startSymbol, 0)

    val initialContext: WrappedContext

    def proceedDetail(wctx: WrappedContext, input: Input): Either[(ProceedDetail, WrappedContext), ParsingError]

    def proceed(wctx: WrappedContext, input: Input): Either[WrappedContext, ParsingError] =
        proceedDetail(wctx, input) match {
            case Left((detail, nextCtx)) => Left(nextCtx)
            case Right(error) => Right(error)
        }

    def parse(source: Inputs.Source): Either[WrappedContext, ParsingError] =
        source.foldLeft[Either[WrappedContext, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => proceed(ctx, input)
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[WrappedContext, ParsingError] =
        parse(Inputs.fromString(source))
}

object Parser {
    case class WrappedContext(gen: Int, ctx: Context, _inputs: List[Input], _history: List[Results[Node]], conditionFate: Map[Condition, Condition]) {
        def nextGen = gen + 1
        def inputs = _inputs.reverse
        def history = (ctx.finishes +: _history).reverse

        def proceed(nextGen: Int, nextCtx: Context, newInput: Input, newConditionFate: Map[Condition, Condition]) = {
            WrappedContext(nextGen, nextCtx, newInput +: _inputs, ctx.finishes +: _history, newConditionFate)
        }
    }

    case class ProceedDetail(baseCtx: Context, expandedCtx: Context, liftedCtx: Context, trimmedCtx: Context, revertedCtx: Context)
}
