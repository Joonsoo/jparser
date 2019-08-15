package com.giyeok.jparser.gramgram.meta2

case class ClassParam(name: String, typ: TypeSpec)

case class ClassDef(name: String, isAbstract: Boolean, params: List[ClassParam], supers: List[String]) {
    // isAbstract --> params must be empty
    assert(!isAbstract || params.isEmpty)
}
