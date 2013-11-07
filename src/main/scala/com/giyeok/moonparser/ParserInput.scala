package com.giyeok.moonparser

import com.giyeok.moonparser.ParsedSymbols._

object ParserInputs {
    import com.giyeok.moonparser.InputPieces._

    abstract class ParserInput {
        val length: Int
        def at(pointer: Int): Input
        def finishedAt(pointer: Int): Boolean
        def nextPointer(pointer: Int): Int =
            if (this finishedAt pointer) pointer else pointer + 1

        def subinput(p: Int): ParserInput
    }
    class StringParserInput(val string: String) extends ParserInput {
        val length = string length
        def at(p: Int) = if (p < length) CharInput(string charAt p) else EndOfFile
        def finishedAt(p: Int): Boolean = p >= string.length

        def subinput(p: Int) = new StringParserInput(string substring p)
    }
    class SeqParserInput(val seq: Seq[Input]) extends ParserInput {
        val length = seq length
        def at(p: Int) = if (p < length) seq(p) else EndOfFile
        def finishedAt(p: Int): Boolean = p >= seq.length

        def subinput(p: Int) = new SeqParserInput(seq drop p)
    }
    object ParserInput {
        def fromString(string: String) = new StringParserInput(string)
        def fromSeq(list: Seq[Input]) = new SeqParserInput(list)
    }
}

abstract sealed class Input
object InputPieces {
    case class CharInput(val char: Char) extends Input {
        override def hashCode = char.hashCode
        override def equals(other: Any) = other match {
            case that: CharInput => (that canEqual this) && (char == that.char)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[CharInput]
    }
    case class VirtInput(val name: String) extends Input {
        override def hashCode = name.hashCode
        override def equals(other: Any) = other match {
            case that: VirtInput => (that canEqual this) && (name == that.name)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[VirtInput]
    }
    case object EndOfFile extends Input
}
