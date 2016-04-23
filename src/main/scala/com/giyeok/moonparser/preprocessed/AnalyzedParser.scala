package com.giyeok.moonparser.preprocessed

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Kernels
import com.giyeok.moonparser.Symbols
import com.giyeok.moonparser.ParseTree
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.preprocessed.PreprocessedParserSpec.KernelSet
import com.giyeok.moonparser.Symbols._
import com.giyeok.moonparser.Inputs._
import com.giyeok.moonparser.Kernels._
import com.giyeok.moonparser.ParseTree._

class AnalyzedParser(grammar: Grammar) extends Parser(grammar) {
    type IPNSymbol = NonAtomicSymbol with Nonterm

    val ctx0 = initialContext

    class AnalyzedParsingContext(val ctx: ParsingContext) {
        val ipns: Set[NonAtomicSymbolProgress[IPNSymbol]] = ctx.allProceededEdges map { _.end.asInstanceOf[NonAtomicSymbolProgress[IPNSymbol]] }
        val ipnsByKernel: Map[NonAtomicNontermKernel[IPNSymbol], Set[NonAtomicSymbolProgress[IPNSymbol]]] = ipns groupBy { _.kernel }
        val kernelSet = ipnsByKernel.keySet

        lazy val termGroups: Set[TermGroupDesc] = {
            import Symbols.Terminals._

            val terminals = ctx.terminalNodes map { _.kernel.symbol }
            val charTerms: Set[CharacterTermGroupDesc] = terminals collect { case x: CharacterTerminal => TermGroupDesc.descOf(x) }
            val virtTerms: Set[VirtualTermGroupDesc] = terminals collect { case x: VirtualTerminal => TermGroupDesc.descOf(x) }

            val charIntersects: Set[CharacterTermGroupDesc] = charTerms flatMap { term1 =>
                charTerms collect {
                    case term2 if term1 != term2 => term1 intersect term2
                } filterNot { _.isEmpty }
            }
            val virtIntersects: Set[VirtualTermGroupDesc] = virtTerms flatMap { term1 =>
                virtTerms collect {
                    case term2 if term1 != term2 => term1 intersect term2
                } filterNot { _.isEmpty }
            }

            // charIntersects foreach { d => println(d.toShortString) }
            // virtIntersects foreach { d => println(d.toShortString) }

            val charTermGroups = (charTerms map { term =>
                charIntersects.foldLeft(term) { _ - _ }
            }) ++ charIntersects
            val virtTermGroups = (virtTerms map { term =>
                virtIntersects.foldLeft(term) { _ - _ }
            }) ++ virtIntersects

            charTermGroups ++ virtTermGroups
        }
    }
}
