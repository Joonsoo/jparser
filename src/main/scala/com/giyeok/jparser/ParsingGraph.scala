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
    case object EmptyNode extends Node {
        val symbol = Empty
    }
    case class TermNode(symbol: Terminal, beginGen: Int) extends Node

    // liftBlockTrigger, liftRevertTrigger는 symbol에 따라서만 결정되는 것이므로 equals 등에서 고려할 필요가 없다
    case class AtomicNode(symbol: AtomicNonterm, beginGen: Int)(val liftBlockTrigger: Option[Node], val reservedReverterType: Option[Trigger.Type.Value]) extends NontermNode {
        override def toString = {
            s"(${symbol.toShortString}, $beginGen)"
        }
    }
    // TODO Compaction
    // case class CompactedNode(symbols: Seq[AtomicNonterm]) extends Node
    case class SequenceNode(symbol: Sequence, pointer: Int, beginGen: Int, endGen: Int) extends NontermNode {
        override def toString = {
            val (p, f) = symbol.seq.splitAt(pointer)
            val kernelStr = ((p map { _.toShortString }) ++ Seq("*") ++ (f map { _.toShortString })).mkString(" ")
            s"($kernelStr, $beginGen-$endGen)"
        }
    }

    case class Trigger(node: Node, triggerType: Trigger.Type.Value)
    object Trigger {
        object Type extends Enumeration {
            // Lift <> Wait, Alive <> Dead - 반대 관계
            val Lift, Wait, Alive, Dead = Value
        }
    }

    sealed trait Edge { val start: NontermNode }
    case class SimpleEdge(start: NontermNode, end: Node, revertTriggers: Set[Trigger]) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge {
        // start must be a node with join
        assert(start.symbol.isInstanceOf[Join])
    }
}

// Results는 ParsingGraph의 results/progresses에서 사용된다
class Results[N <: Node, R <: ParseResult](val resultsMap: Map[N, Map[Set[Trigger], R]]) {
    assert(!(resultsMap exists { _._2.isEmpty }))

    def contains(node: N): Boolean = resultsMap contains node
    def of(node: N): Option[Map[Set[Trigger], R]] = resultsMap get node
    def of(node: N, triggers: Set[Trigger]): Option[R] = (resultsMap get node) flatMap { m => m get triggers }
    def entries: Iterable[(N, Set[Trigger], R)] = resultsMap flatMap { kv => kv._2 map { p => (kv._1, p._1, p._2) } }

    // - results와 progresses의 업데이트는 같은 원리로 동작하는데,
    //   - 해당하는 Node, Set[Trigger]에 대한 R이 없을 경우 새로 추가해주고
    //   - 같은 Node, Set[Trigger]에 대한 R이 이미 있는 경우 덮어쓴다.
    def update(node: N, triggers: Set[Trigger], newResult: R): Results[N, R] = {
        resultsMap get node match {
            case Some(rMap) =>
                new Results(resultsMap + (node -> (rMap + (triggers -> newResult))))
            case None =>
                new Results(resultsMap + (node -> Map(triggers -> newResult)))
        }
    }
    def update(other: Results[N, R]): Results[N, R] = {
        other.entries.foldLeft(this) { (m, i) => m.update(i._1, i._2, i._3) }
    }
    def update(node: N, nodeResults: Map[Set[Trigger], R]): Results[N, R] = {
        nodeResults.foldLeft(this) { (m, i) => m.update(node, i._1, i._2) }
    }

    // results에서 node와 trigger를 map해서 변경한다
    // - 이 때, nodeMap과 triggerMap은 모두 1-1 대응 함수여야 한다. 즉, 두 개의 노드가 같은 노드로 매핑되거나, 두 개의 트리거가 하나의 트리거로 매핑되면 안된다
    def mapNodesTriggers(nodeMap: N => N, triggerMap: Set[Trigger] => Set[Trigger]): Results[N, R] = {
        val mappedMap = resultsMap map { kv =>
            val (node, results) = kv
            nodeMap(node) -> (results map { kv => triggerMap(kv._1) -> kv._2 })
        }
        new Results(mappedMap)
    }

    def filterTo(func: (N, Set[Trigger], R) => Boolean): Results[N, R] = {
        val filteredMap = resultsMap map { kv =>
            val (node, results) = kv
            node -> (results filter { kv => func(node, kv._1, kv._2) })
        }
        new Results(filteredMap filterNot { _._2.isEmpty })
    }

