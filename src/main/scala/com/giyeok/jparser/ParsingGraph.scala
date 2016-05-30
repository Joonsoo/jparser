package com.giyeok.jparser

import com.giyeok.jparser.Symbols._
import ParsingGraph._
import Inputs._

// DerivationGraph와 ParsingContextGraph는 모두 ParsingGraph를 상속받고 같은 노드/엣지 자료구조를 사용함
// 단, DerivationGraph에 나오는 gen은(AtomicNode.beginGen, SequenceNode.beginGen/endGen, result의 gen) 모두0이므로, 실제 ParsingContextGraph에 추가될 때 shiftGen해서 사용한다

object ParsingGraph {
    sealed trait Node {
        val symbol: Symbol
    }
    sealed trait NontermNode extends Node {
        val beginGen: Int
    }
    case object EmptyNode extends Node
    case class TermNode(symbol: Terminal) extends Node

    // liftBlockTrigger, liftRevertTrigger는 symbol에 따라서만 결정되는 것이므로 equals 등에서 고려할 필요가 없다
    case class AtomicNode(symbol: AtomicNonterm, beginGen: Int)(val liftBlockTrigger: Option[Node], val liftRevertType: Option[Trigger.Type.Value]) extends NontermNode
    // TODO Compaction
    // case class CompactedNode(symbols: Seq[AtomicNonterm]) extends Node
    case class SequenceNode(symbol: Sequence, pointer: Int, beginGen: Int, endGen: Int) extends NontermNode

    case class Trigger(node: Node, triggerType: Trigger.Type.Value)
    object Trigger {
        object Type extends Enumeration {
            val Lift, Alive, Wait = Value
        }
    }

    sealed trait Edge { val start: NontermNode }
    case class SimpleEdge(start: NontermNode, end: Node, revertTriggers: Set[Trigger]) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge {
        // start must be a node with join
        assert(start.symbol.isInstanceOf[Join])
    }
}

trait ParsingGraph[R <: ParseResult] {
    val nodes: Set[Node]
    val edges: Set[Edge]
    // results는 가장 마지막 surround를 안한 상태.
    // 즉 진짜 기존의 lift.parsedBy에 해당하는 값이 들어가는 것으로, 진짜 결과값을 구하려면 node.symbol로 surround를 한번 해줘야 한다
    // - 이렇게 해야 DerivationGraph 쓸 때 편함
    val results: Map[Node, Map[Set[Trigger], R]]
    // progresses는 시퀀스 노드의 현재까지 진행 상황을 나타내는 것이므로 여기서 R은 기존의 PraseTree.SequenceNode에 해당하는 것이어야 함
    val progresses: Map[SequenceNode, Map[Set[Trigger], R]]

    // (edges의 모든 노드)+(progresses의 trigger에 등장하는 노드)가 모두 nodes에 포함되어야 함
    assert({
        val nodesOfEdges = edges flatMap {
            _ match {
                case SimpleEdge(start, end, revertTriggers) => Set(start, end) ++ (revertTriggers map { _.node })
                case JoinEdge(start, end, join) => Set(start, end, join)
            }
        }
        val nodesOfReverters = (progresses flatMap { _._2 flatMap { _._1 map { _.node } } })
        (nodesOfEdges ++ nodesOfReverters) subsetOf nodes
    })
    // progresses의 keySet의 모든 노드가 nodes에 포함되어야 함
    assert(progresses.keySet.toSet[Node] subsetOf nodes)

    // Information Retrieval
    def resultOf(node: Node): Map[Set[Trigger], R] = ???
    def flatResultOf(node: Node, resultFunc: ParseResultFunc[R]): Option[R] = {
        val results = resultOf(node) map { _._2 }
        if (results.isEmpty) None
        else Some(results.foldLeft(results.head) { resultFunc.merge(_, _) })
    }
    def resultOf(node: Node, triggers: Set[Trigger]): Option[R] = ???
    def progressOf(node: SequenceNode, triggers: Set[Trigger]): R = progresses(node)(triggers)
    def incomingEdgesTo(node: Node): Set[Edge] = edges collect {
        case edge @ SimpleEdge(_, `node`, _) => edge
        case edge @ (JoinEdge(_, _, `node`) | JoinEdge(_, `node`, _)) => edge
    }
    def incomingSimpleEdgesTo(node: Node): Set[SimpleEdge] = edges collect {
        case edge @ SimpleEdge(_, `node`, _) => edge
    }
    def incomingJoinEdgesTo(node: Node): Set[JoinEdge] = edges collect {
        case edge @ (JoinEdge(_, _, `node`) | JoinEdge(_, `node`, _)) => edge.asInstanceOf[JoinEdge]
    }

