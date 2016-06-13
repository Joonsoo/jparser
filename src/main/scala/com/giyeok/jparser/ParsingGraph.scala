package com.giyeok.jparser

import com.giyeok.jparser.Symbols._
import ParsingGraph._
import Inputs._

// DerivationGraph와 ParsingContextGraph는 모두 ParsingGraph를 상속받고 같은 노드/엣지 자료구조를 사용함
// 단, DerivationGraph에 나오는 gen은(AtomicNode.beginGen, SequenceNode.beginGen/endGen, result의 gen) 모두0이므로, 실제 ParsingContextGraph에 추가될 때 shiftGen해서 사용한다

object ParsingGraph {
    sealed trait Node {
        val symbol: Symbol

        def shiftGen(shiftGen: Int): Node
    }
    sealed trait NontermNode extends Node {
        val beginGen: Int
    }
    case class TermNode(symbol: Terminal, beginGen: Int) extends Node {
        def shiftGen(shiftGen: Int) = TermNode(symbol, beginGen + shiftGen)

        override def toString = s"(${symbol.toShortString}, $beginGen)"
    }

    // liftBlockTrigger, liftRevertTrigger는 symbol에 따라서만 결정되는 것이므로 equals 등에서 고려할 필요가 없다
    case class AtomicNode(symbol: AtomicNonterm, beginGen: Int)(val liftBlockTrigger: Option[Node]) extends NontermNode {
        def noLiftBlockTrigger = AtomicNode(symbol, beginGen)(None)
        def shiftGen(shiftGen: Int) = AtomicNode(symbol, beginGen + shiftGen)(liftBlockTrigger map { _.shiftGen(shiftGen) })

        override def toString = {
            s"(${symbol.toShortString}, $beginGen)"
        }
    }
    case class SequenceNode(symbol: Sequence, pointer: Int, beginGen: Int, endGen: Int) extends NontermNode {
        def shiftGen(shiftGen: Int) = SequenceNode(symbol, pointer, beginGen + shiftGen, endGen + shiftGen)
        override def toString = {
            val (p, f) = symbol.seq.splitAt(pointer)
            val kernelStr = ((p map { _.toShortString }) ++ Seq("*") ++ (f map { _.toShortString })).mkString(" ")
            s"($kernelStr, $beginGen-$endGen)"
        }
    }

    sealed trait Condition {
        def nodes: Set[Node]
        def shiftGen(shiftGen: Int): Condition

        def permanentTrue: Boolean
        def permanentFalse: Boolean

        // evaluate return: (현재 상황에서 이 조건을 가진 results를 사용해도 되는지/아닌지, 이 조건을 다음에 어떻게 바꿔야 하는지)
        // - edge에 붙어있는 Condition에서는 _1은 의미가 없고 _2가 Value(false)로 나오는 경우 그 엣지를 제거하면 되고, 그 외의 경우엔 엣지의 Condition을 _2로 바꾸면 된다
        def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition
        def eligible: Boolean
        def neg: Condition
    }
    object Condition {
        val True: Condition = Value(true)
        val False: Condition = Value(false)
        def Lift(node: Node, pendedUntilGen: Int): Condition = TrueUntilLifted(node, pendedUntilGen)
        def Wait(node: Node, pendedUntilGen: Int): Condition = FalseUntilLifted(node, pendedUntilGen)
        def Alive(node: Node, pendedUntilGen: Int): Condition = TrueUntilAlive(node, pendedUntilGen)
        def conjunct(conds: Condition*): Condition = {
            if (conds forall { _.permanentTrue }) True
            else if (conds exists { _.permanentFalse }) False
            else {
                val conds1 = conds.toSet filterNot { _.permanentTrue }
                if (conds1.size == 1) conds1.head else And(conds1)
            }
        }
        def disjunct(conds: Condition*): Condition = {
            if (conds exists { _.permanentTrue }) True
            else if (conds forall { _.permanentFalse }) False
            else {
                val conds1 = conds.toSet filterNot { _.permanentFalse }
                if (conds1.size == 1) conds1.head else Or(conds1)
            }
        }

