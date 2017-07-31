package com.giyeok.jparser.npreparser

import scala.annotation.tailrec
import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate
import com.giyeok.jparser.nparser.Parser.ContextAccumulate
import com.giyeok.jparser.nparser.Parser.ProceedDetail
import com.giyeok.jparser.nparser.Parser.Transition
import com.giyeok.jparser.nparser.ParsingContext._
import com.giyeok.jparser.nparser.ParsingTasks

class DeriveTipsContext(gen: Int, nextGraph: Graph, val deriveTips: Set[Node], _inputs: List[Input], _history: List[Graph], conditionAccumulate: ConditionAccumulate)
        extends ContextAccumulate(gen, nextGraph, _inputs, _history, conditionAccumulate) {
    // assert(deriveTips subsetOf nextGraph.nodes)
    def proceed(nextGen: Int, resultGraph: Graph, nextGraph: Graph, deriveTips: Set[Node], newInput: Input, newConditionAccumulate: ConditionAccumulate): DeriveTipsContext = {
        new DeriveTipsContext(nextGen, nextGraph, deriveTips, newInput +: _inputs, resultGraph +: _history, newConditionAccumulate)
    }
}

class PreprocessedParser(val grammar: NGrammar) extends Parser[DeriveTipsContext] with ParsingTasks with DerivationPreprocessor {
    // assert(grammar == derivation.grammar)

    val initialContext: DeriveTipsContext = {
        val initial = preprocessedOf(grammar.startSymbol, 0)
        val conditionAccumulate = ConditionAccumulate((initial.graph.nodes map { _.condition } map { x => x -> x }).toMap)
        val initialGraph = Graph(Set(startNode), Set())
        new DeriveTipsContext(0, initialGraph, Set(startNode), List(), List(initial.graph), conditionAccumulate)
    }

    @tailrec private def recNoDerive(nextGen: Int, tasks: List[Task], cc: Cont, deriveTips: Set[Node]): (Cont, Set[Node]) =
        tasks match {
            case DeriveTask(deriveTip @ Node(Kernel(symbolId, pointer, beginGen, endGen), condition)) +: rest if pointer > 0 =>
                val deriveTipTasks = preprocessedOf(symbolId, pointer).conform(beginGen, endGen, condition).baseNodeTasks
                recNoDerive(nextGen, deriveTipTasks ++: rest, cc, deriveTips + deriveTip)
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                recNoDerive(nextGen, newTasks ++: rest, ncc, deriveTips)
            case List() => (cc, deriveTips)
        }

    def proceedDetail(ctx: DeriveTipsContext, input: Input): Either[(ProceedDetail, DeriveTipsContext), ParsingError] = {
        val (graph, nextGen, deriveTips) = (ctx.nextGraph, ctx.nextGen, ctx.deriveTips)
        // finishable term node를 포함한 deriveTip -> term node set
        val expandings0: Seq[(Node, ProgressPreprocessed)] = deriveTips.toSeq flatMap { deriveTip =>
            sliceOf(deriveTip.kernel.symbolId, deriveTip.kernel.pointer, input) map { deriveTip -> _ }
        }
        if (expandings0.isEmpty) {
            Right(UnexpectedInput(input, nextGen))
        } else {
            // 각 derive tip의 ProgressPreprocessed에 대해서(우선 conform 먼저 하고)
            // - result graph를 만들 때는 preprocessed.graph를 사용하고
            // - next graph를 만들 때는 preprocessed.trimmedGraph를 사용한다

            val expandings = expandings0 map { nodePreprocessed =>
                val (node, preprocessed) = nodePreprocessed
                node -> preprocessed.conform(node.kernel.beginGen, node.kernel.endGen, node.condition)
            }

            // expandedGraph0, nextDeriveTips0: graph에 expandings의 preprocessed.trimmedGraph를 병합하고 trimmedGraph에 속한 deriveTip들을 수집
            val expandedGraph: Graph = expandings.foldLeft(graph) { (cc, nodePreprocessed) =>
                cc.merge(nodePreprocessed._2.trimmedGraph)
            }
            val nextDeriveTips0: Set[Node] = (expandings flatMap { _._2.nextDeriveTips }).toSet
            val initialTasks: Seq[ProgressTask] = expandings flatMap { _._2.baseNodeTasks }
            val initialUpdatedNodesMap: Map[Node, Set[Node]] = (expandings flatMap { _._2.updatedNodesMap }).toMap
            val x = (expandings flatMap { kv => kv._2.updatedNodesMap.toSeq map { kv._1 -> _ } }) groupBy { _._2._1 }
            // expandings의 updatedNodesMap에서 key가 같으면 value가 같아야 한다
            // TODO initialUpdatedNodesMap이 필요한가?
            assert(x forall { kv => (kv._2 map { _._2._2 }).toSet.size == 1 })

            // liftedGraph, nextDeriveTips: expandedGraph와 deriveTip에 대한 progress task 처리하고 새로운 deriveTip를 nextDeriveTips0에 추가
            val (Cont(liftedGraph: Graph, _), nextDeriveTips1: Set[Node]) =
                recNoDerive(nextGen, initialTasks.toList, Cont(expandedGraph, Map()), nextDeriveTips0)

            // nextDeriveTips에서 accept condition update 및 filtering
            val (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph) =
                processAcceptCondition(nextGen, liftedGraph, ctx.conditionAccumulate)
            val nextDeriveTips: Set[Node] = nextDeriveTips1 intersect conditionFilteredGraph.nodes
            val trimmedGraph: Graph = trimUnreachables(conditionFilteredGraph, startNode, nextDeriveTips)

            // resultGraph: liftedGraph에 expandings의 preprocessed.graph를 병합
            // TODO 사실 실제로 그래프를 병합할 필요는 없음. 나중에 수정
            val resultGraph: Graph = expandings.foldLeft(liftedGraph) { (cc, nodePreprocessed) =>
                cc.merge(nodePreprocessed._2.graph)
            }

            val nextContext = ctx.proceed(nextGen, resultGraph, trimmedGraph, nextDeriveTips, input, nextConditionAccumulate)

            Left((ProceedDetail(
                graph,
                Transition("expanded", expandedGraph),
                Transition("lifted", liftedGraph),
                Transition("conditionUpdated", conditionUpdatedGraph),
                Transition("conditionFiltered", conditionFilteredGraph),
                Transition("trimmed (next)", trimmedGraph, isNext = true),
                Transition("(result)", resultGraph, isResult = true)
            ), nextContext))
        }
    }
}
