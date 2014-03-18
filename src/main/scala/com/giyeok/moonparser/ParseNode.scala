package com.giyeok.moonparser

object ParseTree {
    import Symbols._
    import Inputs._

    trait ParseNode[+T <: Symbol] {
        val symbol: T
    }

    case object ParsedEmpty extends ParseNode[Empty.type] {
        val symbol = Empty
    }
    case class ParsedTerminal(symbol: Terminal, child: Input) extends ParseNode[Terminal]
    case class ParsedSymbol[T <: Symbol](symbol: T, body: ParseNode[Symbol]) extends ParseNode[T]
    case class ParsedConjunction(symbol: Conjunction, bodies: Set[ParseNode[Symbol]]) extends ParseNode[Conjunction]
}
