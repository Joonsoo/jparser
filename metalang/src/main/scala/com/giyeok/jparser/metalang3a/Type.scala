package com.giyeok.jparser.metalang3a

sealed abstract class Type

object Type {

  object NodeType extends Type

  case class ClassType(name: String) extends Type

  case class OptionalOf(typ: Type) extends Type

  case class ArrayOf(elemType: Type) extends Type

  case class UnionOf(types: Set[Type]) extends Type {
    assert(types.nonEmpty)
  }

  case class EnumType(enumName: String) extends Type

  case class UnspecifiedEnumType(uniqueId: Int) extends Type

  object NullType extends Type

  object AnyType extends Type

  object NothingType extends Type

  object BoolType extends Type

  object CharType extends Type

  object StringType extends Type

  def unifyTypes(types: Set[Type]): Type =
    if (types.isEmpty) {
      Type.NothingType
    } else if (types.size == 1) {
      types.head
    } else if (types.contains(Type.NullType)) {
      val exceptNull = types - Type.NullType
      Type.OptionalOf(unifyTypes(exceptNull))
    } else if (types.contains(Type.NothingType) && types.size > 1) {
      unifyTypes(types - Type.NothingType)
    } else if (types.exists(typ => types.contains(Type.OptionalOf(typ)))) {
      unifyTypes(types.filterNot(typ => types.contains(Type.OptionalOf(typ))))
    } else if (types.exists(_.isInstanceOf[Type.UnionOf])) {
      // UnionType을 포함하고 있으면 풀어줌
      unifyTypes(types.flatMap {
        case Type.UnionOf(types) => types
        case typ => Set(typ)
      })
    } else if (types.forall(typ => typ.isInstanceOf[Type.ArrayOf])) {
      Type.ArrayOf(unifyTypes(types.map(_.asInstanceOf[Type.ArrayOf].elemType)))
    } else {
      Type.UnionOf(types)
    }

  def readableNameOf(typ: Type): String = typ match {
    case NodeType => "node"
    case ClassType(name) => s"class $name"
    case OptionalOf(typ) => s"opt(${readableNameOf(typ)})"
    case ArrayOf(elemType) => s"array(${readableNameOf(elemType)})"
    case UnionOf(types) => s"union(${types.map(readableNameOf).mkString(",")})"
    case EnumType(enumName) => s"enum $enumName"
    case UnspecifiedEnumType(uniqueId) => s"usenum $uniqueId"
    case NullType => "null"
    case AnyType => "any"
    case BoolType => "boolean"
    case CharType => "char"
    case StringType => "string"
    case NothingType => "nothing"
  }
}
