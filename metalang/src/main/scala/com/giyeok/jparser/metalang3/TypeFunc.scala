package com.giyeok.jparser.metalang3

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.Processor

sealed class TypeFunc

object NodeType extends TypeFunc

case class TypeOfSymbol(symbol: MetaGrammar3Ast.Symbol) extends TypeFunc

case class TypeOfProcessor(processor: Processor) extends TypeFunc

case class ClassType(name: String) extends TypeFunc

case class OptionalOf(typ: TypeFunc) extends TypeFunc

case class ArrayOf(typ: TypeFunc) extends TypeFunc

case class ElvisType(value: TypeFunc, ifNull: TypeFunc) extends TypeFunc

case class AddOpType(lhs: TypeFunc, rhs: TypeFunc) extends TypeFunc

case class UnionOf(types: List[TypeFunc]) extends TypeFunc

case class CanonicalEnum(enumName: String) extends TypeFunc

case class UnspecifiedEnum(uniqueId: Int) extends TypeFunc

object NullType extends TypeFunc

object BoolType extends TypeFunc

object CharType extends TypeFunc

object StringType extends TypeFunc
