package com.giyeok.jparser

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def terminal(position: Int, input: Inputs.Input): R
    def bind(symbol: Symbols.Symbol, body: R): R
    def join(symbol: Symbols.Join, body: R, constraint: R): R

    // sequence는 Sequence에서만 쓰임
    def sequence(position: Int, symbol: Symbols.Sequence): R
    def append(sequence: R, child: R): R
    def appendWhitespace(sequence: R, whitespace: R): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): Option[R] =
        if (results.isEmpty) None
        else Some(results.tail.foldLeft(results.head)(merge(_, _)))

    def termFunc(): R
    def substTermFunc(r: R, position: Int, input: Inputs.Input): R
    def shift(r: R, position: Int): R
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

object AlwaysTrue extends ParseResult
object ParseResultTrueFunc extends ParseResultFunc[AlwaysTrue.type] {
    def terminal(position: Int, input: Inputs.Input) = AlwaysTrue
    def bind(symbol: Symbols.Symbol, body: AlwaysTrue.type) = AlwaysTrue
    def join(symbol: Symbols.Join, body: AlwaysTrue.type, constraint: AlwaysTrue.type) = AlwaysTrue

    // sequence는 Sequence에서만 쓰임
    def sequence(position: Int, symbol: Symbols.Sequence) = AlwaysTrue
    def append(sequence: AlwaysTrue.type, child: AlwaysTrue.type) = AlwaysTrue
    def appendWhitespace(sequence: AlwaysTrue.type, whitespace: AlwaysTrue.type) = AlwaysTrue

    def merge(base: AlwaysTrue.type, merging: AlwaysTrue.type) = AlwaysTrue

    def termFunc() = AlwaysTrue
    def substTermFunc(r: AlwaysTrue.type, position: Int, input: Inputs.Input) = AlwaysTrue
    def shift(r: AlwaysTrue.type, position: Int) = AlwaysTrue
}
