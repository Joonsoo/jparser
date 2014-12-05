package com.giyeok.moonparser

import com.giyeok.moonparser.utils.SeqOrderedTester

trait SymbolProgresses extends IsNullable with SeqOrderedTester {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    case class AmbiguousParsingException(name: String) extends Exception
    case class NoDefinitionOfNonterminalException(name: String) extends Exception

    abstract sealed class SymbolProgress {
        val symbol: Symbol
        val parsed: Option[ParseNode[Symbol]]
        def canFinish = parsed.isDefined

        def toShortString = this.toShortString1
    }
    abstract sealed class SymbolProgressTerminal extends SymbolProgress {
        def proceedTerminal(next: Input): Option[SymbolProgressTerminal]
    }
    abstract sealed class SymbolProgressNonterminal extends SymbolProgress {
        /*
         * `derive` and `lift` are opposite operations in a way
         * When the nodes created from `derive` are finished,
         * the finished nodes will be transferred to the origin node via `lift` method
         */
        def derive(gen: Int): Set[Edge]
        def lift(source: SymbolProgress): Option[SymbolProgress]
        val derivedGen: Int
    }
    case object EmptyProgress extends SymbolProgress {
        val symbol = Empty
        val parsed = Some(ParsedEmpty(symbol))
    }

    object SymbolProgress {
        def apply(symbol: Symbol, gen: Int): SymbolProgress = symbol match {
            case symbol: Terminal => TerminalProgress(symbol, None)
            case Empty => EmptyProgress
            case symbol: Nonterminal => NonterminalProgress(symbol, None, gen)
            case symbol: Sequence => SequenceProgress(symbol, List(), List(), gen)
            case symbol: OneOf => OneOfProgress(symbol, None, gen)
            case symbol: Except => ExceptProgress(symbol, None, gen)
            case symbol: LookaheadExcept => LookaheadExceptProgress(symbol, None, gen)
            case symbol: Repeat => RepeatProgress(symbol, List(), gen)
            case symbol: Backup => BackupProgress(symbol, None, gen)
        }
    }

    case class TerminalProgress(symbol: Terminal, parsed: Option[ParsedTerminal])
            extends SymbolProgressTerminal {
        def proceedTerminal(next: Input) = {
            assert(parsed.isEmpty)
            if (symbol accept next) Some(TerminalProgress(symbol, Some(ParsedTerminal(symbol, next))))
            else None
        }
    }

