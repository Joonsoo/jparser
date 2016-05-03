package com.giyeok.jparser

import Kernels._
import DerivationGraph._
import Symbols._
import ParseTree._
import Derivations._
import com.giyeok.jparser.Inputs.TermGroupDesc

case class DerivationGraph(baseNode: Node, nodes: Set[Node], edges: Set[Edge], lifts: Set[Lift], liftReverters: Map[Lift, Set[Node]], edgeReverters: Set[(Trigger, SimpleEdge)], reservedReverters: Set[Trigger]) {
    // baseNode가 nodes에 포함되어야 함
    assert(nodes contains baseNode)
    // edges의 모든 노드가 nodes에 포함되어야 함
    assert(edges flatMap { _ match { case SimpleEdge(start, end) => Set(start, end) case JoinEdge(start, end, join) => Set(start, end, join) } } subsetOf nodes)
    // lifts의 시작 노드가 nodes에 포함되어야 함
    assert(lifts map { _.before } subsetOf nodes)
    // lifts의 after 노드가 있는 경우 모두 nodes에 포함되어야 함
    assert(lifts flatMap { _.after } subsetOf nodes)
    // edgeReverters의 트리거 노드가 모두 nodes에 포함되어야 함
    assert(edgeReverters map { _._1.node } subsetOf nodes)
    // edgeReverters의 대상 엣지가 모두 edges에 포함되어야 함
    assert((edgeReverters map { _._2 }).toSet[Edge] subsetOf edges)
    // reservedReverters의 대상 노드가 모두 nodes에 포함되어야 함
    assert(reservedReverters map { _.node } subsetOf nodes)

    // liftReverter는 결국 전부 edgeReverter로 바뀔 건데, baseNode가 lift되면서 edgeReverter가 따라온 경우에는 위쪽으로 올릴 수 없기 때문에 저장해둠
    // 그래서 실은 여기서 lifts랑 liftReverters 중에는 base와 관련 있는 baseNodeLifts, baseNodeLiftReverters만 의미가 있음

    val liftsMap: Map[Node, Set[Lift]] = lifts groupBy { _.before }
    assert(liftsMap.values forall { !_.isEmpty })

    // key node가 lift되면 value edge set을 모두 무효화시킨다
    val edgeRevertersMap: Map[Trigger, Set[SimpleEdge]] = edgeReverters groupBy { _._1 } mapValues { _ map { _._2 } }

    val baseNodeLifts = (liftsMap get baseNode).getOrElse(Set())
    val baseNodeLiftReverters = baseNodeLifts flatMap { liftReverters.getOrElse(_, Set()) }

    def withNodesEdgesReverters(newNodes: Set[Node], newEdges: Set[Edge], newEdgeReverters: Set[(Trigger, SimpleEdge)], newReservedReverter: Option[Trigger]): DerivationGraph = {
        DerivationGraph(baseNode, nodes ++ newNodes, edges ++ newEdges, lifts, liftReverters, edgeReverters ++ newEdgeReverters, reservedReverters ++ newReservedReverter)
    }
    def withLiftAndLiftReverters(newLift: Lift, newLiftReverters: Set[Node]): DerivationGraph = {
        DerivationGraph(baseNode, nodes, edges, lifts + newLift, liftReverters + (newLift -> (liftReverters.getOrElse(newLift, Set()) ++ newLiftReverters)), edgeReverters, reservedReverters)
    }

    def incomingEdgesTo(node: Node): Set[Edge] = edges filter {
        _ match {
            case SimpleEdge(_, end) if end == node => true
            case JoinEdge(_, end, join) if end == node || join == node => true
        }
    }
    def incomingSimpleEdgesTo(node: Node): Set[SimpleEdge] = edges collect {
        case edge @ SimpleEdge(_, `node`) => edge
    }
    def incomingJoinEdgesTo(node: Node): Set[JoinEdge] = edges collect {
        case edge @ (JoinEdge(_, _, `node`) | JoinEdge(_, `node`, _)) => edge.asInstanceOf[JoinEdge]
    }

    def compaction: DerivationGraph = ???
    def terminals: Set[Terminal] = ???
    def termGroups: Set[TermGroupDesc] = ???
    def sliceByTermGroups: Map[TermGroupDesc, DerivationGraph] = ???
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
    case class NewAtomicNode[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T]) extends NewNode with AtomicNontermNode[T]
    case class NewNonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], progress: ParsedSymbolsSeq[T]) extends NewNode with NonAtomicNontermNode[T]
    case class BaseAtomicNode[T <: AtomicSymbol with Nonterm](kernel: AtomicNontermKernel[T]) extends BaseNode with AtomicNontermNode[T]
    case class BaseNonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T]) extends BaseNode with NonAtomicNontermNode[T] {
        // 원래 여기서는 kernel이 이미 몇단계 진행되어서 progress에 실제 내용이 있어야 하는 상황인데
        // 여기서는 실제 파싱 진행 내용을 모르는 상황이므로 비우고 진행하고
        // 외부에서(실제 파서에서) ParsedSymbolsSeq 두개를 merge해서 쓰는걸로
        val progress = ParsedSymbolsSeq(kernel.symbol, List(), List())
    }

    sealed trait Edge
    case class SimpleEdge(start: NontermNode, end: Node) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge

    case class Lift(before: Node, afterKernel: Kernel, parsed: ParseNode[Symbol], after: Option[Node]) {
        // before 노드, afterKernel, parsed 모두 동일한 심볼을 갖고 있어야 한다
        assert((before.kernel.symbol == afterKernel.symbol) && (afterKernel.symbol == parsed.symbol))
        // afterKernel.derivable하면 after가 있어야 한다. 단 before가 BaseNode인 경우에는 after를 직접 만들지 않고 파서에 일임하기 때문에 어떤 경우든 after가 비어있어야 한다
        assert(if (!before.isInstanceOf[BaseNode]) (afterKernel.derivable == after.isDefined) else after.isEmpty)
        // after 노드가 있다면 그 심볼도 역시 동일해야 한다
        assert(after forall { _.kernel.symbol == afterKernel.symbol })
    }

    sealed trait Trigger { val node: Node }
    case class IfLift(node: Node) extends Trigger
    case class IfAlive(node: Node) extends Trigger

    // case class CompactNode(path: Seq[NewNode]) extends Node

    def instantiateDerivation(baseNode: NontermNode, derivation: Derivation): (Set[Edge], Option[(Node, SimpleEdge)], Option[Trigger]) = {
        def newNode(kernel: Kernel): NewNode = kernel match {
            case EmptyKernel => EmptyNode
            case k: TerminalKernel => NewTermNode(k)
            case k: AtomicNontermKernel[_] => NewAtomicNode(k)
            case k: NonAtomicNontermKernel[_] => NewNonAtomicNode(k, ParsedSymbolsSeq(k.symbol, List(), List()))
        }

        derivation match {
            case EmptyDerivation =>
                (Set[Edge](), Option.empty[(Node, SimpleEdge)], Option.empty[Trigger])
            case SymbolDerivation(derives) =>
                (derives map { k => SimpleEdge(baseNode, newNode(k)) }, None, None)
            case JoinDerivation(derive, join) =>
                (Set(JoinEdge(baseNode, newNode(derive), newNode(join))), None, None)
            case TempLiftBlockableDerivation(derive, blockTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge), Some((newNode(blockTrigger), edge)), None)
            case RevertableDerivation(derive, revertTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge), Some((newNode(revertTrigger), edge)), None)
            case DeriveRevertableDerivation(derive, deriveRevertTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge, SimpleEdge(baseNode, newNode(deriveRevertTrigger))), Some((newNode(deriveRevertTrigger), edge)), None)
            case ReservedLiftTriggeredLiftRevertableDerivation(derive) =>
                (Set(SimpleEdge(baseNode, newNode(derive))), None, Some(IfLift(baseNode)))
            case ReservedAliveTriggeredLiftRevertableDerivation(derive) =>
                (Set(SimpleEdge(baseNode, newNode(derive))), None, Some(IfAlive(baseNode)))
        }
    }

    def deriveFromKernel(grammar: Grammar, startKernel: NontermKernel[Nonterm]): DerivationGraph = {
        sealed trait Task
        case class DeriveTask(node: NontermNode) extends Task
        // node가 finishable해져서 lift하면 afterKernel의 커널과 parsed의 노드를 갖게 된다는 의미
        case class LiftTask(node: Node, afterKernel: Kernel, parsed: ParseNode[Symbol], reverterTriggers: Set[Node]) extends Task {
            assert(node.kernel.symbol == afterKernel.symbol && afterKernel.symbol == parsed.symbol)
        }

        def derive(queue: List[Task], cc: DerivationGraph): DerivationGraph = {
            queue match {
                case DeriveTask(node) +: rest =>
                    var newQueue = rest
                    var newCC = cc

                    val kernel = node.kernel

                    val (derivedEdges, edgeReverterOpt, reservedReverterOpt) =
                        instantiateDerivation(node, kernel.derive(grammar))
                    val newDerivedEdges: Set[Edge] = derivedEdges -- cc.edges

                    val newDerivedNodes: Set[Node] = (newDerivedEdges flatMap {
                        _ match {
                            case SimpleEdge(start, end) => Set(end) ensuring start == node
                            case JoinEdge(start, end, join) => Set(end, join) ensuring start == node
                        }
                    }) -- cc.nodes

                    val newReverterTriggerNodes: Option[Node] = (edgeReverterOpt map { _._1 }) filterNot { cc.nodes contains _ }

                    val newNodes = newDerivedNodes ++ newReverterTriggerNodes

                    // newNodes 중 derivable한 것들(nonterm kernel들) 추려서 Derive 태스크 추가
                    newQueue ++:= (newNodes.toList collect { case nonterm: NontermNode => DeriveTask(nonterm) })
                    newCC = cc.withNodesEdgesReverters(newNodes, newDerivedEdges, (edgeReverterOpt map { p => (IfLift(p._1), p._2) }).toSet, reservedReverterOpt)

                    // TODO lift할 때 nullable한 trigger가 붙은 reverter 어떻게 처리할지 고민
                    //   - 좀 특이한 경우긴 하지만..
                    //   - 특히 TempLiftBlockable. 그냥 reverter의 트리거가 nullable한 건 문법 자체가 좀 이상한 경우일듯

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
                                LiftTask(n, kernel, parseNode, Set())
                            case _ => throw new AssertionError("Cannot happen")
                        }
                    }

                    // newDerivedNodes중 finishable하지는 않은데 cc.lifts에 empty lift 가능한 것으로 되어 있는 것들 추려서 Lift 태스크 추가
                    val liftAgain: Set[LiftTask] = newDerivedNodes flatMap { derivedNode =>
                        cc.liftsMap get derivedNode match {
                            case Some(lifts) =>
                                lifts map { lift =>
                                    val (afterKernel, parsed) = node.lift(lift.parsed)
                                    LiftTask(node, afterKernel, parsed, cc.liftReverters.getOrElse(lift, Set()))
                                }
                            case None => Set[LiftTask]()
                        }
                    }

                    newQueue ++:= (liftTasks ++ liftAgain).toList

                    derive(newQueue, newCC)

                case LiftTask(node: BaseNode, afterKernel, parsed, reverterTriggers) +: rest =>
                    assert(cc.liftsMap(node) exists { _.parsed == parsed })
                    derive(rest, cc.withLiftAndLiftReverters(Lift(node, afterKernel, parsed, None), reverterTriggers))

                case LiftTask(node: NewNode, afterKernel, parsed, reverterTriggers) +: rest =>
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

                    }

                    // lift된 노드가 derivable하면 Derive 태스크 추가
                    if (afterKernel.derivable) {
                        // JoinEdge는 항상 end/join에 atomic symbol만 받기 때문에 여기로 올 수가 없음
                        assert(incomingJoinEdges.isEmpty)

                        // lift 이후에 derivable하다는 것은 NonAtomic 심볼이라는 것이고,
                        // 따라서 parsed도 ParsedSymbolsSeq여야 한다
                        (afterKernel, parsed) match {
                            case (afterKernel: NonAtomicNontermKernel[_], parsed: ParsedSymbolsSeq[_]) =>
                                // afterKernel로 된 노드 및 (incoming 노드->추가된 노드) 엣지들 추가 추가
                                val newNode = NewNonAtomicNode(afterKernel.asInstanceOf[NonAtomicNontermKernel[NonAtomicSymbol with Nonterm]], parsed.asInstanceOf[ParsedSymbolsSeq[NonAtomicSymbol with Nonterm]])
                                val newEdges: Set[SimpleEdge] = incomingSimpleEdges map { e => SimpleEdge(e.start, newNode) }
                                cc.withNodesEdgesReverters(Set(newNode), newEdges.toSet[Edge], newEdgeReverter, None)

                                // Lift.after를 추가된 노드로 지정
                                val lift = Lift(node, afterKernel, parsed, Some(newNode))

                                // DeriveTask 추가
                                newQueue +:= DeriveTask(newNode)

                            case _ => throw new AssertionError("should be nonatomic")
                        }
                    }

                    derive(rest, cc.withLiftAndLiftReverters(Lift(node, afterKernel, parsed, None), reverterTriggers))
            }
        }
        val baseNode = startKernel match {
            case kernel: AtomicNontermKernel[_] => BaseAtomicNode(kernel)
            case kernel: NonAtomicNontermKernel[_] => BaseNonAtomicNode(kernel)
        }
        derive(List(DeriveTask(baseNode)), DerivationGraph(baseNode, Set(baseNode), Set(), Set(), Map(), Set(), Set()))
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
