package com.giyeok.jparser

import Kernels._
import DerivationGraph._
import Symbols._
import ParseTree._
import Derivations._
import com.giyeok.jparser.Inputs.TermGroupDesc

case class DerivationGraph(baseNode: Node, nodes: Set[Node], edges: Set[Edge], lifts: Map[Node, Set[(Kernel, ParseNode[Symbol])]], edgeReverters: Set[(Node, SimpleEdge)], reservedReverters: Set[ReservedReverter]) {
    // baseNode가 nodes에 포함되어야 함
    assert(nodes contains baseNode)
    // edges의 모든 노드가 nodes에 포함되어야 함
    assert(edges flatMap { _ match { case SimpleEdge(start, end) => Set(start, end) case JoinEdge(start, end, join) => Set(start, end, join) } } subsetOf nodes)
    // lifts의 시작 노드가 nodes에 포함되어야 함
    assert(lifts forall { l => nodes contains l._1 })
    // lifts의 lift된 후의 커널, lift된 ParseNode가 모두 원 노드와 같은 심볼을 가져야 함
    assert(lifts forall { l => l._2 forall { v => l._1.kernel.symbol == v._1.symbol && v._1.symbol == v._2.symbol } })
    // edgeReverters의 트리거 노드가 모두 nodes에 포함되어야 함
    assert(edgeReverters map { _._1 } subsetOf nodes)
    // edgeReverters의 대상 엣지가 모두 edges에 포함되어야 함
    assert(edgeReverters map { _._2.asInstanceOf[Edge] } subsetOf edges)
    // reservedReverters의 대상 노드가 모두 nodes에 포함되어야 함
    assert(reservedReverters map { _.node } subsetOf nodes)

    def withNodesEdgesReverters(newNodes: Set[Node], newEdges: Set[Edge], newEdgeReverters: Set[(Node, SimpleEdge)], newReservedReverters: Set[ReservedReverter]): DerivationGraph =
        DerivationGraph(baseNode, nodes ++ newNodes, edges ++ newEdges, lifts, edgeReverters ++ newEdgeReverters, reservedReverters ++ newReservedReverters)

    def compaction: DerivationGraph = ???
    def terminals: Set[Terminal] = ???
    def termGroups: Set[TermGroupDesc] = ???
    def sliceByTermGroups: Map[TermGroupDesc, DerivationGraph] = ???
}

