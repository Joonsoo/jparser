package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

/**
 * Same fixture sweep as [Mgroup3NativeResultBridgeTest], but driven through
 * the in-process FFM channel via [Mgroup3NativeParser] instead of a cargo
 * subprocess.
 *
 * Gated by `MGROUP3_NATIVE_INPROC=1` because it requires the cdylib to be
 * built and (typically) `--enable-native-access=ALL-UNNAMED` to be on the
 * JVM command line. The bbx4 action `runMgroup3NativeTest` sets both.
 */
@EnabledIfEnvironmentVariable(named = "MGROUP3_NATIVE_INPROC", matches = "1")
class Mgroup3NativeInProcessBridgeTest {

  @Test
  fun all_fixtures() {
    val nativeRoot = resolveNativeRoot()
    val committedRoot = File(nativeRoot, "tests/fixtures/parser")
    val generatedRoot = File(nativeRoot, "tests/fixtures/parser_generated")
    val cases = buildList {
      for (root in listOf(committedRoot, generatedRoot)) {
        if (root.isDirectory) {
          addAll(root.listFiles { f -> f.isDirectory }.orEmpty().toList())
        }
      }
    }.sortedBy { it.name }
    assertTrue(cases.isNotEmpty(), "no fixture cases under $committedRoot or $generatedRoot")

    val failures = mutableListOf<String>()
    var inputCount = 0
    for (caseDir in cases) {
      val dataPath = File(caseDir, "data.pb").toPath()
      // Use the native path to avoid lifting the 41MB mulang data.pb into JVM heap.
      Mgroup3NativeParser.fromParserDataFile(dataPath).use { native ->
        val data = Mgroup3ParserData.parseFrom(File(caseDir, "data.pb").readBytes())
        val parser = Mgroup3Parser(data)
        val inputsDir = File(caseDir, "inputs")
        val inputDirs = inputsDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: emptyList()
        for (inputDir in inputDirs) {
          inputCount += 1
          val input = File(inputDir, "input.txt").readText()
          val label = "${caseDir.name}/${inputDir.name}"

          val rustOutcome = native.parse(input)
          val (kotlinAccepted, kotlinHistory, kotlinError) = computeKotlinOutcome(parser, input)

          when {
            kotlinAccepted && rustOutcome is Mgroup3NativeOutcome.Accepted -> {
              val expected = kotlinHistory!!
              val actual = rustOutcome.history
              if (!historiesEqual(expected, actual)) {
                failures.add("$label: kernels_history mismatch")
              }
            }
            !kotlinAccepted && rustOutcome is Mgroup3NativeOutcome.Rejected -> {
              val k = kotlinError!!
              val r = rustOutcome.error
              if (!errorsShallowEqual(k, r)) {
                failures.add("$label: error mismatch — kotlin=$k rust=$r")
              }
            }
            else -> failures.add(
              "$label: accept/reject disagrees — kotlin=$kotlinAccepted rust=${rustOutcome.javaClass.simpleName}"
            )
          }
        }
      }
    }

    println("Mgroup3NativeInProcessBridgeTest: ${cases.size} cases / $inputCount inputs (native version=${Mgroup3NativeParser.nativeVersion()})")
    if (failures.isNotEmpty()) {
      val joined = failures.joinToString("\n  ", prefix = "  ")
      error("${failures.size} fixture(s) diverged:\n$joined")
    }
  }

  private data class KotlinOutcome(
    val accepted: Boolean,
    val history: List<KernelSet>?,
    val error: ParsingError?,
  )

  private fun computeKotlinOutcome(parser: Mgroup3Parser, input: String): KotlinOutcome {
    try {
      val ctx = parser.parse(input)
      return if (parser.isAccepted(ctx)) {
        KotlinOutcome(true, parser.kernelsHistory(ctx), null)
      } else {
        KotlinOutcome(
          false, null,
          ParsingError.UnexpectedEof(
            loc = ctx.gen,
            locLine = ctx.line,
            locCol = ctx.col,
            expected = parser.expectedInputsOf(ctx),
          )
        )
      }
    } catch (e: ParsingError) {
      return KotlinOutcome(false, null, e)
    }
  }

  private fun historiesEqual(a: List<KernelSet>, b: List<KernelSet>): Boolean {
    if (a.size != b.size) return false
    for (i in a.indices) {
      if (a[i].kernels != b[i].kernels) return false
    }
    return true
  }

  private fun errorsShallowEqual(a: ParsingError, b: ParsingError): Boolean = when {
    a is ParsingError.UnexpectedInput && b is ParsingError.UnexpectedInput ->
      a.loc == b.loc && a.locLine == b.locLine && a.locCol == b.locCol && a.actual == b.actual
    a is ParsingError.UnexpectedEof && b is ParsingError.UnexpectedEof ->
      a.loc == b.loc && a.locLine == b.locLine && a.locCol == b.locCol
    else -> false
  }

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
