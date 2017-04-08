package com.giyeok.jparser.npreparser

import scala.annotation.tailrec
import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.VirtualTermGroupDesc
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.Symbols.Terminal
import com.giyeok.jparser.Symbols.Terminals.CharacterTerminal
import com.giyeok.jparser.Symbols.Terminals.VirtualTerminal
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate
import com.giyeok.jparser.nparser.Parser.Context
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.Parser.Transition
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.ParsingTasks

class DeriveTipsContext(gen: Int, nextGraph: Graph, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Graph], updatedNodes: Map[Node, Set[Node]], conditionAccumulate: ConditionAccumulate)
        extends Context(gen, nextGraph, _inputs, _history, updatedNodes, conditionAccumulate) {
    // assert(deriveTips subsetOf ctx.graph.nodes)
    def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, deriveTips: Set[Node], newInput: Input, updatedNodes: Map[Node, Set[Node]], newConditionAccumulate: ConditionAccumulate): DeriveTipsContext = {
        new DeriveTipsContext(nextGen, nextGraph, deriveTips, newInput +: _inputs, resultGraph +: _history, updatedNodes, newConditionAccumulate)
    }
}

class PreprocessedParser(val grammar: NGrammar, val derivation: DerivationPreprocessor) extends Parser[DeriveTipsContext] with ParsingTasks {
    // assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsContext = {
        val initial = derivation.sliceOf(grammar.startSymbol, 0)._1
        assert(initial.cc.graph.nodes contains startNode)
        new DeriveTipsContext(0, initial.cc.graph, Set(startNode), List(), List(initial.cc.graph), initial.cc.updatedNodes, ???)
    }

    //    def expand(ctx: Context, expanding: Map[Node, Preprocessed]): Context = {
    //        expanding.foldLeft(ctx) { (cc, kv) =>
    //            val (deriveTip, preprocessed) = kv
    //            def replaceNode(context: Context, original: Node, replaced: Node): Context =
    //                original match {
    //                    case original: SequenceKernel =>
    //                        Context(context.graph.replaceNode(original, replaced), context.progresses.replaceNode(original, replaced.asInstanceOf[SequenceKernel]), context.finishes.replaceNode(original, replaced))
    //                    case _ =>
    //                        Context(context.graph.replaceNode(original, replaced), context.progresses, context.finishes.replaceNode(original, replaced))
    //                }
    //            cc.merge(replaceNode(preprocessed.context, preprocessed.baseNode, deriveTip))
    //        }
    //    }
    //
    //    @tailrec final def recNoDerive(nextGen: Int, tasks: List[Task], context: Context, deriveTips: Set[SequenceKernel]): (Context, Set[SequenceKernel]) =
    //        tasks match {
    //            case DeriveTask(deriveTip: SequenceKernel) +: rest =>
    //                // context에 deriveTip의 finish task 추가
    //                val preprocessed = derivation.derivationOf(deriveTip)
    //                assert(preprocessed.baseFinishes.isEmpty)
    //                val immediateProgresses = preprocessed.baseProgresses map { condition => ProgressTask(deriveTip, condition.shiftGen(nextGen)) }
    //                recNoDerive(nextGen, immediateProgresses ++: rest, context.updateFinishes(_.merge(preprocessed.context.finishes.shiftGen(nextGen))), deriveTips + deriveTip)
    //            case task +: rest =>
    //                // assert(!task.isInstanceOf[DeriveTask])
    //                val (newContext, newTasks) = process(nextGen, task, context)
    //                recNoDerive(nextGen, newTasks ++: rest, newContext, deriveTips)
    //            case List() => (context, deriveTips)
    //        }

    @tailrec private def recNoDerive(nextGen: Int, tasks: List[Task], cc: Cont, deriveTips: Set[Node]): (Cont, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip) +: rest =>
                recNoDerive(nextGen, rest, cc, deriveTips + deriveTip)
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                recNoDerive(nextGen, newTasks ++: rest, ncc, deriveTips)
            case List() => (cc, deriveTips)
        }

    def proceedDetail(ctx: DeriveTipsContext, input: Input): Either[(ProceedDetail, DeriveTipsContext), ParsingError] = {
        val (graph, gen, nextGen, deriveTips) = (ctx.nextGraph, ctx.gen, ctx.nextGen, ctx.deriveTips)
        // finishable term node를 포함한 deriveTip -> term node set
        val expanding = deriveTips flatMap { deriveTip =>
            derivation.sliceOf(deriveTip.kernel, input)
        }
        if (expanding.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 1. graph에 expanding의 그래프들 추가, updatedNodes 병합, task들 병합
            val expandedGraph: Graph = ???
            val expandedTasks: List[Task] = ???
            val expandedUpdatedNodes: Map[Node, Set[Node]] = ???

            // 2. lift - expand의 결과로 나온 graph, updatedNodes, task로 lift - result graph
            val (Cont(liftedGraph, updatedNodes), deriveTips0) =
                recNoDerive(nextGen, expandedTasks, Cont(expandedGraph, expandedUpdatedNodes), Set())

            // 3. accept condition 처리
            val (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph) =
                processAcceptCondition(nextGen, liftedGraph, updatedNodes, ctx.conditionAccumulate)

            // 4. trimming
            val trimmedGraph: Graph = trimGraph(conditionFilteredGraph, startNode, nextGen)

            val deriveTips = deriveTips0 intersect trimmedGraph.nodes

            val nextContext = ctx.proceed(
                nextGen,
                resultGraph = liftedGraph, nextGraph = trimmedGraph,
                deriveTips = deriveTips,
                input, updatedNodes, nextConditionAccumulate
            )

            Left((ProceedDetail(
                graph,
                Transition("expanded", expandedGraph),
                Transition("lifted(result)", liftedGraph),
                Transition("conditionUpdated", conditionUpdatedGraph),
                Transition("conditionFiltered", conditionFilteredGraph),
                Transition("trimmed(next)", trimmedGraph)
            ), nextContext))
        }
    }
}
