package com.giyeok.jparser

import com.giyeok.jparser.Inputs.{Character, Input, TermGroupDesc, Virtual}

object ParsingErrors {

  abstract class ParsingError extends Exception {
    val msg: String
  }

  sealed trait WithLocation {
    val location: Int
  }

  object ParsingError {
    def apply(_next: Input, _msg: String): ParsingError = new ParsingError {
      val next: Input = _next
      val msg: String = _msg
    }
  }

  case class UnexpectedInput(next: Input, expected: Set[Symbols.Terminal], location: Int) extends ParsingError with WithLocation {
    override val msg: String = next match {
      case Character(char) => s"Unexpected input '$char' at $location"
      case Virtual(name) => s"Unexpected virtual input $name at $location"
    }
  }

  case class UnexpectedInputByTermGroups(next: Input, expected: Set[TermGroupDesc], location: Int) extends ParsingError with WithLocation {
    override val msg: String = next match {
      case Character(char) => s"Unexpected input '$char' at $location, expected=$expected"
      case Virtual(name) => s"Unexpected virtual input $name at $location, expected=$expected"
    }
  }

  case class UnexpectedEOF(expected: Set[Symbols.Terminal], location: Int) extends ParsingError with WithLocation {
    override val msg: String = s"Unexpected EOF at $location"
  }

  case class UnexpectedEOFByTermGroups(expected: Set[TermGroupDesc], location: Int) extends ParsingError with WithLocation {
    override val msg: String = s"Unepxected EOF at $location"
  }

  case object UnexpectedError extends ParsingError {
    val msg = "Unexpected Error??"
  }

  case class AmbiguousParse(msg: String) extends ParsingError
}
