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

    // TODO cyclicBind
    // def cyclicBind(symbol: Symbols.Symbol): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): Option[R] =
        if (results.isEmpty) None
        else Some(results.tail.foldLeft(results.head)(merge(_, _)))

    def termFunc(): R
    def substTermFunc(r: R, position: Int, input: Inputs.Input): R
    def shift(r: R, position: Int): R
}
