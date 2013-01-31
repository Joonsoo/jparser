package com.giyeok.bokparser

abstract class InputStream {
	def pointer: Int
	def length: Int
	def hasNext = pointer < length
	def symbol: InputSymbol
	def next(): InputSymbol

	def subStreamFrom(p: Int): InputStream
}
class StringInputStream(val string: String, private var _pointer: Int = 0) extends InputStream {
	def pointer = _pointer
	def length = string length
	def symbol = if (hasNext) CharInputSymbol(string charAt (pointer)) else EOFSymbol
	def next() = { val s = symbol; _pointer += 1; s }

	def subStreamFrom(p: Int) = new StringInputStream(string, p)
}
class ListInputStream(val list: List[InputSymbol], private var _pointer: Int = 0) extends InputStream {
	def pointer = _pointer
	def length = list length
	def symbol = if (hasNext) list(pointer) else EOFSymbol
	def next() = { val s = symbol; _pointer += 1; s }

	def subStreamFrom(p: Int) = new ListInputStream(list, p)
}
object InputStream {
	def fromString(string: String) = new StringInputStream(string)
	def fromList(list: List[InputSymbol]) = new ListInputStream(list)
}

abstract sealed class InputSymbol
case class CharInputSymbol(val char: Char) extends InputSymbol
case class VirtInputSymbol(val name: String) extends InputSymbol
case object EOFSymbol extends InputSymbol
