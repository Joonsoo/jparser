package com.giyeok.jparser

import Symbols._
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.AbstractInput
import com.giyeok.jparser.Inputs.ConcreteInput
import Symbols.Terminals._
import Inputs._
import ParsingGraph._
import com.giyeok.jparser.DGraph.BaseNode
import com.giyeok.jparser.DGraph.BaseAtomicNode
import com.giyeok.jparser.DGraph.BaseSequenceNode

object DGraph {
    sealed trait BaseNode
    class BaseAtomicNode(_symbol: AtomicNonterm) extends AtomicNode(_symbol, 0) with BaseNode
    class BaseSequenceNode(_symbol: Sequence, _pointer: Int) extends SequenceNode(_symbol, _pointer, 0, 0) with BaseNode
}

case class DGraph[R <: ParseResult](
        baseNode: DGraph.BaseNode with Node,
        nodes: Set[Node],
        edges: Set[Edge],
        results: Results[Node, R],
        progresses: Results[SequenceNode, R],
        baseResults: Map[Condition, ParseResultWithType[R]],
        baseProgresses: Map[Condition, BindedResult[R]]) extends ParsingGraph[R] with TerminalInfo[R] {
    // baseNode가 nodes에 포함되어야 함 - subgraph할 때 그렇지 않을 수 있어서 assert 제외
    // assert(nodes contains baseNode.asInstanceOf[Node])
    // baseNode를 제외하고는 전부 BaseNode가 아니어야 함
    assert((nodes - baseNode) forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })

    // baseNode의 result나 progress는 results/progresses에는 있어선 안 되고 
    assert(results.of(baseNode).isEmpty && (!baseNode.isInstanceOf[SequenceNode] || progresses.of(baseNode.asInstanceOf[SequenceNode]).isEmpty))

    // Information Retrieval
    val nonBaseNodes = nodes - baseNode
    lazy val edgesFromBaseNode = edges filter {
        case SimpleEdge(start, _) => start == baseNode
        case JoinEdge(start, _, _) =>
            // 이런 경우는 일반적으로 발생하진 않아야 함(visualize나 test시에만 발생 가능)
            start == baseNode
    }
    lazy val edgesNotFromBaseNode = edges -- edgesFromBaseNode

    override def nodesInResultsAndProgresses: Set[Node] = {
        val supers = super.nodesInResultsAndProgresses
        val nodesInBaseResults = (baseResults flatMap { _._1.nodes })
        val nodesInBaseProgresses = (baseProgresses flatMap { _._1.nodes })
        supers ++ nodesInBaseResults ++ nodesInBaseProgresses
    }

    // Modification
    def create(nodes: Set[Node], edges: Set[Edge], results: Results[Node, R], progresses: Results[SequenceNode, R]): DGraph[R] =
        DGraph(baseNode, nodes, edges, results, progresses, baseResults, baseProgresses)

    def withNoBaseResults: DGraph[R] = DGraph(baseNode, nodes, edges, results, progresses, Map(), Map())

    def updateBaseResults(condition: Condition, resultAndSymbol: ParseResultWithType[R]): DGraph[R] = {
        val newBaseResults: Map[Condition, ParseResultWithType[R]] = baseResults + (condition -> resultAndSymbol)
        DGraph(baseNode, nodes, edges, results, progresses, newBaseResults, baseProgresses)
    }
    def updateBaseProgresses(condition: Condition, childAndSymbol: BindedResult[R]): DGraph[R] = {
        val newBaseProgresses: Map[Condition, BindedResult[R]] = baseProgresses + (condition -> childAndSymbol)
        DGraph(baseNode, nodes, edges, results, progresses, baseResults, newBaseProgresses)
    }
}