    // resultsMap과 등장하는 trigger 중에서 nodes에 포함되는 것만 남기고 전부 날려서 반환하는 함수
    // 만약 resultsMap에서 trigger를 날리다가 key가 같아지는 경우가 생기면 resultFunc로 merge
    def subresultOf(nodes: Set[Node], resultFunc: ParseResultFunc[R]): Results[N, R] = {
        val newResultsMap: Map[N, Map[Set[Trigger], R]] = resultsMap collect {
            case (node, tr) if nodes contains node =>
                val newTriggersResult = tr.foldLeft(Map[Set[Trigger], R]()) { (map, tr) =>
                    val (triggers, result) = tr
                    val filteredTriggers = triggers filter { t => nodes contains t.node }
                    map get filteredTriggers match {
                        case Some(existingResult) =>
                            map + (filteredTriggers -> (resultFunc.merge(existingResult, result)))
                        case None => map + (filteredTriggers -> result)
                    }
                }
                node -> newTriggersResult
        }
        assert(newResultsMap.keySet.asInstanceOf[Set[Node]] subsetOf nodes)
        new Results(newResultsMap)
    }

    def asMap = resultsMap
    def keyNodesSet = resultsMap.keySet
}
object Results {
    def apply[N <: Node, R <: ParseResult](): Results[N, R] = new Results(Map())
    def apply[N <: Node, R <: ParseResult](items: (N, Map[Set[ParsingGraph.Trigger], R])*): Results[N, R] =
        new Results((items map { kv => kv._1 -> kv._2 }).toMap)
}

trait ParsingGraph[R <: ParseResult] {
    val nodes: Set[Node]
    val edges: Set[Edge]
    // results는 가장 마지막 surround를 안한 상태.
    // 즉 진짜 기존의 lift.parsedBy에 해당하는 값이 들어가는 것으로, 진짜 결과값을 구하려면 node.symbol로 surround를 한번 해줘야 한다
    // - 이렇게 해야 DerivationGraph 쓸 때 편함
    val results: Results[Node, R]
    // progresses는 시퀀스 노드의 현재까지 진행 상황을 나타내는 것이므로 여기서 R은 기존의 PraseTree.SequenceNode에 해당하는 것이어야 함
    val progresses: Results[SequenceNode, R]

    // edges의 모든 노드가 nodes에 포함되어야 함
    // - progresses의 trigger에 등장하는 노드 중에는 다음 generation에서 발생할 노드가 포함될 수 있으므로 확인하지 않음
    assert({
        val nodesOfEdges = edges flatMap {
            _ match {
                case SimpleEdge(start, end, revertTriggers) => Set(start, end) // ++ (revertTriggers map { _.node })
                case JoinEdge(start, end, join) => Set(start, end, join)
            }
        }
        // val nodesOfReverters = (progresses.resultsMap flatMap { _._2 flatMap { _._1 map { _.node } } })

        nodesOfEdges subsetOf nodes
    })
    // progresses의 keySet의 모든 노드가 nodes에 포함되어야 함
    // assert(progresses.keyNodesSet.asInstanceOf[Set[Node]] subsetOf nodes)

    // Information Retrieval
    //    def resultOf(node: Node): Option[Map[Set[Trigger], R]] = results.of(node)
    //    def resultOf(node: Node, triggers: Set[Trigger]): Option[R] = results.of(node, triggers)
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
    def outgoingEdgesFrom(node: Node): Set[Edge] = edges collect {
        case edge @ SimpleEdge(`node`, _, _) => edge
        case edge @ JoinEdge(`node`, _, _) => edge
    }
    def outgoingSimpleEdgesFrom(node: Node): Set[SimpleEdge] = edges collect {
        case edge @ SimpleEdge(`node`, _, _) => edge
    }
    def outgoingJoinEdgesFrom(node: Node): Set[JoinEdge] = edges collect {
        case edge @ JoinEdge(`node`, _, _) => edge
    }

