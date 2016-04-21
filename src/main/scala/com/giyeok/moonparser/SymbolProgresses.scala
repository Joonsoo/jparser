package com.giyeok.moonparser

trait SymbolProgresses {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    case class Kernel(symbol: Symbol, pointer: Int) {
        def toShortString = s"Kernel(${symbol.toShortString}, $pointer)"
    }

    abstract sealed class SymbolProgress {
        val symbol: Symbol
        val derivedGen: Int
        val lastLiftedGen: Option[Int]
        val parsed: Option[ParseNode[Symbol]]
        def canFinish = parsed.isDefined

        val kernel: Kernel

        val id = SymbolProgress.getId(this)
        def toShortString = this.toShortString1

        override def toString = toShortString
    }
    abstract sealed class SymbolProgressTerminal extends SymbolProgress {
        override val symbol: Terminal
        def proceedTerminal(gen: Int, next: Input): Option[SymbolProgressTerminal]
    }
    abstract sealed class SymbolProgressNonterminal extends SymbolProgress {
        override val symbol: Nonterm
        override val parsed: Option[ParseNode[Nonterm]]
        /*
         * `derive` and `lift` are opposite operations in a way
         * When the nodes created from `derive` are finished,
         * the finished nodes will be transferred to the origin node via `lift` method
         */
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter])
        def lift(gen: Int, source: SymbolProgress, edge: DeriveEdge): (Lifting, Set[PreReverter]) = (NontermLifting(this, lift0(gen, source), source, None, edge), Set())
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal
    }
    case class EmptyProgress(derivedGen: Int) extends SymbolProgress {
        val symbol = Empty
        val parsed = Some(ParsedEmpty(symbol))
        val lastLiftedGen: Option[Int] = Some(derivedGen)
        val kernel = Kernel(symbol, 0)
    }
    object SymbolProgress {
        private var cache: Map[SymbolProgress, Int] = Map()
        private var counter = 0
        def getId(sp: SymbolProgress): Int = {
            cache get sp match {
                case Some(i) => i
                case None =>
                    counter += 1
                    cache += ((sp, counter))
                    counter
            }
        }

        def apply(symbol: Symbol, gen: Int): SymbolProgress = symbol match {
            case symbol: Terminal => TerminalProgress(symbol, gen, None, None)
            case Empty => EmptyProgress(gen)
            case symbol: Nonterminal => NonterminalProgress(symbol, gen, None, None)
            case symbol: Sequence => SequenceProgress(symbol, gen, None, false, List(), List())
            case symbol: OneOf => OneOfProgress(symbol, gen, None, None)
            case symbol: Except => ExceptProgress(symbol, gen, None, None)
            case symbol: LookaheadExcept => LookaheadExceptProgress(symbol, gen, None, None)
            case symbol: Repeat => RepeatProgress(symbol, gen, None, List())
            case symbol: Backup => BackupProgress(symbol, gen, None, None)
            case symbol: Join => JoinProgress(symbol, gen, None, None)
            case symbol: Proxy => ProxyProgress(symbol, gen, None, None)
            case symbol: Longest => LongestProgress(symbol, gen, None, None)
            case symbol: EagerLongest => EagerLongestProgress(symbol, gen, None, None)
        }
    }

    case class TerminalProgress(symbol: Terminal, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedTerminal])
            extends SymbolProgressTerminal {
        def proceedTerminal(gen: Int, next: Input) = {
            assert(parsed.isEmpty)
            if (symbol accept next) Some(TerminalProgress(symbol, derivedGen, Some(gen), Some(ParsedTerminal(symbol, next))))
            else None
        }
        val kernel = Kernel(symbol, if (parsed.isDefined) 1 else 0)
    }

    case class NonterminalProgress(symbol: Nonterminal, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[Nonterminal]])
            extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            // assuming grammar rules have a rule for symbol.name
            assert(grammar.rules(symbol.name) contains source.symbol)
            assert(source.parsed.isDefined)
            NonterminalProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[Nonterminal](symbol, source.parsed.get)))
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (parsed.isEmpty) (grammar.rules(symbol.name) map { s => SimpleEdge(this, SymbolProgress(s, gen)) }, Set())
            else (Set(), Set())

        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class SequenceProgress(symbol: Sequence, derivedGen: Int, lastLiftedGen: Option[Int], wsAcceptable: Boolean, _childrenWS: List[ParseNode[Symbol]], _childrenIdx: List[Int])
            extends SymbolProgressNonterminal {
        // childrenIdx: index of childrenWS
        // _childrenIdx: reverse of chidlrenIdx
        assert(_childrenIdx.size <= symbol.seq.size)

        val locInSeq = _childrenIdx.length

        // childrenWS: all children with whitespace
        // children: children with ParseEmpty
        lazy val childrenIdx = _childrenIdx.reverse
        lazy val childrenWS = _childrenWS.reverse
        lazy val children: List[ParseNode[Symbol]] = {
            def pick(_childrenIdx: List[Int], _childrenWS: List[ParseNode[Symbol]], current: Int, cc: List[ParseNode[Symbol]]): List[ParseNode[Symbol]] =
                if (_childrenIdx.isEmpty) cc else {
                    val dropped = _childrenWS drop (current - _childrenIdx.head)
                    pick(_childrenIdx.tail, dropped.tail, _childrenIdx.head - 1, dropped.head +: cc)
                }
            pick(_childrenIdx, _childrenWS, _childrenWS.length - 1, List())
        }

        override def canFinish = (locInSeq == symbol.seq.size)
        val parsed = if (canFinish) Some(ParsedSymbolsSeq[Sequence](symbol, children, Some((childrenWS, childrenIdx)))) else None
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            assert(source.parsed.isDefined)
            val next = source.parsed.get
            val _wsAcceptable = !(next.isInstanceOf[ParsedEmpty[_]]) // && wsAcceptable
            if (source.symbol == symbol.seq(locInSeq)) {
                SequenceProgress(symbol, derivedGen, Some(gen), _wsAcceptable, next +: _childrenWS, (_childrenWS.length) +: _childrenIdx)
            } else {
                // Whitespace
                SequenceProgress(symbol, derivedGen, Some(gen), _wsAcceptable, next +: _childrenWS, _childrenIdx)
            }
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (locInSeq < symbol.seq.size) {
                val elemDerive = SimpleEdge(this, SymbolProgress(symbol.seq(locInSeq), gen))
                if (wsAcceptable) {
                    val wsDerive = (symbol.whitespace map { s => SimpleEdge(this, SymbolProgress(s, gen)) })
                    ((wsDerive + elemDerive).asInstanceOf[Set[DeriveEdge]], Set())
                } else (Set(elemDerive), Set())
            } else (Set(), Set())

        val kernel = Kernel(symbol, locInSeq)
    }

    case class OneOfProgress(symbol: OneOf, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[OneOf]])
            extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            assert(symbol.syms contains source.symbol)
            assert(source.parsed.isDefined)
            OneOfProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[OneOf](symbol, source.parsed.get)))
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (parsed.isEmpty) (symbol.syms map { s => SimpleEdge(this, SymbolProgress(s, gen)) }, Set())
            else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class RepeatProgress(symbol: Repeat, derivedGen: Int, lastLiftedGen: Option[Int], _children: List[ParseNode[Symbol]])
            extends SymbolProgressNonterminal {
        lazy val children = _children.reverse
        val canDerive = symbol.range canProceed _children.size
        val parsed = if (symbol.range contains _children.size) { if (_children.size == 0) Some(ParsedEmpty(symbol)) else Some(ParsedSymbolsSeq(symbol, children, None)) } else None
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            assert(symbol.range canProceed _children.size)
            assert(source.parsed.isDefined)
            assert(source.symbol == symbol.sym)
            RepeatProgress(symbol, derivedGen, Some(gen), source.parsed.get +: _children)
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (canDerive) (Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen))), Set())
            else (Set(), Set())

        val kernel = {
            val kernelPointer = if (symbol.range.upperBounded) {
                _children.size
            } else {
                (canDerive, canFinish) match {
                    case (true, false) => 0
                    case (true, true) => 1
                    case (false, true) => 2
                }
            }
            Kernel(symbol, kernelPointer)
        }
    }

    case class ExceptProgress(symbol: Except, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[Except]])
            extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            // assassin edge will take care of except progress
            assert(source.parsed.isDefined)
            assert(source.symbol == symbol.sym)
            ExceptProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[Except](symbol, source.parsed.get)))
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (parsed.isEmpty) {
                (Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen))),
                    Set(LiftTriggeredTemporaryLiftBlockReverter(SymbolProgress(symbol.except, gen), this)))
            } else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class LookaheadExceptProgress(symbol: LookaheadExcept, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedEmpty[LookaheadExcept]]) extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            assert(source.isInstanceOf[EmptyProgress])
            LookaheadExceptProgress(symbol, derivedGen, Some(gen), Some(ParsedEmpty[LookaheadExcept](symbol)))
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (parsed.isEmpty) {
                val edge = SimpleEdge(this, SymbolProgress(Empty, gen))
                (Set(edge), Set(LiftTriggeredDeriveReverter(SymbolProgress(symbol.except, gen), edge)))
            } else (Set(), Set())
        // derive는 그냥 SimpleEdge만 주고, lift에서 LiftTriggeredLiftReverter를 줘도 동일 효과가 날듯
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class ProxyProgress(symbol: Proxy, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[Proxy]]) extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = ProxyProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[Proxy](symbol, source.parsed.get)))
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (!parsed.isEmpty) (Set(), Set())
            else (Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen))), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class BackupProgress(symbol: Backup, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[Backup]])
            extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = {
            BackupProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[Backup](symbol, source.parsed.get)))
        }
        def derive(gen: Int): (Set[DeriveEdge], Set[PreReverter]) =
            if (parsed.isEmpty) {
                val symP = SymbolProgress(symbol.sym, gen)
                val bkEdge = SimpleEdge(this, SymbolProgress(symbol.backup, gen))
                (Set(SimpleEdge(this, symP), bkEdge), Set(LiftTriggeredDeriveReverter(symP, bkEdge)))
            } else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class JoinProgress(symbol: Join, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbolJoin]) extends SymbolProgressNonterminal {
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = ??? // should never be called
        def liftJoin(gen: Int, source: SymbolProgress, constraint: SymbolProgress, edge: JoinEdge): Lifting = {
            assert(source.parsed.isDefined)
            assert(constraint.parsed.isDefined)
            val after = JoinProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbolJoin(symbol, source.parsed.get, constraint.parsed.get)))
            NontermLifting(this, after, source, Some(constraint), edge)
        }
        def derive(gen: Int) =
            if (parsed.isEmpty) (Set(
                JoinEdge(this, SymbolProgress(symbol.sym, gen), SymbolProgress(symbol.join, gen), false),
                JoinEdge(this, SymbolProgress(symbol.join, gen), SymbolProgress(symbol.sym, gen), true)), Set())
            else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class LongestProgress(symbol: Longest, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[Longest]]) extends SymbolProgressNonterminal {
        override def lift(gen: Int, source: SymbolProgress, edge: DeriveEdge): (Lifting, Set[PreReverter]) = {
            val lifting = NontermLifting(this, lift0(gen, source), source, None, edge)
            (lifting, Set(LiftTriggeredLiftReverter(this, lifting)))
        }
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = LongestProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[Longest](symbol, source.parsed.get)))
        def derive(gen: Int) = if (parsed.isEmpty) (Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen))), Set()) else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    case class EagerLongestProgress(symbol: EagerLongest, derivedGen: Int, lastLiftedGen: Option[Int], parsed: Option[ParsedSymbol[EagerLongest]]) extends SymbolProgressNonterminal {
        override def lift(gen: Int, source: SymbolProgress, edge: DeriveEdge): (Lifting, Set[PreReverter]) = {
            val lifting = NontermLifting(this, lift0(gen, source), source, None, edge)
            (lifting, Set(AliveTriggeredLiftReverter(this, lifting)))
        }
        def lift0(gen: Int, source: SymbolProgress): SymbolProgressNonterminal = EagerLongestProgress(symbol, derivedGen, Some(gen), Some(ParsedSymbol[EagerLongest](symbol, source.parsed.get)))
        def derive(gen: Int) = if (parsed.isEmpty) (Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen))), Set()) else (Set(), Set())
        val kernel = Kernel(symbol, if (canFinish) 1 else 0)
    }

    implicit class ShortStringProgresses(prog: SymbolProgress) {
        def toShortString1: String = toShortString
        def toShortString: String = {
            def locate[T](parsed: Option[T], s: String) = if (parsed.isEmpty) ("* " + s) else (s + " *")
            prog.id + " " + (prog match {
                case EmptyProgress(_) => "ε *"
                case TerminalProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case NonterminalProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case seq: SequenceProgress =>
                    val ls: (Seq[String], Seq[String]) = seq.symbol.seq map { _.toShortString } splitAt seq.locInSeq
                    "(" + (((ls._1 ++ ("*" +: ls._2)) ++ (if ((!ls._2.isEmpty) && seq.canFinish) Seq("*") else Seq())) mkString " ") + ")"
                case OneOfProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case ExceptProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case rep @ RepeatProgress(symbol, _, _, _children) =>
                    (if (symbol.range canProceed _children.size) "* " else "") + symbol.toShortString + (if (rep.canFinish) " *" else "")
                case LookaheadExceptProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case ProxyProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case BackupProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case JoinProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case LongestProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
                case EagerLongestProgress(symbol, _, _, parsed) => locate(parsed, symbol.toShortString)
            })
        }
    }
}