    // Modification
    def updateResultOf[S <: ParsingGraph[R]](node: Node, triggers: Set[Trigger], newResult: R): S
    def updateProgressOf[S <: ParsingGraph[R]](node: SequenceNode, triggers: Set[Trigger], newProgress: R): S
    def withNodeEdgesProgresses[S <: ParsingGraph[R]](newNode: SequenceNode, newEdges: Set[Edge], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): S
    def withNodesEdgesResultsProgresses[S <: ParsingGraph[R]](newNodes: Set[Node], newEdges: Set[Edge], newResults: Map[Node, Map[Set[Trigger], R]], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): S
}

trait TerminalInfo[R <: ParseResult] extends ParsingGraph[R] {
    lazy val terminalNodes: Set[TermNode] = nodes collect { case node: TermNode => node }
    lazy val terminals: Set[Terminal] = terminalNodes map { _.symbol }
    lazy val termGroups: Set[TermGroupDesc] = {
        import Terminals._

        val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
        val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

        def sliceTermGroups(termGroups: Set[CharacterTermGroupDesc]): Set[CharacterTermGroupDesc] = {
            val charIntersects: Set[CharacterTermGroupDesc] = termGroups flatMap { term1 =>
                termGroups collect {
                    case term2 if term1 != term2 => term1 intersect term2
                } filterNot { _.isEmpty }
            }
            val essentials = (termGroups map { g => charIntersects.foldLeft(g) { _ - _ } }) filterNot { _.isEmpty }
            val intersections = if (charIntersects.isEmpty) Set() else sliceTermGroups(charIntersects)
            essentials ++ intersections
        }
        val charTermGroups = sliceTermGroups(charTerms)

        // TODO VirtualTermGroupDesc도 charTermGroups처럼 해야 되는지 고민해보기
        val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
            virtTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }
        val virtTermGroups = (virtTerms map { term =>
            virtIntersects.foldLeft(term) { _ - _ }
        }) ++ virtIntersects

        (charTermGroups ++ virtTermGroups) filterNot { _.isEmpty }
    }
}

trait Reachability[R <: ParseResult] extends TerminalInfo[R] {
    lazy val reachablesMap: Map[Node, Set[TermGroupDesc]] = {
        val cc = scala.collection.mutable.Map[Node, Set[TermGroupDesc]]()
        nodes foreach {
            case node @ TermNode(terminal) =>
                cc(node) = termGroups filter { terminal accept _ }
            case node => cc(node) = Set()
        }
        def reverseTraverse(node: Node): Unit = {
            val incomingEdges = incomingEdgesTo(node)
            incomingEdges foreach {
                case SimpleEdge(start, end, _) =>
                    if (!(cc(end) subsetOf cc(start))) {
                        cc(start) ++= cc(end)
                        reverseTraverse(start)
                    }
                case JoinEdge(start, end, join) =>
                    val intersect = cc(end) intersect cc(join)
                    if (!(intersect subsetOf cc(start))) {
                        cc(start) ++= intersect
                        reverseTraverse(start)
                    }
            }
        }
        terminalNodes foreach { node => reverseTraverse(node) }

        val result: Map[Node, Set[TermGroupDesc]] = cc.toMap
        assert(result.keySet == nodes)
        assert(edges forall {
            case SimpleEdge(start, end, _) =>
                result(end) subsetOf result(start)
            case JoinEdge(start, end, join) =>
                result(start) == (result(end) intersect result(join))
        })
        result
    }
}

object DGraph {
    case class BaseNode(symbol: Nonterm, pointer: Int) extends NontermNode {
        assert(symbol match {
            case _: AtomicNonterm => pointer == 0
            case Sequence(seq, _) => 0 <= pointer && pointer < seq.length
        })
    }
}