        // 지금부터 영영 true 혹은 false임을 의미
        case class Value(value: Boolean) extends Condition {
            def nodes = Set()
            def shiftGen(shiftGen: Int) = this

            def permanentTrue = value == true
            def permanentFalse = value == false

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = this
            def eligible = value
            def neg: Condition = Value(!value)
        }
        case class And(conds: Set[Condition]) extends Condition {
            def nodes = conds flatMap { _.nodes }
            def shiftGen(shiftGen: Int) = And(conds map { _.shiftGen(shiftGen) })

            def permanentTrue = conds forall { _.permanentTrue }
            def permanentFalse = conds forall { _.permanentFalse }

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                conds.foldLeft(Condition.True) { (cc, condition) =>
                    Condition.conjunct(condition.evaluate(gen, results, aliveNodes), cc)
                }
            }
            def eligible = conds forall { _.eligible }
            def neg: Condition = {
                Condition.disjunct((conds map { _.neg }).toSeq: _*)
            }
        }
        case class Or(conds: Set[Condition]) extends Condition {
            def nodes = conds flatMap { _.nodes }
            def shiftGen(shiftGen: Int) = Or(conds map { _.shiftGen(shiftGen) })

            def permanentTrue = conds exists { _.permanentTrue }
            def permanentFalse = conds exists { _.permanentFalse }

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                conds.foldLeft(Condition.False) { (cc, condition) =>
                    Condition.disjunct(condition.evaluate(gen, results, aliveNodes), cc)
                }
            }
            def eligible = conds exists { _.eligible }
            def neg: Condition = {
                Condition.conjunct((conds map { _.neg }).toSeq: _*)
            }
        }
        // 기존의 Lift type trigger에 해당
        // - node가 완성되기 전에는 true, 완성된 이후에는 false, (당연히) 영영 완성되지 않으면 항상 true
        case class TrueUntilLifted(node: Node, pendedUntilGen: Int) extends Condition {
            def nodes = Set(node)
            def shiftGen(shiftGen: Int) = TrueUntilLifted(node.shiftGen(shiftGen), pendedUntilGen + shiftGen)

            def permanentTrue = false
            def permanentFalse = false

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                if (gen < pendedUntilGen) {
                    // pendedUntilGen 이전에는 평가 보류
                    this
                } else {
                    // node가 완전히 완성되었으면(될 수 있으면)(Condition이 Value(true)이면, 즉 condition.permanentTrue이면) Value(false)
                    // node가 results에서 조건부로 완성되면 그 조건들의 disjunction(or)의 negation을 반환
                    // node가 results에는 없고 aliveNodes에 현있으면 아직 모르는 상황(이고 현재는 true인 상황)이므로 this
                    // node가 results에도 없고 aliveNodes에도 없으면 영원히 node는 완성될 가능성이 없으므로 Value(true)
                    results.of(node) match {
                        case Some(resultsMap) =>
                            // 대상이 완성될 수 있는 시점에 나는 false가 돼야 함
                            val resultConditions = resultsMap.keys
                            // if (resultConditions exists { _.permanentTrue }) Condition.False else ???
                            Condition.disjunct(resultConditions.toSeq: _*).evaluate(gen, results, aliveNodes).neg
                        case None =>
                            if (aliveNodes contains node) this
                            else Condition.True
                    }
                }
            }
            def eligible = true
            def neg: Condition = {
                FalseUntilLifted(node, pendedUntilGen)
            }
        }
        // 기존의 Wait type trigger에 해당
        // - node가 완성되기 전에는 false, 완성된 이후에는 true, (당연히) 영영 완성되지 않으면 항상 false
        case class FalseUntilLifted(node: Node, pendedUntilGen: Int) extends Condition {
            def nodes = Set(node)
            def shiftGen(shiftGen: Int) = FalseUntilLifted(node.shiftGen(shiftGen), pendedUntilGen + shiftGen)

            def permanentTrue = false
            def permanentFalse = false

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                if (gen < pendedUntilGen) {
                    // pendedUntilGen 이전에는 평가 보류
                    this
                } else {
                    // node가 완전히 완성되었으면 Value(true)
                    // node가 results에서 조건부로 완성되면 그 조건들의 disjunction(or)를 반환
                    // node가 results에는 없고 aliveNodes에 있으면 아직 모르는 상황(이고 현재는 false인 상황)이므로 this
                    // node가 results에도 없고 aliveNodes에도 없으면 영원히 node는 완성될 가능성이 없으므로 Value(false)
                    results.of(node) match {
                        case Some(resultsMap) =>
                            // 대상이 완성될 수 있는 시점에만 나도 true가 될 수 있음
                            val resultConditions = resultsMap.keys
                            // if (resultConditions exists { _.permanentTrue }) Condition.True else ???
                            Condition.disjunct(resultConditions.toSeq: _*).evaluate(gen, results, aliveNodes)
                        case None =>
                            if (aliveNodes contains node) this
                            else Condition.False
                    }
                }
            }
            def eligible = false
            def neg: Condition = {
                TrueUntilLifted(node, pendedUntilGen)
            }
        }

        // 기존의 Alive type trigger에 해당
        // - node가 evaluate 시점에 살아있으면 false, 죽어있으면 true
        case class TrueUntilAlive(node: Node, pendedUntilGen: Int) extends Condition {
            def nodes = Set(node)
            def shiftGen(shiftGen: Int) = TrueUntilAlive(node.shiftGen(shiftGen), pendedUntilGen + shiftGen)

            def permanentTrue = false
            def permanentFalse = false

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                if (gen < pendedUntilGen) {
                    // pendedUntilGen 이전에는 평가 보류
                    this
                } else {
                    // (results에 관계 없이) node가 aliveNodes에 있으면 Condition.False, aliveNodes에 없으면 Condition.True
                    if (aliveNodes contains node) Condition.False
                    else Condition.True
                }
            }
            def eligible = true
            def neg: Condition = FalseUntilAlive(node, pendedUntilGen)
        }
        case class FalseUntilAlive(node: Node, pendedUntilGen: Int) extends Condition {
            def nodes = Set(node)
            def shiftGen(shiftGen: Int) = TrueUntilAlive(node.shiftGen(shiftGen), pendedUntilGen + shiftGen)

            def permanentTrue = false
            def permanentFalse = false

            def evaluate[R <: ParseResult](gen: Int, results: Results[Node, R], aliveNodes: Set[Node]): Condition = {
                if (gen < pendedUntilGen) {
                    // pendedUntilGen 이전에는 평가 보류
                    this
                } else {
                    // (results에 관계 없이) node가 aliveNodes에 없으면 Condition.False, aliveNodes에 있으면 Condition.True
                    if (aliveNodes contains node) Condition.True
                    else Condition.False
                }
            }
            def eligible = false
            def neg: Condition = TrueUntilAlive(node, pendedUntilGen)
        }
    }

    sealed trait Edge { val start: NontermNode }
    case class SimpleEdge(start: NontermNode, end: Node, aliveCondition: Condition) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge {
        // start must be a node with join
        assert(start.symbol.isInstanceOf[Join])
    }
}

