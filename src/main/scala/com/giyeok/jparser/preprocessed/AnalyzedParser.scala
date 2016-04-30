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
import PreprocessedParserSpec._

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

    // TODO 구체적인 Node 없이 Kernel로만 derive하는 것도 만들어야겠다
    //   - 그래서 얻어낸 Kernel들 중에 TerminalKernel들로 TermGroupDesc를 만들어야지

    private val knownKernelSetActions = scala.collection.mutable.Map[KernelSet, Map[TermGroupDesc, Option[KernelSetAction]]]()
    // Option[KernelSetAction]이 None이면 아직 처리를 안했다는 의미

    def expand0(kernelSet: KernelSet, term: TermGroupDesc): KernelSetAction = {
        val ksAction: KernelSetAction = ???
        if (!(knownKernelSetActions contains kernelSet)) {
            knownKernelSetActions(kernelSet) = ??? // kernelSet으로부터 derive해서 얻어낸 TerminalKernel들로 TermGroupDesc를 구한다
        }
        ksAction
    }
}
