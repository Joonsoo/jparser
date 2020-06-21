package com.giyeok.jparser.metalang3.types

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{EnumTypeName, Processor, TypeName, TypeOrFuncName}
import com.giyeok.jparser.metalang3.valueify.{Op, ValueifyExpr}

sealed class TypeFunc

object TypeFunc {

    object NodeType extends TypeFunc

    case class TypeOfSymbol(symbol: MetaGrammar3Ast.Symbol) extends TypeFunc

    case class TypeOfProcessor(processor: Processor) extends TypeFunc

    case class ClassType(name: TypeName) extends TypeFunc

    case class OptionalOf(typ: TypeFunc) extends TypeFunc

    case class ArrayOf(elemType: TypeFunc) extends TypeFunc

    case class ElvisType(value: TypeFunc, ifNull: TypeFunc) extends TypeFunc

    case class AddOpType(lhs: TypeFunc, rhs: TypeFunc) extends TypeFunc

    case class FuncCallResultType(typeOrFuncName: TypeOrFuncName, params: List[ValueifyExpr]) extends TypeFunc

    case class BinOpResultType(binOp: Op.Value, lhsType: TypeFunc, rhsType: TypeFunc) extends TypeFunc

    case class UnionOf(types: List[TypeFunc]) extends TypeFunc

    case class EnumType(enumTypeName: EnumTypeName) extends TypeFunc

    case class UnspecifiedEnum(uniqueId: Int) extends TypeFunc

    object NullType extends TypeFunc

    object AnyType extends TypeFunc

    object BoolType extends TypeFunc

    object CharType extends TypeFunc

    object StringType extends TypeFunc

}
