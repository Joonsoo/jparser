package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

// AKernel with generation
// created와 updated는 0이나 1이며, created <= updated여야 한다.
// created와 updated는 각각 beginGen과 endGen과 비슷한 의미를 갖지만 실제 스트링에서의 위치가 아니라 이전 세대/현재 세대만 구분한다는 차이가 있다.
case class AKernelGen(symbolId: Int, pointer: Int, created: Int, updated: Int) {
    assert(created <= updated)
    assert(created == 0 || created == 1)
    assert(updated == 0 || updated == 1)

    def kernel = AKernel(symbolId, pointer)

    def initial = AKernelGen(symbolId, 0, created, created)
}

object AKernelGen {
    def from(kernel: AKernel, created: Int, updated: Int) =
        AKernelGen(kernel.symbolId, kernel.pointer, created, updated)
}

sealed trait Task {
    val node: AKernelGen
}

case class DeriveTask(node: AKernelGen) extends Task

case class FinishTask(node: AKernelGen) extends Task

case class ProgressTask(node: AKernelGen) extends Task

case class AKernelGenEdge(start: AKernelGen, end: AKernelGen) extends AbstractEdge[AKernelGen]

case class AKernelGenGraph(nodes: Set[AKernelGen], edges: Set[AKernelGenEdge], edgesByStart: Map[AKernelGen, Set[AKernelGenEdge]], edgesByEnd: Map[AKernelGen, Set[AKernelGenEdge]])
    extends AbstractGraph[AKernelGen, AKernelGenEdge, AKernelGenGraph] {
    def createGraph(nodes: Set[AKernelGen], edges: Set[AKernelGenEdge], edgesByStart: Map[AKernelGen, Set[AKernelGenEdge]], edgesByEnd: Map[AKernelGen, Set[AKernelGenEdge]]): AKernelGenGraph =
        AKernelGenGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object AKernelGenGraph {
    val emptyGraph = AKernelGenGraph(Set(), Set(), Map(), Map())
}


case class AKernelEdge(start: AKernel, end: AKernel) extends AbstractEdge[AKernel]

case class AKernelGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]])
    extends AbstractGraph[AKernel, AKernelEdge, AKernelGraph] {
    def createGraph(nodes: Set[AKernel], edges: Set[AKernelEdge], edgesByStart: Map[AKernel, Set[AKernelEdge]], edgesByEnd: Map[AKernel, Set[AKernelEdge]]): AKernelGraph =
        AKernelGraph(nodes, edges, edgesByStart, edgesByEnd)
}

object AKernelGraph {
    val emptyGraph = AKernelGraph(Set(), Set(), Map(), Map())
}

case class ParsingTaskSimulationResult(tasks: Set[Task], updateMap: Map[AKernelGen, Set[AKernelGen]], nextGraph: AKernelGenGraph) {
    lazy val deriveTasks: Set[DeriveTask] = tasks collect { case task: DeriveTask => task }
    lazy val finishTasks: Set[FinishTask] = tasks collect { case task: FinishTask => task }
    lazy val progressTasks: Set[ProgressTask] = tasks collect { case task: ProgressTask => task }
}

