package com.giyeok.moonparser

trait SymbolProgresses {
    this: Parser =>

    import Symbols._
    import Inputs._
    import ParseTree._

    case class AmbiguousParsingException extends Exception
    case class NoDefinitionOfNonterminalException(name: String) extends Exception

    abstract class SymbolProgress {
        val parsed: Option[ParseNode[Symbol]]
        def canFinish = parsed.isDefined
    }
    abstract class SymbolProgressTerminal extends SymbolProgress {
        def accept(next: Input): Boolean
        def proceedTerminal(next: Input): Option[SymbolProgress]
    }
    abstract class SymbolProgressNonterminal extends SymbolProgress {
        /*
         * `proceed` and `derive` are some kind of opposite operation
         * `proceed` will only receive `SymbolProgress` returned by `derive` method
         * Thus, in general, proceed should not return None.
         * But some special SymbolProgresses such as LookaheadExcept and Except may return None
         * If proceed returns None, that node will be deleted from the next generation,
         * so if you want to check the future progress return Some(this)
         */
        def proceed(references: Set[ParseNode[Symbol]]): Option[SymbolProgress]
        val derive: Set[(SymbolProgress, EdgeKind.Value)]
    }

    object SymbolProgress {
        def apply(symbol: Symbol): SymbolProgress = symbol match {
            case symbol: Terminal => TerminalProgress(symbol, None)
            case Empty => EmptyProgress
            case symbol: Nonterminal => NonterminalProgress(symbol, None)
            case symbol: Sequence => SequenceProgress(symbol, List(), List())
            case symbol: OneOf => OneOfProgress(symbol, None)
            case symbol: Conjunction => ConjunctionProgress(symbol, None)
            case symbol: Except => ExceptProgress(symbol, None)
            case symbol: LookaheadExcept => LookaheadProgress(symbol, None)
            case symbol: Repeat => RepeatProgress(symbol, List())
            case symbol: Backup => BackupProgress(symbol, None)
        }
    }

    case object EmptyProgress extends SymbolProgress {
        val parsed = Some(ParsedEmpty)
    }

    case class TerminalProgress(symbol: Terminal, parsed: Option[ParsedTerminal])
            extends SymbolProgressTerminal {
        def accept(next: Input) = (symbol accept next) ensuring (parsed.isEmpty)
        def proceedTerminal(next: Input) = {
            assert(parsed.isEmpty)
            if (symbol accept next) Some(TerminalProgress(symbol, Some(ParsedTerminal(symbol, next))))
            else None
        }
    }

    private def only[A, B](set: Set[A])(block: A => B): B = {
        assert(!set.isEmpty)
        if (set.size > 1) throw new AmbiguousParsingException
        else {
            assert(set.size == 1)
            block(set.head)
        }
    }

    case class NonterminalProgress(symbol: Nonterminal, parsed: Option[ParsedSymbol[Nonterminal]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(parsed.isEmpty)
            only(references) { next =>
                // assuming grammar rules have a rule for symbol.name
                val rhs = grammar.rules(symbol.name)
                assert(rhs contains next.symbol)
                Some(NonterminalProgress(symbol, Some(ParsedSymbol[Nonterminal](symbol, next))))
            }
        }
        val derive =
            if (parsed.isEmpty) grammar.rules(symbol.name) map { s => (SymbolProgress(s), EdgeKind.Derive) }
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class SequenceProgress(symbol: Sequence, _children: List[ParseNode[Symbol]], _childrenWS: List[ParseNode[Symbol]])
            extends SymbolProgressNonterminal {
        // children: children without whitespace
        // childrenWS: all children with whitespace
        lazy val children = _children.reverse
        lazy val childrenWS = _childrenWS.reverse

        private val locInSeq = _children.size
        val parsed =
            if (locInSeq == symbol.seq.size) Some(ParsedSymbolsSeq[Sequence](symbol, children))
            else None
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(locInSeq < symbol.seq.size)
            Some(references find { _.symbol == symbol.seq(locInSeq) } match {
                case Some(body) =>
                    SequenceProgress(symbol, body +: _children, body +: _childrenWS)
                case None =>
                    // references may have more than one item - does it mean that the grammar is ambiguous?
                    only(references) { next =>
                        assert(symbol.whitespace contains next.symbol)
                        SequenceProgress(symbol, _children, next +: _childrenWS)
                    }
            })
        }
        val derive =
            if (locInSeq < symbol.seq.size)
                (symbol.whitespace + symbol.seq(locInSeq)) map { s => (SymbolProgress(s), EdgeKind.Derive) }
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class OneOfProgress(symbol: OneOf, parsed: Option[ParsedSymbol[OneOf]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(parsed.isEmpty)
            Some(only(references) { next =>
                assert(symbol.syms contains next.symbol)
                OneOfProgress(symbol, Some(ParsedSymbol[OneOf](symbol, next)))
            })
        }
        val derive =
            if (parsed.isEmpty) symbol.syms map { s => (SymbolProgress(s), EdgeKind.Derive) }
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class ConjunctionProgress(symbol: Conjunction, parsed: Option[ParsedSymbol[Conjunction]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(parsed.isEmpty)
            val sym = references find { _.symbol == symbol.sym }
            val also = references find { _.symbol == symbol.also }
            assert(sym.isDefined || also.isDefined)
            if (sym.isDefined && also.isDefined)
                Some(ConjunctionProgress(symbol, sym.asInstanceOf[Option[ParsedSymbol[Conjunction]]]))
            else None
        }
        val derive =
            if (parsed.isEmpty) Set(symbol.sym, symbol.also) map { s => (SymbolProgress(s), EdgeKind.Derive) }
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class ExceptProgress(symbol: Except, parsed: Option[ParsedSymbol[Except]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(parsed.isEmpty)
            val sym = references find { _.symbol == symbol.sym }
            val except = references find { _.symbol == symbol.except }
            assert(sym.isDefined || except.isDefined)
            if (sym.isDefined && except.isEmpty)
                Some(ExceptProgress(symbol, sym.asInstanceOf[Option[ParsedSymbol[Except]]]))
            else None
        }
        val derive =
            if (parsed.isEmpty) Set(symbol.sym, symbol.except) map { s => (SymbolProgress(s), EdgeKind.Derive) }
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class RepeatProgress(symbol: Repeat, _children: List[ParseNode[Symbol]])
            extends SymbolProgressNonterminal {
        lazy val children = _children.reverse
        val parsed =
            if (symbol.range contains _children.size) Some(ParsedSymbolsSeq(symbol, children))
            else None
        def proceed(references: Set[ParseNode[Symbol]]) = {
            assert(symbol.range canProceed _children.size)
            only(references) { next =>
                assert(next.symbol == symbol.sym)
                assert(symbol.range canProceed _children.size)
                Some(RepeatProgress(symbol, next +: _children))
            }
        }
        val derive =
            if (symbol.range canProceed _children.size) Set((SymbolProgress(symbol.sym), EdgeKind.Derive))
            else Set[(SymbolProgress, EdgeKind.Value)]()
    }

    case class LookaheadProgress(symbol: LookaheadExcept, parsed: Option[ParsedSymbol[LookaheadExcept]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

    case class BackupProgress(symbol: Backup, parsed: Option[ParsedSymbol[Backup]])
            extends SymbolProgressNonterminal {
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

}