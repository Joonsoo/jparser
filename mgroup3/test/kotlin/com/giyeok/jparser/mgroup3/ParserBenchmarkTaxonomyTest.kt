package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup2.MilestoneGroupParser
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

// Benchmark taxonomy — 다양한 grammar 카테고리로 m2/kt/m3 비교.
// 한 grammar 만 측정하면 algorithm vs constant overhead 구분 어려워서 분류.
class ParserBenchmarkTaxonomyTest {

  data class Case(
    val name: String,
    val description: String,
    val grammarText: String,
    val makeInput: (Int) -> String,
    // 특정 case 별 size override. null 이면 default sizes 사용.
    val sizes: List<Int>? = null,
    // mgroup2 가 path explosion 이슈 있는 케이스는 skip.
    val skipM2: Boolean = false,
  )

  private val cases = listOf(
    // 1. CFG-only — accept condition 전혀 없음. mgroup3 의 cond bookkeeping 이 모두 idle.
    Case(
      name = "cfg_only",
      description = "Plain left-recursive arithmetic. No accept condition.",
      grammarText = """
        Grammar = Expr
        Expr = N | Expr WS '+' WS N | Expr WS '-' WS N
        N = '0-9'+
        WS = ' '*
      """.trimIndent(),
      makeInput = { n ->
        val sb = StringBuilder("1")
        for (i in 1 until n) {
          sb.append(if (i % 2 == 0) " + " else " - ")
          sb.append(((i % 9) + 1))
        }
        sb.toString()
      },
    ),

    // 2. Longest-only — single longest, no join/lookahead.
    Case(
      name = "longest_only",
      description = "Longest-match tokens separated by space. NLongest but no NJoin/lookahead.",
      grammarText = """
        Grammar = Tk (WS Tk)*
        Tk = <Word>
        Word = 'a-zA-Z' '0-9a-zA-Z_'*
        WS = ' '+
      """.trimIndent(),
      makeInput = { n ->
        val tokens = listOf("foo", "bar", "baz", "qux", "abc123", "xyz")
        (0 until n).joinToString(" ") { tokens[it % tokens.size] }
      },
    ),

    // 3. Lookahead-only — NLookaheadIs / NLookaheadExcept, no longest/join.
    Case(
      name = "lookahead_only",
      description = "Negative lookahead for keyword boundary. No NLongest.",
      grammarText = """
        Grammar = Item (WS Item)*
        Item = "if" &(WS) | Word
        Word = 'a-zA-Z'+
        WS = ' '+
      """.trimIndent(),
      makeInput = { n ->
        val toks = listOf("if", "then", "else", "abc", "xy")
        (0 until n).joinToString(" ") { toks[it % toks.size] }
      },
    ),

    // 4. Mixed (longest + join) — 단일 NJoin keyword in a list. cond root 자주 active.
    // mgroup2 가 path explosion 으로 hang 하는 케이스. mgroup3 / ktparser 만 측정.
    Case(
      name = "longest_join",
      description = "Mixed list of single-keyword NJoin and longest tokens (m2 skipped — path explosion).",
      grammarText = """
        Grammar = Item (WS Item)*
        Item = Kw | Word
        Kw = "mut"&Tk
        Tk = <Word>
        Word = 'a-zA-Z' '0-9a-zA-Z_'*
        WS = ' '+
      """.trimIndent(),
      makeInput = { n ->
        val toks = listOf("mut", "abc", "xyz", "id1", "foo", "bar")
        (0 until n).joinToString(" ") { toks[it % toks.size] }
      },
      skipM2 = true,
    ),

    // 5. Cond-heavy — left recursion + NJoin + NLongest (try_let.mu style 의 stress).
    Case(
      name = "cond_heavy",
      description = "Left recursion + NJoin with NLongest body. Original benchmark grammar.",
      grammarText = """
        Grammar = Expr
        Expr = N | Expr WS Op&OpTk WS N
        N = '0-9'+
        OpTk = <Op>
        Op = ('+' | '-' | "||")+
        WS = ' '*
      """.trimIndent(),
      makeInput = { n ->
        val ops = listOf("||", "+", "-", "||", "+", "||")
        val sb = StringBuilder("1")
        for (i in 1 until n) {
          sb.append(' '); sb.append(ops[i % ops.size]); sb.append(' ')
          sb.append(((i % 9) + 1))
        }
        sb.toString()
      },
      skipM2 = true,
    ),
  )

