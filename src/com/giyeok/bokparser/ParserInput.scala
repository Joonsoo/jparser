package com.giyeok.bokparser

abstract class ParserInput {
	def length: Int
	def at(pointer: Int): InputSymbol

	def subinput(p: Int): ParserInput
}
class StringParserInput(val string: String) extends ParserInput {
	def length = string length
	def at(p: Int) = if (p < length) CharInputSymbol(string charAt p) else EOFSymbol

	def subinput(p: Int) = new StringParserInput(string substring p)
}
class ListParserInput(val list: List[InputSymbol]) extends ParserInput {
	def length = list length
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
case object EOFSymbol extends InputSymbol
