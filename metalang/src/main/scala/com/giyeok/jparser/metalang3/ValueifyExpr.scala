package com.giyeok.jparser.metalang3

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.StringChar

// Node를 받아서 Elem의 값을 얻는 expression
sealed class ValueifyExpr

case object InputNode extends ValueifyExpr

case class MatchNonterminal(nonterminal: MetaGrammar3Ast.Nonterminal, expr: ValueifyExpr) extends ValueifyExpr

case class Unbind(symbol: MetaGrammar3Ast.Symbol, expr: ValueifyExpr) extends ValueifyExpr

case class JoinBodyOf(expr: ValueifyExpr) extends ValueifyExpr

case class JoinCondOf(expr: ValueifyExpr) extends ValueifyExpr

case class ExceptBodyOf(expr: ValueifyExpr) extends ValueifyExpr

case class ExceptCondOf(expr: ValueifyExpr) extends ValueifyExpr

// TODO Except, Lookahead, LookaheadNot

case class SeqElemAt(expr: ValueifyExpr, index: Int) extends ValueifyExpr

case class UnrollOptional(expr: ValueifyExpr) extends ValueifyExpr

case class UnrollRepeatFromZero(expr: ValueifyExpr) extends ValueifyExpr

case class UnrollRepeatFromOne(expr: ValueifyExpr) extends ValueifyExpr

case class UnrollLongest(expr: ValueifyExpr) extends ValueifyExpr

case class UnrollChoices(map: Map[MetaGrammar3Ast.Symbol, ValueifyExpr]) extends ValueifyExpr

case class ConstructExpr(className: String, params: List[ValueifyExpr]) extends ValueifyExpr

case class ArrayExpr(elems: List[ValueifyExpr]) extends ValueifyExpr

object PreOp extends Enumeration {
    val NOT: PreOp.Value = Value
}

case class PrefixOp(prefixOpType: PreOp.Value, expr: ValueifyExpr, exprType: TypeFunc) extends ValueifyExpr

object Op extends Enumeration {
    val ADD, EQ, NE, BOOL_AND, BOOL_OR = Value
}

case class BinOp(op: Op.Value, lhs: ValueifyExpr, rhs: ValueifyExpr, lhsType: TypeFunc, rhsType: TypeFunc) extends ValueifyExpr

case class ElvisOp(expr: ValueifyExpr, ifNull: ValueifyExpr) extends ValueifyExpr

case class TernateryExpr(condition: ValueifyExpr, ifTrue: ValueifyExpr, ifFalse: ValueifyExpr, conditionType: TypeFunc) extends ValueifyExpr

sealed class Literal extends ValueifyExpr

case object NullLiteral extends Literal

case class BoolLiteral(value: Boolean) extends Literal

case class CharLiteral(value: Char) extends Literal

case class StringLiteral(value: String) extends Literal

case object StringLiteral {
    def escape(list: List[StringChar]): String = ???
}

sealed class EnumValue extends ValueifyExpr

case class CanonicalEnumValue(name: String, value: String) extends EnumValue

case class ShortenedEnumValue(value: String) extends EnumValue
