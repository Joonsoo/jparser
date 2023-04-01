package com.giyeok.jparser.examples.metalang3

object MetaLang3ExamplesCatalog {
  val metalang3 = GrammarWithExamples.fromResource(
    "/metalang3",
    listOf("metalang3.cdg")
  )

  val asdl = GrammarWithExamples.fromResource(
    "/asdl",
    listOf("pyast.asdl")
  )

  val bibix2 = GrammarWithExamples.fromResource(
    "/bibix2",
    listOf(
      "example1.bbx",
      "example2.bbx",
      "jparser.bbx",
      "jparser-small.bbx",
    )
  )

  val j1 = GrammarWithExamples.fromResource(
    "/j1",
    listOf(
      "example1.j1",
      "example2.j1",
    )
  )

  val proto3 = GrammarWithExamples.fromResource(
    "/proto3",
    listOf(
      "example1.proto",
      "GrammarProto.proto",
      "TermGroupProto.proto",
    )
  )

  val json = GrammarWithExamples.fromResource(
    "/json",
    listOf(
      "example1.json",
      "example2.json",
    )
  )

  val pyobj = GrammarWithExamples.fromResource(
    "/pyobj",
    listOf()
  )

  val all = listOf(asdl, bibix2, j1, proto3, json, pyobj)
}
