package com.giyeok.jparser

import ParseResultDerivations._

case class ParseResultDerivationsSet(length: Int, derivations: Set[Derivation]) extends ParseResult {
    def +(derivation: Derivation): ParseResultDerivationsSet =
        ParseResultDerivationsSet(length, derivations + derivation)
}

object ParseResultDerivationsSetFunc extends ParseResultFunc[ParseResultDerivationsSet] {
    def terminal(input: Inputs.Input): ParseResultDerivationsSet =
        ParseResultDerivationsSet(1, Set(Term(0, input)))
    def bind(symbol: Symbols.Symbol, body: ParseResultDerivationsSet): ParseResultDerivationsSet =
        body + (Bind(0, body.length, symbol))
    def join(body: ParseResultDerivationsSet, join: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        val length = body.length ensuring (body.length == join.length)
        ParseResultDerivationsSet(length, body.derivations ++ join.derivations)
    }

    def sequence(): ParseResultDerivationsSet =
        ParseResultDerivationsSet(0, Set())
    def append(sequence: ParseResultDerivationsSet, child: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(sequence.length + child.length, sequence.derivations ++ (child.derivations map { _.shift(sequence.length) }) + LastChild(sequence.length, child.length, true))
    }
    def appendWhitespace(sequence: ParseResultDerivationsSet, whitespace: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(sequence.length + whitespace.length, sequence.derivations ++ (whitespace.derivations map { _.shift(sequence.length) }) + LastChild(sequence.length, whitespace.length, true))
    }

    def merge(base: ParseResultDerivationsSet, merging: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        val length = base.length ensuring (base.length == merging.length)
        ParseResultDerivationsSet(length, base.derivations ++ merging.derivations)
    }

    def termFunc(): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(1, Set(TermFunc(0)))
    }
    def substTermFunc(r: ParseResultDerivationsSet, input: Inputs.Input): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(r.length, r.derivations map {
            case TermFunc(position) => Term(position, input)
            case d => d
        })
    }
}

object ParseResultDerivations {
    sealed trait Derivation {
        val position: Int
        val length: Int
        def shift(distance: Int): Derivation

        val range = (position, position + length)
    }

    case class TermFunc(position: Int) extends Derivation {
        val length = 1
        def shift(distance: Int) = TermFunc(position + distance)
    }
    case class Term(position: Int, input: Inputs.Input) extends Derivation {
        val length = 1
        def shift(distance: Int) = Term(position + distance, input)
    }
    case class Bind(position: Int, length: Int, symbol: Symbols.Symbol) extends Derivation {
        def shift(distance: Int) = Bind(position + distance, length, symbol)
    }
    case class LastChild(position: Int, length: Int, content: Boolean) extends Derivation {
        val whitespace = !content
        def shift(distance: Int) = LastChild(position + distance, length, content)
    }
}
