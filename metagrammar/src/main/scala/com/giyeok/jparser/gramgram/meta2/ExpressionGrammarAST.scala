package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.{BindNode, Node}
import com.giyeok.jparser.nparser.NGrammar.{NNonterminal, NStart}

object ExpressionGrammarAST {

    sealed trait Expression

    case class BinOp(op: Node, lhs: Expression, rhs: Term) extends Term

    sealed trait Term extends Expression

    sealed trait Factor extends Term

    case class Paren(expr: Expression) extends Factor

    sealed trait Number extends Factor

    case class Integer(value: List[Node]) extends Number

    case class Variable(name: Node) extends Factor

    case class Array(elems: List[Expression])

    def matchStart(node: Node): Expression = {
        val BindNode(_: NStart, BindNode(NNonterminal(1, _, _), body)) = node
        matchExpression(body)
    }

    def matchExpression(node: Node): Expression = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 1 => // term
                // assert(body.asInstanceOf[BindNode].symbol.id == 3)
                matchTerm(body.asInstanceOf[BindNode].body)
            case 2 => // expression '+' term {@BinOp(op=$1, lhs:Expression=$0, rhs=$2)}
                // body.asInstanceOf[SequenceNode]
                ???
        }
    }

    def matchTerm(node: Node): Term = {
        ???
    }
}
