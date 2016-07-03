package com.giyeok.jparser

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def terminal(input: Inputs.Input): R
    def bind(symbol: Symbols.Symbol, body: R): R
    def join(symbol: Symbols.Join, body: R, constraint: R): R

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
