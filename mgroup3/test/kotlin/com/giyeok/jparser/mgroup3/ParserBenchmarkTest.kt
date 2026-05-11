package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup2.MilestoneGroupParserGen
import com.giyeok.jparser.mgroup2.MilestoneGroupParser
import com.giyeok.jparser.mgroup2.MilestoneGroupParserDataProtobufConverter
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scala.jdk.javaapi.CollectionConverters
import kotlin.system.measureNanoTime

class ParserBenchmarkTest {
  // 세 파서 (mgroup2 scala, ktparser, mgroup3) 의 parse time 비교.
  //
  // - 같은 grammar 사용. accept-condition (NJoin + NLongest) 등장하게 keyword/operator tokenization 패턴.
  // - 세 파서 모두 같은 input 으로 parse 후 accept 여부 검증.
  // - JIT warm-up 후 N 회 측정, median + mean + std 출력.
  //
  // @Disabled 로 둬서 일반 CI 에는 안 돌고, 수동 실행 시에만.
  // runMgroup3Test 에서는 @Disabled 로 skip. runParserBenchmark action 은 --include-classname
  // 으로 명시적으로 이 클래스를 선택해도 @Disabled 면 실행 안 되므로, 일단 수동으로 @Disabled
  // 해제 후 runParserBenchmark 실행. (간단한 절차로 우선)
  @Test
  @Disabled("manual benchmark. Unannotate @Disabled and run runParserBenchmark action.")
  fun benchmarkThreeParsers() {
    // Grammar: arithmetic with NJoin ("||" must be on Op-token boundary) + NLongest (OpTk = longest Op).
    val grammarText = """
      Grammar = Expr
      Expr = N | Expr WS Op&OpTk WS N
      N = '0-9'+
      OpTk = <Op>
      Op = ('+' | '-' | "||")+
      WS = ' '*
    """.trimIndent()
    val grammar: NGrammar = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(grammarText, "Grammar").ngrammar()

    // 세 input 길이: 100, 1000, 10000 chars 정도.
    // 입력 예시: "1 || 2 || 3 + 4 - 5 || 6 ..." — N 으로 분리된 operator chain.
    fun makeInput(numTerms: Int): String {
      val ops = listOf("||", "+", "-", "||", "+", "||")
      val sb = StringBuilder()
      sb.append("1")
      for (i in 1 until numTerms) {
        sb.append(' ')
        sb.append(ops[i % ops.size])
        sb.append(' ')
        sb.append(((i % 9) + 1).toString())
      }
      return sb.toString()
    }

    // mgroup2 (scala) generator + parser
    val m2Gen = MilestoneGroupParserGen(grammar)
    val m2Data = m2Gen.parserData()
    val m2Parser = MilestoneGroupParser(m2Data)

    // ktparser: mgroup2 의 parser data 를 proto 로 변환 후 kotlin parser 에 load
    val m2DataProto = MilestoneGroupParserDataProtobufConverter.toProto(m2Data)
    val ktParser = MilestoneGroupParserKt(m2DataProto)

    // mgroup3 generator + parser
    val m3Gen = Mgroup3ParserGenerator(grammar)
    val m3Data = m3Gen.generate()
    val m3Parser = Mgroup3Parser(m3Data)

    val inputSizes = listOf(10, 100, 1000)

    println("=== Parser benchmark ===")
    println("Grammar: arithmetic with NJoin & NLongest")
    println()
    println("%-8s | %-10s | %-12s %-12s %-12s | %-12s %-12s %-12s | %-12s %-12s %-12s".format(
      "size", "chars", "m2 median", "m2 mean", "m2 std", "kt median", "kt mean", "kt std",
      "m3 median", "m3 mean", "m3 std"))
    println("-".repeat(160))

    for (size in inputSizes) {
      val input = makeInput(size)
      val inputChars = input.length

      // 검증: 세 파서가 모두 accept 하는지.
      val m2Accepted = run {
        val ctx = m2Parser.parse(Inputs.fromString(input))
        ctx.isRight
      }
      val ktCtx = ktParser.parse(input)
      val ktAccepted = isKtAccepted(ktParser, ktCtx, grammar.startSymbol())
      val m3Accepted = m3Parser.isAccepted(m3Parser.parse(input))

      check(m2Accepted) { "mgroup2 did not accept size=$size input" }
      check(ktAccepted) { "ktparser did not accept size=$size input" }
      check(m3Accepted) { "mgroup3 did not accept size=$size input" }

      // Warm-up
      val warmup = 10
      repeat(warmup) {
        m2Parser.parse(Inputs.fromString(input))
        ktParser.parse(input)
        m3Parser.parse(input)
      }

      // Measurement
      val n = 30
      val m2Times = LongArray(n)
      val ktTimes = LongArray(n)
      val m3Times = LongArray(n)
      for (i in 0 until n) {
        // 각 측정 사이에 GC 약간 안정화. (간단한 hint, force 아님)
        System.gc()
        Thread.sleep(5)
        m2Times[i] = measureNanoTime { m2Parser.parse(Inputs.fromString(input)) }
        System.gc()
        Thread.sleep(5)
        ktTimes[i] = measureNanoTime { ktParser.parse(input) }
        System.gc()
        Thread.sleep(5)
        m3Times[i] = measureNanoTime { m3Parser.parse(input) }
      }

      println("%-8d | %-10d | %-12s %-12s %-12s | %-12s %-12s %-12s | %-12s %-12s %-12s".format(
        size, inputChars,
        formatMs(median(m2Times)), formatMs(mean(m2Times)), formatMs(stdev(m2Times)),
        formatMs(median(ktTimes)), formatMs(mean(ktTimes)), formatMs(stdev(ktTimes)),
        formatMs(median(m3Times)), formatMs(mean(m3Times)), formatMs(stdev(m3Times))))
    }
  }

  // ktparser 의 isAccepted helper (없으므로 kernelsHistory 의 마지막 entry 에 start symbol finish 가 있는지로 검증).
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

  private fun mean(arr: LongArray): Double = arr.sum().toDouble() / arr.size

  private fun stdev(arr: LongArray): Double {
    val m = mean(arr)
    val variance = arr.sumOf { (it - m) * (it - m) } / arr.size
    return kotlin.math.sqrt(variance)
  }

  private fun formatMs(nanos: Double): String = "%.3fms".format(nanos / 1_000_000)
}
