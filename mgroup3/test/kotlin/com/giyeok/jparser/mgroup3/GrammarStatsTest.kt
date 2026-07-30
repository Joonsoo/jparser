package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import scala.jdk.javaapi.CollectionConverters
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

// 논문 §9 의 case-study 문법 통계: 소스 라인 수, nonterminal 수,
// 정규화 문법의 조건부 연산자 심볼 수 (연산자별).
// 주의: 연산자 수는 소스상의 표기 횟수가 아니라 정규화 문법의 서로 다른
// 조건부 심볼 수 (동일 부분식은 dedup — 통계 캡션에 그렇게 명시할 것).
// 실행: bibix4 runGrammarStats
@EnabledIfEnvironmentVariable(named = "GRAMMAR_STATS", matches = "1")
class GrammarStatsTest {
  data class Stats(
    val name: String, val lines: Int, val nonterminals: Int,
    val join: Int, val except: Int, val longest: Int, val la: Int, val nla: Int,
    val totalSymbols: Int, val sequences: Int,
  )

  private fun statsOf(name: String, cdgPath: Path, start: String): Stats? {
    if (!cdgPath.exists()) {
      println("$name: $cdgPath not found, skipping")
      return null
    }
    val text = cdgPath.readText()
    val grammar: NGrammar = try {
      `MetaLanguage3$`.`MODULE$`.analyzeGrammar(text, start).ngrammar()
    } catch (e: Exception) {
      println("$name: analyzeGrammar failed: $e")
      return null
    }
    val symbols = CollectionConverters.asJava(grammar.nsymbols()).values
    var join = 0; var except = 0; var longest = 0; var la = 0; var nla = 0; var nonterm = 0
    for (s in symbols) {
      when (s) {
        is NGrammar.NJoin -> join++
        is NGrammar.NExcept -> except++
        is NGrammar.NLongest -> longest++
        is NGrammar.NLookaheadIs -> la++
        is NGrammar.NLookaheadExcept -> nla++
        is NGrammar.NNonterminal -> nonterm++
        else -> {}
      }
    }
    return Stats(
      name, text.lines().size, nonterm,
      join, except, longest, la, nla,
      symbols.size, grammar.nsequences().size(),
    )
  }

  @Test
  fun grammarStats() {
    val rows = listOfNotNull(
      statsOf("JSON", Path.of("examples/metalang3/resources/json/grammar.cdg"), "json"),
      statsOf("Proto3", Path.of("examples/metalang3/resources/proto3/grammar.cdg"), "proto3"),
      statsOf("ES5.1", Path.of("examples/metalang3/resources/es5/grammar.cdg"), "Program"),
      // ES5.1 + 7.9 자동 세미콜론 삽입 — §9.3 ASI 측정 대상 문법.
      statsOf("ES5.1-ASI", Path.of("examples/metalang3/resources/es5-asi/grammar.cdg"), "Program"),
      statsOf("ECMA262(excerpt)", Path.of("examples/metalang3/resources/ecma262-13.cdg"), "Expression"),
      // MULANG_PINNED_CDG override — GroupInventoryStatsTest / AblationBenchmarkTest
      // 와 같은 규약. tab:grammar-stats 의 Mulang 행은 pinned 문법 기준이다.
      statsOf(
        "Mulang",
        Path.of(System.getenv("MULANG_PINNED_CDG") ?: "../mulang/grammar/mulang.cdg"),
        "CompileUnit",
      ),
    )
    println("%-18s %6s %8s | %5s %7s %8s %4s %4s | %8s %5s".format(
      "grammar", "lines", "nonterm", "join", "except", "longest", "la", "nla", "symbols", "seqs"))
    for (r in rows) {
      println("%-18s %6d %8d | %5d %7d %8d %4d %4d | %8d %5d".format(
        r.name, r.lines, r.nonterminals, r.join, r.except, r.longest, r.la, r.nla, r.totalSymbols, r.sequences))
    }
    println()
    println("CSV grammar,lines,nonterminals,join,except,longest,la,nla,symbols,sequences")
    for (r in rows) {
      println("CSV ${r.name},${r.lines},${r.nonterminals},${r.join},${r.except},${r.longest},${r.la},${r.nla},${r.totalSymbols},${r.sequences}")
    }
  }
}
