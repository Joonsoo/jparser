package com.giyeok.jparser.mgroup3

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object Stage2ProtoEmit {
  fun emit(schema: AstSchema): String {
    val sb = StringBuilder()
    sb.append("syntax = \"proto3\";\n\n")
    sb.append("package ").append(schema.packageName).append(";\n\n")

    for (e in schema.enums) {
      sb.append("enum ").append(e.name).append(" {\n")
      sb.append("  ").append(e.name).append("_UNSPECIFIED = 0;\n")
      e.values.forEachIndexed { i, v ->
        sb.append("  ").append(v).append(" = ").append(i + 1).append(";\n")
      }
      sb.append("}\n\n")
    }

    for (m in schema.messages) {
      sb.append("message ").append(m.name).append(" {\n")
      if (m.sealedChildren.isNotEmpty()) {
        sb.append("  oneof value {\n")
        m.sealedChildren.forEachIndexed { i, child ->
          sb.append("    ").append(child).append(" ")
            .append(lowerFirst(child)).append(" = ").append(i + 1).append(";\n")
        }
        sb.append("  }\n")
      } else {
        for (f in m.fields) {
          val (label, typeStr) = renderField(f.type)
          if (label.isNotEmpty()) sb.append("  ").append(label).append(" ")
          else sb.append("  ")
          sb.append(typeStr).append(" ").append(f.name).append(" = ").append(f.number).append(";\n")
        }
      }
      sb.append("}\n\n")
    }
    return sb.toString()
  }

  fun run(schema: AstSchema, out: Path) {
    out.parent?.createDirectories()
    out.writeText(emit(schema))
  }

  // Returns (label, typeName). label is "repeated", "optional", or "".
  private fun renderField(t: SchemaType): Pair<String, String> = when (t) {
    is SchemaType.Arr -> "repeated" to typeName(t.of)
    is SchemaType.Opt -> "optional" to typeName(t.of)
    else -> "" to typeName(t)
  }

  private fun typeName(t: SchemaType): String = when (t) {
    SchemaType.Bool -> "bool"
    SchemaType.Int32 -> "int32"
    SchemaType.Str -> "string"
    SchemaType.NodeBytes -> "bytes"
    is SchemaType.Msg -> t.name
    is SchemaType.Enm -> t.name
    is SchemaType.Opt -> typeName(t.of) // nested optional: collapse
    is SchemaType.Arr -> typeName(t.of) // nested array: collapse (proto3 lacks nested repeated)
  }

  private fun lowerFirst(s: String): String =
    if (s.isEmpty()) s else s[0].lowercaseChar() + s.substring(1)
}
