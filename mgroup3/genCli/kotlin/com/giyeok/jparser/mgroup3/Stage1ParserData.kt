package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

object Stage1ParserData {
  fun run(processed: ProcessedGrammar, out: Path) {
    val data = Mgroup3ParserGenerator(processed.ngrammar()).generate()
    out.parent?.createDirectories()
    out.outputStream().buffered().use { data.writeTo(it) }
  }
}
