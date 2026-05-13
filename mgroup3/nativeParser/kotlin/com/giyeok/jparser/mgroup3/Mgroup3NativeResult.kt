package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.ktlib.Kernel
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.ktlib.TermGroupUtil
import com.giyeok.jparser.mgroup3.proto.KernelGen
import com.giyeok.jparser.mgroup3.proto.KernelMsg
import com.giyeok.jparser.mgroup3.proto.KernelsHistoryMsg
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParseResult
import com.giyeok.jparser.mgroup3.proto.ParseErrorMsg

/**
 * Decoded result of a single Rust-side parse. Returned by
 * [decodeMgroup3ParseResult] when given the bytes emitted by the Rust encoder
 * (`mgroup3_native::parser::encode_parse_result`).
 *
 * Use [kernelsHistoryOrThrow] if the caller wants to throw on the rejected
 * variant and proceed with `List<KernelSet>` directly.
 */
sealed class Mgroup3NativeOutcome {
  data class Accepted(val history: List<KernelSet>) : Mgroup3NativeOutcome()
  data class Rejected(val error: ParsingError) : Mgroup3NativeOutcome()
}

/**
 * Decode bytes produced by the native parser's `encode_parse_result` into a
 * typed outcome. The wire format is `Mgroup3ParseResult` from
 * `Mgroup3ParserResult.proto`.
 *
 * Mapping notes:
 * - `accepted=true` with `outcome=history` → [Mgroup3NativeOutcome.Accepted].
 *   Each `KernelGen.kernels` becomes a [KernelSet].
 * - `outcome=error` → [Mgroup3NativeOutcome.Rejected] wrapping a typed
 *   [ParsingError]. Proto `line`/`col` map to Kotlin `locLine`/`locCol`.
 * - `OUTCOME_NOT_SET` → throws [IllegalStateException].
 *
 * BMP-only caveat: `actual_codepoint` is converted to `Char` via the int
 * constructor, which only handles BMP scalars. Surrogate pairs are not in the
 * current grammar corpus.
 */
fun decodeMgroup3ParseResult(bytes: ByteArray): Mgroup3NativeOutcome {
  val msg = Mgroup3ParseResult.parseFrom(bytes)
  return when (msg.outcomeCase) {
    Mgroup3ParseResult.OutcomeCase.HISTORY -> Mgroup3NativeOutcome.Accepted(decodeHistory(msg.history))
    Mgroup3ParseResult.OutcomeCase.ERROR -> Mgroup3NativeOutcome.Rejected(decodeError(msg.error))
    Mgroup3ParseResult.OutcomeCase.OUTCOME_NOT_SET, null ->
      throw IllegalStateException("Mgroup3ParseResult.outcome not set")
  }
}

fun Mgroup3NativeOutcome.kernelsHistoryOrThrow(): List<KernelSet> = when (this) {
  is Mgroup3NativeOutcome.Accepted -> history
  is Mgroup3NativeOutcome.Rejected -> throw error
}

private fun decodeHistory(history: KernelsHistoryMsg): List<KernelSet> =
  history.entriesList.map { decodeKernelGen(it) }

private fun decodeKernelGen(gen: KernelGen): KernelSet {
  val builder = KernelSet.Builder()
  for (k: KernelMsg in gen.kernelsList) {
    builder.addKernel(k.symbolId, k.pointer, k.beginGen, k.endGen)
  }
  return builder.build()
}

private fun decodeError(error: ParseErrorMsg): ParsingError = when (error.errorCase) {
  ParseErrorMsg.ErrorCase.UNEXPECTED_INPUT -> {
    val ui = error.unexpectedInput
    ParsingError.UnexpectedInput(
      loc = ui.loc,
      locLine = ui.line,
      locCol = ui.col,
      expected = buildTermSet(ui.expectedTermGroupsList),
      actual = Char(ui.actualCodepoint),
    )
  }
  ParseErrorMsg.ErrorCase.UNEXPECTED_EOF -> {
    val eof = error.unexpectedEof
    ParsingError.UnexpectedEof(
      loc = eof.loc,
      locLine = eof.line,
      locCol = eof.col,
      expected = buildTermSet(eof.expectedTermGroupsList),
    )
  }
  ParseErrorMsg.ErrorCase.ERROR_NOT_SET, null ->
    throw IllegalStateException("ParseErrorMsg.error not set")
}

private fun buildTermSet(
  termGroups: List<com.giyeok.jparser.proto.TermGroupProto.TermGroup>,
): com.giyeok.jparser.ktlib.TermSet {
  val builder = TermGroupUtil.TermGroupBuilder()
  for (tg in termGroups) {
    builder.add(tg)
  }
  return builder.build()
}
