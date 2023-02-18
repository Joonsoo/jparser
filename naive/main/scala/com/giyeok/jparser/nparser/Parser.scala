package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.{Inputs, NGrammar}
import Parser._

trait Parser[T <: Context] {
    val grammar: NGrammar

    val startNode = Node(Kernel(grammar.startSymbol, 0, 0, 0), Always)

    val initialContext: T

    def proceedDetail(wctx: T, input: Input): Either[(ProceedDetail, T), ParsingError]

    def proceed(wctx: T, input: Input): Either[T, ParsingError] =
        proceedDetail(wctx, input) match {
            case Left((detail, nextCtx)) => Left(nextCtx)
            case Right(error) => Right(error)
        }

    def parse(source: Inputs.Source): Either[T, ParsingError] =
        source.foldLeft[Either[T, ParsingError]](Left(initialContext)) {
            (cc, input) =>
                cc match {
                    case Left(ctx) => proceed(ctx, input)
                    case error @ Right(_) => error
                }
        }
    def parse(source: String): Either[T, ParsingError] =
        parse(Inputs.fromString(source))
}

object Parser {
    class ConditionAccumulate(val trueFixed: Set[AcceptCondition], val falseFixed: Set[AcceptCondition], val unfixed: Map[AcceptCondition, AcceptCondition]) {
        def update(evaluations: Map[AcceptCondition, AcceptCondition]): ConditionAccumulate = {
            // evaluations의 key에 해당하는 새 조건들이 모두 포함되어야 함
            val eventuallyTrue = (evaluations filter { _._2 == Always }).keySet ++ trueFixed
            val eventuallyFalse = (evaluations filter { _._2 == Never }).keySet ++ falseFixed
            val newUnfixed = (unfixed ++ evaluations -- eventuallyTrue -- eventuallyFalse).view.mapValues { cond =>
                // unfixed A -> cond 에서 cond가 변경되었으면 변경된 값으로 업데이트, 변경되지 않았으면 cond 그대로
                evaluations.getOrElse(cond, cond)
            }
            //            assert({
            //                val covered = eventuallyTrue ++ eventuallyFalse ++ newUnfixed.keySet
            //                evaluations.keySet subsetOf covered
            //            })
            new ConditionAccumulate(eventuallyTrue, eventuallyFalse, newUnfixed.toMap)
        }
    }
    object ConditionAccumulate {
        def apply(conditionsEvaluations: Map[AcceptCondition, AcceptCondition]): ConditionAccumulate = {
            var trueConditions = Set[AcceptCondition]()
            var falseConditions = Set[AcceptCondition]()
            var unfixedConditions = Map[AcceptCondition, AcceptCondition]()

            conditionsEvaluations foreach { kv =>
                kv._2 match {
                    case Always => trueConditions += kv._1
                    case Never => falseConditions += kv._1
                    case _ => unfixedConditions += kv
                }
            }
            new ConditionAccumulate(trueConditions, falseConditions, unfixedConditions)
        }
    }

    abstract class Context(val gen: Int, val nextGraph: Graph, _inputs: List[Input], _history: List[Graph], val conditionAccumulate: ConditionAccumulate) {
        def nextGen: Int = gen + 1
        def inputs: Seq[Input] = _inputs.reverse
        def history: Seq[Graph] = _history.reverse
        def resultGraph: Graph = _history.head
        def conditionFinal: Map[AcceptCondition, Boolean] = {
            val trueFixed = (conditionAccumulate.trueFixed map { c => c -> true }).toMap
            val falseFixed = (conditionAccumulate.falseFixed map { c => c -> false }).toMap
            val notFixed = conditionAccumulate.unfixed map { kv => kv._1 -> kv._2.acceptable(gen, resultGraph) }
            trueFixed ++ falseFixed ++ notFixed
        }
    }

    class NaiveContext(gen: Int, nextGraph: Graph, _inputs: List[Input], _history: List[Graph], conditionAccumulate: ConditionAccumulate)
            extends Context(gen, nextGraph, _inputs, _history, conditionAccumulate) {
        def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, newInput: Input, newConditionAccumulate: ConditionAccumulate): NaiveContext = {
            new NaiveContext(nextGen, nextGraph, newInput +: _inputs, resultGraph +: _history, newConditionAccumulate)
        }
    }

    case class Transition(name: String, result: Graph, isResult: Boolean = false, isNext: Boolean = false)
    case class ProceedDetail(baseGraph: Graph, transitions: Transition*) {
        def graphAt(idx: Int): Graph =
            if (idx == 0) baseGraph else transitions(idx - 1).result
        def isResultAt(idx: Int): Boolean =
            if (idx == 0) false else transitions(idx - 1).isResult
        def nameOf(idx: Int): String =
            transitions(idx - 1).name
    }
}
