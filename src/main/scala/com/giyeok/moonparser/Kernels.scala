package com.giyeok.moonparser

trait Kernels {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    sealed trait Kernel {
        val symbol: Symbol
        val pointer: Int
        def toShortString = s"Kernel(${symbol.toShortString}, $pointer)"

        // TODO finishable인지 알아내는 메소드 필요
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

    // 기존에는 longest/eager_longest가 lift시 this -> lift된 노드로 가는 reverter를 만들어내는 방식으로 처리됐었는데,
    // 노드 자체에 속성을 부여해서 파서에서 처리하는 형태로 바꾸기로 함

    case object EmptyKernel extends Kernel {
        val symbol = Empty
        val pointer = 0
    }
    sealed trait NonEmptyKernel extends Kernel {
    }
    sealed trait AtomicKernel[T <: AtomicSymbol] extends NonEmptyKernel {
        assert(pointer == 0 || pointer == 1)
        def lifted(selfNode: SymbolProgress): Kernel
    }
    trait NontermKernel[T <: Nonterm] extends NonEmptyKernel {
        def derive: (Set[EdgeTmpl], Set[ReverterTmpl])
    }
    trait AtomicNontermKernel[T <: AtomicSymbol with Nonterm] extends NontermKernel[T] with AtomicKernel[T] {
        assert(pointer == 0 || pointer == 1)

        // def lifted: Self = Self(symbol, 1) ensuring pointer == 0

        override def derive: (Set[EdgeTmpl], Set[ReverterTmpl]) = if (pointer == 0) derive0 else (Set(), Set())
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl])
    }
    trait NonAtomicNontermKernel[T <: NonAtomicSymbol with Nonterm] extends NontermKernel[T] {
        val pointer: Int
        def lifted(before: ParsedSymbolsSeq1[T], accepted: ParseNode[Symbol]): (NonAtomicNontermKernel[T], ParsedSymbolsSeq1[T])
    }

    case class TerminalKernel(symbol: Terminal, pointer: Int) extends AtomicKernel[Terminal] {
        def lifted(selfNode: SymbolProgress) = TerminalKernel(symbol, 1) ensuring pointer == 0
    }
    case class NonterminalKernel(symbol: Nonterminal, pointer: Int) extends AtomicNontermKernel[Nonterminal] {
        def lifted(selfNode: SymbolProgress) = NonterminalKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (grammar.rules(symbol.name) map { s => SimpleEdgeTmpl(this, Kernel(s)) }, Set())
    }
    case class OneOfKernel(symbol: OneOf, pointer: Int) extends AtomicNontermKernel[OneOf] {
        def lifted(selfNode: SymbolProgress) = OneOfKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (symbol.syms map { s => SimpleEdgeTmpl(this, Kernel(s)) }, Set())
    }
    case class ExceptKernel(symbol: Except, pointer: Int) extends AtomicNontermKernel[Except] {
        def lifted(selfNode: SymbolProgress) = ExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edges: Set[EdgeTmpl] = Set(SimpleEdgeTmpl(this, Kernel(symbol.sym)))
            val reverters: Set[ReverterTmpl] = Set(LiftTriggeredTempLiftBlockTmpl(Kernel(symbol.except), this))
            (edges, reverters)
        }
    }
    case class LookaheadExceptKernel(symbol: LookaheadExcept, pointer: Int) extends AtomicNontermKernel[LookaheadExcept] {
        def lifted(selfNode: SymbolProgress) = LookaheadExceptKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edge = SimpleEdgeTmpl(this, Kernel(Empty))
            val reverters: Set[ReverterTmpl] = Set(LiftTriggeredDeriveReverterTmpl(Kernel(symbol.except), edge))
            (Set(edge), reverters)
        }
    }
    case class BackupKernel(symbol: Backup, pointer: Int) extends AtomicNontermKernel[Backup] {
        def lifted(selfNode: SymbolProgress) = BackupKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val symKernel = Kernel(symbol.sym)
            val backupEdge = SimpleEdgeTmpl(this, Kernel(symbol.backup))
            (Set(SimpleEdgeTmpl(this, symKernel), backupEdge), Set(LiftTriggeredDeriveReverterTmpl(symKernel, backupEdge)))
        }
    }
    case class JoinKernel(symbol: Join, pointer: Int) extends AtomicNontermKernel[Join] {
        def lifted(selfNode: SymbolProgress) = JoinKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) = {
            val edge1 = JoinEdgeTmpl(this, Kernel(symbol.sym), Kernel(symbol.join), false)
            val edge2 = JoinEdgeTmpl(this, Kernel(symbol.join), Kernel(symbol.sym), true)
            (Set(edge1, edge2), Set())
        }
    }
    case class ProxyKernel(symbol: Proxy, pointer: Int) extends AtomicNontermKernel[Proxy] {
        def lifted(selfNode: SymbolProgress) = ProxyKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())
    }
    // Longest와 EagerLongest는 생성되는 노드 자체가 lfit시 lift돼서 생긴 노드에 대해 Lift/SurviveTriggeredNodeCreationReverter 를 추가하도록 수정
    case class LongestKernel(symbol: Longest, pointer: Int) extends AtomicNontermKernel[Longest] {
        def lifted(selfNode: SymbolProgress) = LongestKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())
    }
    case class EagerLongestKernel(symbol: EagerLongest, pointer: Int) extends AtomicNontermKernel[EagerLongest] {
        def lifted(selfNode: SymbolProgress) = EagerLongestKernel(symbol, 1) ensuring pointer == 0
        def derive0: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())
    }

    case class SequenceKernel(symbol: Sequence, pointer: Int) extends NonAtomicNontermKernel[Sequence] {
        def lifted(before: ParsedSymbolsSeq1[Sequence], accepted: ParseNode[Symbol]): (SequenceKernel, ParsedSymbolsSeq1[Sequence]) = {
            val isContent = (accepted.symbol == symbol.seq(pointer))
            if (isContent) (SequenceKernel(symbol, pointer + 1), before.appendContent(accepted))
            else (SequenceKernel(symbol, pointer), before.appendWhitespace(accepted))
        }
        def derive: (Set[EdgeTmpl], Set[ReverterTmpl]) =
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
    }
    case class RepeatBoundedKernel(symbol: RepeatBounded, pointer: Int) extends NonAtomicNontermKernel[RepeatBounded] {
        // TODO
        def lifted(before: ParsedSymbolsSeq1[RepeatBounded], accepted: ParseNode[Symbol]): (RepeatBoundedKernel, ParsedSymbolsSeq1[RepeatBounded]) = ???
        def derive: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            if (pointer < symbol.upper) (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set()) else (Set(), Set())
    }
    case class RepeatUnboundedKernel(symbol: RepeatUnbounded, pointer: Int) extends NonAtomicNontermKernel[RepeatUnbounded] {
        // TODO
        def lifted(before: ParsedSymbolsSeq1[RepeatUnbounded], accepted: ParseNode[Symbol]): (RepeatUnboundedKernel, ParsedSymbolsSeq1[RepeatUnbounded]) = ???
        def derive: (Set[EdgeTmpl], Set[ReverterTmpl]) =
            (Set(SimpleEdgeTmpl(this, Kernel(symbol.sym))), Set())
    }
}
