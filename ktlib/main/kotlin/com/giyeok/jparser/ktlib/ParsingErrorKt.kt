package com.giyeok.jparser.ktlib

sealed class ParsingErrorKt : Exception() {
  abstract val location: Int

  data class UnexpectedInput(
    override val location: Int,
    val expected: TermSet,
    val actual: Char
  ) : ParsingErrorKt()

  data class UnexpectedEndOfFile(override val location: Int) : ParsingErrorKt()
}
