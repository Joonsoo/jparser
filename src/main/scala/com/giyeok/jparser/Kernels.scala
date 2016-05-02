package com.giyeok.jparser

// will be used in Parser and PreprocessedParser
object Kernels {
    import Symbols._
    import Inputs._
    import ParseTree._

    sealed trait Kernel {
        val symbol: Symbol
        val pointer: Int
        def toShortString = s"Kernel(${symbol.toShortString}, $pointer)"

        def derivable: Boolean
        def finishable: Boolean
    }
    object Kernel {
        def apply(symbol: Symbol): Kernel = symbol match {
            case Empty => EmptyKernel
            case symbol: Terminal => TerminalKernel(symbol, 0)
            case symbol: Nonterminal => NonterminalKernel(symbol, 0)
            case symbol: Sequence => SequenceKernel(symbol, 0)
            case symbol: OneOf => OneOfKernel(symbol, 0)
            case symbol: Except => ExceptKernel(symbol, 0)
            case symbol: LookaheadExcept => LookaheadExceptKernel(symbol, 0)
            case symbol: RepeatBounded => RepeatBoundedKernel(symbol, 0)
            case symbol: RepeatUnbounded => RepeatUnboundedKernel(symbol, 0)
            case symbol: Backup => BackupKernel(symbol, 0)
            case symbol: Join => JoinKernel(symbol, 0)
            case symbol: Proxy => ProxyKernel(symbol, 0)
            case symbol: Longest => LongestKernel(symbol, 0)
            case symbol: EagerLongest => EagerLongestKernel(symbol, 0)
        }
    }

    sealed trait Derivation
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
    sealed trait ReservedLiftRevertableDerivation extends Derivation
    case class ReservedLiftTriggeredLiftRevertableDerivation(derive: Kernel) extends Derivation
    case class ReservedAliveTriggeredLiftRevertableDerivation(derive: Kernel) extends Derivation

    case object EmptyKernel extends Kernel {
        val symbol = Empty
        val pointer = 0
        val derivable = false
        val finishable = true
    }
    sealed trait NonEmptyKernel extends Kernel {
    }
    sealed trait AtomicKernel[+T <: AtomicSymbol] extends NonEmptyKernel {
        assert(pointer == 0 || pointer == 1)
        def lifted: AtomicKernel[T]

        // def lifted: Self = Self(symbol, 1) ensuring pointer == 0

        val derivable = (pointer == 0)
        val finishable = (pointer == 1)
    }
    sealed trait NontermKernel[+T <: Nonterm] extends NonEmptyKernel {
        val symbol: T

        def derive(grammar: Grammar): Derivation
    }
    sealed trait AtomicNontermKernel[+T <: AtomicSymbol with Nonterm] extends NontermKernel[T] with AtomicKernel[T] {
        assert(pointer == 0 || pointer == 1)

        val symbol: T

        override def derive(grammar: Grammar): Derivation = if (pointer == 0) derive0(grammar) else EmptyDerivation
        def derive0(grammar: Grammar): Derivation
        def lifted: AtomicNontermKernel[T]
    }
    sealed trait NonAtomicNontermKernel[T <: NonAtomicSymbol with Nonterm] extends NontermKernel[T] {
        val symbol: T
        def lifted(before: ParsedSymbolsSeq[T], accepted: ParseNode[Symbol]): (NonAtomicNontermKernel[T], ParsedSymbolsSeq[T])
    }

