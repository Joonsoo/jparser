package com.giyeok.jparser.ktglue.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.examples.metalang3.Catalog
import com.giyeok.jparser.ktglue.Milestone2ParserLoader.loadGzippedParserFromResource
import com.giyeok.jparser.ktglue.toKtKernelSet
import com.giyeok.jparser.ktglue.toKtList
import com.giyeok.jparser.ktlib.test.AsdlAst
import org.junit.jupiter.api.Test

object AsdlParser {
  val parser = loadGzippedParserFromResource("/asdl-m2-parserdata.pb.gz")

  fun parse(source: String): AsdlAst.ModuleDef {
    val inputs = Inputs.fromString(source)
    val parseResult = parser.parseOrThrow(inputs)
    val kernels = parser.kernelsHistory(parseResult).toKtList()
      .map { it.toKtKernelSet() }
    val astifier = AsdlAst(source, kernels)
    return astifier.matchStart()
  }

  @Test
  fun testPythonAst() {
    Catalog.asdl.examples.forEach { asdl ->
      val parsed = parse(asdl.example)

      println(parsed)
    }
  }
}
