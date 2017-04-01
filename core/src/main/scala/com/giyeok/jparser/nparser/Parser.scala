package com.giyeok.jparser.nparser

import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs
import Parser._

trait Parser[T <: WrappedContext] {
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
        def of(condition: AcceptCondition): Boolean = {
            if (trueFixed contains condition) true
            else if (falseFixed contains condition) false
            else ??? // unfixed(condition)
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

    abstract class WrappedContext(val gen: Int, val graph: Graph, _inputs: List[Input], _history: List[Set[Node]], val conditionFate: ConditionFate) {
        def nextGen = gen + 1
        def inputs = _inputs.reverse
        def history = (graph.nodes +: _history).reverse
    }

    class NaiveWrappedContext(gen: Int, graph: Graph, _inputs: List[Input], _history: List[Set[Node]], conditionFate: ConditionFate) extends WrappedContext(gen, graph, _inputs, _history, conditionFate) {
        def proceed(nextGen: Int, nextGraph: Graph, newInput: Input, newConditionFate: ConditionFate) = {
            new NaiveWrappedContext(nextGen, nextGraph, newInput +: _inputs, graph.nodes +: _history, newConditionFate)
        }
    }

    class DeriveTipsWrappedContext(gen: Int, graph: Graph, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Set[Node]], conditionFate: ConditionFate) extends WrappedContext(gen, graph, _inputs, _history, conditionFate) {
        // assert(deriveTips subsetOf ctx.graph.nodes)
        def proceed(nextGen: Int, nextGraph: Graph, deriveTips: Set[Node], newInput: Input, newConditionFate: ConditionFate) = {
            new DeriveTipsWrappedContext(nextGen, nextGraph, deriveTips: Set[Node], newInput +: _inputs, graph.nodes +: _history, newConditionFate)
        }
    }

    case class ProceedDetail(baseGraph: Graph, expandedGraph: Graph, liftedGraph: Graph, acceptConditionUpdatedGraph: Graph, trimmedGraph: Graph)

    def evaluateAcceptConditions(nextGen: Int, conditions: Set[AcceptCondition], graph: Graph, updatedNodes: Map[Node, Set[Node]]): Map[AcceptCondition, AcceptCondition] = {
        (conditions map { condition =>
            condition -> condition.evaluate(nextGen, graph, updatedNodes)
        }).toMap
    }
}
