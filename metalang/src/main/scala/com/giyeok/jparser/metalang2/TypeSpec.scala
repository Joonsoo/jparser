package com.giyeok.jparser.metalang2

sealed trait TypeSpec

case object ParseNodeType extends TypeSpec

case class ClassType(className: String) extends TypeSpec

case class ArrayType(elemType: TypeSpec) extends TypeSpec

case class OptionalType(valueType: TypeSpec) extends TypeSpec

case class UnionType(types: Set[TypeSpec]) extends TypeSpec

case class NodeType(fixedType: Option[TypeSpec], inferredTypes: Set[TypeSpec])
