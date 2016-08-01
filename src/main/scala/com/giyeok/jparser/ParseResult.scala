package com.giyeok.jparser

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def terminal(left: Int, input: Inputs.Input): R
    def bind(left: Int, right: Int, symbol: Symbols.Symbol, body: R): R
    def join(left: Int, right: Int, symbol: Symbols.Join, body: R, join: R): R
    def cyclicBind(left: Int, right: Int, symbol: Symbols.Symbol): R

    def sequence(left: Int, right: Int, symbol: Symbols.Sequence): R
    def append(sequence: R, child: R): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): Option[R] =
        if (results.isEmpty) None
        else Some(results.tail.foldLeft(results.head)(merge(_, _)))
}
