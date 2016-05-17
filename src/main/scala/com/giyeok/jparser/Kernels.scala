package com.giyeok.jparser

import Symbols._
import Inputs._
import ParseTree._

// will be used in Parser and PreprocessedParser
object Kernels {
    sealed trait Kernel {
        val symbol: Symbol
        val pointer: Int
        // def toShortString = s"Kernel(${symbol.toShortString}, $pointer)"

        def derivable: Boolean
        def finishable: Boolean
    }
    object Kernel {
        def apply(symbol: Symbol): Kernel = symbol match {
            case Empty => EmptyKernel
            case Start => StartKernel(0)
            case symbol: Terminal => TerminalKernel(symbol, 0)
            case symbol: Nonterminal => NonterminalKernel(symbol, 0)
            case symbol: Sequence => SequenceKernel(symbol, 0)
            case symbol: OneOf => OneOfKernel(symbol, 0)
            case symbol: Except => ExceptKernel(symbol, 0)
            case symbol: LookaheadIs => LookaheadIsKernel(symbol, 0)
            case symbol: LookaheadExcept => LookaheadExceptKernel(symbol, 0)
            case symbol: RepeatBounded => RepeatBoundedKernel(symbol, 0)
            case symbol: RepeatUnbounded => RepeatUnboundedKernel(symbol, 0)
            case symbol: Backup => BackupKernel(symbol, 0)
            case symbol: Join => JoinKernel(symbol, 0)
            case symbol: Proxy => ProxyKernel(symbol, 0)
            case symbol: Longest => LongestKernel(symbol, 0)
            case symbol: EagerLongest => EagerLongestKernel(symbol, 0)
        }

