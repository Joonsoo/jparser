package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.Type
import com.giyeok.jparser.metalang3.`Type$`
import scala.jdk.javaapi.CollectionConverters

object SchemaBuilder {
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
      MessageDef(name = className, fields = fields, sealedChildren = emptyList())
    }

    val sealedWrappers = abstractClasses.entries.sortedBy { it.key }.map { (parent, children) ->
      MessageDef(name = parent, fields = emptyList(), sealedChildren = children)
    }

    // classRelations 에 등장하지만 classParamTypes 에 없고 abstract 도 아닌 클래스 —
    // parameter 없는 case object/empty case class. 빈 message 로 emit 한다.
    val concreteNames = concreteMessages.map { it.name }.toSet()
    val abstractNames = abstractClasses.keys
    val referencedConcretes = abstractClasses.values.flatten().toSet()
    val emptyConcretes = referencedConcretes
      .filter { it !in concreteNames && it !in abstractNames }
      .map { MessageDef(name = it, fields = emptyList(), sealedChildren = emptyList()) }

    val allMessages = (concreteMessages + sealedWrappers + emptyConcretes)
      .distinctBy { it.name }
      .sortedBy { it.name }

    val enums = enumValues.entries.sortedBy { it.key }.map { (enumName, scalaValues) ->
      val values = CollectionConverters.asJava(scalaValues).toList().sorted()
      EnumDef(name = enumName, values = values)
    }

    return AstSchema(packageName = packageName, messages = allMessages, enums = enums)
  }

  private fun mapType(t: Type, processed: ProcessedGrammar): SchemaType {
    // Scala case object 들 (BoolType, CharType, ...) 은 Kotlin import 가 어려워 (`Type$BoolType$`
    // 식별자에 `$` 가 들어가서 unresolved) class simple name 으로 분기한다.
    when (t::class.java.simpleName) {
      "Type\$BoolType\$" -> return SchemaType.Bool
      "Type\$CharType\$" -> return SchemaType.Int32
      "Type\$StringType\$" -> return SchemaType.Str
      "Type\$NodeType\$" -> return SchemaType.NodeBytes
      "Type\$NullType\$" -> return SchemaType.NodeBytes
      "Type\$AnyType\$" -> return SchemaType.NodeBytes
      "Type\$NothingType\$" -> return SchemaType.NodeBytes
    }
    return mapTypeRest(t, processed)
  }

  private fun mapTypeRest(t: Type, processed: ProcessedGrammar): SchemaType = when {
    t is Type.ClassType -> SchemaType.Msg(t.name())
    t is Type.EnumType -> SchemaType.Enm(t.enumName())
    t is Type.UnspecifiedEnumType -> {
      val name = processed.shortenedEnumTypesMap().get(t.uniqueId())
      if (name.isDefined) SchemaType.Enm(name.get() as String) else SchemaType.NodeBytes
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
