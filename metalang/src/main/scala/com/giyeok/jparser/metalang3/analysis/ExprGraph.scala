package com.giyeok.jparser.metalang3.analysis

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.analysis.ExprGraph.{Edge, Node}
import com.giyeok.jparser.metalang3.types.TypeFunc
import com.giyeok.jparser.metalang3.valueify.ValueifyExpr
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

// ExprGraph는 ValueifyExpr, Symbol, Class 타입간의 관계를 나타냄
class ExprGraph private(val nodes: Set[Node], val edges: Set[Edge], val edgesByStart: Map[Node, Set[Edge]], val edgesByEnd: Map[Node, Set[Edge]]) extends AbstractGraph[Node, Edge, ExprGraph] {
    def createGraph(nodes: Set[Node], edges: Set[Edge], edgesByStart: Map[Node, Set[Edge]], edgesByEnd: Map[Node, Set[Edge]]): ExprGraph =
        new ExprGraph(nodes, edges, edgesByStart, edgesByEnd)

    def addSymbolTypeSpec(symbol: Symbols.Symbol, typeFunc: TypeFunc): Unit = ???
}

object ExprGraph {
    val empty = new ExprGraph(Set(), Set(), Map(), Map())

    sealed class Node

    case class ClassParamNode(className: String, index: Int) extends Node

    // BinOp: lhs=0, rhs=1
    // Elvis, value=0, ifNull=1,
    // PrefixOp: expr=0
    // ArrayExpr: index=elem index
    case class ExprParamNode(caller: ValueifyExpr, index: Int) extends Node

    // ClassParamNode --accepts--> ValueifyExprNode
    // ExprParamNode --accepts--> ValueifyExprNode
    case class ExprNode(expr: ValueifyExpr) extends Node

    // ExprNode --accepts--> SymbolNode
    // SymbolNode --accepts--> ExprNode
    case class SymbolNode(symbol: Symbols.Symbol) extends Node

    case class Edge(val start: Node, val end: Node) extends AbstractEdge[Node]

}
