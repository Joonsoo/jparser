package com.giyeok.moonparser

trait SymbolProgresses {
    this: ParsingProblem =>

    import Symbols._
    import Inputs._
    import ParseTree._

    case class AmbiguousParsingException extends Exception
    case class NoDefinitionOfNonterminalException(name: String) extends Exception

    abstract class SymbolProgress {
        val parsed: Option[ParseNode[Symbol]]
        val canFinish = parsed.isDefined
        def proceedTerminal(next: Input): Option[SymbolProgress]

        /*
         * `proceed` and `derive` are some kind of opposite operation
         */
        def proceed(references: Set[ParseNode[Symbol]]): Option[SymbolProgress]
        val derive: Set[SymbolProgress]
    }
    object SymbolProgress {
        def apply(symbol: Symbol): SymbolProgress = symbol match {
            case symbol: Terminal => TerminalProgress(symbol, None)
            case Empty => EmptyProgress
            case symbol: Nonterminal => NonterminalProgress(symbol, None)
            case symbol: Sequence => SequenceProgress(symbol, List())
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
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = None
        val derive = Set[SymbolProgress]()
    }

    case class TerminalProgress(symbol: Terminal, parsed: Option[ParsedTerminal])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = if (parsed.isDefined) None else {
            if (symbol accept next) Some(TerminalProgress(symbol, Some(ParsedTerminal(symbol, next))))
            else None
        }
        def proceed(references: Set[ParseNode[Symbol]]) = None
        val derive = Set[SymbolProgress]()
    }

    private def only[A, B](set: Set[A])(block: A => Option[B]): Option[B] =
        if (set.isEmpty) None
        else if (set.size > 1) throw new AmbiguousParsingException
        else {
            assert(set.size == 1)
            block(set.head)
        }

    case class NonterminalProgress(symbol: Nonterminal, parsed: Option[ParsedSymbol[Nonterminal]])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) =
            if (parsed.isDefined) None else
                only(references) { next =>
                    grammar.rules get symbol.name match {
                        case Some(rhs) if rhs contains next.symbol =>
                            Some(NonterminalProgress(symbol, Some(ParsedSymbol[Nonterminal](symbol, next))))
                        case Some(_) => None
                        case None => throw NoDefinitionOfNonterminalException(symbol.name)
                    }
                }
        val derive =
            if (parsed.isDefined) Set[SymbolProgress]() else {
                grammar.rules get symbol.name match {
                    case Some(rhs) =>
                        rhs map { SymbolProgress(_) }
                    case None =>
                        throw NoDefinitionOfNonterminalException(symbol.name)
                }
            }
    }

    case class SequenceProgress(symbol: Sequence, childrenReversed: List[ParseNode[Symbol]])
            extends SymbolProgress {
        lazy val children = childrenReversed.reverse
        val parsed = ???
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

    case class OneOfProgress(symbol: OneOf, parsed: Option[ParsedSymbol[OneOf]])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) =
            if (parsed.isDefined) None else
                only(references) { next =>
                    if (symbol.syms contains next.symbol)
                        Some(OneOfProgress(symbol, Some(ParsedSymbol[OneOf](symbol, next))))
                    else None
                }
        val derive = if (parsed.isDefined) Set[SymbolProgress]() else {
            symbol.syms map { SymbolProgress(_) }
        }
    }

    case class ConjunctionProgress(symbol: Conjunction, parsed: Option[ParsedConjunction])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) =
            if (parsed.isDefined) None else {
                val refSyms = references map { _.symbol }
                if (symbol.syms forall { refSyms contains _ }) {
                    Some(ConjunctionProgress(symbol, Some(ParsedConjunction(symbol, references))))
                } else None
            }
        val derive = if (parsed.isDefined) Set[SymbolProgress]() else {
            symbol.syms map { SymbolProgress(_) }
        }
    }

    case class ExceptProgress(symbol: Except, parsed: Option[ParsedSymbol[Except]])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

    case class LookaheadProgress(symbol: LookaheadExcept, parsed: Option[ParsedSymbol[LookaheadExcept]])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

    case class RepeatProgress(symbol: Repeat, childrenReversed: List[ParseNode[Symbol]])
            extends SymbolProgress {
        lazy val children = childrenReversed.reverse
        val parsed = ???
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

    case class BackupProgress(symbol: Backup, parsed: Option[ParsedSymbol[Backup]])
            extends SymbolProgress {
        def proceedTerminal(next: Input) = None
        def proceed(references: Set[ParseNode[Symbol]]) = ???
        val derive = ???
    }

}