package com.giyeok.jparser.examples.metalang3

interface GrammarWithExamples {
  val name: String

  val grammarText: String

  val examples: List<GrammarTestExample>

  companion object {
    fun fromResource(
      name: String,
      directory: String,
      exampleNames: List<String>,
      grammarName: String = "grammar.cdg",
    ): GrammarWithExamplesFromResource {
      return GrammarWithExamplesFromResource(
        name,
        "$directory/$grammarName",
        exampleNames.map { exampleName ->
          val examplePath = "$directory/examples/$exampleName"
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
  val name: String,
  val example: String,
  val valuefyResultString: String?,
)

class GrammarWithExamplesFromResource(
  override val name: String,
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
        example.examplePath,
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
