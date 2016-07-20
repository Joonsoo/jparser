package com.giyeok.jparser.deprecated

import com.giyeok.jparser.ParseResult
import com.giyeok.jparser.ParseResultFunc
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Symbols

object AlwaysTrue extends ParseResult
object ParseResultTrueFunc extends ParseResultFunc[AlwaysTrue.type] {
    def terminal(position: Int, input: Inputs.Input) = AlwaysTrue
    def bind(symbol: Symbols.Symbol, body: AlwaysTrue.type) = AlwaysTrue
    def join(symbol: Symbols.Join, body: AlwaysTrue.type, constraint: AlwaysTrue.type) = AlwaysTrue

    // sequence는 Sequence에서만 쓰임
    def sequence(position: Int, symbol: Symbols.Sequence) = AlwaysTrue
    def append(sequence: AlwaysTrue.type, child: AlwaysTrue.type) = AlwaysTrue

    def merge(base: AlwaysTrue.type, merging: AlwaysTrue.type) = AlwaysTrue

    def termFunc() = AlwaysTrue
    def substTermFunc(r: AlwaysTrue.type, position: Int, input: Inputs.Input) = AlwaysTrue
    def shift(r: AlwaysTrue.type, position: Int) = AlwaysTrue
}