// Results는 ParsingGraph의 results/progresses에서 사용된다
class Results[N <: Node, R <: ParseResult](val resultsMap: Map[N, Map[Condition, R]]) {
    assert(!(resultsMap exists { _._2.isEmpty }))

    def isEmpty = resultsMap.isEmpty
    def of(node: N): Option[Map[Condition, R]] = resultsMap get node
    def of(node: N, condition: Condition): Option[R] = (resultsMap get node) flatMap { m => m get condition }
    def entries: Iterable[(N, Condition, R)] = resultsMap flatMap { kv => kv._2 map { p => (kv._1, p._1, p._2) } }

    // - results와 progresses의 업데이트는 같은 원리로 동작하는데,
    //   - 해당하는 Node, Condition에 대한 R이 없을 경우 새로 추가해주고
    //   - 같은 Node, Condition에 대한 R이 이미 있는 경우 덮어쓴다.
    def update(node: N, condition: Condition, newResult: R): Results[N, R] = {
        resultsMap get node match {
            case Some(rMap) =>
                new Results(resultsMap + (node -> (rMap + (condition -> newResult))))
            case None =>
                new Results(resultsMap + (node -> Map(condition -> newResult)))
        }
    }
    def update(other: Results[N, R]): Results[N, R] = {
        other.entries.foldLeft(this) { (m, i) => m.update(i._1, i._2, i._3) }
    }
    def update(node: N, nodeResults: Map[Condition, R]): Results[N, R] = {
        nodeResults.foldLeft(this) { (m, i) => m.update(node, i._1, i._2) }
    }

