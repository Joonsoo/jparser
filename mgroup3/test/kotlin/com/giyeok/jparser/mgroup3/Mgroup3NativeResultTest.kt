package com.giyeok.jparser.mgroup3

import com.giyeok.jparser.mgroup3.proto.KernelGen
import com.giyeok.jparser.mgroup3.proto.KernelMsg
import com.giyeok.jparser.mgroup3.proto.KernelsHistoryMsg
import com.giyeok.jparser.mgroup3.proto.Mgroup3ParseResult
import com.giyeok.jparser.mgroup3.proto.ParseErrorMsg
import com.giyeok.jparser.proto.TermGroupProto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM-only unit test for the decoder. Hand-crafted Mgroup3ParseResult bytes
 * (produced by the Java proto builder) round-trip through
 * `decodeMgroup3ParseResult`. The cross-language test that drives Rust bytes
 * lives in Step 4.5.
 */
class Mgroup3NativeResultTest {
  private fun chars(s: String): TermGroupProto.TermGroup =
    TermGroupProto.TermGroup.newBuilder()
      .setCharsGroup(TermGroupProto.CharsGroup.newBuilder().setChars(s).build())
      .build()

  @Test
  fun decode_accepted_history() {
    val msg = Mgroup3ParseResult.newBuilder()
      .setAccepted(true)
      .setHistory(
        KernelsHistoryMsg.newBuilder()
          .addEntries(KernelGen.getDefaultInstance())
          .addEntries(
            KernelGen.newBuilder()
              .addKernels(KernelMsg.newBuilder().setSymbolId(1).setPointer(1).setBeginGen(0).setEndGen(1))
              .addKernels(KernelMsg.newBuilder().setSymbolId(2).setPointer(0).setBeginGen(0).setEndGen(0))
              .build()
          )
          .build()
      )
      .build()
    val outcome = decodeMgroup3ParseResult(msg.toByteArray())
    assertTrue(outcome is Mgroup3NativeOutcome.Accepted)
    val history = (outcome as Mgroup3NativeOutcome.Accepted).history
    assertEquals(2, history.size)
    assertEquals(0, history[0].kernels.size)
    assertEquals(2, history[1].kernels.size)
    assertNotNull(history[1].findByBeginGenOpt(1, 1, 0))
  }

  @Test
  fun decode_unexpected_input() {
    val msg = Mgroup3ParseResult.newBuilder()
      .setError(
        ParseErrorMsg.newBuilder()
          .setUnexpectedInput(
            ParseErrorMsg.UnexpectedInput.newBuilder()
              .setLoc(3)
              .setLine(0)
              .setCol(3)
              .setActualCodepoint('z'.code)
              .addExpectedTermGroups(chars("abc"))
          )
          .build()
      )
      .build()
    val outcome = decodeMgroup3ParseResult(msg.toByteArray())
    assertTrue(outcome is Mgroup3NativeOutcome.Rejected)
    val err = (outcome as Mgroup3NativeOutcome.Rejected).error
    assertTrue(err is ParsingError.UnexpectedInput)
    val ui = err as ParsingError.UnexpectedInput
    assertEquals(3, ui.loc)
    assertEquals(0, ui.locLine)
    assertEquals(3, ui.locCol)
    assertEquals('z', ui.actual)
  }

  @Test
  fun decode_unexpected_eof() {
    val msg = Mgroup3ParseResult.newBuilder()
      .setError(
        ParseErrorMsg.newBuilder()
          .setUnexpectedEof(
            ParseErrorMsg.UnexpectedEof.newBuilder()
              .setLoc(5).setLine(1).setCol(2)
              .addExpectedTermGroups(chars("xyz"))
          )
          .build()
      )
      .build()
    val outcome = decodeMgroup3ParseResult(msg.toByteArray())
    assertTrue(outcome is Mgroup3NativeOutcome.Rejected)
    val err = (outcome as Mgroup3NativeOutcome.Rejected).error
    assertTrue(err is ParsingError.UnexpectedEof)
    val eof = err as ParsingError.UnexpectedEof
    assertEquals(5, eof.loc)
    assertEquals(1, eof.locLine)
    assertEquals(2, eof.locCol)
  }

  @Test
  fun kernels_history_or_throw_on_accepted() {
    val msg = Mgroup3ParseResult.newBuilder()
      .setAccepted(true)
      .setHistory(KernelsHistoryMsg.newBuilder().addEntries(KernelGen.getDefaultInstance()).build())
      .build()
    val list = decodeMgroup3ParseResult(msg.toByteArray()).kernelsHistoryOrThrow()
    assertEquals(1, list.size)
  }

  @Test
  fun kernels_history_or_throw_on_rejected() {
    val msg = Mgroup3ParseResult.newBuilder()
      .setError(
        ParseErrorMsg.newBuilder().setUnexpectedEof(
          ParseErrorMsg.UnexpectedEof.newBuilder().setLoc(0).setLine(0).setCol(0)
        ).build()
      )
      .build()
    assertThrows(ParsingError.UnexpectedEof::class.java) {
      decodeMgroup3ParseResult(msg.toByteArray()).kernelsHistoryOrThrow()
    }
  }

  @Test
  fun decode_with_no_outcome_throws() {
    val msg = Mgroup3ParseResult.newBuilder().build() // outcome not set
    assertThrows(IllegalStateException::class.java) {
      decodeMgroup3ParseResult(msg.toByteArray())
    }
  }
}
