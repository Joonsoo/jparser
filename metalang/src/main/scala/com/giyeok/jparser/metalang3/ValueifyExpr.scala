package com.giyeok.jparser.metalang3

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{EnumTypeName, EnumValueName, InPlaceSequence, NamedParam, RHS, StringChar, TypeName, TypeOrFuncName}
import com.giyeok.jparser.metalang3.TypeFunc.{BoolType, CharType, NodeType, NullType, StringType}

// Node를 받아서 Elem의 값을 얻는 expression
abstract sealed class ValueifyExpr {
    val resultType: TypeFunc
}

case object InputNode extends ValueifyExpr {
    override val resultType: TypeFunc = NodeType
}

case class MatchNonterminal(nonterminal: MetaGrammar3Ast.Nonterminal, expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class Unbind(symbol: MetaGrammar3Ast.Symbol, expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class JoinBodyOf(expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class JoinCondOf(expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class ExceptBodyOf(expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class ExceptCondOf(expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

// TODO Except, Lookahead, LookaheadNot

case class SeqElemAt(expr: ValueifyExpr, index: Int, override val resultType: TypeFunc) extends ValueifyExpr

case class UnrollRepeat(minimumRepeat: Int, arrayExpr: ValueifyExpr, elemProcessExpr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class UnrollChoices(choiceExpr: ValueifyExpr, map: Map[DerivationChoice, ValueifyExpr], override val resultType: TypeFunc) extends ValueifyExpr

sealed class DerivationChoice

case class InPlaceSequenceChoice(inPlaceSequence: InPlaceSequence) extends DerivationChoice

case class SymbolChoice(symbol: MetaGrammar3Ast.Symbol) extends DerivationChoice

case class RightHandSideChoice(rhs: RHS) extends DerivationChoice

case object EmptySeqChoice extends DerivationChoice

case class NamedConstructCall(className: TypeName, params: List[(NamedParam, ValueifyExpr)], override val resultType: TypeFunc) extends ValueifyExpr

case class UnnamedConstructCall(className: TypeName, params: List[ValueifyExpr], override val resultType: TypeFunc) extends ValueifyExpr

case class FuncCall(funcName: TypeOrFuncName, params: List[ValueifyExpr], override val resultType: TypeFunc) extends ValueifyExpr

case class ArrayExpr(elems: List[ValueifyExpr], override val resultType: TypeFunc) extends ValueifyExpr

object PreOp extends Enumeration {
    val NOT: PreOp.Value = Value
}

case class PrefixOp(prefixOpType: PreOp.Value, expr: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

object Op extends Enumeration {
    val ADD, EQ, NE, BOOL_AND, BOOL_OR = Value
}

case class BinOp(op: Op.Value, lhs: ValueifyExpr, rhs: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class ElvisOp(expr: ValueifyExpr, ifNull: ValueifyExpr, override val resultType: TypeFunc) extends ValueifyExpr

case class TernaryExpr(condition: ValueifyExpr, ifTrue: ValueifyExpr, ifFalse: ValueifyExpr, conditionType: TypeFunc, override val resultType: TypeFunc) extends ValueifyExpr

abstract sealed class Literal extends ValueifyExpr

case object NullLiteral extends Literal {
    override val resultType: TypeFunc = NullType
}

case class BoolLiteral(value: Boolean) extends Literal {
    override val resultType: TypeFunc = BoolType
}

case class CharLiteral(value: Char) extends Literal {
    override val resultType: TypeFunc = CharType
}

case class StringLiteral(value: String) extends Literal {
    override val resultType: TypeFunc = StringType
}

case object StringLiteral {
    def escape(list: List[StringChar]): String = ???
}

abstract sealed class EnumValue extends ValueifyExpr

case class CanonicalEnumValue(name: EnumTypeName, value: EnumValueName, override val resultType: TypeFunc) extends EnumValue

case class ShortenedEnumValue(value: EnumValueName, override val resultType: TypeFunc) extends EnumValue
