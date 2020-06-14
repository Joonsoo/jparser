package com.giyeok.jparser.metalang3.graphs

import com.giyeok.jparser.metalang3.graphs.TypeGraph.{Supers, Node}
import com.giyeok.jparser.metalang3.types.TypeHierarchy
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

// TypeGraph는 TypeGraphGen이 만들어내는 수동적 그래프
class TypeGraph private(val nodes: Set[Node], val edges: Set[Supers], val edgesByStart: Map[Node, Set[Supers]], val edgesByEnd: Map[Node, Set[Supers]]) extends AbstractGraph[Node, Supers, TypeGraph] {
    def createGraph(nodes: Set[Node], edges: Set[Supers], edgesByStart: Map[Node, Set[Supers]], edgesByEnd: Map[Node, Set[Supers]]): TypeGraph =
        new TypeGraph(nodes, edges, edgesByStart, edgesByEnd)

    def toTypeHierarchy: TypeHierarchy = ???
}

object TypeGraph {
    val empty = new TypeGraph(Set(), Set(), Map(), Map())

    sealed class Node

    case class AbstractType(name: String) extends Node

    case class ConcreteType(name: String) extends Node

    // EnumType은 엣지가 들어오거나 나갈 수 없음
    case class EnumType(name: String) extends Node

    // start는 AbstractType, end는 AbstractType 혹은 ConcreteType. "class end extends start"
    sealed class Supers(override val start: Node, override val end: Node) extends AbstractEdge[Node]

}
