package com.giyeok.moonparser

import com.giyeok.moonparser.dynamic.Parser

sealed abstract class ParsedSymbol {
    val text: String
    val source: List[Input]
}
case object StartSymbol extends ParsedSymbol {
    val text = ""
    val source = Nil
}
case class NontermSymbol(item: Parser#StackEntry#ParsingItem) extends ParsedSymbol {
    lazy val text = (item.children map (_ text)) mkString
    lazy val source = {
        def rec(l: List[ParsedSymbol]): List[Input] = l match {
            case x :: xs => x.source ++ rec(xs)
            case Nil => List()
        }
        rec(item.children)
    }
}
case class TermSymbol(input: Input, pointer: Int) extends ParsedSymbol {
    val text: String = input match {
        case CharInput(c) => String valueOf c
        case TokenInput(token) => token text
        case _ => ""
    }
    val source = List(input)
}
case class EmptySymbol(item: GrElem) extends ParsedSymbol {
    val text = ""
    val source = Nil
}
