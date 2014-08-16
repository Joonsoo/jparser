package com.giyeok.moonparser

import com.giyeok.moonparser.utils.SeqOrderedTester

trait SymbolProgresses extends IsNullable with SeqOrderedTester {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    case class AmbiguousParsingException(name: String) extends Exception
    case class NoDefinitionOfNonterminalException(name: String) extends Exception

    implicit class ShortStringSymbol(sym: Symbol) {
        // TODO improve short string
        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case FuncChar => "<func>"
            case ExactChar(c) => s"'$c'"
            case Chars(cs) => s"{${cs.toSeq.sorted map { c => s"'$c'" } mkString ","}}"
            case Unicode(c) => s"<unicode>"
            case EndOfFile => "<eof>"
            case s: Nonterminal => s.name
            case s: Sequence => s.seq map { _.toShortString } mkString " "
            case s: OneOf => s.syms map { _.toShortString } mkString "|"
            case s: Both => s"${s.sym.toShortString}&${s.also.toShortString}"
            case s: Except => s"${s.sym.toShortString}-${s.except.toShortString}"
            case s: Repeat => s"${s.sym.toShortString}[${s.range.toShortString}]"
            case s =>
                println(s)
                s.toString
        }
    }

    abstract class SymbolProgress {
        val parsed: Option[ParseNode[Symbol]]
        def canFinish = parsed.isDefined
        def toShortString: String
    }
    abstract class SymbolProgressTerminal extends SymbolProgress {
        def accept(next: Input): Boolean
        def proceedTerminal(next: Input): Option[SymbolProgress]
    }
    abstract class SymbolProgressNonterminal extends SymbolProgress {
        /*
         * `derive` and `lift` are opposite operations in a way
         * When the nodes created from `derive` are finished,
         * the finished nodes will be transferred to the origin node via `lift` method
         */
        val derive: Set[Edge]
        def lift(edge: LiftingEdge): Option[SymbolProgress]
    }

    object SymbolProgress {
        def apply(symbol: Symbol): SymbolProgress = symbol match {
            case symbol: Terminal => TerminalProgress(symbol, None)
            case Empty => EmptyProgress
            case symbol: Nonterminal => NonterminalProgress(symbol, None)
            case symbol: Sequence => SequenceProgress(symbol, List(), List())
            case symbol: OneOf => OneOfProgress(symbol, None)
            case symbol: Both => BothProgress(symbol, None)
            case symbol: Except => ExceptProgress(symbol, None)
            case symbol: LookaheadExcept => LookaheadExceptProgress(symbol, None)
            case symbol: Repeat => RepeatProgress(symbol, List())
            case symbol: Backup => BackupProgress(symbol, None)
        }
    }

    case object EmptyProgress extends SymbolProgress {
        val parsed = Some(ParsedEmpty)
        def toShortString = "ε"
    }

    def locate[T](parsed: Option[T], s: String) = if (parsed.isEmpty) "* " + s else s + " *"

    case class TerminalProgress(symbol: Terminal, parsed: Option[ParsedTerminal])
            extends SymbolProgressTerminal {
        def accept(next: Input) = (symbol accept next) ensuring (parsed.isEmpty)
        def proceedTerminal(next: Input) = {
            assert(parsed.isEmpty)
            if (symbol accept next) Some(TerminalProgress(symbol, Some(ParsedTerminal(symbol, next))))
            else None
        }
        def toShortString = locate(parsed, symbol.toShortString)
    }

    private def simple[A](edge: LiftingEdge)(f: ParseNode[Symbol] => A): A = {
        assert(edge.isInstanceOf[LiftingSimpleEdge])
        f(edge.asInstanceOf[LiftingSimpleEdge].parsed)
    }
    private def double[A](edge: LiftingEdge)(f: (ParseNode[Symbol], Option[ParseNode[Symbol]]) => A): A = {
        assert(edge.isInstanceOf[LiftingDoubleEdge])
        val e = edge.asInstanceOf[LiftingDoubleEdge]
        f(e.parsed, e.doub)
    }

    case class NonterminalProgress(symbol: Nonterminal, parsed: Option[ParsedSymbol[Nonterminal]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = {
            assert(parsed.isEmpty)
            simple(edge) { next =>
                // assuming grammar rules have a rule for symbol.name
                assert(grammar.rules(symbol.name) contains next.symbol)
                Some(NonterminalProgress(symbol, Some(ParsedSymbol[Nonterminal](symbol, next))))
            }
        }
        val derive: Set[Edge] =
            if (parsed.isEmpty) grammar.rules(symbol.name) map { s => SimpleEdge(this, SymbolProgress(s)) }
            else Set[Edge]()
        def toShortString = locate(parsed, symbol.toShortString)
    }

    case class SequenceProgress(symbol: Sequence, _childrenWS: List[ParseNode[Symbol]], _idxMapping: List[(Int, Int)])
            extends SymbolProgressNonterminal {
        // idxMapping: index of childrenWS(not in reversed order) -> index of children
        assert(_idxMapping map { _._1 } isStrictlyDecreasing)
        assert(_idxMapping map { _._2 } isStrictlyDecreasing)
        assert(_idxMapping map { _._1 } forall { i => i >= 0 && i < _childrenWS.size })

        private val locInSeq = if (_idxMapping isEmpty) 0 else _idxMapping.head._2
        assert(locInSeq < symbol.seq.size)
        private val visibles = {
            // TODO verify this
            def vis(loc: Int): Int = if (symbol.seq(loc).isNullable) vis(loc + 1) else loc
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
                val starting = MappingContext(_childrenWS, _childrenWS.size - 1, List(), locInSeq)
                _idxMapping.foldLeft(starting) { (context, idxMapping) =>
                    val (cwsPtr, cPtr) = idxMapping
                    val _cws = context._cws drop (context._cwsPtr - cwsPtr)
                    val _c = (0 until (context._cPtr - cPtr)).foldLeft(context._c) { (m, _) => ParsedEmpty +: m }
                    MappingContext(_cws.tail, cwsPtr - 1, _cws.head +: _c, cPtr - 1)
                }._c
            }
        }

        val parsed =
            if (locInSeq == symbol.seq.size) Some(ParsedSymbolsSeq[Sequence](symbol, children))
            else None
        def lift(edge: LiftingEdge) = {
            // TODO verify this
            simple(edge) { next =>
                def first(range: Seq[Int]): Option[Int] = {
                    if (range.isEmpty) None
                    else if (symbol.seq(range.head) == next.symbol) Some(range.head)
                    else first(range.tail)
                }
                first(locInSeq to visibles) match {
                    case Some(index) =>
                        Some(SequenceProgress(symbol, next +: _childrenWS, (_childrenWS.size, index) +: _idxMapping))
                    case None =>
                        // must be whitespace
                        assert(symbol.whitespace contains next.symbol)
                        Some(SequenceProgress(symbol, next +: _childrenWS, _idxMapping))
                }
            }
        }
        val derive: Set[Edge] =
            if (locInSeq < symbol.seq.size) {
                (symbol.whitespace ++ symbol.seq.slice(locInSeq, visibles + 1)) map { s => SimpleEdge(this, SymbolProgress(s)) }
            } else Set[Edge]()

        def toShortString = {
            val l = symbol.seq map { _.toShortString }
            val ls = l splitAt locInSeq
            val s = (ls._1 ++ ("*" +: ls._2))
            s mkString " "
        }
    }

    case class OneOfProgress(symbol: OneOf, parsed: Option[ParsedSymbol[OneOf]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = {
            assert(parsed.isEmpty)
            simple(edge) { next =>
                assert(symbol.syms contains next.symbol)
                Some(OneOfProgress(symbol, Some(ParsedSymbol[OneOf](symbol, next))))
            }
        }
        val derive: Set[Edge] =
            if (parsed.isEmpty) symbol.syms map { s => SimpleEdge(this, SymbolProgress(s)) }
            else Set[Edge]()
        def toShortString = locate(parsed, symbol.toShortString)
    }

    case class BothProgress(symbol: Both, parsed: Option[ParsedSymbol[Both]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = {
            assert(parsed.isEmpty)
            double(edge) { ((next, doub) =>
                if (doub.isDefined) Some(BothProgress(symbol, Some(ParsedSymbol[Both](symbol, next))))
                else None)
            }
        }
        val derive: Set[Edge] =
            if (parsed.isEmpty) Set(DoubleEdge(this, SymbolProgress(symbol.sym), SymbolProgress(symbol.also)))
            else Set[Edge]()
        def toShortString = locate(parsed, symbol.toShortString)
    }

    case class ExceptProgress(symbol: Except, parsed: Option[ParsedSymbol[Except]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = {
            assert(parsed.isEmpty)
            double(edge) { (next, doub) =>
                if (doub.isEmpty) Some(ExceptProgress(symbol, Some(ParsedSymbol[Except](symbol, next))))
                else None
            }
        }
        val derive: Set[Edge] =
            if (parsed.isEmpty) Set(DoubleEdge(this, SymbolProgress(symbol.sym), SymbolProgress(symbol.except)))
            else Set[Edge]()
        def toShortString = locate(parsed, symbol.toShortString)
    }

    case class RepeatProgress(symbol: Repeat, _children: List[ParseNode[Symbol]])
            extends SymbolProgressNonterminal {
        lazy val children = _children.reverse
        val parsed =
            if (symbol.range contains _children.size) Some(ParsedSymbolsSeq(symbol, children))
            else None
        def lift(edge: LiftingEdge) = {
            assert(symbol.range canProceed _children.size)
            simple(edge) { next =>
                assert(next.symbol == symbol.sym)
                Some(RepeatProgress(symbol, next +: _children))
            }
        }
        val derive: Set[Edge] =
            if (symbol.range canProceed _children.size) Set(SimpleEdge(this, SymbolProgress(symbol.sym)))
            else Set[Edge]()
        def toShortString = locate(parsed, symbol.toShortString)
    }

    case class LookaheadExceptProgress(symbol: LookaheadExcept, parsed: Option[ParsedSymbol[LookaheadExcept]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = ???
        val derive = ???
        def toShortString = ???
    }

    case class BackupProgress(symbol: Backup, parsed: Option[ParsedSymbol[Backup]])
            extends SymbolProgressNonterminal {
        def lift(edge: LiftingEdge) = ???
        val derive = ???
        def toShortString = ???
    }

}
