package com.giyeok.jparser.nparser

import scala.annotation.tailrec
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.nparser.Parser.ConditionAccumulate

trait ParsingTasks {
    val grammar: NGrammar

    case class Cont(graph: ParsingContext, updatedFinalStatesMap: Map[SymbolIdAndBeginGen, Set[State]]) {
        // assert((updatedNodesMap flatMap { kv => kv._2 + kv._1 }).toSet subsetOf context.nodes)
    }

    sealed trait Task { val node: State }
    case class DeriveTask(node: State) extends Task
    case class FinishTask(node: State) extends Task
    case class ProgressTask(node: State, condition: AcceptCondition) extends Task

    private def symbolOf(symbolId: Int): NSymbol =
        grammar.symbolOf(symbolId)
    private def atomicSymbolOf(symbolId: Int): NAtomicSymbol =
        symbolOf(symbolId).asInstanceOf[NAtomicSymbol]
    private def newNodeOf(symbolId: Int, beginGen: Int): State =
        State(Kernel(symbolId, 0, beginGen, beginGen)(symbolOf(symbolId)), Always)

    private case class GraphTasksCont(context: ParsingContext, newTasks: List[Task])
    private def addNode(cc: GraphTasksCont, newNode: State): GraphTasksCont = {
        if (!(cc.context.states contains newNode)) {
            // 새로운 노드이면 그래프에 추가하고 task도 추가
            val newNodeTask: Task = if (newNode.kernel.isFinal) FinishTask(newNode) else DeriveTask(newNode)
            GraphTasksCont(cc.context.addState(newNode), newNodeTask +: cc.newTasks)
        } else {
            // 이미 있는 노드이면 아무 일도 하지 않음
            cc
        }
    }

    def deriveTask(nextGen: Int, task: DeriveTask, cc: Cont): (Cont, Seq[Task]) = {
        val DeriveTask(startState) = task

        assert(cc.graph.states contains startState)
        assert(!startState.kernel.isFinal)
        assert(startState.kernel.endGen == nextGen)

        def nodeOf(symbolId: Int): State = newNodeOf(symbolId, nextGen)

        // updatedFinalNodesMap는 참조만 하고 변경하지는 않음
        val updatedFinalNodesMap = cc.updatedFinalStatesMap

        def derive0(cc: GraphTasksCont, symbolId: Int): GraphTasksCont = {
            val newNode = nodeOf(symbolId)
            if (!(cc.context.states contains newNode)) {
                val ncc = addNode(cc, newNode)
                GraphTasksCont(ncc.context.addExpect(Expectation(startState, newNode.kernelBase)), ncc.newTasks)
            } else if (newNode.isFinal) {
                val newGraph = cc.context.addExpect(Expectation(startState, newNode.kernelBase))
                GraphTasksCont(newGraph, ProgressTask(startState, newNode.condition) +: cc.newTasks)
            } else {
                val updatedFinalStates = updatedFinalNodesMap.getOrElse(newNode.kernelBase, Set())
                assert(updatedFinalStates forall { _.isFinal })
                val newOperations = updatedFinalStates map { finishedNode => ProgressTask(startState, finishedNode.condition) }

                val newGraph = cc.context.addExpect(Expectation(startState, newNode.kernelBase))
                GraphTasksCont(newGraph, newOperations ++: cc.newTasks)
            }
        }

        val gtc0 = GraphTasksCont(cc.graph, List())
        val GraphTasksCont(newGraph, newTasks) = startState.kernel.symbol match {
            case symbol: NAtomicSymbol =>
                symbol match {
                    case _: NTerminal =>
                        gtc0 // nothing to do
                    case simpleDerives: NSimpleDerive =>
                        simpleDerives.produces.foldLeft(gtc0) { (cc, deriveSymbolId) => derive0(cc, deriveSymbolId) }
                    case NExcept(_, body, except) =>
                        addNode(derive0(gtc0, body), nodeOf(except))
                    case NJoin(_, body, join) =>
                        addNode(derive0(gtc0, body), nodeOf(join))
                    case NLongest(_, body) =>
                        derive0(gtc0, body)
                    case lookaheadSymbol: NLookaheadSymbol =>
                        addNode(derive0(gtc0, lookaheadSymbol.emptySeqId), nodeOf(lookaheadSymbol.lookahead))
                }
            case NSequence(_, seq) =>
                assert(seq.nonEmpty) // empty인 sequence는 derive시점에 모두 처리되어야 함
                assert(startState.kernel.pointer < seq.length) // node의 pointer는 sequence의 length보다 작아야 함
                derive0(gtc0, seq(startState.kernel.pointer))
        }
        (Cont(newGraph, cc.updatedFinalStatesMap), newTasks)
    }

