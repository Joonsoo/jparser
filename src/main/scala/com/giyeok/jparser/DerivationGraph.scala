package com.giyeok.jparser

import Kernels._
import DerivationGraph._
import Symbols._
import ParseTree._
import Derivations._
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Inputs.AbstractInput
import com.giyeok.jparser.Inputs.ConcreteInput

case class DerivationGraph(baseNode: Node, nodes: Set[Node], edges: Set[Edge], lifts: Set[Lift]) {
    // baseNode가 nodes에 포함되어야 함
    assert(nodes contains baseNode)
    // baseNode를 제외하고는 전부 NewNode여야 함
    assert((nodes - baseNode) forall { _.isInstanceOf[NewNode] })
    // edges의 모든 노드가 nodes에 포함되어야 함
    assert({
        val nodesOfEdges = edges flatMap {
            _ match {
                case SimpleEdge(start, end, revertTriggers) => Set(start, end) ++ (revertTriggers map { _.node })
                case JoinEdge(start, end, join) => Set(start, end, join)
            }
        }
        nodesOfEdges subsetOf nodes
    })
    // lifts의 시작 노드가 nodes에 포함되어야 함
    assert(lifts map { _.before } subsetOf nodes)
    // lifts의 after 노드가 있는 경우 모두 nodes에 포함되어야 함
    assert(lifts flatMap { _.after } subsetOf nodes)

    // liftReverter는 결국 전부 edgeReverter로 바뀔 건데, baseNode가 lift되면서 edgeReverter가 따라온 경우에는 위쪽으로 올릴 수 없기 때문에 저장해둠
    // 그래서 실은 여기서 lifts랑 liftReverters 중에는 base와 관련 있는 baseNodeLifts, baseNodeLiftReverters만 의미가 있음

    val liftsMap: Map[Node, Set[Lift]] = lifts groupBy { _.before }
    assert(liftsMap.values forall { !_.isEmpty })

    val baseNodeLifts = (liftsMap get baseNode).getOrElse(Set())

    def withNodesEdges(newNodes: Set[Node], newEdges: Set[Edge]): DerivationGraph = {
        DerivationGraph(baseNode, nodes ++ newNodes, edges ++ newEdges, lifts)
    }
    def withLift(newLift: Lift): DerivationGraph = {
        DerivationGraph(baseNode, nodes, edges, lifts + newLift)
    }

    def incomingEdgesTo(node: Node): Set[Edge] = edges collect {
        case e @ SimpleEdge(_, end, _) if end == node => e
        case e @ JoinEdge(_, end, join) if end == node || join == node => e
    }
    def incomingSimpleEdgesTo(node: Node): Set[SimpleEdge] = edges collect {
        case edge @ SimpleEdge(_, `node`, _) => edge
    }
    def incomingJoinEdgesTo(node: Node): Set[JoinEdge] = edges collect {
        case edge @ (JoinEdge(_, _, `node`) | JoinEdge(_, `node`, _)) => edge.asInstanceOf[JoinEdge]
    }

    lazy val terminalNodes: Set[NewTermNode] = nodes collect { case node: NewTermNode => node }
    lazy val terminals: Set[Terminal] = terminalNodes map { _.kernel.symbol }
    lazy val termGroups: Set[TermGroupDesc] = {
        val terminals = this.terminals

        import Symbols.Terminals._
        import Inputs._

        val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
        val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

        val charIntersects: Set[CharacterTermGroupDesc] = charTerms flatMap { term1 =>
            charTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }
        val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
            virtTerms collect {
                case term2 if term1 != term2 => term1 intersect term2
            } filterNot { _.isEmpty }
        }

        val charTermGroups = (charTerms map { term =>
            charIntersects.foldLeft(term) { _ - _ }
        }) ++ charIntersects
        val virtTermGroups = (virtTerms map { term =>
            virtIntersects.foldLeft(term) { _ - _ }
        }) ++ virtIntersects

        (charTermGroups ++ virtTermGroups) filterNot { _.isEmpty }
    }
    lazy val reachablesMap: Map[Node, Set[TermGroupDesc]] = {
        val cc = scala.collection.mutable.Map[Node, Set[TermGroupDesc]]()
        nodes foreach {
            case node @ NewTermNode(TerminalKernel(terminal, 0)) =>
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
        result foreach { kv =>
            println(s"${kv._1} -> ${kv._2}")
        }
        assert(edges forall {
            case SimpleEdge(start, end, _) =>
                result(end) subsetOf result(start)
            case JoinEdge(start, end, join) =>
                result(start) == (result(end) intersect result(join))
        })
        result
    }
    lazy val sliceByTermGroups: Map[TermGroupDesc, Option[DerivationGraph]] = {
        (termGroups map { termGroup =>
            val subgraph = if (reachablesMap(baseNode) contains termGroup) {
                val input = AbstractInput(termGroup)
                val subnodes0 = nodes filter { node => reachablesMap(node) contains termGroup }
                // liftBlockTrigger 거르기
                val subnodesMap = (subnodes0 map {
                    case node @ NewAtomicNode(kernel, liftBlockTrigger, reservedReverter) =>
                        node -> NewAtomicNode(kernel, liftBlockTrigger filter { subnodes0 contains _ }, reservedReverter)
                    case node => node -> node
                }).toMap
                def refineRevertTriggers(revertTriggers: Set[Trigger], map: Map[Node, Node]): Set[Trigger] = revertTriggers collect {
                    case Trigger(node, ttype) if subnodes0 contains node => Trigger(map(node), ttype)
                }
                // revertTriggers 거르기, liftBlockTrigger 걸러진 노드로 바꾸기
                val subedges: Set[Edge] = edges collect {
                    case e @ SimpleEdge(start, end, revertTriggers) if (subnodes0 contains start) && (subnodes0 contains end) =>
                        SimpleEdge(subnodesMap(start).asInstanceOf[NontermNode], subnodesMap(end), refineRevertTriggers(revertTriggers, subnodesMap))
                    case e @ JoinEdge(start, end, join) if (subnodes0 contains start) && (subnodes0 contains end) && (subnodes0 contains join) =>
                        JoinEdge(subnodesMap(start).asInstanceOf[NontermNode], subnodesMap(end), subnodesMap(join))
                }
                val sublifts = lifts collect {
                    case Lift(before, afterKernel, parsed, after, revertTriggers) if (subnodes0 contains before) && (after forall { subnodes0 contains _ }) =>
                        Lift(subnodesMap(before), afterKernel, parsed, after map { subnodesMap(_) }, refineRevertTriggers(revertTriggers, subnodesMap))
                }
                Some(DerivationGraph(baseNode, subnodesMap.values.toSet, subedges, sublifts))
            } else None
            termGroup -> subgraph
        }).toMap
    }
    def subgraphTo(termGroup: TermGroupDesc): Option[DerivationGraph] =
        sliceByTermGroups(termGroup) ensuring (termGroups contains termGroup)
    def subgraphTo(input: ConcreteInput): Option[DerivationGraph] =
        termGroups find { _.contains(input) } flatMap { subgraphTo(_) }
    def compaction: DerivationGraph = ???
}

