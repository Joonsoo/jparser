package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.bibix.targetIdData
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.Path

class GrammarGenTest {
  val j1Req = mapOf(
    "cdgFile" to FileValue(Path("../j1/grammar/grammar.cdg")),
    "astifierClassName" to StringValue("J1Ast"),
    "parserDataFileName" to StringValue("j1-parserdata.pb"),
    "trimParserData" to BooleanValue(true),
  )

  val bibix2Req = mapOf(
    "cdgFile" to FileValue(Path("examples/metalang3/resources/bibix2/grammar.cdg")),
    "astifierClassName" to StringValue("com.giyeok.jparser.ktlib.test.BibixAst"),
    "parserDataFileName" to StringValue("bibix2-mg2-parserdata-trimmed.pb"),
    "trimParserData" to BooleanValue(true),
  )

  val asdlReq = mapOf(
    "cdgFile" to FileValue(Path("examples/metalang3/resources/asdl/grammar.cdg")),
    "astifierClassName" to StringValue("com.giyeok.jparser.ktlib.test.AsdlAst"),
    "parserDataFileName" to StringValue("asdl-m2-parserdata.pb"),
    "trimParserData" to BooleanValue(true),
  )

  @Test
  fun test() {
    val result = GenKtAstMgroup2().build(BuildContext(
      buildEnv = BuildEnv(OS.MacOSX("", ""), Architecture.Aarch_64),
      fileSystem = FileSystems.getDefault(),
      mainBaseDirectory = Path("."),
      callerBaseDirectory = null,
      ruleDefinedDirectory = null,
      arguments = bibix2Req,
      targetIdData = targetIdData { },
      targetId = "",
      hashChanged = true,
      prevBuildTime = null,
      prevResult = null,
      destDirectoryPath = Path("bbxbuild/objects/abc"),
      progressLogger = object : ProgressLogger {
        override fun logError(message: String) {
        }

        override fun logInfo(message: String) {
          println(message)
        }
      },
      repo = object : BaseRepo {
        override fun prepareSharedDirectory(sharedRepoName: String): Path {
          TODO("Not yet implemented")
        }
      }
    ))
    println(result)
  }
}