    def finishTask(nextGen: Int, task: FinishTask, cc: Cont): (Cont, Seq[Task]) = {
        val FinishTask(node) = task

        assert(cc.graph.states contains node)
        assert(node.kernel.isFinal)
        assert(node.kernel.endGen == nextGen)

        // 원래는 cc.context.edgesByEnd(node.initial) 를 사용해야 하는데, trimming돼서 다 없어져버려서 그냥 둠
        // 사실 trimming 없으면 언제나 cc.context.edgesByEnd(node.initial) == cc.context.edgesByEnd(node)
        val incomingStates: Set[State] = cc.graph.expectsByEnd(node.kernelBase) map { _.state }
        val chainTasks: Seq[Task] = incomingStates.toSeq map { incomingState =>
            ProgressTask(incomingState, node.condition)
        }
        (cc, chainTasks)
    }

    def progressTask(nextGen: Int, task: ProgressTask, cc: Cont): (Cont, Seq[Task]) = {
        val ProgressTask(state, incomingCondition) = task

        assert(!state.kernel.isFinal)
        assert(cc.graph.states contains state)

        // nodeSymbolOpt에서 opt를 사용하는 것은 finish는 SequenceNode에 대해서도 실행되기 때문
        val newCondition = state.kernel.symbol match {
            case NLongest(_, longest) =>
                NotExists(state.kernel.beginGen, nextGen + 1, longest)(atomicSymbolOf(longest))
            case NExcept(_, _, except) =>
                Unless(state.kernel.beginGen, nextGen, except)(atomicSymbolOf(except))
            case NJoin(_, _, join) =>
                OnlyIf(state.kernel.beginGen, nextGen, join)(atomicSymbolOf(join))
            case NLookaheadIs(_, _, lookahead) =>
                Exists(nextGen, nextGen, lookahead)(atomicSymbolOf(lookahead))
            case NLookaheadExcept(_, _, lookahead) =>
                NotExists(nextGen, nextGen, lookahead)(atomicSymbolOf(lookahead))
            case _ => Always
        }
        val newKernel = {
            val kernel = state.kernel
            Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, nextGen)(kernel.symbol)
        }
        val updatedNode = State(newKernel, conjunct(state.condition, incomingCondition, newCondition))
        if (!(cc.graph.states contains updatedNode)) {
            val GraphTasksCont(newGraph, newTasks) = addNode(GraphTasksCont(cc.graph, List()), updatedNode)

            // cc에 updatedNodes에 state -> updatedNode 추가
            val newUpdatedNodesMap = if (updatedNode.isFinal) {
                cc.updatedFinalStatesMap + (updatedNode.kernelBase -> (cc.updatedFinalStatesMap.getOrElse(updatedNode.kernelBase, Set()) + updatedNode))
            } else {
                cc.updatedFinalStatesMap
            }

            (Cont(newGraph, newUpdatedNodesMap), newTasks)
        } else {
            // 할 일 없음
            // 그런데 이런 상황 자체가 나오면 안되는건 아닐까?
            (cc, Seq())
        }
    }

    def process(nextGen: Int, task: Task, cc: Cont): (Cont, Seq[Task]) =
        task match {
            case task: DeriveTask => deriveTask(nextGen, task, cc)
            case task: ProgressTask => progressTask(nextGen, task, cc)
            case task: FinishTask => finishTask(nextGen, task, cc)
        }

    @tailrec final def rec(nextGen: Int, tasks: List[Task], cc: Cont): Cont =
        tasks match {
            case task +: rest =>
                val (ncc, newTasks) = process(nextGen, task, cc)
                rec(nextGen, newTasks ++: rest, ncc)
            case List() => cc
        }

    // acceptable하지 않은 조건을 가진 기존의 노드에 대해서는 task를 진행하지 않는다
    def rec(nextGen: Int, tasks: List[Task], graph: ParsingContext): Cont =
        rec(nextGen, tasks, Cont(graph, Map()))

    def finishableTermNodes(graph: ParsingContext, nextGen: Int, input: Inputs.Input): Set[State] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(NTerminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.states collect {
            case node @ State(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def finishableTermNodes(graph: ParsingContext, nextGen: Int, input: Inputs.TermGroupDesc): Set[State] = {
        def acceptable(symbolId: Int): Boolean =
            grammar.nsymbols get symbolId match {
                case Some(NTerminal(terminal)) => terminal accept input
                case _ => false
            }
        graph.states collect {
            case node @ State(Kernel(symbolId, 0, `nextGen`, `nextGen`), _) if acceptable(symbolId) => node
        }
    }

    def termStates(ctx: ParsingContext, nextGen: Int): Set[State] = {
        ctx.states filter { node =>
            val isTerminal = node.kernel.symbol.isInstanceOf[NTerminal]
            (node.kernel.beginGen == nextGen) && isTerminal
        }
    }

    def processAcceptCondition(nextGen: Int, liftedGraph: ParsingContext, conditionAccumulate: ConditionAccumulate): (ConditionAccumulate, ParsingContext, ParsingContext) = {
        // 2a. Evaluate accept conditions
        val conditionsEvaluations: Map[AcceptCondition, AcceptCondition] = {
            val conditions = (liftedGraph.states map { _.condition }) ++ conditionAccumulate.unfixed.values.toSet
            (conditions map { condition =>
                condition -> condition.evaluate(nextGen, liftedGraph)
            }).toMap
        }
        // 2b. ConditionAccumulate update
        val nextConditionAccumulate: ConditionAccumulate = {
            //                val evaluated = wctx.conditionFate.unfixed map { kv => kv._1 -> kv._2.evaluate(nextGen, trimmedGraph) }
            //                val newConditions = (revertedGraph.finishedNodes map { _.condition } map { c => (c -> c) }).toMap
            //                evaluated ++ newConditions // filter { _._2 != False }
            conditionAccumulate.update(conditionsEvaluations)
        }
        // 2c. Update accept conditions in context
        val conditionUpdatedGraph = liftedGraph mapStateCondition { node =>
            State(node.kernel, conditionsEvaluations(node.condition))
        }
        // 2d. Remove never condition nodes
        val conditionFilteredGraph = conditionUpdatedGraph filterStates { _.condition != Never }

        (nextConditionAccumulate, conditionUpdatedGraph, conditionFilteredGraph)
    }

    def trimUnreachables(ctx: ParsingContext, start: State, ends: Set[State]): ParsingContext = {
        if (!(ctx.states contains start)) {
            ParsingContext(Set(), Set())
        } else {
            def visitFromStart(queue: List[SymbolIdAndBeginGen], bases: Set[SymbolIdAndBeginGen], states: Set[State], expectations: Set[Expectation]): (Set[State], Set[Expectation]) = {
                queue match {
                    case head +: rest =>
                        // expects의 모든 states

                        val reachableStates = ctx.statesByBase.getOrElse(head, Set())
                        val reachableExpects = reachableStates flatMap ctx.expectsByStart
                        val reachableBases = reachableExpects map { _.expect }

                        // head가 conditional nonterm인 경우 처리
                        val potentialReachableBases: Set[SymbolIdAndBeginGen] = head.symbol match {
                            case NExcept(_, _, except) => Set(SymbolIdAndBeginGen(except, head.beginGen)(head.symbol))
                            case NJoin(_, _, join) => Set(SymbolIdAndBeginGen(join, head.beginGen)(head.symbol))
                            // lookahead는 항상 바로 progress되므로 conditionReachables에서 처리됨
                            case _ => Set()
                        }
                        val newReachableBases = (reachableBases ++ potentialReachableBases) -- bases
                        visitFromStart(newReachableBases.toList ++: rest, bases ++ newReachableBases, states ++ reachableStates, expectations ++ reachableExpects)
                    case List() =>
                        (states, expectations)
                }
            }
            def visitFromEnd(queue: List[SymbolIdAndBeginGen], bases: Set[SymbolIdAndBeginGen], states: Set[State], expectations: Set[Expectation]): (Set[State], Set[Expectation]) = {
                queue match {
                    case head +: rest =>
                        val reachableExpects = ctx.expectsByEnd(head)
                        val reachableStates = reachableExpects map { _.state }
                        val reachableBases = reachableStates map { _.kernelBase }

                        val newReachableBases = reachableBases -- bases
                        visitFromEnd(newReachableBases.toList ++: rest, bases ++ newReachableBases, states ++ reachableStates, expectations ++ reachableExpects)
                    case List() =>
                        (states, expectations)
                }
            }

            val startExpects = ctx.expectsByStart(start)
            val startBases = startExpects map { _.expect }
            val reachableFromStart = visitFromStart(startBases.toList, startBases, Set(start), startExpects)

            val endBases = ends map { _.kernelBase }
            val reachableFromEnd = visitFromEnd(endBases.toList, endBases, ends, Set())

            // ParsingContext(reachableFromStart._1 intersect reachableFromEnd._1, reachableFromStart._2 intersect reachableFromEnd._2)
            // ParsingContext(reachableFromStart._1, reachableFromStart._2)
            ParsingContext(reachableFromEnd._1, reachableFromEnd._2)
        }
    }

    def trimGraph(ctx: ParsingContext, startNode: State, nextGen: Int): ParsingContext = {
        // 트리밍 - 사용이 완료된 터미널 노드/acceptCondition이 never인 지우기
        val trimmed1 = ctx filterStates { node =>
            // TODO node.kernel.isFinal 인 노드도 지워도 될까?
            (node.condition != Never) && (!node.kernel.isFinal) && (node.kernel.symbol match {
                case NTerminal(_) => node.kernel.beginGen == nextGen
                case _ => true
            })
        }
        // 2차 트리밍 - startNode와 accept condition에서 사용되는 노드에서 도달 불가능한 노드/새로운 terminal node로 도달 불가능한 노드 지우기
        trimUnreachables(trimmed1, startNode, termStates(ctx, nextGen))
    }
}