object DerivationGraph {
    sealed trait Node { val kernel: Kernel }

    sealed trait NewNode extends Node
    sealed trait BaseNode extends NontermNode

    sealed trait NonEmptyNode extends Node

    sealed trait NontermNode extends NonEmptyNode {
        val kernel: NontermKernel[Nonterm]
        def lift(by: ParseNode[Symbol]): (Kernel, ParseNode[Symbol])
    }

    sealed trait AtomicNontermNode[T <: AtomicSymbol with Nonterm] extends NontermNode {
        val kernel: AtomicNontermKernel[T]
        def lift(by: ParseNode[Symbol]): (Kernel, ParseNode[Symbol]) =
            (kernel.lifted, ParsedSymbol(kernel.symbol, by))
    }

    sealed trait NonAtomicNontermNode[T <: NonAtomicSymbol with Nonterm] extends NontermNode {
        val kernel: NonAtomicNontermKernel[T]
        val progress: ParsedSymbolsSeq[T]

        def lift(by: ParseNode[Symbol]): (Kernel, ParseNode[Symbol]) =
            kernel.lifted(progress, by)
    }

    case object EmptyNode extends NewNode { val kernel = EmptyKernel }
    case class NewTermNode(kernel: TerminalKernel) extends NewNode with NonEmptyNode
    case class NewAtomicNode[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T], liftBlockTrigger: Option[Node], reservedReverter: Option[Trigger.Type.Value]) extends NewNode with AtomicNontermNode[T] {
        // liftBlockTrigger와 reservedReverter가 동시에 있을 수는 없다
        assert(liftBlockTrigger.isEmpty || reservedReverter.isEmpty)
    }
    case class NewNonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], progress: ParsedSymbolsSeq[T]) extends NewNode with NonAtomicNontermNode[T]
    case class BaseAtomicNode[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T]) extends BaseNode with AtomicNontermNode[T]
    case class BaseNonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T]) extends BaseNode with NonAtomicNontermNode[T] {
        // 원래 여기서는 kernel이 이미 몇단계 진행되어서 progress에 실제 내용이 있어야 하는 상황인데
        // 여기서는 실제 파싱 진행 내용을 모르는 상황이므로 비우고 진행하고
        // 외부에서(실제 파서에서) ParsedSymbolsSeq 두개를 merge해서 쓰는걸로
        val progress = ParsedSymbolsSeq(kernel.symbol, List(), List())
    }

    sealed trait Edge
    case class SimpleEdge(start: NontermNode, end: Node, revertTriggers: Set[Trigger]) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge

    case class Lift(before: Node, afterKernel: Kernel, parsed: ParseNode[Symbol], after: Option[Node], revertTriggers: Set[Trigger]) {
        // before 노드, afterKernel, parsed 모두 동일한 심볼을 갖고 있어야 한다
        assert((before.kernel.symbol == afterKernel.symbol) && (afterKernel.symbol == parsed.symbol))
        // afterKernel.derivable하면 after가 있어야 한다. 단 before가 BaseNode인 경우에는 after를 직접 만들지 않고 파서에 일임하기 때문에 어떤 경우든 after가 비어있어야 한다
        println(s"before: ${before.kernel.toShortString}")
        println(s"afterKernel: ${afterKernel.toShortString}")
        println(s"after: ${after map { _.kernel.toShortString }}")
        assert(if (!before.isInstanceOf[BaseNode]) (afterKernel.derivable == after.isDefined) else after.isEmpty)
        // after 노드가 있다면 그 심볼도 역시 동일해야 한다
        assert(after forall { _.kernel.symbol == afterKernel.symbol })
    }

    case class Trigger(node: Node, triggerType: Trigger.Type.Value)
    object Trigger {
        object Type extends Enumeration {
            val Lift, Alive = Value
        }
    }

    // case class CompactNode(path: Seq[NewNode]) extends Node

    def newNode(symbol: Symbol): NewNode = Kernel(symbol) match {
        case EmptyKernel => EmptyNode

        case k: TerminalKernel => NewTermNode(k)

        case k: ExceptKernel => NewAtomicNode(k, Some(newNode(k.symbol.except)), None)
        case k: LongestKernel => NewAtomicNode(k, None, Some(Trigger.Type.Lift))
        case k: EagerLongestKernel => NewAtomicNode(k, None, Some(Trigger.Type.Alive))

        case k: AtomicNontermKernel[_] => NewAtomicNode(k, None, None)
        case k: NonAtomicNontermKernel[_] => NewNonAtomicNode(k, ParsedSymbolsSeq(k.symbol, List(), List()))
    }

    def deriveNode(grammar: Grammar, baseNode: NontermNode, revertTriggers: Set[Trigger]): Set[Edge] = baseNode.kernel match {
        case k: StartKernel =>
            Set(SimpleEdge(baseNode, newNode(grammar.startSymbol), Set()))
        case k: NonterminalKernel =>
            grammar.rules(k.symbol.name) map { s => SimpleEdge(baseNode, newNode(s), Set()) }
        case k: OneOfKernel =>
            k.symbol.syms map { s => SimpleEdge(baseNode, newNode(s), Set()) }
        case k: LookaheadExceptKernel =>
            Set(SimpleEdge(baseNode, newNode(Empty), Set(Trigger(newNode(k.symbol.except), Trigger.Type.Lift))))
        case k: BackupKernel =>
            val preferNode = newNode(k.symbol.sym)
            Set(SimpleEdge(baseNode, preferNode, Set()),
                SimpleEdge(baseNode, newNode(k.symbol.backup), Set(Trigger(preferNode, Trigger.Type.Lift))))
        case k: SequenceKernel =>
            assert(k.pointer < k.symbol.seq.size)
            val (symbol, pointer) = (k.symbol, k.pointer)
            val sym = symbol.seq(pointer)
            if (pointer > 0 && pointer < symbol.seq.size) {
                // whitespace only between symbols
                (symbol.whitespace + sym) map { newNode _ } map { SimpleEdge(baseNode, _, revertTriggers) }
            } else {
                Set(SimpleEdge(baseNode, newNode(sym), revertTriggers))
            }

        case k: JoinKernel =>
            Set(JoinEdge(baseNode, newNode(k.symbol.sym), newNode(k.symbol.join)))
        case k: RepeatKernel[_] =>
            Set(SimpleEdge(baseNode, newNode(k.symbol.sym), Set()))
        case k: ProxyKernel =>
            Set(SimpleEdge(baseNode, newNode(k.symbol.sym), Set()))
        case k: ExceptKernel =>
            // baseNode가 NewAtomicNode이고 liftBlockTrigger가 k.symbol.except 가 들어있어야 함
            Set(SimpleEdge(baseNode, newNode(k.symbol.sym), Set()))
        case k: LongestKernel =>
            // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Lift)여야 함
            Set(SimpleEdge(baseNode, newNode(k.symbol.sym), Set()))
        case k: EagerLongestKernel =>
            // baseNode가 NewAtomicNode이고 reservedRevertter가 Some(Trigger.Type.Alive)여야 함
            Set(SimpleEdge(baseNode, newNode(k.symbol.sym), Set()))
        // Except, Longest, EagerLongest의 경우를 제외하고는 모두 liftBlockTrigger와 reservedReverter가 비어있어야 함
    }

    def deriveFromKernel(grammar: Grammar, startKernel: NontermKernel[Nonterm]): DerivationGraph = {
        // assert(startKernel.symbol == Start || startKernel.symbol.isInstanceOf[NonAtomicSymbol])

        sealed trait Task
        case class DeriveTask(node: NontermNode, revertTriggers: Set[Trigger]) extends Task
        // node가 finishable해져서 lift하면 afterKernel의 커널과 parsed의 노드를 갖게 된다는 의미
        case class LiftTask(node: Node, afterKernel: Kernel, parsed: ParseNode[Symbol], revertTriggers: Set[Trigger]) extends Task {
            assert(node.kernel.symbol == afterKernel.symbol && afterKernel.symbol == parsed.symbol)
        }

        def derive(queue: List[Task], cc: DerivationGraph): DerivationGraph = {
            queue match {
                case DeriveTask(baseNode, revertTriggers) +: rest =>
                    assert(baseNode.kernel.derivable)
                    // lift하면서 revertTriggers가 쌓인 상태에서 derive를 시작하는 경우에만(baseNode가 NonAtomicNontermNode인 경우에만) revertTriggers가 있을 수 있고 그 외의 경우엔 없어야 한다
                    assert(baseNode.isInstanceOf[NonAtomicNontermNode[_]] || revertTriggers.isEmpty)

                    var newQueue = rest
                    var newCC = cc

                    val kernel = baseNode.kernel

                    // TODO derivedEdges가 cc.edges하고 겹치는게 있으면 안 될 것 같은데 확인해보기
                    val newDerivedEdges: Set[Edge] = deriveNode(grammar, baseNode, revertTriggers) -- cc.edges

                    val derivedNodes0: Set[Node] = (newDerivedEdges flatMap {
                        _ match {
                            case SimpleEdge(start, end, revertTriggers) => (Set(end) ++ (revertTriggers map { _.node })) ensuring start == baseNode
                            case JoinEdge(start, end, join) => Set(end, join) ensuring start == baseNode
                        }
                    })
                    val derivedNodes: Set[Node] = derivedNodes0 flatMap {
                        _ match {
                            case n: NewAtomicNode[_] => Set(n) ++ n.liftBlockTrigger
                            case n => Set(n)
                        }
                    }
                    val newDerivedNodes = derivedNodes -- cc.nodes

                    // newDerivedNodes 중 derivable한 것들(nonterm kernel들) 추려서 Derive 태스크 추가
                    newQueue ++:= (newDerivedNodes.toList collect { case nonterm: NontermNode if nonterm.kernel.derivable => DeriveTask(nonterm, Set()) })
                    newCC = cc.withNodesEdges(newDerivedNodes, newDerivedEdges)

                    // newDerivedNodes중 finishable한 것들을 추려서 Lift 태스크 추가
                    val finishableDerivedNodes = newDerivedNodes filter { _.kernel.finishable }
                    // 이런식으로 finishable할 수 있는 심볼은 Empty, Repeat, 드물겠지만 빈 Sequence 심볼 뿐
                    assert(finishableDerivedNodes forall { n => n.kernel == EmptyKernel || n.kernel.isInstanceOf[NonAtomicNontermKernel[_]] })
                    // Empty는 ParsedEmpty(Empty)
                    // NonAtomic은 progress를 주면 ParsedSymbolsSeq(symbol, List(), List())가 될 것
                    val liftTasks: Set[LiftTask] = finishableDerivedNodes map { n =>
                        n.kernel match {
                            case EmptyKernel => LiftTask(n, EmptyKernel, ParsedEmpty(Empty), Set())
                            case kernel: NonAtomicNontermKernel[_] =>
                                val parseNode = n.asInstanceOf[NewNonAtomicNode[NonAtomicSymbol with Nonterm]].progress
                                assert(parseNode == ParsedSymbolsSeq(kernel.symbol, List(), List()))
                                // 이 엣지에 붙어있는 reverter, lift되는 노드에 붙은 reserved reverter는 LiftTask에서 처리함
                                LiftTask(n, kernel, parseNode, Set())
                            case _ => throw new AssertionError("Cannot happen")
                        }
                    }

                    // newDerivedNodes중 (그 자체로는) finishable하지 않은데 cc.lifts에 empty lift 가능한 것으로 되어 있는 것들 추려서 Lift 태스크 추가
                    val liftAgain: Set[LiftTask] = (derivedNodes intersect cc.nodes) flatMap { derivedNode =>
                        cc.liftsMap get derivedNode match {
                            case Some(lifts) =>
                                lifts map { lift =>
                                    // 이 엣지에 붙어있는 reverter, lift되는 노드에 붙은 reserved reverter는 LiftTask에서 처리함
                                    // 다만 과거의 lift에서 얻어온 revertTriggers는 전달해주어야 함
                                    val (afterKernel, parsed) = baseNode.lift(lift.parsed)
                                    LiftTask(baseNode, afterKernel, parsed, lift.revertTriggers)
                                }
                            case None => Set[LiftTask]()
                        }
                    }

                    newQueue ++:= (liftTasks ++ liftAgain).toList

                    derive(newQueue, newCC)

                case LiftTask(node: BaseNode, afterKernel, parsed, revertTriggers) +: rest =>
                    // TODO base node에 붙은 reserved reverter는 여기 말고 밖에 파서에서 처리해야 하나?
                    derive(rest, cc.withLift(Lift(node, afterKernel, parsed, None, revertTriggers)))

                case LiftTask(node: NewNode, afterKernel, parsed, revertTriggers) +: rest =>
                    var newQueue = rest
                    var newCC = cc

                    val incomingSimpleEdges: Set[SimpleEdge] = cc.incomingSimpleEdgesTo(node)
                    val incomingJoinEdges: Set[JoinEdge] = cc.incomingJoinEdgesTo(node)
                    val eligibleJoinEdges: Set[JoinEdge] = incomingJoinEdges filter {
                        _ match {
                            case JoinEdge(_, `node`, constraint) => cc.liftsMap contains constraint
                            case JoinEdge(_, constraint, `node`) => cc.liftsMap contains constraint
                        }
                    }

                    // afterKernel.finishable이면 incoming node들에 대해서도 lift
                    if (afterKernel.finishable) {
                        // - LiftTask만들 때 통과하는 edge에 붙은 revertTriggers를 revertTriggers로 추가해주어야 함
                        // - reserved reverter가 lift되면 reverterTrigger에 해당 노드/조건이 추가돼야됨

                        val reservedRevertTrigger: Set[Trigger] = node match {
                            case node: NewAtomicNode[_] => node.reservedReverter match {
                                case Some(triggerType) => Set(Trigger(node, triggerType))
                                case None => Set()
                            }
                            case _ => Set()
                        }

                        // incomingSimpleEdges
                        newQueue ++:= incomingSimpleEdges map { e =>
                            val (newAfterKernel, newParsed) = e.start.lift(parsed)
                            LiftTask(e.start, newAfterKernel, newParsed, e.revertTriggers ++ revertTriggers ++ reservedRevertTrigger)
                        }

                        // eligibleJoinEdges
                        newQueue ++:= eligibleJoinEdges flatMap { e =>
                            assert(e.start.kernel.isInstanceOf[JoinKernel])
                            assert(reservedRevertTrigger.isEmpty)
                            val startKernel = e.start.kernel.asInstanceOf[JoinKernel]
                            val lifts: Set[(ParsedSymbolJoin, Set[Trigger])] =
                                if (node == e.end) {
                                    cc.liftsMap(e.join) map { lift =>
                                        (new ParsedSymbolJoin(startKernel.symbol, parsed, lift.parsed), lift.revertTriggers)
                                    }
                                } else {
                                    assert(node == e.join)
                                    cc.liftsMap(e.end) map { lift =>
                                        (new ParsedSymbolJoin(startKernel.symbol, lift.parsed, parsed), lift.revertTriggers)
                                    }
                                }
                            lifts map { pr =>
                                LiftTask(e.start, startKernel.lifted, pr._1, revertTriggers ++ pr._2)
                            }
                        }
                    }

                    // lift된 노드가 derivable하면 Derive 태스크 추가
                    if (afterKernel.derivable) {
                        // JoinEdge는 항상 end/join에 atomic symbol만 받기 때문에 여기로 올 수가 없음
                        assert(incomingJoinEdges.isEmpty)

                        // lift 이후에 derivable하다는 것은 NonAtomic 심볼이라는 것이고,
                        // 따라서 parsed도 ParsedSymbolsSeq여야 한다
                        (afterKernel, parsed) match {
                            case (afterKernel: NonAtomicNontermKernel[_], parsed: ParsedSymbolsSeq[_]) =>
                                // afterKernel로 된 노드 및 (incoming 노드->추가된 노드) 엣지들 및 Lift.after를 추가된 노드로 지정한 Lift 추가
                                val newNode = NewNonAtomicNode(afterKernel.asInstanceOf[NonAtomicNontermKernel[NonAtomicSymbol with Nonterm]], parsed.asInstanceOf[ParsedSymbolsSeq[NonAtomicSymbol with Nonterm]])
                                val newEdges: Set[SimpleEdge] = incomingSimpleEdges map { e => SimpleEdge(e.start, newNode, revertTriggers) }
                                newCC = cc.withNodesEdges(Set(newNode), newEdges.toSet[Edge]).withLift(Lift(node, afterKernel, parsed, Some(newNode), revertTriggers))

                                // DeriveTask 추가
                                newQueue +:= DeriveTask(newNode, revertTriggers)

                            case _ => throw new AssertionError("should be nonatomic")
                        }
                    } else {
                        newCC = cc.withLift(Lift(node, afterKernel, parsed, None, revertTriggers))
                    }
                    derive(newQueue, newCC)
                case List() => cc
            }
        }
        val baseNode = startKernel match {
            case kernel: AtomicNontermKernel[_] => BaseAtomicNode(kernel)
            case kernel: NonAtomicNontermKernel[_] => BaseNonAtomicNode(kernel)
        }
        val result = derive(List(DeriveTask(baseNode, Set())), DerivationGraph(baseNode, Set(baseNode), Set(), Set()))
        // TODO assertion
        // - 각 노드에 대한 테스트
        // - 노드에서 나와야 할 엣지에 대한 테스트
        // - 각 노드의 커널에서 deriveFromKernel해서 나오는 그래프가 subgraph인지 테스트
        // - reverter 테스트
        result
    }
}

