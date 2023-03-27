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
      "jparser.bbx"
    )
  )
}
