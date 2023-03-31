package com.giyeok.jparser.metalang3

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.MetaLanguage3.{IllegalGrammar, check}
import com.giyeok.jparser.metalang3.Type.{readableNameOf, unifyTypes}

// bottom-up type inference
// nonterminalTypes는 처음에 사용자가 지정한 값들만 채워짐
// 나중에 nonterminal의 RHS의 타입이 확정되면 추가됨

// 모든 nonterminal이 nonterminalTypes에 포함될 때까지 인퍼런싱 반복
// 문법에 nonterminal 타입이 명시되어 있지 않으면 RHS의 타입을 bottom-up inference해서 확정되면 그 타입으로 지정
class TypeInferer(val startNonterminalName: String, val nonterminalTypes: Map[String, Type]) {
  def typeOfNonterminal(nonterminalName: String): Option[Type] = nonterminalTypes get nonterminalName

  def typeOfSymbol(symbol: Symbols.Symbol): Option[Type] = symbol match {
    case symbol: Symbols.AtomicSymbol => symbol match {
      case _: Symbols.Terminal => Some(Type.NodeType)
      case Symbols.Start => typeOfNonterminal(startNonterminalName)
      case Symbols.Nonterminal(name) => typeOfNonterminal(name)
      case Symbols.OneOf(syms) =>
        val symTypes = syms.map(typeOfSymbol)
        if (symTypes.contains(None)) None else Some(unifyTypes(symTypes.flatten))
      case Symbols.Repeat(sym, _) => typeOfSymbol(sym).map(Type.ArrayOf)
      case Symbols.Except(sym, _) => typeOfSymbol(sym)
      case _: Symbols.Lookahead => Some(Type.NodeType)
      case Symbols.Proxy(sym) => typeOfSymbol(sym)
      case Symbols.Join(sym, _) => typeOfSymbol(sym)
      case Symbols.Longest(sym) => typeOfSymbol(sym)
    }
    case Symbols.Sequence(seq) => if (seq.isEmpty) Some(Type.NodeType) else typeOfSymbol(seq.last)
  }

  // 현재까지 알려진 정보로는 valuefyExpr의 타입을 확정할 수 없으면 None 반환
  def typeOfValuefyExpr(valuefyExpr: ValuefyExpr): Option[Type] = valuefyExpr match {
    case ValuefyExpr.InputNode => Some(Type.NodeType)
    case ValuefyExpr.MatchNonterminal(nonterminalName) => typeOfNonterminal(nonterminalName)
    case ValuefyExpr.Unbind(_, expr) => typeOfValuefyExpr(expr)
    case ValuefyExpr.JoinBody(bodyProcessor) => typeOfValuefyExpr(bodyProcessor)
    case ValuefyExpr.JoinCond(bodyProcessor) => typeOfValuefyExpr(bodyProcessor)
    case ValuefyExpr.SeqElemAt(_, expr) => typeOfValuefyExpr(expr)
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) => typeOfValuefyExpr(elemProcessor).map(Type.ArrayOf)
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(_, elemProcessor) => typeOfValuefyExpr(elemProcessor).map(Type.ArrayOf)
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) => typeOfValuefyExpr(elemProcessor).map(Type.ArrayOf)
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(_, elemProcessor) => typeOfValuefyExpr(elemProcessor).map(Type.ArrayOf)
    case ValuefyExpr.UnrollChoices(choices) =>
      val rhsTypes = choices.values.toSet.map(typeOfValuefyExpr)
      if (rhsTypes.contains(None)) None else Some(unifyTypes(rhsTypes.flatten))
    case ValuefyExpr.ConstructCall(className, params) => Some(Type.ClassType(className))
    case ValuefyExpr.FuncCall(funcType, params) => Some(funcType match {
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsPresent =>
        // TODO params의 길이가 1이고, params[0]의 type이 optional이거나 array이거나
        Type.BoolType
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.IsEmpty =>
        // TODO params의 길이가 1이고, params[0]의 type이 optional이거나 array이거나
        Type.BoolType
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Chr =>
        // TODO params 체크
        Type.CharType
      case com.giyeok.jparser.metalang3.ValuefyExpr.FuncType.Str =>
        // TODO params 체크
        Type.StringType
    })
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemTypes = elems.toSet.map(typeOfValuefyExpr)
      if (elemTypes.contains(None)) None else Some(Type.ArrayOf(unifyTypes(elemTypes.flatten)))
    case ValuefyExpr.BinOp(op, lhs, rhs) => op match {
      case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.ADD =>
        val lhsType = typeOfValuefyExpr(lhs)
        val rhsType = typeOfValuefyExpr(rhs)
        if (lhsType.isEmpty || rhsType.isEmpty) None else (lhsType.get, rhsType.get) match {
          case (Type.StringType, Type.StringType) => Some(Type.StringType)
          case (Type.ArrayOf(lhsElemType), Type.ArrayOf(rhsElemType)) => Some(Type.ArrayOf(unifyTypes(Set(lhsElemType, rhsElemType))))
          case (lhsType, rhsType) =>
            throw IllegalGrammar(s"+ operator cannot be applied to: lhs=${readableNameOf(lhsType)} and rhs=${readableNameOf(rhsType)}")
        }
      case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.EQ => Some(Type.BoolType)
      case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.NE => Some(Type.BoolType)
      case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.BOOL_AND => Some(Type.BoolType)
      case com.giyeok.jparser.metalang3.ValuefyExpr.BinOpType.BOOL_OR => Some(Type.BoolType)
    }
    case ValuefyExpr.PreOp(op, expr) => op match {
      case com.giyeok.jparser.metalang3.ValuefyExpr.PreOpType.NOT =>
        val exprType = typeOfValuefyExpr(expr)
        if (exprType.isEmpty) None else {
          check(exprType.get == Type.BoolType, "Prefix Operator ! must get boolean expression")
          Some(Type.BoolType)
        }
    }
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      val exprType0 = typeOfValuefyExpr(expr)
      val ifNullType = typeOfValuefyExpr(ifNull)
      if (exprType0.isEmpty || ifNullType.isEmpty) None else {
        val exprType = exprType0.get
        check(exprType.isInstanceOf[Type.OptionalOf], "Elvis operator only can be used with optional expression")
        Some(unifyTypes(Set(exprType.asInstanceOf[Type.OptionalOf].typ, ifNullType.get)))
      }
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      val conditionType = typeOfValuefyExpr(condition)
      val ifTrueType = typeOfValuefyExpr(ifTrue)
      val ifFalseType = typeOfValuefyExpr(ifFalse)
      if (conditionType.isEmpty || ifTrueType.isEmpty || ifFalseType.isEmpty) None else {
        check(conditionType.get == Type.BoolType, "Condition of ternary operator must be boolean")
        Some(unifyTypes(Set(ifTrueType.get, ifFalseType.get)))
      }
    case literal: ValuefyExpr.Literal => Some(literal match {
      case ValuefyExpr.NullLiteral => Type.NullType
      case ValuefyExpr.BoolLiteral(_) => Type.BoolType
      case ValuefyExpr.CharLiteral(_) => Type.CharType
      case ValuefyExpr.CharFromTerminalLiteral => Type.CharType
      case ValuefyExpr.StringLiteral(_) => Type.StringType
    })
    case value: ValuefyExpr.EnumValue => value match {
      case ValuefyExpr.CanonicalEnumValue(name, _) => Some(Type.EnumType(name))
      case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, _) =>
        Some(Type.UnspecifiedEnumType(unspecifiedEnumTypeId))
    }
  }
}
