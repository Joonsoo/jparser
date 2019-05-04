package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol

case class AKernel(symbolId: Int, pointer: Int) extends Ordered[AKernel] {
    def toReadableString(grammar: NGrammar, pointerString: String = "\u2022"): String = {
        val symbols = grammar.symbolOf(symbolId) match {
            case atomicSymbol: NAtomicSymbol => Seq(atomicSymbol.symbol.toShortString)
            case NGrammar.NSequence(_, sequence) => sequence map { elemId =>
                grammar.symbolOf(elemId).symbol.toShortString
            }
        }
        (symbols.take(pointer) mkString " ") + pointerString + (symbols.drop(pointer) mkString " ")
    }

    override def compare(that: AKernel): Int =
        if (symbolId != that.symbolId) symbolId - that.symbolId else pointer - that.pointer
}

case class AKernelSet(items: Set[AKernel]) {
    def sortedItems: List[AKernel] = items.toList.sorted

    def toReadableString(grammar: NGrammar, pointerString: String = "\u2022"): String =
        sortedItems map (_.toReadableString(grammar, pointerString)) mkString "|"

    def toReadableStrings(grammar: NGrammar, pointerString: String = "\u2022"): Seq[String] =
        sortedItems map (_.toReadableString(grammar, pointerString))
}

case class AKernelSetPath(path: List[AKernelSet])