    case class TerminalKernel(symbol: Terminal, pointer: Int) extends AtomicKernel[Terminal] {
        def lifted = TerminalKernel(symbol, 1) ensuring pointer == 0
    }
    case class NonterminalKernel(symbol: Nonterminal, pointer: Int) extends AtomicNontermKernel[Nonterminal] {
        def lifted = NonterminalKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            SymbolDerivation(grammar.rules(symbol.name) map { Kernel(_) })
    }
    case class OneOfKernel(symbol: OneOf, pointer: Int) extends AtomicNontermKernel[OneOf] {
        def lifted = OneOfKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            SymbolDerivation(symbol.syms map { Kernel(_) })
    }
    case class ExceptKernel(symbol: Except, pointer: Int) extends AtomicNontermKernel[Except] {
        def lifted = ExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            TempLiftBlockableDerivation(Kernel(symbol.sym), Kernel(symbol.except))
    }
    case class LookaheadExceptKernel(symbol: LookaheadExcept, pointer: Int) extends AtomicNontermKernel[LookaheadExcept] {
        def lifted = LookaheadExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            RevertableDerivation(Kernel(Empty), Kernel(symbol.except))
    }
    case class BackupKernel(symbol: Backup, pointer: Int) extends AtomicNontermKernel[Backup] {
        def lifted = BackupKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            DeriveRevertableDerivation(Kernel(symbol.backup), Kernel(symbol.sym))
    }
    case class JoinKernel(symbol: Join, pointer: Int) extends AtomicNontermKernel[Join] {
        def lifted = JoinKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            JoinDerivation(Kernel(symbol.sym), Kernel(symbol.join))
    }
    case class ProxyKernel(symbol: Proxy, pointer: Int) extends AtomicNontermKernel[Proxy] {
        def lifted = ProxyKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            SymbolDerivation(Set(Kernel(symbol.sym)))
    }
    // Longest와 EagerLongest는 생성되는 노드 자체가 lfit시 lift돼서 생긴 노드에 대해 Lift/SurviveTriggeredNodeCreationReverter 를 추가하도록 수정
    case class LongestKernel(symbol: Longest, pointer: Int) extends AtomicNontermKernel[Longest] {
        def lifted = LongestKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            ReservedLiftTriggeredLiftRevertableDerivation(Kernel(symbol.sym))
    }
    case class EagerLongestKernel(symbol: EagerLongest, pointer: Int) extends AtomicNontermKernel[EagerLongest] {
        def lifted = EagerLongestKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): Derivation =
            ReservedAliveTriggeredLiftRevertableDerivation(Kernel(symbol.sym))
    }

    case class SequenceKernel(symbol: Sequence, pointer: Int) extends NonAtomicNontermKernel[Sequence] {
        assert(0 <= pointer && pointer <= symbol.seq.size)
        def lifted(before: ParsedSymbolsSeq[Sequence], accepted: ParseNode[Symbol]): (SequenceKernel, ParsedSymbolsSeq[Sequence]) = {
            val isContent = (accepted.symbol == symbol.seq(pointer))
            if (isContent) (SequenceKernel(symbol, pointer + 1), before.appendContent(accepted))
            else (SequenceKernel(symbol, pointer), before.appendWhitespace(accepted))
        }
        def derive(grammar: Grammar): Derivation =
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

        val derivable = pointer < symbol.seq.size
        val finishable = pointer == symbol.seq.size
    }
    trait RepeatKernel[T <: Repeat] extends NonAtomicNontermKernel[T]
    case class RepeatBoundedKernel(symbol: RepeatBounded, pointer: Int) extends RepeatKernel[RepeatBounded] {
        assert(0 <= pointer && pointer <= symbol.upper)
        def lifted(before: ParsedSymbolsSeq[RepeatBounded], accepted: ParseNode[Symbol]): (RepeatBoundedKernel, ParsedSymbolsSeq[RepeatBounded]) =
            (RepeatBoundedKernel(symbol, pointer + 1), before.appendContent(accepted))
        def derive(grammar: Grammar): Derivation =
            if (derivable) SymbolDerivation(Set(Kernel(symbol.sym))) else EmptyDerivation

        val derivable = pointer < symbol.upper
        val finishable = pointer >= symbol.lower
    }
    case class RepeatUnboundedKernel(symbol: RepeatUnbounded, pointer: Int) extends RepeatKernel[RepeatUnbounded] {
        assert(0 <= pointer && pointer <= symbol.lower)
        def lifted(before: ParsedSymbolsSeq[RepeatUnbounded], accepted: ParseNode[Symbol]): (RepeatUnboundedKernel, ParsedSymbolsSeq[RepeatUnbounded]) =
            (RepeatUnboundedKernel(symbol, if (pointer < symbol.lower) pointer + 1 else pointer), before.appendContent(accepted))
        def derive(grammar: Grammar): Derivation =
            SymbolDerivation(Set(Kernel(symbol.sym)))

        val derivable = true
        val finishable = pointer >= symbol.lower
    }

    implicit class ShortString(k: Kernel) {
        def toShortString: String = {
            (k match {
                case EmptyKernel => "ε *"
                case k: AtomicKernel[_] =>
                    (if (k.derivable) "* " else "") + (k.symbol.toShortString) + (if (k.finishable) " *" else "") ensuring (k.derivable != k.finishable)
                case SequenceKernel(symbol, pointer) =>
                    val (past, future) = symbol.seq.splitAt(pointer)
                    (((past map { _.toShortString }) :+ "*") ++ (future map { _.toShortString })) mkString " "
                case k: RepeatKernel[_] =>
                    (if (k.derivable) "* " else "") + (k.symbol.toShortString) + (if (k.finishable) " *" else "")
            })
        }
    }
}
