package com.giyeok.jparser.mgroup3

sealed class SchemaType {
  object Bool : SchemaType()
  object Int32 : SchemaType()
  // Kotlin/Rust AST 의 Char — proto 표현은 int32.
  object Chr : SchemaType()
  object Str : SchemaType()
  object NodeBytes : SchemaType()
  data class Msg(val name: String) : SchemaType()
  data class Enm(val name: String) : SchemaType()
  data class Opt(val of: SchemaType) : SchemaType()
  data class Arr(val of: SchemaType) : SchemaType()
}

data class FieldDef(val name: String, val number: Int, val type: SchemaType)

data class MessageDef(
  // proto/Rust 측 이름 — Rust 키워드/프렐류드와 충돌하는 원본 이름은 변경됨
  // (예: Self → SelfNode). SchemaBuilder.rustSafeName 참고.
  val name: String,
  // Kotlin typed AST 의 원본 클래스 이름 (KotlinOptCodeGen 산출물과 일치).
  val kotlinName: String,
  val fields: List<FieldDef>,
  val sealedChildren: List<String>,
)

data class EnumDef(val name: String, val kotlinName: String, val values: List<String>)

data class AstSchema(
  val packageName: String,
  val messages: List<MessageDef>,
  val enums: List<EnumDef>,
)