class DerivationFunc[R <: ParseResult](val grammar: Grammar, val resultFunc: ParseResultFunc[R])
        extends LiftTasks[R, DGraph[R]] with DeriveTasks[R, DGraph[R]] {
    // cc에서 task를 처리해서
    // (변경된 cc, 새로 추가되어야 하는 task)를 반환한다
    // - 나중에 visualize할 수 있게 하려고 process/rec 분리했음
    def process(task: Task, cc: DGraph[R]): (DGraph[R], Seq[Task]) = {
        task match {
            case task: DeriveTask => deriveTask(task, cc)
            case task: FinishingTask =>
                if (task.node.isInstanceOf[BaseNode]) {
                    assert(task.node == cc.baseNode)
                    (cc.updateBaseResults(task.condition, task.resultWithType), Seq())
                } else finishingTask(task, cc)
            case task: SequenceProgressTask =>
                if (task.node.isInstanceOf[BaseNode]) {
                    assert(task.node == cc.baseNode)
                    (cc.updateBaseProgresses(task.condition, task.childAndSymbol), Seq())
                } else sequenceProgressTask(task, cc)
        }
    }

    def rec(tasks: List[Task], cc: DGraph[R]): DGraph[R] =
        tasks match {
            case task +: rest =>
                val (newCC, newTasks) = process(task, cc)
                rec((newTasks.toList ++ rest).distinct, newCC)
            case List() => cc
        }

    def derive(baseNode: BaseNode with NontermNode): DGraph[R] = {
        val dgraph = rec(List(DeriveTask(0, baseNode)), DGraph(baseNode, Set(baseNode), Set(), Results(), Results(), Map(), Map()))
        assert(dgraph.nodes forall {
            case TermNode(_, beginGen) => beginGen <= 1
            case _: DGraph.BaseNode => true
            case AtomicNode(_, beginGen) => beginGen <= 1
            case SequenceNode(_, _, beginGen, endGen) => beginGen <= 1 && endGen <= 1
        })
        dgraph
    }

    def deriveAtomic(symbol: AtomicNonterm): DGraph[R] = derive(new BaseAtomicNode(symbol))
    def deriveSequence(symbol: Sequence, pointer: Int): DGraph[R] = derive(new BaseSequenceNode(symbol, pointer))

    def compaction(dgraph: DGraph[R]): DGraph[R] = {
        // Compaction
        // 가능한 지점에서 atomic node path를 하나의 atomic node로 묶어서 반환
        // atomic node이되 symbol이 특수 심볼
        ???
    }
}