object DerivationGraph {
    sealed trait Node { val kernel: Kernel }
    sealed trait NontermNode extends Node { val kernel: NontermKernel[Nonterm] }
    case class BaseNode(kernel: NontermKernel[Nonterm]) extends NontermNode // BaseNode에는 사실 kernel이 있을 필요는 없는데 구현의 편의성을 위해 넣었음
    case class NewAtomicNode(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm]) extends NontermNode
    case class NewNonAtomicNode[T <: NonAtomicSymbol with Nonterm](kernel: NonAtomicNontermKernel[T], progress: ParsedSymbolsSeq[T]) extends NontermNode
    case class NewTermNode(kernel: TerminalKernel) extends Node
    case object EmptyNode extends Node { val kernel = EmptyKernel }

    sealed trait Edge
    case class SimpleEdge(start: NontermNode, end: Node) extends Edge
    case class JoinEdge(start: NontermNode, end: Node, join: Node) extends Edge

    sealed trait ReservedReverter { val node: Node }
    case class ReservedLiftTriggeredLiftReverter(node: Node) extends ReservedReverter
    case class ReservedAliveTriggeredLiftReverter(node: Node) extends ReservedReverter

    // case class CompactNode(path: Seq[NewNode]) extends Node

    def instantiateDerivation(baseNode: NontermNode, derivation: Derivation): (Set[Edge], Set[(Node, SimpleEdge)], Set[ReservedReverter]) = {
        def newNode(kernel: Kernel): Node = kernel match {
            case EmptyKernel => EmptyNode
            case k: TerminalKernel => NewTermNode(k)
            case k: AtomicNontermKernel[_] => NewAtomicNode(k)
            case k: NonAtomicNontermKernel[_] => NewNonAtomicNode(k, ParsedSymbolsSeq(k.symbol, List(), List()))
        }

        derivation match {
            case EmptyDerivation =>
                (Set[Edge](), Set[(Node, SimpleEdge)](), Set[ReservedReverter]())
            case SymbolDerivation(derives) =>
                (derives map { k => SimpleEdge(baseNode, newNode(k)) }, Set(), Set())
            case JoinDerivation(derive, join) =>
                (Set(JoinEdge(baseNode, newNode(derive), newNode(join))), Set(), Set())
            case TempLiftBlockableDerivation(derive, blockTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge), Set((newNode(blockTrigger), edge)), Set())
            case RevertableDerivation(derive, revertTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge), Set((newNode(revertTrigger), edge)), Set())
            case DeriveRevertableDerivation(derive, deriveRevertTrigger) =>
                val edge = SimpleEdge(baseNode, newNode(derive))
                (Set(edge, SimpleEdge(baseNode, newNode(deriveRevertTrigger))), Set((newNode(deriveRevertTrigger), edge)), Set())
            case ReservedLiftTriggeredLiftRevertableDerivation(derive) =>
                (Set(SimpleEdge(baseNode, newNode(derive))), Set(), Set(ReservedLiftTriggeredLiftReverter(baseNode)))
            case ReservedAliveTriggeredLiftRevertableDerivation(derive) =>
                (Set(SimpleEdge(baseNode, newNode(derive))), Set(), Set(ReservedAliveTriggeredLiftReverter(baseNode)))
        }
    }

    def deriveFromKernel(grammar: Grammar, startKernel: NontermKernel[Nonterm]): DerivationGraph = {
        sealed trait Task
        case class Derive(node: NontermNode) extends Task
        case class Lift(node: Node, parsed: ParseNode[Symbol]) extends Task

        def derive(queue: List[Task], cc: DerivationGraph): DerivationGraph = {
            queue match {
                case Derive(node) +: rest =>
                    var newQueue = rest
                    var newCC = cc

                    val kernel = node.kernel

                    val (derivedEdges, edgeReverters, reservedReverters) =
                        instantiateDerivation(node, kernel.derive(grammar))
                    val (newDerivedEdges, newEdgeReverters, newReservedReverters) =
                        (derivedEdges -- cc.edges, edgeReverters -- cc.edgeReverters, reservedReverters -- cc.reservedReverters)

                    val newDerivedNodes: Set[Node] = (newDerivedEdges flatMap {
                        _ match {
                            case SimpleEdge(start, end) => Set(end) ensuring start == node
                            case JoinEdge(start, end, join) => Set(end, join) ensuring start == node
                        }
                    }) -- cc.nodes

                    val newReverterTriggerNodes: Set[Node] = (newEdgeReverters map { _._1 }) -- cc.nodes

                    val newNodes = newDerivedNodes ++ newReverterTriggerNodes

                    // newNodes 중 derivable한 것들(nonterm kernel들) 추려서 Derive 태스크 추가
                    newQueue ++:= (newNodes.toList collect { case nonterm: NontermNode => Derive(nonterm) })
                    newCC = cc.withNodesEdgesReverters(newNodes, newDerivedEdges, newEdgeReverters, newReservedReverters)

                    // TODO lift할 때 nullable한 trigger가 붙은 reverter 어떻게 처리할지 고민
                    //   - 좀 특이한 경우긴 하지만..
                    //   - 특히 TempLiftBlockable. 그냥 reverter의 트리거가 nullable한 건 문법 자체가 좀 이상한 경우일듯

                    // TODO newDerivedNodes중 finishable한 것들을 추려서 Lift 태스크 추가
                    val finishableDerivedNodes = newDerivedNodes filter { _.kernel.finishable }
                    // 이런식으로 finishable할 수 있는 심볼은 Empty, Repeat, 드물겠지만 빈 Sequence 심볼 뿐
                    assert(finishableDerivedNodes forall { n => n.kernel == EmptyKernel || n.kernel.isInstanceOf[NonAtomicNontermKernel[_]] })
                    // Empty는 ParsedEmpty(Empty)
                    // NonAtomic은 progress를 주면 ParsedSymbolsSeq(symbol, List(), List())가 될 것
                    finishableDerivedNodes map { n =>
                        n.kernel match {
                            case EmptyKernel => ParsedEmpty(Empty)
                            case kernel: NonAtomicNontermKernel[_] => n.asInstanceOf[NewNonAtomicNode[NonAtomicSymbol with Nonterm]].progress
                            case _ => throw new AssertionError("Cannot happen")
                        }
                    }

                    // TODO newDerivedNodes중 finishable하지는 않은데 cc.lifts에 empty lift 가능한 것으로 되어 있는 것들 추려서 Lift 태스크 추가
                    val liftAgain = newDerivedNodes flatMap { node =>
                        cc.lifts get node match {
                            case Some(lifts) =>
                                ???
                            case None => Set()
                        }
                    }

                    derive(newQueue, newCC)

                case Lift(node @ BaseNode(_), parsed) +: rest =>
                    assert(cc.lifts(node) exists { _._2 == parsed })
                    // nothing to do
                    derive(rest, cc)

                case Lift(node, parsed) +: rest =>
                    // node의 incoming node들에 parsed로 lift 적용
                    // lift된 노드가 derivable하면 Derive 태스크 추가
                    ???
            }
        }
        val baseNode = BaseNode(startKernel)
        derive(List(Derive(baseNode)), DerivationGraph(baseNode, Set(baseNode), Set(), Map(), Set(), Set()))
        ???
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