        def allKernelsOf(symbol: Symbol): Set[Kernel] = symbol match {
            case Empty => Set(EmptyKernel)
            case symbol: AtomicSymbol =>
                symbol match {
                    case Start => Set(StartKernel(0), StartKernel(1))
                    case s: Terminal => Set(TerminalKernel(s, 0), TerminalKernel(s, 1))
                    case s: Nonterminal => Set(NonterminalKernel(s, 0), NonterminalKernel(s, 1))
                    case s: OneOf => Set(OneOfKernel(s, 0), OneOfKernel(s, 1))
                    case s: Except => Set(ExceptKernel(s, 0), ExceptKernel(s, 1))
                    case s: LookaheadIs => Set(LookaheadIsKernel(s, 0), LookaheadIsKernel(s, 1))
                    case s: LookaheadExcept => Set(LookaheadExceptKernel(s, 0), LookaheadExceptKernel(s, 1))
                    case s: Backup => Set(BackupKernel(s, 0), BackupKernel(s, 1))
                    case s: Join => Set(JoinKernel(s, 0), JoinKernel(s, 1))
                    case s: Proxy => Set(ProxyKernel(s, 0), ProxyKernel(s, 1))
                    case s: Longest => Set(LongestKernel(s, 0), LongestKernel(s, 1))
                    case s: EagerLongest => Set(EagerLongestKernel(s, 0), EagerLongestKernel(s, 1))
                }
            case s: Sequence => ((0 to s.seq.length) map { SequenceKernel(s, _) }).toSet
            case s: RepeatBounded => ((0 to s.upper) map { RepeatBoundedKernel(s, _) }).toSet
            case s: RepeatUnbounded => ((0 to s.lower) map { RepeatUnboundedKernel(s, _) }).toSet
        }
    }

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
    }
    sealed trait AtomicNontermKernel[+T <: AtomicSymbol with Nonterm] extends NontermKernel[T] with AtomicKernel[T] {
        assert(pointer == 0 || pointer == 1)

        val symbol: T

        def lifted: AtomicNontermKernel[T]
    }
    sealed trait NonAtomicNontermKernel[T <: NonAtomicSymbol with Nonterm] extends NontermKernel[T] {
        val symbol: T
        def lifted(before: ParsedSymbolsSeq[T], accepted: ParseNode[Symbol]): (NonAtomicNontermKernel[T], ParsedSymbolsSeq[T])
    }

    case class TerminalKernel(symbol: Terminal, pointer: Int) extends AtomicKernel[Terminal] {
        def lifted = TerminalKernel(symbol, 1) ensuring pointer == 0
    }
    case class StartKernel(pointer: Int) extends AtomicNontermKernel[Start.type] {
        assert(pointer == 0 || pointer == 1)
        val symbol = Start
        def lifted = StartKernel(1) ensuring pointer == 0
    }
    case class NonterminalKernel(symbol: Nonterminal, pointer: Int) extends AtomicNontermKernel[Nonterminal] {
        def lifted = NonterminalKernel(symbol, 1) ensuring pointer == 0
    }
    case class OneOfKernel(symbol: OneOf, pointer: Int) extends AtomicNontermKernel[OneOf] {
        def lifted = OneOfKernel(symbol, 1) ensuring pointer == 0
    }
    case class ExceptKernel(symbol: Except, pointer: Int) extends AtomicNontermKernel[Except] {
        def lifted = ExceptKernel(symbol, 1) ensuring pointer == 0
    }
    case class LookaheadIsKernel(symbol: LookaheadIs, pointer: Int) extends AtomicNontermKernel[LookaheadIs] {
        def lifted = LookaheadIsKernel(symbol, 1) ensuring pointer == 0
    }
    case class LookaheadExceptKernel(symbol: LookaheadExcept, pointer: Int) extends AtomicNontermKernel[LookaheadExcept] {
        def lifted = LookaheadExceptKernel(symbol, 1) ensuring pointer == 0
    }
    case class BackupKernel(symbol: Backup, pointer: Int) extends AtomicNontermKernel[Backup] {
        def lifted = BackupKernel(symbol, 1) ensuring pointer == 0
    }
    case class JoinKernel(symbol: Join, pointer: Int) extends AtomicNontermKernel[Join] {
        def lifted = JoinKernel(symbol, 1) ensuring pointer == 0
    }
    case class ProxyKernel(symbol: Proxy, pointer: Int) extends AtomicNontermKernel[Proxy] {
        def lifted = ProxyKernel(symbol, 1) ensuring pointer == 0
    }
    // Longest와 EagerLongest는 생성되는 노드 자체가 lfit시 lift돼서 생긴 노드에 대해 Lift/SurviveTriggeredNodeCreationReverter 를 추가하도록 수정
    case class LongestKernel(symbol: Longest, pointer: Int) extends AtomicNontermKernel[Longest] {
        def lifted = LongestKernel(symbol, 1) ensuring pointer == 0
    }
    case class EagerLongestKernel(symbol: EagerLongest, pointer: Int) extends AtomicNontermKernel[EagerLongest] {
        def lifted = EagerLongestKernel(symbol, 1) ensuring pointer == 0
    }

    case class SequenceKernel(symbol: Sequence, pointer: Int) extends NonAtomicNontermKernel[Sequence] {
        assert(0 <= pointer && pointer <= symbol.seq.size)
        def lifted(before: ParsedSymbolsSeq[Sequence], accepted: ParseNode[Symbol]): (SequenceKernel, ParsedSymbolsSeq[Sequence]) = {
            val isContent = (accepted.symbol == symbol.seq(pointer))
            if (isContent) (SequenceKernel(symbol, pointer + 1), before.appendContent(accepted))
            else (SequenceKernel(symbol, pointer), before.appendWhitespace(accepted))
        }

        val derivable = pointer < symbol.seq.size
        val finishable = pointer == symbol.seq.size
    }
    sealed trait RepeatKernel[T <: Repeat] extends NonAtomicNontermKernel[T]
    case class RepeatBoundedKernel(symbol: RepeatBounded, pointer: Int) extends RepeatKernel[RepeatBounded] {
        assert(0 <= pointer && pointer <= symbol.upper)
        def lifted(before: ParsedSymbolsSeq[RepeatBounded], accepted: ParseNode[Symbol]): (RepeatBoundedKernel, ParsedSymbolsSeq[RepeatBounded]) =
            (RepeatBoundedKernel(symbol, pointer + 1), before.appendContent(accepted))

        val derivable = pointer < symbol.upper
        val finishable = pointer >= symbol.lower
    }
    case class RepeatUnboundedKernel(symbol: RepeatUnbounded, pointer: Int) extends RepeatKernel[RepeatUnbounded] {
        assert(0 <= pointer && pointer <= symbol.lower)
        def lifted(before: ParsedSymbolsSeq[RepeatUnbounded], accepted: ParseNode[Symbol]): (RepeatUnboundedKernel, ParsedSymbolsSeq[RepeatUnbounded]) =
            (RepeatUnboundedKernel(symbol, if (pointer < symbol.lower) pointer + 1 else pointer), before.appendContent(accepted))

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
