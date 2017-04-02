package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs
import Parser._

trait Parser[T <: Context] {
    val grammar: NGrammar

    val startNode = Node(Kernel(grammar.startSymbol, 0, 0, 0)(grammar.nsymbols(grammar.startSymbol)), Always)

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
        def of(condition: AcceptCondition): Boolean = {
            if (trueFixed contains condition) true
            else if (falseFixed contains condition) false
            else ??? // unfixed(condition)
        }

        def update(evaluations: Map[AcceptCondition, AcceptCondition]): ConditionAccumulate = {
            val eventuallyTrue = (evaluations filter { _._2 == Always }).keySet ++ trueFixed
            val eventuallyFalse = (evaluations filter { _._2 == Never }).keySet ++ falseFixed
            val stillNotFixed = evaluations -- eventuallyTrue -- eventuallyFalse
            val newUnfixed = (unfixed -- eventuallyTrue -- eventuallyFalse) mapValues { cond =>
                // unfixed A -> cond 에서 cond가 변경되었으면 변경된 값으로 업데이트, 변경되지 않았으면 cond 그대로
                stillNotFixed.getOrElse(cond, cond)
            }
            new ConditionAccumulate(eventuallyTrue, eventuallyFalse, newUnfixed)
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
    }

    class NaiveContext(gen: Int, nextGraph: Graph, _inputs: List[Input], _history: List[Graph], conditionAccumulate: ConditionAccumulate)
            extends Context(gen, nextGraph, _inputs, _history, conditionAccumulate) {
        def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, newInput: Input, newConditionAccumulate: ConditionAccumulate): NaiveContext = {
            new NaiveContext(nextGen, nextGraph, newInput +: _inputs, resultGraph +: _history, newConditionAccumulate)
        }
    }

    class DeriveTipsContext(gen: Int, nextGraph: Graph, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Graph], conditionAccumulate: ConditionAccumulate)
            extends Context(gen, nextGraph, _inputs, _history, conditionAccumulate) {
        // assert(deriveTips subsetOf ctx.graph.nodes)
        def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, deriveTips: Set[Node], newInput: Input, newConditionAccumulate: ConditionAccumulate): DeriveTipsContext = {
            new DeriveTipsContext(nextGen, nextGraph, deriveTips, newInput +: _inputs, resultGraph +: _history, newConditionAccumulate)
        }
    }

    case class Transition(name: String, result: Graph)
    case class ProceedDetail(baseGraph: Graph, transitions: Transition*) {
        def graphAt(idx: Int): Graph =
            if (idx == 0) baseGraph else transitions(idx - 1).result
        def nameOf(idx: Int): String =
            transitions(idx - 1).name
    }
}
