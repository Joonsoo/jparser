package com.giyeok.jparser.examples

import com.giyeok.jparser.ParseResultTree.Node

object ExpressionAST {

    case class Array(elems: List[List[Expression]])

    sealed trait Term extends Expression

    sealed trait Number extends Factor

    case class Variable(name: Node) extends Factor

    case class BinOp(op: Node, lhs: Expression, rhs: Term) extends Term

    sealed trait Factor extends Term

    case class Integer(value: List[Node]) extends Number

    sealed trait Expression

    case class Paren(expr: Expression) extends Factor

}
