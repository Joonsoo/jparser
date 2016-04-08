package com.giyeok.moonparser

import com.giyeok.moonparser.utils.SeqOrderedTester

trait SymbolProgresses extends SeqOrderedTester {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    abstract sealed class SymbolProgress {
        val symbol: Symbol
        val parsed: Option[ParseNode[Symbol]]
        def canFinish = parsed.isDefined

        val id = SymbolProgress.getId(this)
        def toShortString = this.toShortString1

        override def toString = toShortString
    }
    abstract sealed class SymbolProgressTerminal extends SymbolProgress {
        def proceedTerminal(next: Input): Option[SymbolProgressTerminal]
    }
    abstract sealed class SymbolProgressNonterminal extends SymbolProgress {
        override val symbol: Nonterm
        override val parsed: Option[ParseNode[Nonterm]]
        /*
         * `derive` and `lift` are opposite operations in a way
         * When the nodes created from `derive` are finished,
         * the finished nodes will be transferred to the origin node via `lift` method
         */
        def derive(gen: Int): Set[Edge]
        def lift(source: SymbolProgress): (Lifting, Set[Edge]) = (NontermLifting(this, lift0(source), source), Set())
        def lift0(source: SymbolProgress): SymbolProgressNonterminal
        val derivedGen: Int
    }
    case class EmptyProgress() extends SymbolProgress {
        val symbol = Empty
        val parsed = Some(ParsedEmpty(symbol))
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
            case symbol: Terminal => TerminalProgress(symbol, None, gen)
            case Empty => EmptyProgress()
            case symbol: Nonterminal => NonterminalProgress(symbol, None, gen)
            case symbol: Sequence => SequenceProgress(symbol, false, List(), List(), gen)
            case symbol: OneOf => OneOfProgress(symbol, None, gen)
            case symbol: Except => ExceptProgress(symbol, None, gen)
            case symbol: LookaheadExcept => LookaheadExceptProgress(symbol, gen)
            case symbol: Repeat => RepeatProgress(symbol, List(), gen)
            case symbol: Backup => BackupProgress(symbol, None, gen)
            case symbol: Join => JoinProgress(symbol, None, gen)
            case symbol: Join.Proxy => JoinProxyProgress(symbol, None, gen)
            case symbol: Longest => LongestProgress(symbol, None, gen)
        }
    }

    case class TerminalProgress(symbol: Terminal, parsed: Option[ParsedTerminal], derivedGen: Int)
            extends SymbolProgressTerminal {
        def proceedTerminal(next: Input) = {
            assert(parsed.isEmpty)
            if (symbol accept next) Some(TerminalProgress(symbol, Some(ParsedTerminal(symbol, next)), derivedGen))
            else None
        }
    }

    case class NonterminalProgress(symbol: Nonterminal, parsed: Option[ParsedSymbol[Nonterminal]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            // assuming grammar rules have a rule for symbol.name
            assert(grammar.rules(symbol.name) contains source.symbol)
            assert(source.parsed.isDefined)
            NonterminalProgress(symbol, Some(ParsedSymbol[Nonterminal](symbol, source.parsed.get)), derivedGen)
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) grammar.rules(symbol.name) map { s => SimpleEdge(this, SymbolProgress(s, gen)) }
            else Set[Edge]()
    }

    case class SequenceProgress(symbol: Sequence, wsAcceptable: Boolean, _childrenWS: List[ParseNode[Symbol]], _childrenIdx: List[Int], derivedGen: Int)
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
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            assert(source.parsed.isDefined)
            val next = source.parsed.get
            val _wsAcceptable = !(next.isInstanceOf[ParsedEmpty[_]]) // && wsAcceptable
            if (source.symbol == symbol.seq(locInSeq)) {
                SequenceProgress(symbol, _wsAcceptable, next +: _childrenWS, (_childrenWS.length) +: _childrenIdx, derivedGen)
            } else {
                // Whitespace
                SequenceProgress(symbol, _wsAcceptable, next +: _childrenWS, _childrenIdx, derivedGen)
            }
        }
        def derive(gen: Int): Set[Edge] =
            if (locInSeq < symbol.seq.size) {
                val elemDerive = SimpleEdge(this, SymbolProgress(symbol.seq(locInSeq), gen))
                if (wsAcceptable) {
                    val wsDerive = (symbol.whitespace map { s => SimpleEdge(this, SymbolProgress(s, gen)) })
                    (wsDerive + elemDerive).asInstanceOf[Set[Edge]]
                } else Set(elemDerive)
            } else Set[Edge]()
    }

    case class OneOfProgress(symbol: OneOf, parsed: Option[ParsedSymbol[OneOf]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            assert(symbol.syms contains source.symbol)
            assert(source.parsed.isDefined)
            OneOfProgress(symbol, Some(ParsedSymbol[OneOf](symbol, source.parsed.get)), derivedGen)
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) symbol.syms map { s => SimpleEdge(this, SymbolProgress(s, gen)) }
            else Set[Edge]()
    }

    case class RepeatProgress(symbol: Repeat, _children: List[ParseNode[Symbol]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        lazy val children = _children.reverse
        val parsed = if (symbol.range contains _children.size) { if (_children.size == 0) Some(ParsedEmpty(symbol)) else Some(ParsedSymbolsSeq(symbol, children, None)) } else None
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            assert(symbol.range canProceed _children.size)
            assert(source.parsed.isDefined)
            assert(source.symbol == symbol.sym)
            RepeatProgress(symbol, source.parsed.get +: _children, derivedGen)
        }
        def derive(gen: Int): Set[Edge] =
            if (symbol.range canProceed _children.size) Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen)))
            else Set[Edge]()
    }

    case class ExceptProgress(symbol: Except, parsed: Option[ParsedSymbol[Except]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            // assassin edge will take care of except progress
            assert(source.parsed.isDefined)
            assert(source.symbol == symbol.sym)
            ExceptProgress(symbol, Some(ParsedSymbol[Except](symbol, source.parsed.get)), derivedGen)
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) {
                Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen)),
                    LiftTriggeredLiftKillEdge(SymbolProgress(symbol.except, gen), this))
            } else Set[Edge]()
    }

    case class LookaheadExceptProgress(symbol: LookaheadExcept, derivedGen: Int) extends SymbolProgressNonterminal {
        val parsed: Option[ParseNode[Nonterm]] = Some(ParsedEmpty(symbol))
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            // this `lift` does not mean anything
            this
        }
        def derive(gen: Int) = Set(LiftTriggeredNodeKillEdge(SymbolProgress(symbol.except, gen), this))
    }

    case class BackupProgress(symbol: Backup, parsed: Option[ParsedSymbol[Backup]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = {
            println(this)
            println(source)
            BackupProgress(symbol, Some(ParsedSymbol[Backup](symbol, source.parsed.get)), derivedGen)
        }
        def derive(gen: Int) = if (!parsed.isEmpty) Set() else {
            val symP = SymbolProgress(symbol.sym, gen)
            val bkP = SymbolProgress(symbol.backup, gen)
            Set(
                SimpleEdge(this, symP),
                SimpleEdge(this, bkP),
                LiftTriggeredNodeKillEdge(symP, bkP))
        }
    }

    case class JoinProgress(symbol: Join, parsed: Option[ParsedSymbolJoin], derivedGen: Int) extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = ??? // should never be called
        def liftJoin(source: SymbolProgress, constraint: SymbolProgress): Lifting = {
            assert(source.parsed.isDefined)
            assert(constraint.parsed.isDefined)
            val after = JoinProgress(symbol, Some(ParsedSymbolJoin(symbol, source.parsed.get, constraint.parsed.get)), derivedGen)
            NontermLifting(this, after, source)
        }
        def derive(gen: Int) = if (!parsed.isEmpty) Set() else Set(
            JoinEdge(this, SymbolProgress(symbol.sym, gen), SymbolProgress(symbol.join, gen), false),
            JoinEdge(this, SymbolProgress(symbol.join, gen), SymbolProgress(symbol.sym, gen), true))
    }

    case class JoinProxyProgress(symbol: Join.Proxy, parsed: Option[ParsedSymbol[Join.Proxy]], derivedGen: Int) extends SymbolProgressNonterminal {
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = JoinProxyProgress(symbol, Some(ParsedSymbol[Join.Proxy](symbol, source.parsed.get)), derivedGen)
        def derive(gen: Int) = if (!parsed.isEmpty) Set() else Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen)))
    }

    case class LongestProgress(symbol: Longest, parsed: Option[ParsedSymbol[Longest]], derivedGen: Int) extends SymbolProgressNonterminal {
        override def lift(source: SymbolProgress): (Lifting, Set[Edge]) = {
            val lifted = lift0(source)
            (NontermLifting(this, lifted, source), Set(LiftTriggeredNodeKillEdge(this, lifted)))
        }
        def lift0(source: SymbolProgress): SymbolProgressNonterminal = LongestProgress(symbol, Some(ParsedSymbol[Longest](symbol, source.parsed.get)), derivedGen)
        def derive(gen: Int) = if (!parsed.isEmpty) Set() else Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen)))
    }

    implicit class ShortStringProgresses(prog: SymbolProgress) {
        def toShortString1: String = toShortString
        def toShortString: String = {
            def locate[T](parsed: Option[T], s: String) = if (parsed.isEmpty) ("* " + s) else (s + " *")
            prog.id + " " + (prog match {
                case EmptyProgress() => "ε *"
                case TerminalProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case NonterminalProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case seq: SequenceProgress =>
                    val ls: (Seq[String], Seq[String]) = seq.symbol.seq map { _.toShortString } splitAt seq.locInSeq
                    "(" + (((ls._1 ++ ("*" +: ls._2)) ++ (if ((!ls._2.isEmpty) && seq.canFinish) Seq("*") else Seq())) mkString " ") + ")"
                case OneOfProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case ExceptProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case rep @ RepeatProgress(symbol, _children, _) =>
                    (if (symbol.range canProceed _children.size) "* " else "") + symbol.toShortString + (if (rep.canFinish) " *" else "")
                case LookaheadExceptProgress(symbol, _) => symbol.toShortString
                case BackupProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case JoinProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case JoinProxyProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case LongestProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
            })
        }
    }
}