sealed trait Derivation
object Derivations {
    // 빈 derive
    case object EmptyDerivation extends Derivation
    // Nonterminal, OneOf, Repeat, Proxy, Sequence - 일반적인 derive
    case class SymbolDerivation(derives: Set[Kernel]) extends Derivation
    // Join
    case class JoinDerivation(derive: Kernel, join: Kernel) extends Derivation
    // Except - (self->derive)로 가는 SimpleEdge + (blockTrigger->self)로 오는 TempLiftBlockReverter
    case class TempLiftBlockableDerivation(derive: Kernel, blockTrigger: Kernel) extends Derivation
    // LookaheadExcept - (self->derive)로 가는 SimpleEdge + (revertTrigger->(self->derive))인 DeriveReverter
    case class RevertableDerivation(derive: Kernel, revertTrigger: Kernel) extends Derivation
    // Backup - (self->derive)로 가는 SimpleEdge + (self->deriveRevertTrigger)로 가는 SimpleEdge + (deriveRevertTrigger->(self->derive))인 DeriveReverter
    case class DeriveRevertableDerivation(derive: Kernel, deriveRevertTrigger: Kernel) extends Derivation
    // Longest/EagerLongest
    // derive로 가는 SimpleEdge가 있고, 노드 자신에 ReservedReverter가 붙어 있음
    sealed trait ReservedLiftRevertableDerivation extends Derivation { val derive: Kernel }
    case class ReservedLiftTriggeredLiftRevertableDerivation(derive: Kernel) extends ReservedLiftRevertableDerivation
    case class ReservedAliveTriggeredLiftRevertableDerivation(derive: Kernel) extends ReservedLiftRevertableDerivation
}
