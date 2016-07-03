package com.giyeok.jparser

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def terminal(input: Inputs.Input): R
    def bind(symbol: Symbols.Symbol, body: R): R
    def join(body: R, constraint: R): R

    // sequence는 Sequence에서만 쓰임
    def sequence(): R
    def append(sequence: R, child: R): R
    def appendWhitespace(sequence: R, whitespace: R): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): Option[R] =
        if (results.isEmpty) None
        else Some(results.tail.foldLeft(results.head)(merge(_, _)))

    def termFunc(): R
    def substTermFunc(r: R, input: Inputs.Input): R
}

sealed trait ParseResultWithType[R <: ParseResult] {
    val result: R
    def mapResult(func: R => R): ParseResultWithType[R]
}
case class TermResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): TermResult[R] = TermResult(func(result))
}
case class SequenceResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): SequenceResult[R] = SequenceResult(func(result))
}
case class JoinResult[R <: ParseResult](result: R) extends ParseResultWithType[R] {
    def mapResult(func: R => R): JoinResult[R] = JoinResult(func(result))
}
case class BindedResult[R <: ParseResult](result: R, symbol: Symbols.Symbol) extends ParseResultWithType[R] {
    def mapResult(func: R => R): BindedResult[R] = BindedResult(func(result), symbol)
}

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
    def join(body: ParseResultDerivationsSet, constraint: ParseResultDerivationsSet): ParseResultDerivationsSet =
        body // nothing to do?

    def sequence(): ParseResultDerivationsSet =
        ParseResultDerivationsSet(0, Set())
    def append(sequence: ParseResultDerivationsSet, child: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(sequence.length + child.length, sequence.derivations ++ (child.derivations map { _.shift(sequence.length) }))
    }
    def appendWhitespace(sequence: ParseResultDerivationsSet, whitespace: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(sequence.length + whitespace.length, sequence.derivations ++ (whitespace.derivations map { _.shift(sequence.length) }))
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
}
