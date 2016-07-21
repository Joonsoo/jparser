package com.giyeok.jparser

object ParsingErrors {
    import Inputs._

    abstract class ParsingError {
        val msg: String
    }
    object ParsingError {
        def apply(_next: Input, _msg: String) = new ParsingError {
            val next = _next
            val msg = _msg
        }
    }

    case class UnexpectedInput(next: Input) extends ParsingError {
        val msg = next match {
            case Character(char, location) => s"Unexpected input '$char' at $location"
            case Virtual(name, location) => s"Unexpected virtual input $name at $location"
            case AbstractInput(chars) => throw new AssertionError("Wrong Abstract Input")
        }
    }

    case object UnexpectedError extends ParsingError {
        val msg = "Unexpected Error??"
    }
}
