package com.giyeok.jparser.metalang3.types

class TypeHierarchy {

    case class EnumType(name: String, values: List[String])

    case class AbstractType(name: String)

    case class ClassType(name: String, params: List[ClassParam])

    case class ClassParam(name: String, specifiedType: Option[TypeFunc])

}
