package com.giyeok.moonparser

trait ParsingErrors {
    this: Parser =>

    import Inputs._

    abstract class ParsingError {
        val next: Input
        val msg: String
    }
    object ParsingError {
        def apply(_next: Input, _msg: String) = new ParsingError {
            val next = _next
            val msg = _msg
        }
    }
    object ParsingErrors {
        case class UnexpectedInput(next: Input) extends ParsingError {
            val msg = s"Unexpected input at ${next.location}"
        }
    }
}