// nullable은 어떻게 처리해야되지??
class ParsingTaskSimulator(val grammar: NGrammar) {
    // deriveGraph는 simulate를 하러 오는 시점의 그래프이고, 리턴하는 그래프는 simulate한 뒤의 그래프이다
    // 같은 AKernel이 deriveGraph와 리턴한 그래프에 있어도 실제로는 endGen이 달라서 다른 노드.
    // DeriveTask에서는 grammar를 보고 추가할 노드 목록을 만들고, 새로 추가된 노드인지 확인하기 위해 cc.nextGraph를 사용
    // FinishTask에서는 incoming edge를 찾기 위해 기존 그래프인 deriveGraph를 사용
    private def simulate(queue: List[Task], boundaryTasks: Set[Task], cc: ParsingTaskSimulationResult): ParsingTaskSimulationResult = queue match {
        case head +: rest if boundaryTasks contains head =>
            simulate(rest, boundaryTasks, cc)
        case DeriveTask(node) +: rest =>
            val gen = node.updated
            // DeriveTask에서는 grammar를 사용
            val derivedNodes: Set[AKernelGen] = grammar.symbolOf(node.symbolId) match {
                case NTerminal(_) => Set()
                case NSequence(_, sequence) => Set(AKernelGen(sequence(node.pointer), 0, gen, gen))
                case simpleDerive: NSimpleDerive => simpleDerive.produces map (AKernelGen(_, 0, gen, gen))
                case NExcept(_, body, except) => ???
                case NJoin(_, body, join) => ???
                case NLongest(_, body) => ???
                case lookahead: NLookaheadSymbol => ???
            }
            val newDerivedNodes = derivedNodes -- cc.nextGraph.nodes
            // (derivedNodes intersect cc.nextGraph.nodes) 중 nullable인 것은 다시 NullableFinish해야함
            val nullableFinishes = derivedNodes flatMap { derived =>
                if (grammar.lastPointerOf(derived.symbolId) == 0) Set(FinishTask(derived)) else {
                    val fins = cc.updateMap.getOrElse(derived.initial, Set()) filter { k =>
                        k.pointer == grammar.lastPointerOf(k.symbolId)
                    }
                    fins map FinishTask
                }
            }
            val nextGraph0 = newDerivedNodes.foldLeft(cc.nextGraph) { (g, n) => g.addNode(n) }
            val nextGraph = derivedNodes.foldLeft(nextGraph0) { (g, n) => g.addEdge(AKernelGenEdge(node, n)) }
            val newTasks: Set[Task] = newDerivedNodes map { n =>
                grammar.symbolOf(n.symbolId) match {
                    case NSequence(_, seq) if seq.isEmpty => FinishTask(n)
                    case _ => DeriveTask(n)
                }
            }
            simulate(rest ++ (nullableFinishes ++ newTasks).toList, boundaryTasks,
                ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.updateMap, nextGraph))

        case FinishTask(node) +: rest =>
            val initialNode = node.initial
            if (cc.nextGraph.nodes contains initialNode) {
                val incomingNodes = cc.nextGraph.edgesByEnd(initialNode) map (_.start)
                val newTasks = incomingNodes map ProgressTask
                simulate(newTasks.toList ++ rest, boundaryTasks,
                    ParsingTaskSimulationResult(cc.tasks ++ newTasks, cc.updateMap, cc.nextGraph))
            } else {
                // initialNode가 그래프상에 없으면 무시하면 될듯
                simulate(rest, boundaryTasks, cc)
            }

        case ProgressTask(node) +: rest =>
            val newKernelGen = AKernelGen(node.symbolId, node.pointer + 1, node.created, 1)
            val newTask = grammar.symbolOf(node.symbolId) match {
                case NSequence(_, seq) if newKernelGen.pointer < seq.size => DeriveTask(newKernelGen)
                case _ => FinishTask(newKernelGen)
            }
            val newUpdateMap = cc.updateMap +
                (node.initial -> (cc.updateMap.getOrElse(node.initial, Set()) + newKernelGen))
            simulate(newTask +: rest, boundaryTasks,
                ParsingTaskSimulationResult(cc.tasks + newTask, newUpdateMap, cc.nextGraph.addNode(newKernelGen)))

        case List() => cc
    }

    // deriveGraph에서 tasks를 실행하면서 실행되는 task들과, 모든 task가 실행된 뒤의 graph를 반환한다
    def simulate(baseGraph: AKernelGenGraph, tasks: List[Task], boundaryTasks: Set[Task]): ParsingTaskSimulationResult =
        simulate(tasks, boundaryTasks, ParsingTaskSimulationResult(tasks.toSet, Map(), baseGraph))
}
