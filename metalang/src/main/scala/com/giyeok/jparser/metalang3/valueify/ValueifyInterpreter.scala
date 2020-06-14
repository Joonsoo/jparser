package com.giyeok.jparser.metalang3.valueify

import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node}
import com.giyeok.jparser.metalang3.AnalysisResult
import com.giyeok.jparser.metalang3.valueify.ValueifyInterpreter._

// TODO ParseResultTree.Node, ValueifyExpr 받아서 결과물 반환
class ValueifyInterpreter(val analysis: AnalysisResult) {

    def check(cond: Boolean, msg: String): Unit = {
        // TODO
    }

    def valueify(node: Node, valueifyExpr: ValueifyExpr): Value = valueifyExpr match {
        case InputNode => NodeValue(node)
        case MatchNonterminal(nonterminalName, expr, _) =>
            val e = valueify(node, expr)
            assert(e.isInstanceOf[NodeValue])
            val matcher = analysis.valueifyExprsMap(nonterminalName)
            valueify(e.asInstanceOf[NodeValue].astNode, matcher)
        case Unbind(symbol, expr) =>
            val BindNode(bindSymbol, body) = node
            check(bindSymbol.id == analysis.symbolOf(symbol).id, "")
            valueify(body, expr)
        case JoinBodyOf(joinSymbol, joinExpr, bodyProcessorExpr, _) =>
            val JoinNode(bindSymbol, body, _) = node
            check(bindSymbol.id == analysis.symbolOf(joinSymbol).id, "")
            valueify(body, joinExpr)
        case JoinCondOf(joinSymbol, joinExpr, condProcessorExpr, resultType) =>
            val JoinNode(bindSymbol, _, cond) = node
            check(bindSymbol.id == analysis.symbolOf(joinSymbol).id, "")
            valueify(cond, joinExpr)
        case SeqElemAt(expr, index, resultType) => ???
        case UnrollRepeat(minimumRepeat, arrayExpr, elemProcessExpr, resultType) => ???
        case UnrollChoices(choiceExpr, map, resultType) => ???
        case NamedConstructCall(className, params, resultType) => ???
        case UnnamedConstructCall(className, params, resultType) => ???
        case FuncCall(funcName, params, resultType) => ???
        case ArrayExpr(elems, resultType) => ???
        case PrefixOp(prefixOpType, expr, resultType) => ???
        case BinOp(op, lhs, rhs, resultType) => ???
        case ElvisOp(expr, ifNull, resultType) => ???
        case TernaryExpr(condition, ifTrue, ifFalse, resultType) => ???
        case literal: Literal =>
            literal match {
                case NullLiteral => NullValue(node)
                case BoolLiteral(value) => BoolValue(node, value)
                case CharLiteral(value) => CharValue(node, value)
                case StringLiteral(value) => StringValue(node, value)
            }
        case value: EnumValue => ???
    }
}

object ValueifyInterpreter {

    case class ValueifyException(msg: String) extends Exception

    abstract sealed class Value {
        val astNode: Node
    }

    case class NodeValue(override val astNode: Node) extends Value

    case class ClassValue(override val astNode: Node,
                          className: String, args: List[Value]) extends Value

    case class ArrayValue(override val astNode: Node, elems: List[Value]) extends Value

    case class EnumValue(override val astNode: Node, enumType: String, enumValue: String) extends Value

    case class NullValue(override val astNode: Node) extends Value

    case class BoolValue(override val astNode: Node, value: Boolean) extends Value

    case class CharValue(override val astNode: Node, value: Char) extends Value

    case class StringValue(override val astNode: Node, value: String) extends Value

}
