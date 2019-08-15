package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.gramgram.meta2.Analyzer.{Analysis, Analyzer}

class ScalaDefGenerator(val analysis: Analysis) {
    def grammarDef(): String = ???

    def typeSpecToString(typeSpec: TypeSpec): String = typeSpec match {
        case ParseNodeType => "Node"
        case ClassType(className) => className
        case ArrayType(elemType) =>
            s"List[${typeSpecToString(elemType)}]"
        case OptionalType(valueType) =>
            s"Option[${typeSpecToString(valueType)}]"
        case _: UnionType | _: UnionNodeType | _: ArrayConcatNodeType =>
            throw new Exception(s"Union type is not supported: $typeSpec")
    }

    def classDefsList(): Map[String, String] = (analysis.classDefs map { d =>
        val supers = if (d.supers.isEmpty) "" else {
            s" extends ${d.supers mkString " with "}"
        }
        val defString: String = if (d.isAbstract) {
            s"sealed trait ${d.name}$supers"
        } else {
            val params = d.params map { p =>
                s"${p.name}:${typeSpecToString(p.typ)}"
            } mkString ", "
            s"case class ${d.name}($params)$supers"
        }
        d.name -> defString
    }).toMap

    def classDefs(): String = {
        // TODO sort classDefsList() by analysis.typeHierarchyGraph.topologicalClassTypesOrder
        classDefsList().values mkString "\n"
    }

    def astifierDefs(): String = ???
}
