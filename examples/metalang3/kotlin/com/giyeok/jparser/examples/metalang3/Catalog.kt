package com.giyeok.jparser.examples.metalang3

object Catalog {
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
    )
  )

  val j1 = GrammarWithExamples.fromResource(
    "/j1",
    listOf(
      "example1.j1",
      "example2.j1",
    )
  )
}
