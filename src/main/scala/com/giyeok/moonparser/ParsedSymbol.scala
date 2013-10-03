package com.giyeok.moonparser

import com.giyeok.moonparser.dynamic.Parser
import com.giyeok.moonparser.GrElem._

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
case class TermSymbol(input: Input, pointer: Int) extends ParsedSymbol with ConcreteSymbol {
    lazy val text = input match {
        case CharInput(c) => String valueOf c
        case VirtInput(name) => s"<$name>"
        case TokenInput(token) => token text
        case _ => ""
    }
    lazy val source = Seq(input)
}
case class NontermSymbol(elem: GrElem, children: Seq[ConcreteSymbol])
    extends ParsedSymbol with NamedSymbol with ConcreteSymbol {
    lazy val text = children map { _ text } mkString
    lazy val source = children flatMap { _ source }
}
case class VirtSymbol(elem: GrElem) extends ParsedSymbol with NamedSymbol

case class Token(source: Seq[Input], compats: Set[GrElem]) extends ParsedSymbol with ConcreteSymbol {
    lazy val text = source flatMap {
        case x: CharInput => Seq(x.char)
        case _ => Nil
    } mkString

    def compat(item: GrElem) =
        item match {
            case StringInput(string) => this.source.length == string.length && this.text == string
            case _: VirtualInput | _: Nonterminal | _: OneOf => compats contains item
            case _ => false
        }
}
