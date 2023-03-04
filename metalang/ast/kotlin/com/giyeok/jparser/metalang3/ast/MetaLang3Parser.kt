package com.giyeok.jparser.metalang3.ast

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.Milestone2ParserLoader
import com.giyeok.jparser.ktlib.toKtList
import com.giyeok.jparser.ktlib.toKtSet

object MetaLang3Parser {
  val parser = Milestone2ParserLoader.loadParserFromResource("/cdglang3-parserdata.pb")

  fun parse(source: String): MetaLang3Ast.Grammar {
    val inputs = Inputs.fromString(source)
    val parseResult = parser.parseOrThrow(inputs)
    val history = parser.kernelsHistory(parseResult).toKtList()
      .map { KernelSet(it.toKtSet()) }
    return MetaLang3Ast(inputs.toKtList(), history).matchStart()
  }
}
