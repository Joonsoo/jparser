package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.Symbols

sealed abstract class ValuefyExpr

object ValuefyExpr {

    case object InputNode extends ValuefyExpr

    // input ParseNode를 nonterminal의 RHS로 보고 valuefy
    case class MatchNonterminal(nonterminalName: String) extends ValuefyExpr

    // input ParseNode를 Unbind해서 binding된 심볼이 symbol인지 확인하고 body에 대해서 expr로 valuefy
    case class Unbind(symbol: Symbols.Symbol, expr: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 joinSymbol의 JoinNode일 것으로 가정. JoinNode의 sym 부분을 bodyProcessor로 valuefy
    case class JoinBody(joinSymbol: Symbols.Join, bodyProcessor: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 joinSymbol의 JoinNode일 것으로 가정. JoinNode의 join 부분을 bodyProcessor로 valuefy
    case class JoinCond(joinSymbol: Symbols.Join, bodyProcessor: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 SequenceNode일 것으로 가정하고, index번째 Node에 대해 expr로 valuefy
    case class SeqElemAt(index: Int, expr: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 0+ Repeat symbol일 것으로 보고 풀어서, 각 ParseNode에 대해 elemProcessor로 valuefy
    case class UnrollRepeatFromZero(elemProcessor: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 1+ Repeat symbol일 것으로 보고 풀어서, 각 ParseNode에 대해 elemProcessor로 valuefy
    case class UnrollRepeatFromOne(elemProcessor: ValuefyExpr) extends ValuefyExpr

    // input ParseNode가 BindNode일 것으로 보고, 어떤 symbol로 bind됐는지 보고 맞는 node에 대해 valuefy
    case class UnrollChoices(choices: Map[Symbols.Symbol, ValuefyExpr]) extends ValuefyExpr

    // input ParamNode를 params로 valuefy해서 class 생성해서 반환
    case class ConstructCall(className: String, params: List[ValuefyExpr]) extends ValuefyExpr

    object FuncType extends Enumeration {
        val IsPresent, IsEmpty, Chr, Str = Value
    }

    // TODO input ParamNode를 params로 valuefy해서 builtin function 콜해서 반환
    case class FuncCall(funcType: FuncType.Value, params: List[ValuefyExpr]) extends ValuefyExpr

    // input ParamNode를 elems로 valuefy해서 array 반환
    case class ArrayExpr(elems: List[ValuefyExpr]) extends ValuefyExpr

    object BinOpType extends Enumeration {
        val ADD, EQ, NE, BOOL_AND, BOOL_OR = Value
    }

    // TODO input ParseNode를 lhs로 가공한 값, rhs로 가공한 값을 op으로 binary 연산해서 반환
    case class BinOp(op: BinOpType.Value, lhs: ValuefyExpr, rhs: ValuefyExpr) extends ValuefyExpr

    object PreOpType extends Enumeration {
        val NOT: PreOpType.Value = Value
    }

    // TODO input ParseNode를 expr로 가공한 값을 op으로 binary 연산해서 반환
    case class PreOp(op: PreOpType.Value, expr: ValuefyExpr) extends ValuefyExpr

    // TODO input ParseNode를 expr로 가공한 값, ifNull로 가공한 값을 elvis 연산해서 반환
    case class ElvisOp(expr: ValuefyExpr, ifNull: ValuefyExpr) extends ValuefyExpr

    // TODO input ParseNode를 condition으로 가공한 값이 true이면 input을 ifTrue로 가공한 값을, false이면 input을 ifFalse로 가공한 값을 반환
    case class TernaryOp(condition: ValuefyExpr, ifTrue: ValuefyExpr, ifFalse: ValuefyExpr) extends ValuefyExpr

    // Literal과 Enum은 input ParseNode와 관계없이 지정된 값을 반환
    abstract sealed class Literal extends ValuefyExpr

    case object NullLiteral extends Literal

    case class BoolLiteral(value: Boolean) extends Literal

    case class CharLiteral(value: Char) extends Literal

    case class StringLiteral(value: String) extends Literal

    abstract sealed class EnumValue extends ValuefyExpr

    case class CanonicalEnumValue(name: String, value: String) extends EnumValue

    case class ShortenedEnumValue(unspecifiedEnumTypeId: Int, value: String) extends EnumValue

}
