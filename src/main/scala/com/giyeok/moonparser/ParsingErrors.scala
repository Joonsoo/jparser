package com.giyeok.moonparser

trait ParsingErrors {
    this: Parser =>

    import Inputs._

    abstract class ParsingError {
        val next: Input
        val msg: String
    }
    object ParsingError {
        def apply(_next: Input, _msg: String) = {
            new ParsingError {
                val next = _next
                val msg = _msg
            }
        }
    }
    object ParsingErrors {
        case class UnexpectedInput(next: Input) extends ParsingError {
            val msg = next match {
                case Character(char, location) => s"Unexpected input '$char' at $location"
                case Virtual(name, location) => s"Unexpected virtual input $name at $location"
                case EOF(location) => s"Unexpected EOF at $location"
            }
        }
    }
}
