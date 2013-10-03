package com.giyeok.moonparser

import com.giyeok.moonparser.dynamic.Token

abstract class ParserInput {
    val length: Int
    def at(pointer: Int): Input

    def subinput(p: Int): ParserInput
}
class StringParserInput(val string: String) extends ParserInput {
    val length = string length
    def at(p: Int) = if (p < length) CharInput(string charAt p) else EOF

    def subinput(p: Int) = new StringParserInput(string substring p)
}
class ListParserInput(val list: List[Input]) extends ParserInput {
    val length = list length
    def at(p: Int) = if (p < length) list(p) else EOF

    def subinput(p: Int) = new ListParserInput(list drop p)
}
object ParserInput {
    def fromString(string: String) = new StringParserInput(string)
    def fromList(list: List[Input]) = new ListParserInput(list)
}

abstract sealed class Input
case class CharInput(val char: Char) extends Input
case class VirtInput(val name: String) extends Input
case class TokenInput(val token: Token) extends Input
case object EOF extends Input
