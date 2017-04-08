package com.giyeok.jparser

import ParseResultDerivations._
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar._

// TODO 수정 필요
case class ParseResultDerivationsSet(left: Int, right: Int, derivations: Set[Derivation]) extends ParseResult

object ParseResultDerivationsSetFunc extends ParseResultFunc[ParseResultDerivationsSet] {
    def terminal(left: Int, input: Inputs.Input): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, left + 1, Set(Term(left, input)))
    def bind(left: Int, right: Int, symbol: NSymbol, body: ParseResultDerivationsSet): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, right, body.derivations + Bind(left, right, symbol)) ensuring (body.left == left && body.right == right)
    def cyclicBind(left: Int, right: Int, symbol: NSymbol): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, right, Set(Bind(left, right, symbol)))
    def join(left: Int, right: Int, symbol: NGrammar.Join, body: ParseResultDerivationsSet, join: ParseResultDerivationsSet): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, right, body.derivations ++ join.derivations) ensuring (body.left == left && body.right == right && join.left == left && join.right == right)

    def sequence(left: Int, right: Int, symbol: Sequence, pointer: Int): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, right, Set())
    def cyclicSequence(left: Int, right: Int, symbol: Sequence, pointer: Int): ParseResultDerivationsSet =
        ParseResultDerivationsSet(left, right, Set())
    def append(sequence: ParseResultDerivationsSet, child: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        if (sequence.right != child.left) {
            println(sequence.left, sequence.right)
            println(child.left, child.right)
        }
        ParseResultDerivationsSet(sequence.left, child.right, (sequence.derivations ++ child.derivations) + LastChild(sequence.right, child.right)) ensuring (sequence.right == child.left)
    }

    def merge(base: ParseResultDerivationsSet, merging: ParseResultDerivationsSet): ParseResultDerivationsSet = {
        ParseResultDerivationsSet(base.left, base.right, base.derivations ++ merging.derivations) ensuring (base.left == merging.left && base.right == merging.right)
    }
}

object ParseResultDerivations {
    sealed trait Derivation {
        val left: Int
        val right: Int

        val range = (left, right)
    }

    case class Term(left: Int, input: Inputs.Input) extends Derivation {
        val right = left + 1
    }
    case class Bind(left: Int, right: Int, symbol: NSymbol) extends Derivation
    case class LastChild(left: Int, right: Int) extends Derivation
}
