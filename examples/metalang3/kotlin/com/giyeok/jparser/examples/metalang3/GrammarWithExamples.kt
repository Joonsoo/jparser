package com.giyeok.jparser.examples.metalang3

interface GrammarWithExamples {
  val grammarText: String

  val examples: List<GrammarTestExample>

  companion object {
    fun fromResource(
      directory: String,
      exampleNames: List<String>
    ): GrammarWithExamplesFromResource {
      return GrammarWithExamplesFromResource(
        "$directory/grammar.cdg",
        exampleNames.map { name ->
          val examplePath = "$directory/examples/$name"
          val valuefyResultPath = "$examplePath.ast"
          GrammarTestExampleFromResource(
            examplePath,
            if (this::class.java.getResource(valuefyResultPath) != null) valuefyResultPath else null
          )
        })
    }
  }
}

data class GrammarTestExample(
  val example: String,
  val valuefyResultString: String?,
)

class GrammarWithExamplesFromResource(
  val grammarPath: String,
  val examplePaths: List<GrammarTestExampleFromResource>,
) : GrammarWithExamples {
  fun readResource(path: String) =
    this::class.java.getResourceAsStream(path)!!.reader().use {
      it.readText()
    }

  override val grammarText: String by lazy {
    readResource(grammarPath)
  }

  override val examples: List<GrammarTestExample> by lazy {
    examplePaths.map { example ->
      GrammarTestExample(
        readResource(example.examplePath),
        example.valuefyResultPath?.let { path ->
          if (this::class.java.getResource(path) != null) {
            readResource(path)
          } else {
            null
          }
        })
    }
  }
}

data class GrammarTestExampleFromResource(
  val examplePath: String,
  val valuefyResultPath: String?,
)

data class GrammarWithExamplesStatic(
  val grammarText: String,
  val examples: List<String>
)