    def filterKeys(keys: Set[Node]): Results[N, R] = new Results(resultsMap filterKeys keys)

    def merge(other: Results[N, R], resultFunc: ParseResultFunc[R]): Results[N, R] = {
        other.entries.foldLeft(this) { (cc, entry) =>
            val (node, condition, result) = entry
            cc.of(node, condition) match {
                case Some(existing) => cc.update(node, condition, resultFunc.merge(existing, result))
                case None => cc.update(node, condition, result)
            }
        }
    }

    // results에서 node와 trigger를 map해서 변경한다
    // - 이 때, nodeMap과 triggerMap은 모두 1-1 대응 함수여야 한다. 즉, 두 개의 노드가 같은 노드로 매핑되거나, 두 개의 트리거가 하나의 트리거로 매핑되면 안된다
    def map(nodeMap: N => N, conditionMap: Condition => Condition, resultMap: R => R): Results[N, R] = {
        val mappedMap = resultsMap map { kv =>
            val (node, results) = kv
            nodeMap(node) -> (results map { kv => conditionMap(kv._1) -> resultMap(kv._2) })
        }
        new Results(mappedMap)
    }

    def asMap = resultsMap
    def keyNodesSet = resultsMap.keySet
}
object Results {
    def apply[N <: Node, R <: ParseResult](): Results[N, R] = new Results(Map())
    def apply[N <: Node, R <: ParseResult](items: (N, Map[ParsingGraph.Condition, R])*): Results[N, R] =
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

    // Information Retrieval
    lazy val edgesByStart: Map[Node, Set[Edge]] = edges groupBy { _.start }
    lazy val edgesByEnds: Map[Node, Set[Edge]] = {
        def addEdge(map: Map[Node, Set[Edge]], node: Node, edge: Edge): Map[Node, Set[Edge]] =
            map get node match {
                case Some(edges) => map + (node -> (edges + edge))
                case None => map + (node -> Set(edge))
            }
        edges.foldLeft(Map[Node, Set[Edge]]()) { (cc, e) =>
            e match {
                case edge @ SimpleEdge(_, end, _) =>
                    addEdge(cc, end, edge)
                case edge @ JoinEdge(_, end, join) =>
                    addEdge(addEdge(cc, end, edge), join, edge)
            }
        }
    }
    def incomingEdgesTo(node: Node): Set[Edge] = edgesByEnds.getOrElse(node, Set())
    def incomingSimpleEdgesTo(node: Node): Set[SimpleEdge] = incomingEdgesTo(node) collect { case edge: SimpleEdge => edge }
    def incomingJoinEdgesTo(node: Node): Set[JoinEdge] = incomingEdgesTo(node) collect { case edge: JoinEdge => edge }
    def outgoingEdgesFrom(node: Node): Set[Edge] = edgesByStart.getOrElse(node, Set())
    def outgoingSimpleEdgesFrom(node: Node): Set[SimpleEdge] = outgoingEdgesFrom(node) collect { case edge: SimpleEdge => edge }
    def outgoingJoinEdgesFrom(node: Node): Set[JoinEdge] = outgoingEdgesFrom(node) collect { case edge: JoinEdge => edge }

    // Modification
    def create(nodes: Set[Node], edges: Set[Edge], results: Results[Node, R], progresses: Results[SequenceNode, R]): ParsingGraph[R]
    def updateResultOf(node: Node, condition: Condition, result: R): ParsingGraph[R] = {
        create(nodes, edges, results.update(node, condition, result), progresses)
    }
    def updateProgressesOf(node: SequenceNode, nodeProgresses: Map[Condition, R]): ParsingGraph[R] = {
        create(nodes, edges, results, progresses.update(node, nodeProgresses))
    }
    def withNodeEdgesProgresses(newNode: SequenceNode, newEdges: Set[Edge], newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        create(nodes + newNode, edges ++ newEdges, results, progresses.update(newProgresses))
    }
    def withNodes(newNodes: Set[Node]): ParsingGraph[R] = {
        create(nodes ++ newNodes, edges, results, progresses)
    }
    def withNodesEdgesProgresses(newNodes: Set[Node], newEdges: Set[Edge], newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        assert(newNodes forall { n => !(n.isInstanceOf[DGraph.BaseNode]) })
        create(nodes ++ newNodes, edges ++ newEdges, results, progresses.update(newProgresses))
    }
    def withNoResults: ParsingGraph[R] = {
        create(nodes, edges, Results(), progresses)
    }
    def updateResults(newResults: Results[Node, R]): ParsingGraph[R] = {
        create(nodes, edges, newResults, progresses)
    }
    def updateProgresses(newProgresses: Results[SequenceNode, R]): ParsingGraph[R] = {
        create(nodes, edges, results, newProgresses)
    }

