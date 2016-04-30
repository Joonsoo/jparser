package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Kernels.Kernel
import com.giyeok.jparser.Symbols.Terminal

object DerivationGraph {
    sealed trait Node { val kernel: Kernel }

    // BaseNode는 이전 세대에서 계승되는 노드, NewNode는 새로 생기는 노드
    sealed trait BaseNode extends Node
    sealed trait NewNode extends Node

    object ReservedReverterType extends Enumeration {
        val IfLifted, IfAlive = Value
    }
    sealed trait ReservedReverterNode extends Node { val reverterType: ReservedReverterType.Value }
    sealed trait TempBlockableNode extends Node { val trigger: Node }

    sealed trait Edge
    case class SimpleEdge(start: Node, end: Node) extends Edge
    case class JoinEdge(start: Node, end: Node, join: Node, endJoinSwitched: Boolean) extends Edge
    case class RevertableSimpleEdge(start: Node, end: Node, trigger: Node) extends Edge
}

object DerivationSubgraph {
    sealed trait ProgNodeSpec
    sealed trait NonBaseNodeSpec extends ProgNodeSpec
    // cycles는 해당 노드가 lift되면 그 지점에서부터 cycles subgraph를 다시 expand한 뒤 마지막 지점마다 해당 노드 lift해서 나온 parse tree를 덧붙여서 lift를 다시 진행 
    case class BaseNode(children: Seq[NonBaseNodeSpec], cycles: Seq[Seq[NonBaseNodeSpec]]) extends ProgNodeSpec
    case class SymbolNode(kernel: Kernel, children: Seq[NonBaseNodeSpec], cycles: Seq[Seq[NonBaseNodeSpec]]) extends NonBaseNodeSpec
    case class JoinNode(kernel: Kernel, child: NonBaseNodeSpec, join: NonBaseNodeSpec, endJoinSwitched: Boolean) extends NonBaseNodeSpec
    case class TermNode(terminal: Terminal) extends NonBaseNodeSpec
    case object EmptyNode extends NonBaseNodeSpec
}
