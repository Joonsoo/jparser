package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.*
import com.giyeok.bibix.objectId
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.Path

class J1GrammarGen {
  @Test
  fun test() {
    val result = GenKtAstMilestone2().build(BuildContext(
      BuildEnv(OS.MacOSX("", ""), Architecture.Aarch_64),
      FileSystems.getDefault(),
      Path("."),
      null,
      null,
      mapOf(
        "cdgFile" to FileValue(Path("../j1/grammar/grammar.cdg")),
        "astifierClassName" to StringValue("J1Ast"),
        "parserDataFileName" to StringValue("j1-parserdata.pb"),
        "trimParserData" to BooleanValue(true),
      ),
//      mapOf(
//        "cdgFile" to FileValue(Path("examples/main/resources/cdglang3.cdg")),
//        "astifierClassName" to StringValue("MetaLang3Ast"),
//        "parserDataFileName" to StringValue("cdglang3-parserdata.pb"),
//        "trimParserData" to BooleanValue(false),
//      ),
      true,
      objectId {},
      "asdf",
      Path("bbxbuild/objects/abc"),
      object : ProgressLogger {
        override fun logError(message: String) {
        }

        override fun logInfo(message: String) {
          println(message)
        }
      },
      object : BaseRepo {
        override fun prepareSharedDirectory(sharedRepoName: String): Path {
          TODO("Not yet implemented")
        }
      }
    ))
    println(result)
  }
}
