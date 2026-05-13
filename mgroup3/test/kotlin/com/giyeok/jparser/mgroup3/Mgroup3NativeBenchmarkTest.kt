package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

/**
 * Wall-clock comparison: mgroup3 Kotlin reference vs mgroup3-native via the
 * FFM in-process channel. Uses the mulang corpus checked in under
 * `mgroup3-native/tests/fixtures/parser_generated/mulang/`.
 *
 * Headline metric: ratio of medians on the larger inputs (`rbln_examples`,
 * `cpp_ast`). Tiny inputs (`annotation_defs`) are included as a check on
 * channel overhead — channel + protobuf round-trip can dominate there.
 *
 * Gated by `MGROUP3_NATIVE_BENCH=1`. Run via `bibix4 runMgroup3NativeBenchmark`.
 */
@EnabledIfEnvironmentVariable(named = "MGROUP3_NATIVE_BENCH", matches = "1")
class Mgroup3NativeBenchmarkTest {

  private data class BenchInput(val name: String, val chars: Int, val text: String)

  /** (warmupRounds, measureRounds) — bigger inputs use fewer rounds. */
  private fun roundsFor(chars: Int): Pair<Int, Int> =
    if (chars >= 2000) 5 to 20 else 10 to 30

  @Test
  fun benchmarkMulangCorpus() {
    val nativeRoot = resolveNativeRoot()
    val mulangDir = File(nativeRoot, "tests/fixtures/parser_generated/mulang")
    require(mulangDir.isDirectory) {
      "mulang fixture not found at $mulangDir — run `bibix4 runMgroup3FixtureGen` first."
    }
    val dataFile = File(mulangDir, "data.pb")
    require(dataFile.exists()) { "missing $dataFile" }
    val inputs = listOf(
      "01_annotation_defs",
      "09_exprs",
      "07_try_let",
      "06_match",
    ).mapNotNull { dirName ->
      val d = File(mulangDir, "inputs/$dirName")
      if (!d.isDirectory) {
        println("[skip] no fixture $dirName")
        null
      } else {
        val text = File(d, "input.txt").readText()
        BenchInput(dirName, text.length, text)
      }
    }

    println("=== Mgroup3 Kotlin vs mgroup3-native FFM ===")
    println("native version: ${Mgroup3NativeParser.nativeVersion()}")
    println(
      "%-22s | %-7s | %-12s %-12s %-12s | %-12s %-12s %-12s | %-8s".format(
        "input", "chars",
        "kt median", "kt mean", "kt stdev",
        "rs median", "rs mean", "rs stdev",
        "kt/rs",
      )
    )

    // Build Kotlin parser once (re-uses the same Mgroup3ParserData proto).
    val data = Mgroup3ParserData.parseFrom(dataFile.readBytes())
    val ktParser = Mgroup3Parser(data)

    // Build native parser once via the JVM-heap-bypassing file path so we
    // measure exactly the channel the user-facing API recommends.
    Mgroup3NativeParser.fromParserDataFile(dataFile.toPath()).use { native ->
      for (input in inputs) {
        // Sanity: both accept.
        val ktCtx = ktParser.parse(input.text)
        check(ktParser.isAccepted(ktCtx)) { "Kotlin rejects ${input.name}" }
        val rsOutcome = native.parse(input.text)
        check(rsOutcome is Mgroup3NativeOutcome.Accepted) {
          "Rust rejects ${input.name}: $rsOutcome"
        }

        val (warmup, n) = roundsFor(input.chars)
        // Fair comparison: both sides materialize kernels_history. The
        // native path always does (via encode_parse_result inside the FFI),
        // so the Kotlin path must too — otherwise we'd be timing Kotlin's
        // parse-only against Rust's parse+history+encode.
        repeat(warmup) {
          val c = ktParser.parse(input.text); ktParser.kernelsHistory(c)
          native.parse(input.text)
        }
        val ktTimes = LongArray(n)
        val rsTimes = LongArray(n)
        for (i in 0 until n) {
          System.gc(); Thread.sleep(5)
          ktTimes[i] = measureNanoTime {
            val c = ktParser.parse(input.text); ktParser.kernelsHistory(c)
          }
          System.gc(); Thread.sleep(5)
          rsTimes[i] = measureNanoTime { native.parse(input.text) }
        }
        val ktMed = median(ktTimes)
        val rsMed = median(rsTimes)
        val ratio = if (rsMed > 0) ktMed / rsMed else 0.0
        println(
          "%-22s | %-7d | %-12s %-12s %-12s | %-12s %-12s %-12s | %-8s".format(
            input.name, input.chars,
            formatMs(ktMed), formatMs(mean(ktTimes)), formatMs(stdev(ktTimes)),
            formatMs(rsMed), formatMs(mean(rsTimes)), formatMs(stdev(rsTimes)),
            "%.2fx".format(ratio),
          )
        )
      }
    }
    println("=== done ===")
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
    return sqrt(variance)
  }

  private fun formatMs(nanos: Double): String = "%.3fms".format(nanos / 1_000_000)

  private fun resolveNativeRoot(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
      val candidate = File(dir, "mgroup3-native")
      if (candidate.isDirectory) return candidate
      dir = dir.parentFile
    }
    error("could not locate mgroup3-native/ from cwd=${System.getProperty("user.dir")}")
  }
}
