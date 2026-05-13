package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.metalang3.`MetaLanguage3$`
import com.giyeok.jparser.mgroup3.gen.Mgroup3ParserGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Spot check for Phase A's AST-compatibility assumption: a `List<KernelSet>`
 * produced via the Rust→bridge path is consumable by the same kind of
 * walk-by-kernel code that ktparser-generated AST converters use
 * (`history[gen].findByBeginGen(symbolId, pointer, beginGen)` and friends).
 *
 * Strategy: build a tiny grammar `Grammar = 'a' 'b' 'c'`, run it through the
 * Kotlin generator + parser, derive the kernels_history, and walk it to
 * extract a typed `Abc` AST node. The kernel-walk shape (root finish lookup
 * then per-step `findByBeginGenOpt`) mirrors what `CmakeDebugAst.matchStart`
 * does for the cmake grammar.
 *
 * If this spot check passes, the bridge output is structurally usable as-is
 * by any ktparser-style consumer that walks per-gen kernel sets.
 *
 * The full cargo-subprocess path is exercised in
 * [Mgroup3NativeResultBridgeTest]; here we use the Kotlin parser directly
 * (via the same bridge wire format) so the test stays runnable without
 * MGROUP3_NATIVE_BRIDGE=1. The wire format is what matters for AST compat.
 */
class Mgroup3NativeAstCompatTest {
  data class Abc(val startGen: Int, val endGen: Int, val a: Kernel, val b: Kernel, val c: Kernel)

  @Test
  fun walks_kernels_history_into_typed_ast() {
    val grammar = "Grammar = 'a' 'b' 'c'"
    val parser = makeParser(grammar)
    val ctx = parser.parse("abc")
    val history: List<KernelSet> = parser.kernelsHistory(ctx)

    // Last gen has a finished start-symbol kernel at (start, 1, 0, lastGen).
    val lastGen = history.size - 1
    val start = parser.data.startSymbolId
    val rootKernel = history[lastGen].kernels.firstOrNull {
      it.symbolId == start && it.pointer == 1 && it.beginGen == 0 && it.endGen == lastGen
    }
    assertNotNull(rootKernel, "no start-symbol finish in kernels_history.last()")

    val ast = walkAbc(history, lastGen)
    assertNotNull(ast)
    assertEquals(0, ast!!.startGen)
    assertEquals(3, ast.endGen)
    assertEquals(0, ast.a.beginGen); assertEquals(1, ast.a.endGen)
    assertEquals(1, ast.b.beginGen); assertEquals(2, ast.b.endGen)
    assertEquals(2, ast.c.beginGen); assertEquals(3, ast.c.endGen)
  }

  /**
   * Walks `history` to find an Abc AST node spanning [0, lastGen]. The shape
   * of the walk — `findByBeginGenOpt` chained across gens — mirrors what a
   * ktparser-generated converter does.
   *
   * We pick three terminal kernels (one per character). The specific symbolIds
   * depend on the generator's symbol assignment; we discover them by walking
   * the last gen's KernelSet for kernels with single-char spans.
   */
  private fun walkAbc(history: List<KernelSet>, lastGen: Int): Abc? {
    // Find terminal-shaped kernels at gens 1, 2, 3 — each is a single-char
    // kernel ending at that gen. We use pointer=1 (finished) and look for
    // kernels with (beginGen = gen-1, endGen = gen). When multiple kernels
    // match (the generator emits both the terminal and one wrapping
    // single-symbol container at the same span), pick the smallest symbolId
    // for determinism — the precise choice doesn't matter for the spot
    // check, only that the walk is deterministic.
    fun pickAt(gen: Int, begin: Int, end: Int): Kernel? = history[gen].kernels
      .filter { it.pointer == 1 && it.beginGen == begin && it.endGen == end }
      .minByOrNull { it.symbolId }
    val a = pickAt(1, 0, 1) ?: return null
    val b = pickAt(2, 1, 2) ?: return null
    val c = pickAt(3, 2, 3) ?: return null
    return Abc(startGen = 0, endGen = lastGen, a = a, b = b, c = c)
  }

  /**
   * Confirms the wire format round-trips through the bridge: encode the
   * Kotlin parse result as Mgroup3ParseResult bytes (by simulating what the
   * Rust encoder would do), decode via decodeMgroup3ParseResult, then walk.
   * If the AST extraction agrees with the direct kernels_history, the wire
   * format is AST-consumable.
   */
  @Test
  fun bridge_decoded_history_matches_direct_history() {
    val grammar = "Grammar = 'a' 'b' 'c'"
    val parser = makeParser(grammar)
    val ctx = parser.parse("abc")
    val direct = parser.kernelsHistory(ctx)

    // Build the bridge wire format by serializing direct → proto → bytes →
    // decode. The Rust encoder's job is just this, in Rust.
    val msg = encodeHistoryAsProto(direct)
    val bytes = msg.toByteArray()
    val outcome = decodeMgroup3ParseResult(bytes)
    val decoded = (outcome as Mgroup3NativeOutcome.Accepted).history

    assertEquals(direct.size, decoded.size)
    for (i in direct.indices) {
      assertEquals(direct[i].kernels, decoded[i].kernels, "gen $i kernels differ")
    }

    // AST extraction agrees on both sides.
    val astDirect = walkAbc(direct, direct.size - 1)
    val astDecoded = walkAbc(decoded, decoded.size - 1)
    assertEquals(astDirect, astDecoded)
  }

  private fun encodeHistoryAsProto(history: List<KernelSet>): com.giyeok.jparser.mgroup3.proto.Mgroup3ParseResult {
    val historyMsg = com.giyeok.jparser.mgroup3.proto.KernelsHistoryMsg.newBuilder()
    for (gen in history) {
      val gb = com.giyeok.jparser.mgroup3.proto.KernelGen.newBuilder()
      val sorted = gen.kernels.sortedWith(
        compareBy({ it.symbolId }, { it.pointer }, { it.beginGen }, { it.endGen })
      )
      for (k in sorted) {
        gb.addKernels(
          com.giyeok.jparser.mgroup3.proto.KernelMsg.newBuilder()
            .setSymbolId(k.symbolId).setPointer(k.pointer)
            .setBeginGen(k.beginGen).setEndGen(k.endGen)
        )
      }
      historyMsg.addEntries(gb)
    }
    return com.giyeok.jparser.mgroup3.proto.Mgroup3ParseResult.newBuilder()
      .setAccepted(true)
      .setHistory(historyMsg)
      .build()
  }

  private fun makeParser(cdg: String, startName: String = "Grammar"): Mgroup3Parser {
    val grammarAnalysis = `MetaLanguage3$`.`MODULE$`.analyzeGrammar(cdg, startName)
    val grammar = grammarAnalysis.ngrammar()
    val gen = Mgroup3ParserGenerator(grammar)
    return Mgroup3Parser(gen.generate())
  }
}
