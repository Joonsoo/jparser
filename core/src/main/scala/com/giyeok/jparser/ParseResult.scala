package com.giyeok.jparser

import com.giyeok.jparser.nparser.NGrammar._

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def terminal(left: Int, input: Inputs.Input): R
    def bind(left: Int, right: Int, symbol: NSymbol, body: R): R
    def join(left: Int, right: Int, symbol: NJoin, body: R, join: R): R
    def cyclicBind(left: Int, right: Int, symbol: NSymbol): R

    def sequence(left: Int, right: Int, symbol: NSequence, pointer: Int): R
    def append(sequence: R, child: R): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): R =
        results.tail.foldLeft(results.head)(merge) ensuring results.nonEmpty
}