    case class NonterminalProgress(symbol: Nonterminal, parsed: Option[ParsedSymbol[Nonterminal]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift(source: SymbolProgress): Option[SymbolProgress] = {
            // assuming grammar rules have a rule for symbol.name
            assert(grammar.rules(symbol.name) contains source.symbol)
            assert(source.parsed.isDefined)
            Some(NonterminalProgress(symbol, Some(ParsedSymbol[Nonterminal](symbol, source.parsed.get)), derivedGen))
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) grammar.rules(symbol.name) map { s => SimpleEdge(this, SymbolProgress(s, gen)) }
            else Set[Edge]()
    }

    case class SequenceProgress(symbol: Sequence, _childrenWS: List[ParseNode[Symbol]], _idxMapping: List[(Int, Int)], derivedGen: Int)
            extends SymbolProgressNonterminal {
        // idxMapping: index of childrenWS(not in reversed order) -> index of children
        assert(_idxMapping map { _._1 } isStrictlyDecreasing)
        assert(_idxMapping map { _._2 } isStrictlyDecreasing)
        assert(_idxMapping map { _._1 } forall { i => i >= 0 && i < _childrenWS.size })

        val locInSeq = if (_idxMapping isEmpty) 0 else (_idxMapping.head._2 + 1)
        assert(locInSeq <= symbol.seq.size)
        private val visibles = {
            // return the index of the first non-nullable symbol or the last symbol of sequence
            def vis(loc: Int): Int =
                if (loc < symbol.seq.length) (if (symbol.seq(loc).isNullable) vis(loc + 1) else loc)
                else (loc - 1)
            vis(locInSeq)
        }

        // childrenWS: all children with whitespace (but without ParseEmpty)
        // children: children without whitespace (but with ParseEmpty)
        lazy val idxMapping: Map[Int, Int] = _idxMapping.toMap
        lazy val childrenWS = _childrenWS.reverse
        lazy val children: List[ParseNode[Symbol]] = {
            // TODO verify this
            if (_idxMapping.isEmpty) List() else {
                case class MappingContext(_cws: List[ParseNode[Symbol]], _cwsPtr: Int, _c: List[ParseNode[Symbol]], _cPtr: Int)
                val starting = MappingContext(_childrenWS, _childrenWS.size - 1, List(), symbol.seq.length - 1 /*_idxMapping.head._2*/ )
                val result0 = _idxMapping.foldLeft(starting) { (context, idxMapping) =>
                    val (cwsPtr, cPtr) = idxMapping
                    val _cws = context._cws drop (context._cwsPtr - cwsPtr)
                    val _c = (0 until (context._cPtr - cPtr)).foldLeft(context._c) { (m, i) => ParsedEmpty(symbol.seq(context._cPtr - i)) +: m }
                    MappingContext(_cws.tail, cwsPtr - 1, _cws.head +: _c, cPtr - 1)
                }._c
                val result = (symbol.seq take (symbol.seq.size - result0.size)).foldRight(result0) { ParsedEmpty(_) +: _ }
                result ensuring (!canFinish || (result.size == symbol.seq.size))
            }
        }

        override def canFinish =
            (locInSeq == symbol.seq.size) || ((visibles == symbol.seq.length - 1) && symbol.seq(visibles).isNullable)
        val parsed = if (canFinish) Some(ParsedSymbolsSeq[Sequence](symbol, children)) else None
        def lift(source: SymbolProgress): Option[SymbolProgress] = {
            assert(source.parsed.isDefined)
            // assert(derive map { _.to.symbol } contains source.symbol)
            // TODO verify this
            def first(range: Seq[Int]): Option[Int] = {
                if (range.isEmpty) None
                else if (symbol.seq(range.head) == source.symbol) Some(range.head)
                else first(range.tail)
            }
            val next = source.parsed.get
            first(locInSeq to visibles) match {
                case Some(index) =>
                    Some(SequenceProgress(symbol, next +: _childrenWS, (_childrenWS.size, index) +: _idxMapping, derivedGen))
                case None =>
                    // must be whitespace
                    assert(symbol.whitespace contains next.symbol)
                    Some(SequenceProgress(symbol, next +: _childrenWS, _idxMapping, derivedGen))
            }
        }
        def derive(gen: Int): Set[Edge] =
            if (locInSeq < symbol.seq.size) {
                (symbol.whitespace ++ symbol.seq.slice(locInSeq, visibles + 1)) map { s => SimpleEdge(this, SymbolProgress(s, gen)) }
            } else Set[Edge]()
    }

    case class OneOfProgress(symbol: OneOf, parsed: Option[ParsedSymbol[OneOf]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift(source: SymbolProgress): Option[SymbolProgress] = {
            assert(symbol.syms contains source.symbol)
            assert(source.parsed.isDefined)
            Some(OneOfProgress(symbol, Some(ParsedSymbol[OneOf](symbol, source.parsed.get)), derivedGen))
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) symbol.syms map { s => SimpleEdge(this, SymbolProgress(s, gen)) }
            else Set[Edge]()
    }

    case class ExceptProgress(symbol: Except, parsed: Option[ParsedSymbol[Except]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift(source: SymbolProgress): Option[SymbolProgress] = {
            // assassin edge will take care of except progress
            assert(source.parsed.isDefined)
            Some(ExceptProgress(symbol, Some(ParsedSymbol[Except](symbol, source.parsed.get)), derivedGen))
        }
        def derive(gen: Int): Set[Edge] =
            if (parsed.isEmpty) Set(
                SimpleEdge(this, SymbolProgress(symbol.sym, gen)),
                AssassinEdge(SymbolProgress(symbol.except, gen), this))
            else Set[Edge]()
    }

    case class RepeatProgress(symbol: Repeat, _children: List[ParseNode[Symbol]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        lazy val children = _children.reverse
        val parsed = if (symbol.range contains _children.size) Some(ParsedSymbolsSeq(symbol, children)) else None
        def lift(source: SymbolProgress): Option[SymbolProgress] = {
            assert(symbol.range canProceed _children.size)
            assert(source.parsed.isDefined)
            assert(source.symbol == symbol.sym)
            Some(RepeatProgress(symbol, source.parsed.get +: _children, derivedGen))
        }
        def derive(gen: Int): Set[Edge] =
            if (symbol.range canProceed _children.size) Set(SimpleEdge(this, SymbolProgress(symbol.sym, gen)))
            else Set[Edge]()
    }

    case class LookaheadExceptProgress(symbol: LookaheadExcept, parsed: Option[ParsedSymbol[LookaheadExcept]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift(source: SymbolProgress): Option[SymbolProgress] = ???
        def derive(gen: Int) = ???
    }

    case class BackupProgress(symbol: Backup, parsed: Option[ParsedSymbol[Backup]], derivedGen: Int)
            extends SymbolProgressNonterminal {
        def lift(source: SymbolProgress): Option[SymbolProgress] = ???
        def derive(gen: Int) = ???
    }

    implicit class ShortStringProgresses(prog: SymbolProgress) {
        def toShortString1: String = toShortString
        def toShortString: String = {
            def locate[T](parsed: Option[T], s: String) = if (parsed.isEmpty) ("* " + s) else (s + " *")
            prog match {
                case EmptyProgress => "ε *"
                case TerminalProgress(symbol, parsed) => locate(parsed, symbol.toShortString)
                case NonterminalProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case seq: SequenceProgress =>
                    val ls = seq.symbol.seq map { _.toShortString } splitAt seq.locInSeq
                    "(" + ((ls._1 ++ ("*" +: ls._2)) mkString " ") + ")"
                case OneOfProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case ExceptProgress(symbol, parsed, _) => locate(parsed, symbol.toShortString)
                case rep @ RepeatProgress(symbol, _children, _) =>
                    (if (symbol.range canProceed _children.size) "* " else "") + symbol.toShortString + (if (rep.canFinish) " *" else "")
                case LookaheadExceptProgress(symbol, parsed, _) => ???
                case BackupProgress(symbol, parsed, _) => ???
            }
        }
    }
}
