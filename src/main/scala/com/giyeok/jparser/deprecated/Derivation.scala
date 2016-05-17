package com.giyeok.jparser.deprecated

import com.giyeok.jparser.Kernels._
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Symbols._

sealed trait Derivation
object Derivations {
    // 빈 derive
    case object EmptyDerivation extends Derivation
    // Nonterminal, OneOf, Repeat, Proxy, Sequence - 일반적인 derive
    case class SymbolDerivation(derives: Set[Kernel]) extends Derivation
    // Join
    case class JoinDerivation(derive: Kernel, join: Kernel) extends Derivation
    // Except - (self->derive)로 가는 SimpleEdge + (blockTrigger->self)로 오는 TempLiftBlockReverter
    case class TempLiftBlockableDerivation(derive: Kernel, blockTrigger: Kernel) extends Derivation
    // LookaheadExcept - (self->derive)로 가는 SimpleEdge + (revertTrigger->(self->derive))인 DeriveReverter
    case class RevertableDerivation(derive: Kernel, revertTrigger: Kernel) extends Derivation
    // Backup - (self->derive)로 가는 SimpleEdge + (self->deriveRevertTrigger)로 가는 SimpleEdge + (deriveRevertTrigger->(self->derive))인 DeriveReverter
    case class DeriveRevertableDerivation(derive: Kernel, deriveRevertTrigger: Kernel) extends Derivation
    // Longest/EagerLongest
    // derive로 가는 SimpleEdge가 있고, 노드 자신에 ReservedReverter가 붙어 있음
    sealed trait ReservedLiftRevertableDerivation extends Derivation { val derive: Kernel }
    case class ReservedLiftTriggeredLiftRevertableDerivation(derive: Kernel) extends ReservedLiftRevertableDerivation
    case class ReservedAliveTriggeredLiftRevertableDerivation(derive: Kernel) extends ReservedLiftRevertableDerivation

    def derive(grammar: Grammar, kernel: NontermKernel[_]) = kernel match {
        case kernel: AtomicNontermKernel[_] =>
            if (kernel.pointer == 0) deriveAtomicKernel(grammar, kernel) else EmptyDerivation

        case SequenceKernel(symbol, pointer) =>
            if (pointer < symbol.seq.size) {
                val elemDerive = Kernel(symbol.seq(pointer))
                if (pointer > 0 && pointer < symbol.seq.size) {
                    // whitespace only between symbols
                    val wsDerives: Set[Kernel] = symbol.whitespace map { Kernel(_) }
                    SymbolDerivation(wsDerives + elemDerive)
                } else {
                    SymbolDerivation(Set(elemDerive))
                }
            } else {
                EmptyDerivation
            }

        case kernel @ RepeatBoundedKernel(symbol, pointer) =>
            if (kernel.derivable) SymbolDerivation(Set(Kernel(symbol.sym))) else EmptyDerivation
        case RepeatUnboundedKernel(symbol, pointer) =>
            SymbolDerivation(Set(Kernel(symbol.sym)))
    }

    def deriveAtomicKernel[T <: AtomicSymbol with Nonterm](grammar: Grammar, kernel: AtomicNontermKernel[T]): Derivation = {
        assert(kernel.pointer == 0)
        kernel match {
            case StartKernel(pointer) =>
                SymbolDerivation(Set(Kernel(grammar.startSymbol)))
            case NonterminalKernel(symbol, pointer) =>
                SymbolDerivation(grammar.rules(symbol.name) map { Kernel(_) })
            case OneOfKernel(symbol, pointer) =>
                SymbolDerivation(symbol.syms map { Kernel(_) })
            case ExceptKernel(symbol, pointer) =>
                TempLiftBlockableDerivation(Kernel(symbol.sym), Kernel(symbol.except))
            case LookaheadIsKernel(symbol, pointer) =>
                // not supported
                ???
            case LookaheadExceptKernel(symbol, pointer) =>
                RevertableDerivation(Kernel(Empty), Kernel(symbol.except))
            case BackupKernel(symbol, pointer) =>
                DeriveRevertableDerivation(Kernel(symbol.backup), Kernel(symbol.sym))
            case JoinKernel(symbol, pointer) =>
                JoinDerivation(Kernel(symbol.sym), Kernel(symbol.join))
            case ProxyKernel(symbol, pointer) =>
                SymbolDerivation(Set(Kernel(symbol.sym)))
            case LongestKernel(symbol, pointer) =>
                ReservedLiftTriggeredLiftRevertableDerivation(Kernel(symbol.sym))
            case EagerLongestKernel(symbol, pointer) =>
                ReservedAliveTriggeredLiftRevertableDerivation(Kernel(symbol.sym))
        }
    }
}