class DerivationSliceFunc[R <: ParseResult](grammar: Grammar, resultFunc: ParseResultFunc[R])
        extends DerivationFunc[R](grammar, resultFunc) {
    def preprocess: Unit = {
        grammar.usedSymbols foreach {
            case symbol: AtomicNonterm =>
                val dgraph = deriveAtomic(symbol)
                derivationGraphCache((symbol, 0)) = dgraph
                derivationSliceCache((symbol, 0)) = sliceByTermGroups(dgraph)
            case symbol: Sequence =>
                (0 until symbol.seq.length) foreach { pointer =>
                    val dgraph = deriveSequence(symbol, pointer)
                    derivationGraphCache((symbol, pointer)) = dgraph
                    derivationSliceCache((symbol, pointer)) = sliceByTermGroups(dgraph)
                }
            case _ => // nothing to do
        }
    }

    private val derivationGraphCache = scala.collection.mutable.Map[(Nonterm, Int), DGraph[R]]()
    private val derivationSliceCache = scala.collection.mutable.Map[(Nonterm, Int), Map[TermGroupDesc, (DGraph[R], Set[NontermNode])]]()

    def kernelOf(node: NontermNode) = node match {
        case AtomicNode(symbol, _) => (symbol, 0)
        case SequenceNode(symbol, pointer, _, _) => (symbol, pointer)
    }
    def derive(baseNode: NontermNode): DGraph[R] = {
        val kernel = kernelOf(baseNode)
        derivationGraphCache get kernel match {
            case Some(cached) => cached
            case None =>
                val dgraph = baseNode match {
                    case _: DGraph.BaseNode => // BaseNode일 수 없음
                        throw new AssertionError("")
                    case AtomicNode(symbol, _) => deriveAtomic(symbol)
                    case SequenceNode(symbol, pointer, _, _) => deriveSequence(symbol, pointer)
                }
                derivationGraphCache(kernel) = dgraph
                dgraph
        }
    }
    def deriveSlice(baseNode: NontermNode): Map[TermGroupDesc, (DGraph[R], Set[NontermNode])] = {
        val kernel = kernelOf(baseNode)
        derivationSliceCache get kernel match {
            case Some(cache) => cache
            case None =>
                val sliceMap = sliceByTermGroups(derive(baseNode))
                derivationSliceCache(kernel) = sliceMap
                sliceMap
        }
    }

    def sliceByTermGroups(dgraph: DGraph[R]): Map[TermGroupDesc, (DGraph[R], Set[NontermNode])] = {
        val baseNode = dgraph.baseNode
        (dgraph.termGroups flatMap { termGroup =>
            // 이 곳에서의 일은 원래 NewParser에서 하던 일의 일부를 미리 해두는 것이라고 보면 된다
            // - 즉, baseNode 이하 derivation graph에서의 1차 리프트+1차 트리밍을 여기서 대신 해주는 것
            // 실제 파싱할 때는
            // - 기존 expand와 마찬가지로
            //   - 노드 및 엣지 추가하고
            //   - results와 progresses도 trigger들을 shiftGen하고 result는 resultFunc.substTermFunc해서 추가해준다
            // - 새로 추가된 derivables에 대한 DeriveTask와
            // - baseNode에 대한 FinishingTask/SequenceProgressTask 들을 진행한다
            //   - 이 때, Task들도 모두 nextGen으로 shiftGen 해서 진행해야 함
            def lift(graph: DGraph[R], nextGen: Int, termNodes: Set[TermNode]): (DGraph[R], Set[SequenceNode]) = {
                val initialTasks = termNodes map { termNode => FinishingTask(1, termNode, TermResult(resultFunc.termFunc()), Condition.True) }
                // FinishingTask.node가 liftBlockedNodes에 있으면 해당 task는 제외
                // DeriveTask(node)가 나오면 실제 Derive 진행하지 않고 derivation tip nodes로 넣는다 (이 때 node는 항상 SequenceNode임)
                def rec(tasks: List[Task], graphCC: DGraph[R], derivablesCC: Set[SequenceNode]): (DGraph[R], Set[SequenceNode]) =
                    tasks match {
                        case task +: rest =>
                            task match {
                                case task: DeriveTask =>
                                    assert(!task.baseNode.isInstanceOf[BaseNode] && task.baseNode.isInstanceOf[SequenceNode])

                                    val immediateProgresses: Seq[SequenceProgressTask] = (derive(task.baseNode).baseProgresses map { kv =>
                                        val (condition, childAndSymbol) = kv
                                        // triggers를 nextGen만큼 shift해주기
                                        val shiftedCondition: Condition = condition.shiftGen(nextGen)
                                        val shiftedResult = childAndSymbol mapResult { resultFunc.shift(_, nextGen) }

                                        SequenceProgressTask(nextGen, task.baseNode.asInstanceOf[SequenceNode], shiftedResult, shiftedCondition)
                                    }).toSeq

                                    rec(rest ++ immediateProgresses, graphCC, derivablesCC + task.baseNode.asInstanceOf[SequenceNode])
                                case task: FinishingTask =>
                                    if (task.node.isInstanceOf[BaseNode]) {
                                        assert(task.node == baseNode)
                                        rec(rest, graphCC.updateBaseResults(task.condition, task.resultWithType), derivablesCC)
                                    } else {
                                        val (newGraphCC, newTasks) = finishingTask(task, graphCC)
                                        rec(rest ++ newTasks, newGraphCC, derivablesCC)
                                    }
                                case task: SequenceProgressTask =>
                                    if (task.node.isInstanceOf[BaseNode]) {
                                        assert(task.node == baseNode)
                                        rec(rest, graphCC.updateBaseProgresses(task.condition, task.childAndSymbol), derivablesCC)
                                    } else {
                                        val (newGraphCC, newTasks) = sequenceProgressTask(task, graphCC)
                                        rec(rest ++ newTasks, newGraphCC, derivablesCC)
                                    }
                            }
                        case List() => (graphCC, derivablesCC)
                    }
                rec(initialTasks.toList, graph.withNoBaseResults.withNoResults.asInstanceOf[DGraph[R]], Set())
            }

            // eligibleTerminalNodes에서 각 terminalNode의 result로 resultFunc.termFunc()를 주고 시작해서 lift 및 trimming을 한 번 진행한다
            val eligibleTerminalNodes = dgraph.terminalNodes filter { _.symbol.accept(termGroup) }

            val (liftedGraph, derivables) = lift(dgraph, 1, eligibleTerminalNodes)
            // assert(liftedGraph.progresses.keyNodesSet == derivables)
            val trimmingStartNodes = Set(baseNode) ++ dgraph.nodesInResultsAndProgresses
            val trimmedGraph = liftedGraph.subgraphIn(trimmingStartNodes, derivables.asInstanceOf[Set[Node]], resultFunc).asInstanceOf[DGraph[R]]

            // trimmedGraph에 등장하는 노드는 모두 gen이 0이나 1이어야 함
            // - 원래 있던 노드는 gen이 0이고 새로 생긴 노드는 gen이 1이어야 함

            if (!trimmedGraph.nodes.isEmpty) {
                assert(trimmedGraph.results == liftedGraph.results && trimmedGraph.baseResults == liftedGraph.baseResults && trimmedGraph.baseProgresses == liftedGraph.baseProgresses)
                val survivedDerivables: Set[NontermNode] = (trimmedGraph.nodes intersect derivables.asInstanceOf[Set[Node]]).asInstanceOf[Set[NontermNode]]
                Some(termGroup -> ((trimmedGraph, survivedDerivables)))
            } else {
                if (!(liftedGraph.baseResults.isEmpty) || !(liftedGraph.baseProgresses.isEmpty)) {
                    val sliceGraph = DGraph(baseNode, Set[Node](baseNode), Set(), liftedGraph.results, Results(), liftedGraph.baseResults, liftedGraph.baseProgresses)
                    Some(termGroup -> ((sliceGraph, Set[NontermNode]())))
                } else if (!(liftedGraph.results.isEmpty)) {
                    val sliceGraph = DGraph(baseNode, Set[Node](baseNode), Set(), liftedGraph.results, Results(), Map(), Map())
                    Some(termGroup -> ((sliceGraph, Set[NontermNode]())))
                } else {
                    None
                }
            }
        }).toMap
    }
}
