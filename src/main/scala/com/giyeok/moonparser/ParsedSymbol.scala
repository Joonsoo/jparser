package com.giyeok.moonparser

import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.InputPieces._

object ParsedSymbols {
    sealed abstract class ParsedSymbol
    sealed trait ConcreteSymbol extends ParsedSymbol {
        val text: String
        val source: Seq[Input]
    }
    sealed trait NamedSymbol extends ParsedSymbol {
        val elem: GrElem
    }

    case object StartSymbol extends ParsedSymbol
    case class EmptySymbol(elem: GrElem) extends ParsedSymbol with ConcreteSymbol {
        val text = ""
        val source = Nil
    }
    case class TermSymbol[+T <: Input](input: T, pointer: Int) extends ParsedSymbol with ConcreteSymbol {
        lazy val text = input match {
            case CharInput(c) => String valueOf c
            case VirtInput(name) => s"<$name>"
            case TokenInput(token) => token text
            case _ => ""
        }
        lazy val source = Seq(input)
    }
    case class NontermSymbol(elem: GrElem, children: Seq[ParsedSymbol])
            extends ParsedSymbol with NamedSymbol with ConcreteSymbol {
        lazy val text = children map { case c: ConcreteSymbol => c.text case _ => "" } mkString
        lazy val source = children flatMap { case c: ConcreteSymbol => c.source case _ => Nil }
    }
    class NontermSymbolWS(elem: GrElem, children: Seq[ParsedSymbol], val childrenWS: Seq[ParsedSymbol], val mappings: Map[Int, Int])
            extends NontermSymbol(elem, children) {
        override lazy val text = childrenWS map { case c: ConcreteSymbol => c.text case _ => "" } mkString
        override lazy val source = childrenWS flatMap { case c: ConcreteSymbol => c.source case _ => Nil }
    }

    // TODO implement virtual symbols for static analysis of the grammar
    case class VirtSymbol(elem: GrElem) extends ParsedSymbol with NamedSymbol

    case class Token(source: Seq[Input], compats: Set[GrElem]) extends ParsedSymbol with ConcreteSymbol {
        lazy val text = source flatMap {
            case x: CharInput => Seq(x.char)
            case _ => Nil
        } mkString

        def compat(item: GrElem) =
            item match {
                case StringInputElem(string) => this.source.length == string.length && this.text == string
                case _: VirtualInputElem | _: Nonterminal | _: OneOf => compats contains item
                case _ => false
            }
    }
}
