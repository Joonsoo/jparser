package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs
import Parser._

trait Parser[T <: WrappedContext] {
    val grammar: NGrammar

    val startNode = SymbolNode(grammar.startSymbol, 0)

    val initialContext: T

    def proceedDetail(wctx: T, input: Input): Either[(ProceedDetail, T), ParsingError]

    def proceed(wctx: T, input: Input): Either[T, ParsingError] =
        proceedDetail(wctx, input) match {
            case Left((detail, nextCtx)) => Left(nextCtx)
            case Right(error) => Right(error)
        }

    def parse(source: Inputs.Source): Either[T, ParsingError] =
        source.foldLeft[Either[T, ParsingError]](Left(initialContext)) {
            (ctx, input) =>
                ctx match {
                    case Left(ctx) => proceed(ctx, input)
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[T, ParsingError] =
        parse(Inputs.fromString(source))
}

object Parser {
    class ConditionFate(val trueFixed: Set[AcceptCondition], val falseFixed: Set[AcceptCondition], val unfixed: Map[AcceptCondition, AcceptCondition]) {
        def of(condition: AcceptCondition): AcceptCondition = {
            if (trueFixed contains condition) Always
            else if (falseFixed contains condition) Never
            else unfixed(condition)
        }

        def update(newConditionFate: Map[AcceptCondition, AcceptCondition]): ConditionFate = {
            var trueConditions = trueFixed
            var falseConditions = falseFixed
            var unfixedConditions = Map[AcceptCondition, AcceptCondition]()

            newConditionFate foreach { kv =>
                kv._2 match {
                    case Always => trueConditions += kv._1
                    case Never => falseConditions += kv._1
                    case _ => unfixedConditions += kv
                }
            }
            new ConditionFate(trueConditions, falseConditions, unfixedConditions)
        }
    }
    object ConditionFate {
        def apply(conditionFate: Map[AcceptCondition, AcceptCondition]): ConditionFate = {
            var trueConditions = Set[AcceptCondition]()
            var falseConditions = Set[AcceptCondition]()
            var unfixedConditions = Map[AcceptCondition, AcceptCondition]()

            conditionFate foreach { kv =>
                kv._2 match {
                    case Always => trueConditions += kv._1
                    case Never => falseConditions += kv._1
                    case _ => unfixedConditions += kv
                }
            }
            new ConditionFate(trueConditions, falseConditions, unfixedConditions)
        }
    }

    abstract class WrappedContext(val gen: Int, val ctx: Context, _inputs: List[Input], _history: List[Results[Node]], val conditionFate: ConditionFate) {
        def nextGen = gen + 1
        def inputs = _inputs.reverse
        def history = (ctx.finishes +: _history).reverse
    }

    class NaiveWrappedContext(gen: Int, ctx: Context, _inputs: List[Input], _history: List[Results[Node]], conditionFate: ConditionFate) extends WrappedContext(gen, ctx, _inputs, _history, conditionFate) {
        def proceed(nextGen: Int, nextCtx: Context, newInput: Input, newConditionFate: Map[AcceptCondition, AcceptCondition]) = {
            new NaiveWrappedContext(nextGen, nextCtx, newInput +: _inputs, ctx.finishes +: _history, conditionFate update newConditionFate)
        }
    }

    class DeriveTipsWrappedContext(gen: Int, ctx: Context, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Results[Node]], conditionFate: ConditionFate) extends WrappedContext(gen, ctx, _inputs, _history, conditionFate) {
        // assert(deriveTips subsetOf ctx.graph.nodes)
        def proceed(nextGen: Int, nextCtx: Context, deriveTips: Set[Node], newInput: Input, newConditionFate: Map[AcceptCondition, AcceptCondition]) = {
            new DeriveTipsWrappedContext(nextGen, nextCtx, deriveTips: Set[Node], newInput +: _inputs, ctx.finishes +: _history, conditionFate update newConditionFate)
        }
    }

    case class ProceedDetail(baseCtx: Context, expandedCtx: Context, liftedCtx: Context, trimmedCtx: Context, revertedCtx: Context)
}
