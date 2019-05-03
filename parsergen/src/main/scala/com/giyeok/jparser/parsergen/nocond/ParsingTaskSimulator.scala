package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

sealed trait Task {
    val node: AKernel
}

case class DeriveTask(node: AKernel) extends Task

case class FinishTask(node: AKernel) extends Task

case class NullableFinishTask(node: AKernel) extends Task

case class ProgressTask(node: AKernel) extends Task

case class NullableProgressTask(node: AKernel) extends Task


case class AKernelEdge(start: AKernel, end: AKernel) extends AbstractEdge[AKernel]

case class AKernelGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]])
    extends AbstractGraph[AKernel, AKernelEdge, AKernelGraph] {
    def createGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]]): AKernelGraph =
        AKernelGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object AKernelGraph {
    val emptyGraph = AKernelGraph(Set(), Set(), Map(), Map())
}

case class ParsingTaskSimulationResult(tasks: Set[Task], nullableSymbolIds: Set[Int], nextGraph: AKernelGraph) {
    lazy val deriveTasks: Set[DeriveTask] = tasks collect { case task: DeriveTask => task }
    lazy val finishTasks: Set[FinishTask] = tasks collect { case task: FinishTask => task }
    lazy val nullableFinishTasks: Set[NullableFinishTask] = tasks collect { case task: NullableFinishTask => task }
    lazy val progressTasks: Set[ProgressTask] = tasks collect { case task: ProgressTask => task }
    lazy val nullableProgressTasks: Set[NullableProgressTask] = tasks collect { case task: NullableProgressTask => task }
}

// nullable은 어떻게 처리해야되지??
class ParsingTaskSimulator(val grammar: NGrammar) {
    // deriveGraph는 simulate를 하러 오는 시점의 그래프이고, 리턴하는 그래프는 simulate한 뒤의 그래프이다
    // 같은 AKernel이 deriveGraph와 리턴한 그래프에 있어도 실제로는 endGen이 달라서 다른 노드.
    // DeriveTask에서는 grammar를 보고 추가할 노드 목록을 만들고, 새로 추가된 노드인지 확인하기 위해 cc.nextGraph를 사용
    // FinishTask에서는 incoming edge를 찾기 위해 기존 그래프인 deriveGraph를 사용
    private def simulate(baseGraph: AKernelGraph, queue: List[Task], cc: ParsingTaskSimulationResult): ParsingTaskSimulationResult = queue match {
        case DeriveTask(node) +: rest =>
            // DeriveTask에서는 grammar를 사용
            val derivedNodes: Set[AKernel] = grammar.symbolOf(node.symbolId) match {
                case NTerminal(_) => Set()
                case NSequence(_, sequence) => Set(AKernel(sequence(node.pointer), 0))
                case simpleDerive: NSimpleDerive => simpleDerive.produces map (AKernel(_, 0))
                case NExcept(_, body, except) => ???
                case NJoin(_, body, join) => ???
                case NLongest(_, body) => ???
                case lookahead: NLookaheadSymbol => ???
            }
            val newDerivedNodes = derivedNodes -- cc.nextGraph.nodes
            // (derivedNodes intersect cc.nextGraph.nodes) 중 nullable인 것은 다시 NullableFinish해야함
            val repeatingNullableFinishes =
                derivedNodes intersect cc.nextGraph.nodes map (_.symbolId) intersect cc.nullableSymbolIds map { symbolId =>
                    NullableFinishTask(AKernel(symbolId, grammar.lastPointerOf(symbolId)))
                }
            assert(repeatingNullableFinishes subsetOf cc.nullableFinishTasks)
            val nextGraph0 = newDerivedNodes.foldLeft(cc.nextGraph) { (g, n) => g.addNode(n) }
            val nextGraph = derivedNodes.foldLeft(nextGraph0) { (g, n) => g.addEdge(AKernelEdge(node, n)) }
            val newTasks: Set[Task] = newDerivedNodes map { n =>
                grammar.symbolOf(n.symbolId) match {
                    case NSequence(_, seq) if seq.isEmpty => NullableFinishTask(n)
                    case _ => DeriveTask(n)
                }
            }
            simulate(baseGraph, rest ++ repeatingNullableFinishes.toList ++ newTasks,
                ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.nullableSymbolIds, nextGraph))

        case FinishTask(node) +: rest =>
            // FinishTask에서는 새로 생긴 노드는 무시해야하므로 baseGraph 사용
            if (baseGraph.nodes contains AKernel(node.symbolId, 0)) {
                val incomingNodes = baseGraph.edgesByEnd(AKernel(node.symbolId, 0)) map (_.start)
                val newTasks = incomingNodes map ProgressTask
                simulate(baseGraph, newTasks.toList ++ rest,
                    ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.nullableSymbolIds, cc.nextGraph))
            } else {
                simulate(baseGraph, rest, cc)
            }
        case NullableFinishTask(node) +: rest =>
            // FinishTask와 유사하지만 baseGraph 및 cc.nextGraph에서도 찾음
            val zeroKernel = AKernel(node.symbolId, 0)
            if (cc.nextGraph.nodes contains zeroKernel) {
                val incomingNodes = cc.nextGraph.edgesByEnd(zeroKernel) map (_.start)
                val newTasks = incomingNodes map NullableProgressTask
                simulate(baseGraph, newTasks.toList ++ rest,
                    ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.nullableSymbolIds + node.symbolId, cc.nextGraph))
            } else if (baseGraph.nodes contains zeroKernel) {
                val incomingNodes = baseGraph.edgesByEnd(zeroKernel) map (_.start)
                val newTasks = incomingNodes map ProgressTask
                simulate(baseGraph, newTasks.toList ++ rest,
                    ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.nullableSymbolIds + node.symbolId, cc.nextGraph))
            } else simulate(baseGraph, rest, cc)

        case ProgressTask(node) +: rest =>
            val newKernel = AKernel(node.symbolId, node.pointer + 1)
            val newTask = grammar.symbolOf(node.symbolId) match {
                case NSequence(_, seq) if newKernel.pointer < seq.size => DeriveTask(newKernel)
                case _ => FinishTask(newKernel)
            }
            simulate(baseGraph, newTask +: rest,
                ParsingTaskSimulationResult(cc.tasks + newTask, cc.nullableSymbolIds, cc.nextGraph.addNode(newKernel)))
        case NullableProgressTask(node) +: rest =>
            // ProgressTask와 동일한데 FinishTask 대신 NullableFinishTask를 생성함
            val newKernel = AKernel(node.symbolId, node.pointer + 1)
            val newTask = grammar.symbolOf(node.symbolId) match {
                case NSequence(_, seq) if newKernel.pointer < seq.size => DeriveTask(newKernel)
                case _ => NullableFinishTask(newKernel)
            }
            simulate(baseGraph, newTask +: rest,
                ParsingTaskSimulationResult(cc.tasks + newTask, cc.nullableSymbolIds, cc.nextGraph.addNode(newKernel)))
        case List() => cc
    }

    // deriveGraph에서 tasks를 실행하면서 실행되는 task들과, 모든 task가 실행된 뒤의 graph를 반환한다
    def simulate(baseGraph: AKernelGraph, initNextGraph: AKernelGraph, tasks: List[Task]): ParsingTaskSimulationResult =
        simulate(baseGraph, tasks, ParsingTaskSimulationResult(tasks.toSet, Set(), initNextGraph))

    def simulateProgress(baseGraph: AKernelGraph, tasks: List[ProgressTask]): ParsingTaskSimulationResult =
        simulate(baseGraph, AKernelGraph.emptyGraph, tasks)
}