    // Modification
    def create(nodes: Set[Node], edges: Set[Edge], results: Results[Node, R], progresses: Results[SequenceNode, R]): ParsingGraph[R]
    def updateResultOf(node: Node, triggers: Set[Trigger], result: R): ParsingGraph[R] = {
        create(nodes, edges, results.update(node, triggers, result), progresses)
    }
    def updateProgressesOf(node: SequenceNode, nodeProgresses: Map[Set[Trigger], R]): ParsingGraph[R] = {
        create(nodes, edges, results, progresses.update(node, nodeProgresses))
    }
    def withNodeEdgesProgresses(newNode: SequenceNode, newEdges: Set[Edge], newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        create(nodes + newNode, edges ++ newEdges, results, progresses.update(newProgresses))
    }
    def withNodesEdgesProgresses(newNodes: Set[Node], newEdges: Set[Edge], newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        assert(newNodes forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })
        create(nodes ++ newNodes, edges ++ newEdges, results, progresses.update(newProgresses))
    }
    def withNoResults: ParsingGraph[R] = {
        create(nodes, edges, Results(), progresses)
    }
    def filterEdges(func: Edge => Boolean): ParsingGraph[R] = {
        create(nodes, edges filter { func(_) }, results, progresses)
    }
    def updateResults(newResults: Results[Node, R]): ParsingGraph[R] = {
        create(nodes, edges, newResults, progresses)
    }
    def updateProgresses(newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        create(nodes, edges, results, newProgresses)
    }
    def withNodes(newNodes: Set[Node]): ParsingGraph[R] = {
        create(nodes ++ newNodes, edges, results, progresses)
    }

    // nodes와 edges만 포함하는 서브그래프를 반환한다
    // - 이 때, nodes, edges, results, progresses에 붙어 있는 트리거 중 이 nodes에 포함되지 않은 것들은 제거한다
    // - results나 progresses에서 트리거를 제거하면 합쳐져야 할 엔트리들이 생기면 resultFunc로 merge해준다 
    def subgraphOf(subNodes: Set[Node], subEdges: Set[Edge], resultFunc: ParseResultFunc[R]): ParsingGraph[R] = {
        val triggerFilteredSubEdges = subEdges map {
            case SimpleEdge(start, end, revertTriggers) =>
                val filteredRevertTriggers = revertTriggers filter { subNodes contains _.node }
                SimpleEdge(start, end, filteredRevertTriggers)
            case e => e
        }
        // results는 이 그래프의 이전 세대 그래프 노드->결과 이기 때문에 subresultOf를 하지 않는다
        create(subNodes, triggerFilteredSubEdges, results, progresses.subresultOf(subNodes, resultFunc))
    }
    // start에서 ends(중 아무곳이나) 도달할 수 있는 모든 경로만 포함하는 서브그래프를 반환한다
    // - 노드가 start로부터 도달 가능하고, ends중 하나 이상으로 도달 가능해야 포함시킨다
    def subgraphIn(genLimit: Int, start: Node, ends: Set[Node], resultFunc: ParseResultFunc[R]): Option[ParsingGraph[R]] = {
        // TODO traverse할 때 SimpleEdge에서 revertTriggers 어떻게 해야 하는지 고민
        def traverseBackward(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
            case task +: rest =>
                val liftBlockTriggerNode = task match {
                    case node: AtomicNode => node.liftBlockTrigger
                    case _ => None
                }

                val incomingEdges = incomingEdgesTo(task)
                val reachables: Set[(Set[Node], Option[Edge])] = incomingEdges map {
                    case edge @ SimpleEdge(start, end, revertTriggers) =>
                        val newNodes: Set[Node] = Set(start) // ++ ((revertTriggers map { _.node }) intersect nodes)
                        (newNodes, Some(edge))
                    case edge @ JoinEdge(start, end, join) =>
                        if ((end == task && (nodesCC contains join)) || (join == task && (nodesCC contains end))) (Set[Node](start), Some(edge))
                        else (Set[Node](), None)
                }
                val (reachableNodes, reachableEdges) = reachables.foldLeft((liftBlockTriggerNode.toSet, Set[Edge]())) { (m, i) => (m._1 ++ i._1, m._2 ++ i._2) }
                traverseBackward(rest ++ (reachableNodes -- nodesCC).toList, nodesCC ++ reachableNodes, edgesCC ++ reachableEdges)
            case List() => (nodesCC, edgesCC)
        }
        def traverseForward(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
            case task +: rest =>
                // 이것도 빼야되나?
                val liftBlockTriggerNode = task match {
                    case node: AtomicNode => node.liftBlockTrigger
                    case _ => None
                }

                val outgoingEdges = outgoingEdgesFrom(task)
                val reachables: Set[(Set[Node], Edge)] = outgoingEdges map {
                    case edge @ SimpleEdge(start, end, revertTriggers) =>
                        val newNodes: Set[Node] = Set(end) // ++ ((revertTriggers map { _.node }) intersect nodes)
                        (newNodes, edge)
                    case edge @ JoinEdge(start, end, join) =>
                        (Set(end, join), edge)
                }
                val (reachableNodes, reachableEdges) = reachables.foldLeft((liftBlockTriggerNode.toSet, Set[Edge]())) { (m, i) => (m._1 ++ i._1, m._2 + i._2) }
                traverseForward(rest ++ (reachableNodes -- nodesCC).toList, nodesCC ++ reachableNodes, edgesCC ++ reachableEdges)
            case List() => (nodesCC, edgesCC)
        }

        val reachableToEnds = traverseBackward(ends.toList, ends, Set())
        if (!(reachableToEnds._1 contains start)) {
            None
        } else {
            val reachableFromStarts = traverseForward(List(start), Set(start), Set())

            val pendedNodes = nodes filter {
                case EmptyNode => false
                case TermNode(_, gen) => gen > genLimit
                case AtomicNode(_, gen) => gen > genLimit
                case SequenceNode(_, _, _, gen) => gen > genLimit
            }
            val subNodes = (reachableToEnds._1 intersect reachableFromStarts._1) ++ pendedNodes
            val subEdges = reachableToEnds._2 intersect reachableFromStarts._2
            assert(subNodes subsetOf nodes)
            Some(subgraphOf(subNodes, subEdges, resultFunc))
        }
    }
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
