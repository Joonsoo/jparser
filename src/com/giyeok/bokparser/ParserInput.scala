package com.giyeok.bokparser

import com.giyeok.bokparser.dynamic.Token

abstract class ParserInput {
	val length: Int
	def at(pointer: Int): InputSymbol

	def subinput(p: Int): ParserInput
}
class StringParserInput(val string: String) extends ParserInput {
	val length = string length
	def at(p: Int) = if (p < length) CharInputSymbol(string charAt p) else EOFSymbol

	def subinput(p: Int) = new StringParserInput(string substring p)
}
class ListParserInput(val list: List[InputSymbol]) extends ParserInput {
	val length = list length
	def at(p: Int) = if (p < length) list(p) else EOFSymbol

	def subinput(p: Int) = new ListParserInput(list drop p)
}
object ParserInput {
	def fromString(string: String) = new StringParserInput(string)
	def fromList(list: List[InputSymbol]) = new ListParserInput(list)
}

abstract sealed class InputSymbol
case class CharInputSymbol(val char: Char) extends InputSymbol
case class VirtInputSymbol(val name: String) extends InputSymbol
case class TokenInputSymbol(val token: Token) extends InputSymbol
case object EOFSymbol extends InputSymbol
