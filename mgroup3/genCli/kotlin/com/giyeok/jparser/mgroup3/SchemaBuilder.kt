package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.Type
import com.giyeok.jparser.metalang3.`Type$`
import scala.jdk.javaapi.CollectionConverters

object SchemaBuilder {
  /**
   * Rust 의 타입 위치에서 쓸 수 없는 이름들 (raw identifier 불가 키워드 + 생성
   * 코드가 unqualified 로 참조하는 프렐류드/지원 타입). 충돌 시 `<이름>Node` 로
   * 변경 — proto message/Rust 타입 이름에만 적용, Kotlin AST 는 원본 유지.
   * RustOptCodeGen.RustReservedTypeNames 와 같은 목록 유지할 것.
   */
  private val RUST_RESERVED_TYPE_NAMES = setOf(
    "Self", "Box", "Option", "Vec", "String", "Result", "Some", "None", "Ok", "Err",
    "Ctx", "Encoder", "Kernel", "KernelSet", "IdIssuer",
  )

  fun rustSafeName(name: String): String =
    if (name in RUST_RESERVED_TYPE_NAMES) name + "Node" else name

  fun build(processed: ProcessedGrammar, packageName: String): AstSchema {
    val classParams = CollectionConverters.asJava(processed.classParamTypes())
    val enumValues = CollectionConverters.asJava(processed.enumValuesMap())
    val relations = processed.classRelations()

    val abstractClasses = mutableMapOf<String, List<String>>()
    if (relations != null) {
      for (parent in CollectionConverters.asJava(relations.nodes())) {
        val children = relations.edgesByStart().apply(parent)
        val subs = CollectionConverters.asJava(children).map { sup -> sup.subclass() }.sorted()
        if (subs.isNotEmpty()) {
          abstractClasses[parent] = subs
        }
      }
    }

    val concreteMessages = classParams.entries.sortedBy { it.key }.map { (className, scalaParams) ->
      val params = CollectionConverters.asJava(scalaParams)
      val fields = params.mapIndexed { idx, tup ->
        val name = tup._1() as String
        val rawType = tup._2() as Type
        val schemaType = mapType(rawType, processed)
        FieldDef(name = name, number = idx + 1, type = schemaType)
      }
      MessageDef(
        name = rustSafeName(className), kotlinName = className,
        fields = fields, sealedChildren = emptyList(),
      )
    }

    val sealedWrappers = abstractClasses.entries.sortedBy { it.key }.map { (parent, children) ->
      MessageDef(
        name = rustSafeName(parent), kotlinName = parent,
        fields = emptyList(), sealedChildren = children.map { rustSafeName(it) },
      )
    }

    // classRelations 에 등장하지만 classParamTypes 에 없고 abstract 도 아닌 클래스 —
    // parameter 없는 case object/empty case class. 빈 message 로 emit 한다.
    val concreteNames = concreteMessages.map { it.name }.toSet()
    val abstractNames = abstractClasses.keys
    val referencedConcretes = abstractClasses.values.flatten().toSet()
    val emptyConcretes = referencedConcretes
      .filter { rustSafeName(it) !in concreteNames && it !in abstractNames }
      .map {
        MessageDef(name = rustSafeName(it), kotlinName = it, fields = emptyList(), sealedChildren = emptyList())
      }

    val allMessages = (concreteMessages + sealedWrappers + emptyConcretes)
      .distinctBy { it.name }
      .sortedBy { it.name }

    val enums = enumValues.entries.sortedBy { it.key }.map { (enumName, scalaValues) ->
      val values = CollectionConverters.asJava(scalaValues).toList().sorted()
      EnumDef(name = rustSafeName(enumName), kotlinName = enumName, values = values)
    }

    return AstSchema(packageName = packageName, messages = allMessages, enums = enums)
  }

  private fun mapType(t: Type, processed: ProcessedGrammar): SchemaType {
    // Scala case object 들 (BoolType, CharType, ...) 은 Kotlin import 가 어려워 (`Type$BoolType$`
    // 식별자에 `$` 가 들어가서 unresolved) class simple name 으로 분기한다.
    // 주의: nested class 의 getSimpleName() 은 enclosing `Type$` prefix 를 빼고
    // `StringType$` 만 반환한다 (binary name 인 `Type$StringType$` 가 아니다).
    when (t::class.java.simpleName) {
      "BoolType\$" -> return SchemaType.Bool
      "CharType\$" -> return SchemaType.Chr
      "StringType\$" -> return SchemaType.Str
      "NodeType\$" -> return SchemaType.NodeBytes
      "NullType\$" -> return SchemaType.NodeBytes
      "AnyType\$" -> return SchemaType.NodeBytes
      "NothingType\$" -> return SchemaType.NodeBytes
    }
    return mapTypeRest(t, processed)
  }

  private fun mapTypeRest(t: Type, processed: ProcessedGrammar): SchemaType = when {
    t is Type.ClassType -> SchemaType.Msg(rustSafeName(t.name()))
    t is Type.EnumType -> SchemaType.Enm(rustSafeName(t.enumName()))
    t is Type.UnspecifiedEnumType -> {
      val name = processed.shortenedEnumTypesMap().get(t.uniqueId())
      if (name.isDefined) SchemaType.Enm(rustSafeName(name.get() as String)) else SchemaType.NodeBytes
    }
    t is Type.OptionalOf -> SchemaType.Opt(mapType(t.typ(), processed))
    t is Type.ArrayOf -> SchemaType.Arr(mapType(t.elemType(), processed))
    t is Type.UnionOf -> {
      val reduced = processed.reduceUnionType(t)
      if (reduced is Type.UnionOf) {
        System.err.println("warning: irreducible UnionOf -> NodeBytes: ${`Type$`.`MODULE$`.readableNameOf(t)}")
        SchemaType.NodeBytes
      } else mapType(reduced, processed)
    }
    else -> SchemaType.NodeBytes
  }
}
