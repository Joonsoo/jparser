package com.giyeok.moonparser

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

    sealed trait EdgeTmpl
    case class SimpleEdgeTmpl(start: Kernel, end: Kernel) extends EdgeTmpl
    case class JoinEdgeTmpl(start: Kernel, end: Kernel, join: Kernel, endJoinSwitched: Boolean) extends EdgeTmpl

    sealed trait ReverterTmpl
    // 1. 노드가 만들어지면서 동시에 만들어지는 reverter
    case class LiftTriggeredTempLiftBlockTmpl(trigger: Kernel, target: Kernel) extends ReverterTmpl
    case class LiftTriggeredDeriveReverterTmpl(trigger: Kernel, target: SimpleEdgeTmpl) extends ReverterTmpl
    // 이 reverter들에서는 trigger, target(엣지인 경우 엣지 시작점 도착점 모두)이 전부 같은 derivedGen

    // 2. 기존에는 longest/eager_longest가 lift시 (this -> lift된 노드)로 가는 reverter를 만들어내는 방식으로 처리됐었는데,
    //    이제 기존 동작을 나타내는 트리거를 derive시 반환하고 노드 자체에 속성을 부여해서 파서에서 처리하는 형태로 바꾸기로 함
    case class ReservedLiftTriggeredLiftedNodeReverterTmpl(trigger: Kernel) extends ReverterTmpl
    case class ReservedAliveTriggeredLiftedNodeReverterTmpl(trigger: Kernel) extends ReverterTmpl

    case object EmptyKernel extends Kernel {
        val symbol = Empty
        val pointer = 0
        val derivable = false
        val finishable = true
    }
    sealed trait NonEmptyKernel extends Kernel {
    }
    sealed trait AtomicKernel[T <: AtomicSymbol] extends NonEmptyKernel {
        assert(pointer == 0 || pointer == 1)
        def lifted: Kernel

        // def lifted: Self = Self(symbol, 1) ensuring pointer == 0

        val derivable = (pointer == 0)
        val finishable = (pointer == 1)
    }
    trait NontermKernel[T <: Nonterm] extends NonEmptyKernel {
        def derive(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl])
    }
    trait AtomicNontermKernel[T <: AtomicSymbol with Nonterm] extends NontermKernel[T] with AtomicKernel[T] {
        assert(pointer == 0 || pointer == 1)

        override def derive(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) = if (pointer == 0) derive0(grammar) else (Set(), Set())
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl])
    }
    trait NonAtomicNontermKernel[T <: NonAtomicSymbol with Nonterm] extends NontermKernel[T] {
        val pointer: Int
        def lifted(before: ParsedSymbolsSeq1[T], accepted: ParseNode[Symbol]): (NonAtomicNontermKernel[T], ParsedSymbolsSeq1[T])
    }

    case class TerminalKernel(symbol: Terminal, pointer: Int) extends AtomicKernel[Terminal] {
        def lifted = TerminalKernel(symbol, 1) ensuring pointer == 0
    }
    case class NonterminalKernel(symbol: Nonterminal, pointer: Int) extends AtomicNontermKernel[Nonterminal] {
        def lifted = NonterminalKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (grammar.rules(symbol.name) map { s => SimpleEdgeTmpl(this, Kernel(s)) }, Set())
    }
    case class OneOfKernel(symbol: OneOf, pointer: Int) extends AtomicNontermKernel[OneOf] {
        def lifted = OneOfKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (symbol.syms map { s => SimpleEdgeTmpl(this, Kernel(s)) }, Set())
    }
    case class ExceptKernel(symbol: Except, pointer: Int) extends AtomicNontermKernel[Except] {
        def lifted = ExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edges: Set[EdgeTmpl] = Set(SimpleEdgeTmpl(this, Kernel(symbol.sym)))
            val reverters: Set[ReverterTmpl] = Set(LiftTriggeredTempLiftBlockTmpl(Kernel(symbol.except), this))
            (edges, reverters)
        }
    }
    case class LookaheadExceptKernel(symbol: LookaheadExcept, pointer: Int) extends AtomicNontermKernel[LookaheadExcept] {
        def lifted = LookaheadExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edge = SimpleEdgeTmpl(this, Kernel(Empty))
            val reverters: Set[ReverterTmpl] = Set(LiftTriggeredDeriveReverterTmpl(Kernel(symbol.except), edge))
            (Set(edge), reverters)
        }
    }
    case class BackupKernel(symbol: Backup, pointer: Int) extends AtomicNontermKernel[Backup] {
        def lifted = BackupKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val symKernel = Kernel(symbol.sym)
            val backupEdge = SimpleEdgeTmpl(this, Kernel(symbol.backup))
            (Set(SimpleEdgeTmpl(this, symKernel), backupEdge), Set(LiftTriggeredDeriveReverterTmpl(symKernel, backupEdge)))
        }
    }
    case class JoinKernel(symbol: Join, pointer: Int) extends AtomicNontermKernel[Join] {
        def lifted = JoinKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edge1 = JoinEdgeTmpl(this, Kernel(symbol.sym), Kernel(symbol.join), false)
            val edge2 = JoinEdgeTmpl(this, Kernel(symbol.join), Kernel(symbol.sym), true)
            (Set(edge1, edge2), Set())
        }
    }
    case class ProxyKernel(symbol: Proxy, pointer: Int) extends AtomicNontermKernel[Proxy] {
        def lifted = ProxyKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())
    }
    // Longest와 EagerLongest는 생성되는 노드 자체가 lfit시 lift돼서 생긴 노드에 대해 Lift/SurviveTriggeredNodeCreationReverter 를 추가하도록 수정
    case class LongestKernel(symbol: Longest, pointer: Int) extends AtomicNontermKernel[Longest] {
        def lifted = LongestKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set(ReservedLiftTriggeredLiftedNodeReverterTmpl(this)))
    }
    case class EagerLongestKernel(symbol: EagerLongest, pointer: Int) extends AtomicNontermKernel[EagerLongest] {
        def lifted = EagerLongestKernel(symbol, 1) ensuring pointer == 0
        def derive0(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set(ReservedAliveTriggeredLiftedNodeReverterTmpl(this)))
    }

    case class SequenceKernel(symbol: Sequence, pointer: Int) extends NonAtomicNontermKernel[Sequence] {
        assert(0 <= pointer && pointer <= symbol.seq.size)
        def lifted(before: ParsedSymbolsSeq1[Sequence], accepted: ParseNode[Symbol]): (SequenceKernel, ParsedSymbolsSeq1[Sequence]) = {
            val isContent = (accepted.symbol == symbol.seq(pointer))
            if (isContent) (SequenceKernel(symbol, pointer + 1), before.appendContent(accepted))
            else (SequenceKernel(symbol, pointer), before.appendWhitespace(accepted))
        }
        def derive(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            if (pointer < symbol.seq.size) {
                val elemDerive = SimpleEdgeTmpl(this, Kernel(symbol.seq(pointer)))
                if (pointer > 0 && pointer < symbol.seq.size) {
                    // whitespace only between symbols
                    val wsDerives: Set[EdgeTmpl] = symbol.whitespace map { s => SimpleEdgeTmpl(this, Kernel(s)) }
                    ((wsDerives + elemDerive), Set())
                } else {
                    (Set(elemDerive), Set())
                }
            } else {
                (Set(), Set())
            }

        val derivable = pointer < symbol.seq.size
        val finishable = pointer == symbol.seq.size
    }
    case class RepeatBoundedKernel(symbol: RepeatBounded, pointer: Int) extends NonAtomicNontermKernel[RepeatBounded] {
        assert(0 <= pointer && pointer <= symbol.upper)
        def lifted(before: ParsedSymbolsSeq1[RepeatBounded], accepted: ParseNode[Symbol]): (RepeatBoundedKernel, ParsedSymbolsSeq1[RepeatBounded]) =
            (RepeatBoundedKernel(symbol, pointer + 1), before.appendContent(accepted))
        def derive(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            if (derivable) (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set()) else (Set(), Set())

        val derivable = pointer < symbol.upper
        val finishable = pointer >= symbol.lower
    }
    case class RepeatUnboundedKernel(symbol: RepeatUnbounded, pointer: Int) extends NonAtomicNontermKernel[RepeatUnbounded] {
        assert(0 <= pointer && pointer <= symbol.lower)
        def lifted(before: ParsedSymbolsSeq1[RepeatUnbounded], accepted: ParseNode[Symbol]): (RepeatUnboundedKernel, ParsedSymbolsSeq1[RepeatUnbounded]) =
            (RepeatUnboundedKernel(symbol, if (pointer < symbol.lower) pointer + 1 else pointer), before.appendContent(accepted))
        def derive(grammar: Grammar): (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())

        val derivable = true
        val finishable = pointer >= symbol.lower
    }
}
