package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.ParseTree._
import com.giyeok.jparser.Symbols._
import PreprocessedGrammar._

case class PreprocessedGrammar(startingExpansion: AtomicKernelExpansion, kernelExpansions: Map[Kernel, KernelExpansion])

object PreprocessedGrammar {
    case class Cycles(cycles: Set[Cycle])
    case class Cycle(path: Seq[Symbol])

    sealed trait SubDerive
    // NOTE start node와 non-atomic node만 base node가 될 수 있음 - SymbolDerivation밖에 나올 수가 없다는 의미
    case class BaseSubDerive(derives: Set[SubDerive]) {
        // assert children을 계속 쫓아가면 마지막엔 항상 TermLeafNode가 나와야 함
    }
    sealed trait AtomicNontermSubDerive extends SubDerive {
        val kernel: AtomicNontermKernel[AtomicSymbol with Nonterm]
    }
    // Nonterminal, OneOf, Proxy
    case class AtomicSymbolSubDerive(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derives: Set[SubDerive], cycles: Cycles) extends AtomicNontermSubDerive
    // Repeat, Sequence - 특히 sequence의 경우엔 kernel.pointer > 0 인 경우 progress가 필요
    case class NonAtomicSymbolSubDerive(kernel: Kernel, progress: Seq[ParseNode[Symbol]], derives: Set[SubDerive], cycles: Cycles) extends SubDerive
    // Join
    case class JoinSubDerive(kernel: JoinKernel, derive: SubDerive, join: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    // Except
    case class TempLiftBlockSubDerive(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derive: SubDerive, blockTrigger: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    // LookaheadExcept
    case class RevertableSubDerive(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derive: SubDerive, revertTrigger: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    // Backup
    case class DeriveRevertableSubDerive(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derive: SubDerive, deriveRevertTrigger: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    // Longest, EagerLongest
    case class ReservedLiftTriggeredLiftReverterDeriveExpansion(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derive: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    case class ReservedAliveTriggeredLiftReverterDeriveExpansion(kernel: AtomicNontermKernel[AtomicSymbol with Nonterm], derive: SubDerive, cycles: Cycles) extends AtomicNontermSubDerive
    // Terminal
    case class TermLeafNode(kernel: TerminalKernel) extends SubDerive
    case object EmptyLeafNode extends SubDerive

    sealed trait KernelExpansion {
        val termExpansions: Map[TermGroupDesc, BaseSubDerive]
    }
    case class AtomicKernelExpansion(immediateLifts: Set[ParseNode[Symbol]], termExpansions: Map[TermGroupDesc, BaseSubDerive]) extends KernelExpansion
    case class NonAtomicKernelExpansion(immediateLifts: Set[ParsedSymbolsSeq[NonAtomicSymbol]], termExpansions: Map[TermGroupDesc, BaseSubDerive]) extends KernelExpansion
}

// TODO 같은 내용의 SubDerive에는 같은 id가 붙어서 바로 비교가 가능하게 했으면 좋겠는데..
// SubDerive를 NamedSubDerive라던가 하는 별도의 형태로 바꿔서 해야될듯

trait PreprocessedGrammarSerializer {
    def serialize(spec: PreprocessedGrammar): String
}
