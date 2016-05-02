package com.giyeok.jparser

import Kernels._
import DerivationGraph._
import Symbols._
import ParseTree._
import Derivations._
import com.giyeok.jparser.Inputs.TermGroupDesc

case class DerivationGraph(nodes: Set[Node], edges: Set[Edge]) {
    def withNode(newNode: Node) = DerivationGraph(nodes + newNode, edges)
    def withNodeAndEdge(newNode: Node, newEdge: Edge) = DerivationGraph(nodes + newNode, edges + newEdge)
    def withNodeAndEdges(newNode: Node, newEdges: Set[Edge]) = DerivationGraph(nodes + newNode, edges ++ newEdges)

    def compaction: DerivationGraph = ???
    def terminals: Set[Terminal] = ???
    def termGroups: Set[TermGroupDesc] = ???
    def sliceByTermGroups: Map[TermGroupDesc, DerivationGraph] = ???
}

object DerivationGraph {
    sealed trait Node { val kernel: Kernel }
    case class BaseNode(kernel: Kernel) extends Node // BaseNode에는 사실 kernel이 있을 필요는 없는데 구현의 편의성을 위해 넣었음
    case class NewNode(kernel: Kernel) extends Node

    sealed trait Edge
    case class SimpleEdge(start: Node, end: Node) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node) extends Edge

    // case class CompactNode(path: Seq[NewNode]) extends Node

    def deriveFromKernel(grammar: Grammar, startKernel: Kernel): (DerivationGraph, Set[(Kernel, ParseNode[Symbol])]) = {
        def derive(node: Node, cc: DerivationGraph): DerivationGraph = {
            if (cc.nodes contains NewNode(node.kernel)) cc
            else {
                node.kernel match {
                    case EmptyKernel => cc
                    case kernel: TerminalKernel =>
                        val newNode = NewNode(kernel)
                        cc.withNodeAndEdge(newNode, SimpleEdge(node, newNode))
                    case kernel: NontermKernel[_] =>
                        kernel.derive(grammar) match {
                            case EmptyDerivation =>
                                ???
                            case SymbolDerivation(derives) =>
                                ???
                            case JoinDerivation(derive, join) =>
                                ???
                            case TempLiftBlockableDerivation(derive, blockTrigger) =>
                                ???
                            case RevertableDerivation(derive, revertTrigger) =>
                                ???
                            case DeriveRevertableDerivation(derive, deriveRevertTrigger) =>
                                ???
                            case ReservedLiftTriggeredLiftRevertableDerivation(derive) =>
                                ???
                            case ReservedAliveTriggeredLiftRevertableDerivation(derive) =>
                                ???
                        }
                        ???
                }
            }
        }
        derive(BaseNode(startKernel), DerivationGraph(Set(), Set()))
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