case class DGraph[R <: ParseResult](
        baseNode: DGraph.BaseNode,
        nodes: Set[Node],
        edges: Set[Edge],
        results: Map[Node, Map[Set[Trigger], R]],
        progresses: Map[SequenceNode, Map[Set[Trigger], R]]) extends ParsingGraph[R] with Reachability[R] {
    // baseNode가 nodes에 포함되어야 함
    assert(nodes contains baseNode)
    // baseNode를 제외하고는 전부 BaseNode가 아니어야 함
    assert((nodes - baseNode) forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })

    // DerivationGraph에 등장하는 모든 gen이 0이어야 한다
    assert(nodes forall {
        case EmptyNode | TermNode(_) => true
        case AtomicNode(_, beginGen) => beginGen == 0
        case SequenceNode(_, _, beginGen, endGen) => beginGen == 0 && endGen == 0
    })

    // Information Retrieval
    def edgesFromBaseNode = edges filter {
        case SimpleEdge(start, _, _) => start == baseNode
        case JoinEdge(start, _, _) =>
            // 이런 경우는 일반적으로 발생하진 않아야 함(visualize나 test시에만 발생 가능)
            start == baseNode
    }
    def edgesNotFromBaseNode = edges -- edgesFromBaseNode

    // Modification
    // - results와 progresses의 업데이트는 같은 원리로 동작하는데,
    //   - 해당하는 Node, Set[Trigger]에 대한 R이 없을 경우 새로 추가해주고
    //   - 같은 Node, Set[Trigger]에 대한 R이 이미 있는 경우 덮어쓴다.
    def withNodesEdgesResultsProgresses(newNodes: Set[Node], newEdges: Set[Edge], newResults: Map[Node, Map[Set[Trigger], R]], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): DGraph[R] = {
        assert(newNodes forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })
        val updatedResults = ??? // results ++ newResults
        val updatedProgresses = ??? // results ++ newProgresses
        DGraph(baseNode, nodes ++ newNodes, edges ++ newEdges, results ++ newResults, progresses ++ newProgresses)
    }
    def withNodeEdgesProgresses(newNode: Node, newEdges: Set[Edge], newProgresses: Map[SequenceNode, Map[Set[Trigger], R]]): DGraph[R] = {
        assert(!newNode.isInstanceOf[DGraph.BaseNode])
        val updatedResults = ??? // results ++ newResults
        val updatedProgresses = ??? // results ++ newProgresses
        DGraph(baseNode, nodes + newNode, edges ++ newEdges, results, progresses ++ newProgresses)
    }

    def updateResultOf(node: Node, triggers: Set[Trigger], result: R): DGraph[R] = {
        val updatedResults = ???
        DGraph(baseNode, nodes, edges, updatedResults, progresses)
    }
    def updateProgressOf(node: Node, triggers: Set[Trigger], progress: R): DGraph[R] = {
        val updatedProgresses = ???
        DGraph(baseNode, nodes, edges, results, updatedProgresses)
    }

    def shiftGen(gen: Int): DGraph[R] = {
        def shiftNode[T <: Node](node: T): T = node match {
            case n: AtomicNode => (AtomicNode(n.symbol, n.beginGen + gen)(n.liftBlockTrigger map { shiftNode _ }, n.liftRevertType)).asInstanceOf[T]
            case n: SequenceNode => SequenceNode(n.symbol, n.pointer, n.beginGen + gen, n.endGen + gen).asInstanceOf[T]
            case n => n
        }
        def shiftTrigger(trigger: Trigger): Trigger = Trigger(shiftNode(trigger.node), trigger.triggerType)
        val shiftedNodes: Set[Node] = nodes map { shiftNode _ }
        val shiftedEdges: Set[Edge] = edges map {
            case SimpleEdge(start, end, revertTriggers) => SimpleEdge(shiftNode(start), shiftNode(end), revertTriggers map { shiftTrigger _ })
            case JoinEdge(start, end, join) => JoinEdge(shiftNode(start), shiftNode(end), shiftNode(join))
        }
        val shiftedResults = results map { kv =>
            (shiftNode(kv._1), kv._2 map { p => (p._1 map { shiftTrigger _ }, p._2) })
        }
        val shiftedProgresses = progresses map { kv =>
            (shiftNode(kv._1), kv._2 map { p => (p._1 map { shiftTrigger _ }, p._2) })
        }
        // baseNode는 shiftGen 할 필요 없음
        DGraph(baseNode, shiftedNodes, shiftedEdges, shiftedResults, shiftedProgresses)
    }

    // Misc.
    lazy val sliceByTermGroups: Map[TermGroupDesc, Option[DGraph[R]]] = {
        (termGroups map { termGroup =>
            val subgraph = if (reachablesMap(baseNode) contains termGroup) {
                // TODO 다시 구현
                Some(this)
            } else None
            termGroup -> subgraph
        }).toMap
    }

    def subgraphTo(termGroup: TermGroupDesc): Option[DGraph[R]] =
        sliceByTermGroups(termGroup) ensuring (termGroups contains termGroup)
    def subgraphTo(input: ConcreteInput): Option[DGraph[R]] =
        termGroups find { _.contains(input) } flatMap { subgraphTo(_) }
    def trim: DGraph[R] = {
        // TODO baseNode에서 reachable한 node/edge로만 구성된 subgraph 반환
        this
    }
    def compaction: DGraph[R] = ???
}
