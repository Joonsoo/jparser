package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.NGrammar.NAtomicSymbol
import com.giyeok.jparser.parsergen.utils.ListOrder

case class AKernel(symbolId: Int, pointer: Int) extends Ordered[AKernel] {
    def toReadableString(grammar: NGrammar, pointerString: String = "\u2022"): String = {
        val symbols = grammar.symbolOf(symbolId) match {
            case atomicSymbol: NAtomicSymbol => Seq(atomicSymbol.symbol.toShortString)
            case NGrammar.NSequence(_, _, sequence) => sequence map { elemId =>
                grammar.symbolOf(elemId).symbol.toShortString
            }
        }
        (symbols.take(pointer) mkString " ") + pointerString + (symbols.drop(pointer) mkString " ")
    }

    override def compare(that: AKernel): Int =
        if (symbolId != that.symbolId) symbolId - that.symbolId else pointer - that.pointer
}

case class AKernelSet(items: Set[AKernel]) extends Ordered[AKernelSet] {
    def sortedItems: List[AKernel] = items.toList.sorted

    def toReadableString(grammar: NGrammar, pointerString: String = "\u2022"): String =
        sortedItems map (_.toReadableString(grammar, pointerString)) mkString "|"

    def toReadableStrings(grammar: NGrammar, pointerString: String = "\u2022"): Seq[String] =
        sortedItems map (_.toReadableString(grammar, pointerString))

    override def compare(that: AKernelSet): Int = ListOrder.compare(sortedItems, that.sortedItems)
}

case class AKernelSetPath(path: List[AKernelSet]) extends Ordered[AKernelSetPath] {
    def replaceLast(newLast: AKernelSet): AKernelSetPath =
        AKernelSetPath(path.init :+ newLast)

    override def compare(that: AKernelSetPath): Int = ListOrder.compare(path, that.path)
}

case class AKernelSetPathSet(paths: Seq[AKernelSetPath]) {
    def sortedItems: List[AKernelSetPath] = paths.toList.sorted

    def heads: Seq[AKernelSet] = paths.map(_.path.head)

    def lasts: Seq[AKernelSet] = paths.map(_.path.last)
}
