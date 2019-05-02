package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol

case class AKernel(symbolId: Int, pointer: Int) {
    def toReadableString(grammar: NGrammar, pointerString: String = "*"): String = {
        val symbols = grammar.symbolOf(symbolId) match {
            case atomicSymbol: NAtomicSymbol => Seq(atomicSymbol.symbol.toShortString)
            case NGrammar.NSequence(_, sequence) => sequence map { elemId =>
                grammar.symbolOf(elemId).symbol.toShortString
            }
        }
        (symbols.take(pointer) mkString " ") + pointerString + (symbols.drop(pointer) mkString " ")
    }
}

case class AKernelSet(items: Set[AKernel]) {
    def sortedItems: List[AKernel] = items.toList.sortBy(k => (k.symbolId, k.pointer))

    def toReadableString(grammar: NGrammar, pointerString: String = "*"): String =
        sortedItems map (_.toReadableString(grammar, pointerString)) mkString " | "

    def toReadableStrings(grammar: NGrammar, pointerString: String = "*"): Seq[String] =
        sortedItems map (_.toReadableString(grammar, pointerString))
}

case class AKernelSetPath(path: List[AKernelSet])