  @Test
  fun benchmarkTaxonomy() {
    val defaultSizes = listOf(100, 1000)
    val warmup = 10
    val measureN = 30

    // bibix4 의 stdout streaming 이 build job log 와 섞이며 앞 case 의 출력을 누락. 결과를
    // 별도 파일에도 같이 기록. 경로: /tmp/jparser-benchmark-taxonomy.txt — 시작 시점에 truncate.
    val resultFile = java.io.File("/tmp/jparser-benchmark-taxonomy.txt")
    resultFile.writeText("")
    fun out(line: String) {
      println(line)
      System.out.flush()
      resultFile.appendText(line + "\n")
    }

    for (case in cases) {
      val grammar: NGrammar = `MetaLanguage3$`.`MODULE$`
        .analyzeGrammar(case.grammarText, "Grammar").ngrammar()

      val m2Data = MilestoneGroupParserGen(grammar).parserData()
      val m2Parser = MilestoneGroupParser(m2Data)
      val ktParser = MilestoneGroupParserKt(MilestoneGroupParserDataProtobufConverter.toProto(m2Data))
      val m3Parser = Mgroup3Parser(Mgroup3ParserGenerator(grammar).generate())

      out("")
      out("=== ${case.name} : ${case.description} ===")
      out("%-6s | %-8s | %-12s %-12s %-12s | ratio m3/kt".format("size", "chars", "m2 median", "kt median", "m3 median"))
      out("-".repeat(80))

      val sizes = case.sizes ?: defaultSizes
      for (size in sizes) {
        val input = case.makeInput(size)

        // 검증 + 안전한 timing — m2 가 OOM 같은 케이스 있으므로 try-catch / 명시적 skip.
        val m2OK = if (case.skipM2) false else try {
          m2Parser.parse(Inputs.fromString(input)).isRight
        } catch (e: Throwable) { false }
        val ktCtx = try { ktParser.parse(input) } catch (e: Throwable) { null }
        val ktOK = ktCtx?.let { isKtAccepted(ktParser, it, grammar.startSymbol()) } ?: false
        val m3OK = try {
          m3Parser.isAccepted(m3Parser.parse(input))
        } catch (e: Throwable) { false }

        if (!m3OK) {
          out("%-6d | %-8d | mgroup3 did not accept — skip".format(size, input.length))
          continue
        }

        // warm up (m2 만 OOM 일 수 있어 m2 는 try-catch)
        repeat(warmup) {
          if (m2OK) try { m2Parser.parse(Inputs.fromString(input)) } catch (e: Throwable) {}
          if (ktOK) ktParser.parse(input)
          m3Parser.parse(input)
        }

        val m2Times = LongArray(measureN)
        val ktTimes = LongArray(measureN)
        val m3Times = LongArray(measureN)
        for (i in 0 until measureN) {
          System.gc(); Thread.sleep(2)
          m2Times[i] = if (m2OK) measureNanoTime {
            try { m2Parser.parse(Inputs.fromString(input)) } catch (e: Throwable) {}
          } else -1L
          System.gc(); Thread.sleep(2)
          ktTimes[i] = if (ktOK) measureNanoTime { ktParser.parse(input) } else -1L
          System.gc(); Thread.sleep(2)
          m3Times[i] = measureNanoTime { m3Parser.parse(input) }
        }

        val m2Med = if (m2OK) median(m2Times) else -1.0
        val ktMed = if (ktOK) median(ktTimes) else -1.0
        val m3Med = median(m3Times)
        val ratio = if (ktMed > 0) m3Med / ktMed else -1.0
        out("%-6d | %-8d | %-12s %-12s %-12s | %s".format(
          size, input.length,
          if (m2OK) formatMs(m2Med) else "OOM/err",
          if (ktOK) formatMs(ktMed) else "OOM/err",
          formatMs(m3Med),
          if (ratio > 0) "%.2fx".format(ratio) else "n/a"))
      }
    }
  }

  private fun isKtAccepted(
    parser: MilestoneGroupParserKt,
    ctx: com.giyeok.jparser.ktparser.mgroup2.ParsingContextKt,
    startSymbolId: Int,
  ): Boolean {
    val kernels = parser.kernelsHistory(ctx)
    val last = kernels.lastOrNull() ?: return false
    return last.kernels.any { it.symbolId == startSymbolId && it.pointer == 1 && it.beginGen == 0 }
  }

  private fun median(arr: LongArray): Double {
    val sorted = arr.sortedArray()
    val n = sorted.size
    return if (n % 2 == 0) (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 else sorted[n / 2].toDouble()
  }

  private fun formatMs(nanos: Double): String = "%.3fms".format(nanos / 1_000_000)
}
