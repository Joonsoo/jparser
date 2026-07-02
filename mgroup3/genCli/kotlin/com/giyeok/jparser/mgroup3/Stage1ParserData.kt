package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.outputStream

object Stage1ParserData {
  // trimGrammar: grammar(NGrammar) 필드 제거 — 파서 런타임(Kotlin/Rust)은 읽지 않는
  // 디버그 정보라 배포 크기를 줄인다. out 이 .gz 로 끝나면 gzip 으로 기록.
  fun run(processed: ProcessedGrammar, out: Path, trimGrammar: Boolean = false) {
    val data0 = Mgroup3ParserGenerator(processed.ngrammar()).generate()
    val data = if (trimGrammar) data0.toBuilder().clearGrammar().build() else data0
    out.parent?.createDirectories()
    val stream =
      if (out.name.endsWith(".gz")) GZIPOutputStream(out.outputStream().buffered())
      else out.outputStream().buffered()
    stream.use { data.writeTo(it) }
  }
}
