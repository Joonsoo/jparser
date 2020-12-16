package com.giyeok.jparser.nparser

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.nparser.ParseTreeConstructor2.ProgressedAndFinished
import com.giyeok.jparser.nparser.ParsingContext.Kernel
import com.giyeok.jparser.{NGrammar, ParseResult, ParseResultFunc}

class ParseTreeConstructor2[R <: ParseResult](resultFunc: ParseResultFunc[R])(grammar: NGrammar)(input: Seq[Input], history: Seq[ProgressedAndFinished]) {

  def reconstruct(): Option[R] =
    reconstruct(Kernel(grammar.startSymbol, 1, 0, input.length)(null), input.length)

  def reconstruct(kernel: Kernel, gen: Int) =
    if (kernel.pointer > 0 && history(gen).finished.contains(kernel)) Some(reconstruct(kernel, gen, Set())) else None

  private def reconstruct(kernel: Kernel, gen: Int, traces: Set[(Int, Int)]): R = {
    ???
  }
}

object ParseTreeConstructor2 {

  class ProgressedAndFinished(val progressed: Set[Kernel], val finished: Set[Kernel])

}
