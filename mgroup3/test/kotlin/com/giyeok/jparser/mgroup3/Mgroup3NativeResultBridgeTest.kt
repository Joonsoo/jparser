package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParserData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cross-language differential test for Phase A. For every fixture under
 * `mgroup3-native/tests/fixtures/parser/<case>/`:
 *
 *   1. Shell out to `cargo run --release --quiet --bin dump_result -- <data.pb> <input.txt>`
 *      (or to a pre-built binary if MGROUP3_NATIVE_DUMP_BIN is set).
 *   2. Decode the captured stdout bytes via the Kotlin bridge.
 *   3. Run the same parse in pure Kotlin via [Mgroup3Parser].
 *   4. Assert:
 *      - Both sides agree on accept/reject.
 *      - On accept: deep-equal kernels_history (KernelSet.kernels per gen).
 *      - On reject: error variant + loc/line/col match. TermSet equality is
 *        skipped because Kotlin's TermSet has no structural equals.
 *
 * Gated by `MGROUP3_NATIVE_BRIDGE=1` so CI without cargo doesn't fail.
 */
@EnabledIfEnvironmentVariable(named = "MGROUP3_NATIVE_BRIDGE", matches = "1")
class Mgroup3NativeResultBridgeTest {

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
      val dataBytes = File(caseDir, "data.pb").readBytes()
      val data = Mgroup3ParserData.parseFrom(dataBytes)
      val parser = Mgroup3Parser(data)
      val inputsDir = File(caseDir, "inputs")
      val inputDirs = inputsDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: emptyList()
      for (inputDir in inputDirs) {
        inputCount += 1
        val input = File(inputDir, "input.txt").readText()
        val rustBytes = dumpResult(File(caseDir, "data.pb"), File(inputDir, "input.txt"))
        val rustOutcome = decodeMgroup3ParseResult(rustBytes)

        var kotlinAccepted = false
        var kotlinHistory: List<KernelSet>? = null
        var kotlinError: ParsingError? = null
        try {
          val ctx = parser.parse(input)
          if (parser.isAccepted(ctx)) {
            kotlinAccepted = true
            kotlinHistory = parser.kernelsHistory(ctx)
          } else {
            // parsed but not accepted — Rust folds this into UnexpectedEof.
            kotlinError = ParsingError.UnexpectedEof(
              loc = ctx.gen,
              locLine = ctx.line,
              locCol = ctx.col,
              expected = parser.expectedInputsOf(ctx),
            )
          }
        } catch (e: ParsingError) {
          kotlinError = e
        }

        val label = "${caseDir.name}/${inputDir.name}"
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
          else -> failures.add("$label: accept/reject disagrees — kotlin=$kotlinAccepted rust=${rustOutcome.javaClass.simpleName}")
        }
      }
    }

    println("Mgroup3NativeResultBridgeTest: ${cases.size} cases / $inputCount inputs")
    if (failures.isNotEmpty()) {
      val joined = failures.joinToString("\n  ", prefix = "  ")
      error("${failures.size} fixture(s) diverged:\n$joined")
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

  private fun dumpResult(dataPath: File, inputPath: File): ByteArray {
    val prebuilt = System.getenv("MGROUP3_NATIVE_DUMP_BIN")
    val cmd = if (prebuilt != null) {
      listOf(prebuilt, dataPath.absolutePath, inputPath.absolutePath)
    } else {
      listOf(
        "cargo", "run", "--release", "--quiet", "--bin", "dump_result",
        "--", dataPath.absolutePath, inputPath.absolutePath,
      )
    }
    val workDir = if (prebuilt != null) null else resolveNativeRoot()
    val proc = ProcessBuilder(cmd)
      .also { if (workDir != null) it.directory(workDir) }
      .redirectErrorStream(false)
      .start()
    val stdout = ByteArrayOutputStream()
    proc.inputStream.copyTo(stdout)
    val stderr = proc.errorStream.bufferedReader().readText()
    if (!proc.waitFor(120, TimeUnit.SECONDS)) {
      proc.destroyForcibly()
      error("dump_result timed out: cmd=$cmd")
    }
    if (proc.exitValue() != 0) {
      error("dump_result failed: cmd=$cmd exit=${proc.exitValue()} stderr=$stderr")
    }
    return stdout.toByteArray()
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
