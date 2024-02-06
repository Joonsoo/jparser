package com.giyeok.jparser.examples.metalang3

object MetaLang3ExamplesCatalog {
  val metalang3 = GrammarWithExamples.fromResource(
    "metalang3",
    "/metalang3",
    listOf("metalang3.cdg")
  )

  val asdl = GrammarWithExamples.fromResource(
    "asdl",
    "/asdl",
    listOf("pyast.asdl")
  )

  val bibix2 = GrammarWithExamples.fromResource(
    "bibix2",
    "/bibix2",
    listOf(
      "example1.bbx",
      "example2.bbx",
      "jparser.bbx",
      "jparser-small.bbx",
    )
  )

  val autodb3problem = GrammarWithExamples.fromResource(
    "autodb3problem",
    "/autodb3problem",
    listOf("test.autodb3"),
    grammarName = "debugging.cdg"
  )

  val j1mark1 = GrammarWithExamples.fromResource(
    "j1-mark1",
    "/j1-mark1",
    listOf(
      "example1.j1",
    )
  )

  val j1mark1subset = GrammarWithExamples.fromResource(
    "j1-mark1-subset",
    "/j1-mark1-sub",
    listOf(
      "example1.j1",
    )
  )

  val j1mark2 = GrammarWithExamples.fromResource(
    "j1-mark2",
    "/j1-mark2",
    listOf(
      "example1.j1",
      "example2.j1",
    )
  )

  val j1mark2subset = GrammarWithExamples.fromResource(
    "j1-mark2-subset",
    "/j1-mark2-sub",
    listOf(
      "example1.j1",
      "example2.j1",
    )
  )

  val proto3 = GrammarWithExamples.fromResource(
    "proto3",
    "/proto3",
    listOf(
      "example1.proto",
      "GrammarProto.proto",
      "TermGroupProto.proto",
    )
  )

  val json = GrammarWithExamples.fromResource(
    "json",
    "/json",
    listOf(
      "example1.json",
      "example2.json",
    )
  )

  val pyobj = GrammarWithExamples.fromResource(
    "pyobj",
    "/pyobj",
    listOf()
  )

  val dartStringTest = GrammarWithExamples.fromResource(
    "dart-string-test",
    "/dart-string-test",
    listOf(
      "a.dst",
      "b.dst",
    ),
    grammarName = "dart-string-test.cdg"
  )

  val all = listOf(asdl, bibix2, j1mark1, j1mark2, proto3, json, pyobj)
}
