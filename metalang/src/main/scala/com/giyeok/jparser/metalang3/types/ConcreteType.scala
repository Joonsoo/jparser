package com.giyeok.jparser.metalang3.types

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{EnumTypeName, TypeName}

sealed class ConcreteType

object ConcreteType {

    object NodeType extends ConcreteType

    case class ClassType(name: TypeName) extends ConcreteType

    case class OptionalOf(typ: ConcreteType) extends ConcreteType

    case class ArrayOf(elemType: ConcreteType) extends ConcreteType

    case class UnionOf(types: List[ConcreteType]) extends ConcreteType

    case class EnumType(enumName: EnumTypeName) extends ConcreteType

    object NullType extends ConcreteType

    object BoolType extends ConcreteType

    object CharType extends ConcreteType

    object StringType extends ConcreteType

}