    // start에서 ends(중 아무곳이나) 도달할 수 있는 모든 경로만 포함하는 서브그래프를 반환한다
    // - 노드가 start로부터 도달 가능하고, ends중 하나 이상으로 도달 가능해야 포함시킨다
    def subgraphIn(start: Node, ends: Set[Node], resultFunc: ParseResultFunc[R]): Option[ParsingGraph[R]] = {
        // - condition을 통하지 않고는 접근 불가능한 노드들은 subgraph에 포함되지 않는다
        // - 하지만 lift를 통해 그 결과가 바뀔 수 있는 노드들은 포함되어야 한다

        // backward순환할 때는 condition들은 모두 무시한다
        def traverseBackward(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
            case task +: rest =>
                val incomingEdges = incomingEdgesTo(task)
                val reachables: Set[(Set[Node], Option[Edge])] = incomingEdges map {
                    case edge @ SimpleEdge(start, _, _) =>
                        val newNodes: Set[Node] = Set(start)
                        (newNodes, Some(edge))
                    case edge @ JoinEdge(start, end, join) =>
                        if ((end == task && (nodesCC contains join)) || (join == task && (nodesCC contains end))) (Set[Node](start), Some(edge))
                        else (Set[Node](), None)
                }
                val (reachableNodes, reachableEdges) = reachables.foldLeft((Set[Node](), Set[Edge]())) { (m, i) => (m._1 ++ i._1, m._2 ++ i._2) }
                traverseBackward(rest ++ (reachableNodes -- nodesCC).toList, nodesCC ++ reachableNodes, edgesCC ++ reachableEdges)
            case List() => (nodesCC, edgesCC)
        }
        // forward순회할 때는 condition들의 노드도 모두 포함한다
        def traverseForward(queue: List[Node], nodesCC: Set[Node], edgesCC: Set[Edge]): (Set[Node], Set[Edge]) = queue match {
            case task +: rest =>
                val liftBlockTriggerOpt: Option[Node] = task match {
                    case task: AtomicNode => task.liftBlockTrigger
                    case _ => None
                }
                val outgoingEdges = outgoingEdgesFrom(task)
                val reachables: Set[(Set[Node], Edge)] = outgoingEdges map {
                    case edge @ SimpleEdge(_, end, aliveCondition) =>
                        val newNodes: Set[Node] = Set(end) ++ (aliveCondition.nodes intersect nodes)
                        (newNodes, edge)
                    case edge @ JoinEdge(_, end, join) =>
                        (Set(end, join), edge)
                }
                val (reachableNodes, reachableEdges) = reachables.foldLeft((liftBlockTriggerOpt.toSet, Set[Edge]())) { (m, i) => (m._1 ++ i._1, m._2 + i._2) }
                traverseForward(rest ++ (reachableNodes -- nodesCC).toList, nodesCC ++ reachableNodes, edgesCC ++ reachableEdges)
            case List() => (nodesCC, edgesCC)
        }

        val reachableToEnds = traverseBackward(ends.toList, ends, Set())
        if (!(reachableToEnds._1 contains start)) {
            None
        } else {
            val reachableFromStarts = traverseForward(List(start), Set(start), Set())

            val subNodes = reachableToEnds._1 intersect reachableFromStarts._1
            val subEdges = reachableToEnds._2 intersect reachableFromStarts._2
            assert(subNodes subsetOf nodes)
            Some(create(subNodes, subEdges, results, progresses filterKeys subNodes))
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
