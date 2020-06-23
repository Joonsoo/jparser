package com.giyeok.jparser.metalang3a

sealed abstract class Type

object Type {

    object NodeType extends Type

    case class ClassType(name: String) extends Type

    case class OptionalOf(typ: Type) extends Type

    case class ArrayOf(elemType: Type) extends Type

    case class UnionOf(types: Set[Type]) extends Type

    case class EnumType(enumName: String) extends Type

    case class UnspecifiedEnumType(uniqueId: Int) extends Type

    object NullType extends Type

    object AnyType extends Type

    object BoolType extends Type

    object CharType extends Type

    object StringType extends Type

}
