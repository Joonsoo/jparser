package com.giyeok.jparser.gramgram.meta2

sealed trait TypeSpec

sealed trait OptionableTypeSpec extends TypeSpec

sealed trait ActualTypeSpec extends TypeSpec

case object ParseNodeType extends OptionableTypeSpec with ActualTypeSpec

case class ClassType(className: String) extends OptionableTypeSpec with ActualTypeSpec

case class UnionNodeType(types: Set[NodeType]) extends TypeSpec

case class UnionType(types: Set[TypeSpec]) extends TypeSpec

case class ArrayType(elemType: TypeSpec) extends OptionableTypeSpec with ActualTypeSpec

case class OptionalType(valueType: OptionableTypeSpec) extends TypeSpec with ActualTypeSpec

case class NodeType(fixedType: Option[TypeSpec], inferredTypes: Set[TypeSpec])

