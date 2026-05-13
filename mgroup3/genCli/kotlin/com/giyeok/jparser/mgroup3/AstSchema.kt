package com.giyeok.jparser.mgroup3

sealed class SchemaType {
  object Bool : SchemaType()
  object Int32 : SchemaType()
  object Str : SchemaType()
  object NodeBytes : SchemaType()
  data class Msg(val name: String) : SchemaType()
  data class Enm(val name: String) : SchemaType()
  data class Opt(val of: SchemaType) : SchemaType()
  data class Arr(val of: SchemaType) : SchemaType()
}

data class FieldDef(val name: String, val number: Int, val type: SchemaType)

data class MessageDef(
  val name: String,
  val fields: List<FieldDef>,
  val sealedChildren: List<String>,
)

data class EnumDef(val name: String, val values: List<String>)

data class AstSchema(
  val packageName: String,
  val messages: List<MessageDef>,
  val enums: List<EnumDef>,
)
