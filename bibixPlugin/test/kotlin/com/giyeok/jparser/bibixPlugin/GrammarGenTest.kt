package com.giyeok.jparser.bibixPlugin

import com.giyeok.bibix.base.BooleanValue
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import org.junit.jupiter.api.Test
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

  val dartReq = mapOf(
    "cdgFile" to FileValue(Path("/examples/metalang3/resources/dart/dart-wip.cdg")),
    "astifierClassName" to StringValue("com.giyeok.jparser.ktlib.test.DartAst"),
    "parserDataFileName" to StringValue("dart-m2-parserdata.pb"),
    "trimParserData" to BooleanValue(true),
  )

  @Test
  fun test() {
//

  }
}
