package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.InputPieces._

trait ParsingItems {
    this: Parser =>

    def defItemToState(i: GrElem): ParsingItem = i match {
        case Empty => ParsingEmpty
        case EOFInput => ParsingEOFInput()
        case j: CharacterInput => ParsingCharacterInput(j)
        case j: StringInput => ParsingStringInput(j)
        case j: VirtualInput => ParsingVirtualInput(j)
        case j: Nonterminal => ParsingNonterminal(j)
        case j: OneOf => ParsingOneOf(j)
        case j: Repeat => ParsingRepeat(j)
        case j: Sequence => new ParsingSequence(j)
        case j: Except => ParsingExcept(j)
        case j: LookaheadExcept => ParsingLookaheadExcept(j)
        case j: Backup => ParsingBackup(j)
    }

    abstract class ParsingItem {
        val elem: GrElem

        val finish: Option[ParsedSymbol]
        val subs: Set[ParsingItem]
        def proceed(sym: ParsedSymbol): Option[ParsingItem]

        // FUTURE expecting will be used to make more informative parse error message
        // `expecting` is the set of grammar elements that are expected to be appeared at the point
        // val expecting: Set[GrElem]
    }

    case object ParsingEmpty extends ParsingItem {
        val elem = Empty
        object EmptyObject extends EmptySymbol(Empty)
        val finish: Option[EmptySymbol] = Some(EmptyObject)
        val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingCharacterInput(elem: CharacterInput, input: Option[TermSymbol[CharInput]] = None) extends ParsingItem {
        lazy val finish: Option[TermSymbol[CharInput]] = input
        // dumb code: if (input.isDefined) Some(input.get) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (input.isDefined) None else {
            sym match {
                case term @ TermSymbol(CharInput(c), _) if elem accept c =>
                    // NOTE term will always be TermSymbol[CharInput] here, but scala infers term as TermSymbol[Input]
                    // This happens many times in this file
                    Some(ParsingCharacterInput(elem, Some(term.asInstanceOf[TermSymbol[CharInput]])))
                case _ => None
            }
        }
    }
    trait TokenCompatibles extends ParsingItem { self: ParsingItem =>
        class GotToken(token: TermSymbol[TokenInput]) extends ParsingItem {
            val elem = self.elem
            val finish: Option[NontermSymbol] = Some(NontermSymbol(elem, Seq(token)))
            val subs: Set[ParsingItem] = Set()
            def proceed(sym: ParsedSymbol) = None
        }
    }
    case class ParsingStringInput(elem: StringInput, input: List[TermSymbol[CharInput]] = Nil)
            extends ParsingItem with TokenCompatibles {
        lazy val isFinished = input.length >= elem.string.length
        lazy val finish: Option[NontermSymbol] =
            if (isFinished) Some(NontermSymbol(elem, input.reverse)) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (isFinished) None else {
            sym match {
                case term @ TermSymbol(CharInput(c), _) if elem.string.charAt(input.length) == c =>
                    Some(ParsingStringInput(elem, term.asInstanceOf[TermSymbol[CharInput]] +: input))
                case term @ TermSymbol(TokenInput(t), _) if input.isEmpty && (t compat elem) =>
                    Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                case _ => None
            }
        }
    }
    case class ParsingVirtualInput(elem: VirtualInput, input: Option[TermSymbol[VirtInput]] = None)
            extends ParsingItem with TokenCompatibles {
        lazy val finish: Option[ParsedSymbol] = input
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (input.isDefined) None else {
            sym match {
                case term @ TermSymbol(VirtInput(v), _) =>
                    Some(ParsingVirtualInput(elem, Some(term.asInstanceOf[TermSymbol[VirtInput]])))
                case term @ TermSymbol(TokenInput(t), _) =>
                    Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                case _ => None
            }
        }
    }
    case class ParsingEOFInput(input: Option[TermSymbol[EndOfFile.type]] = None) extends ParsingItem {
        val elem = EOFInput
        lazy val finish: Option[ParsedSymbol] = input
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (input.isDefined) None else {
            sym match {
                case term @ TermSymbol(EndOfFile, _) =>
                    Some(ParsingEOFInput(Some(term.asInstanceOf[TermSymbol[EndOfFile.type]])))
                case _ => None
            }
        }
    }
    case class ParsingNonterminal(elem: Nonterminal, input: Option[ConcreteSymbol] = None)
            extends ParsingItem with TokenCompatibles {
        lazy val finish: Option[ParsedSymbol] = if (input.isDefined) Some(NontermSymbol(elem, Seq(input.get))) else None
        lazy val subs: Set[ParsingItem] =
            if (input.isDefined) Set() else (grammar.rules(elem.name) map defItemToState)
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (input.isDefined) None else {
            sym match {
                case ns @ NontermSymbol(rhs, _) if grammar.rules(elem.name) contains rhs =>
                    Some(ParsingNonterminal(elem, Some(ns)))
                case term @ TermSymbol(TokenInput(t), _) if t compat elem =>
                    Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                case _ => None
            }
        }
    }
    case class ParsingOneOf(elem: OneOf, input: Option[ParsedSymbol] = None)
            extends ParsingItem with TokenCompatibles {
        lazy val finish: Option[ParsedSymbol] = input
        lazy val subs: Set[ParsingItem] =
            if (input.isDefined) Set() else (elem.elems map defItemToState)
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (input.isDefined) None else {
            sym match {
                case NontermSymbol(e, _) if elem.elems contains e =>
                    Some(ParsingOneOf(elem, Some(sym)))
                case term @ TermSymbol(TokenInput(t), _) if t compat elem =>
                    Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                case _ => None
            }
        }
    }
    case class ParsingRepeat(elem: Repeat, input: List[ConcreteSymbol] = Nil) extends ParsingItem {
        lazy val finish: Option[ParsedSymbol] =
            if (elem.range contains input.length) Some(NontermSymbol(elem, input.reverse)) else None
        lazy val canProceed = elem.range canProceed input.length
        lazy val subs: Set[ParsingItem] = if (canProceed) Set(defItemToState(elem.elem)) else Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = if (!canProceed) None else {
            sym match {
                case ns @ NontermSymbol(e, _) if elem.elem == e =>
                    Some(ParsingRepeat(elem, ns +: input))
                // NOTE needs token proceed?
                case _ => None
            }
        }
    }
    // TODO implement the rest
    case class ParsingSequence(elem: Sequence, input: List[ConcreteSymbol] = Nil, inputWS: List[ConcreteSymbol] = Nil, mappings: Map[Int, Int] = Map())
            extends ParsingItem {
        // input: list of symbols WITHOUT white spaces
        // inputWS: list of symbols WITH white spaces
        // mappings: mapping from the index of actual child item of `elem: Sequence` -> index of inputWS
        val finish: Option[ParsedSymbol] = None
        val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingExcept(elem: Except) extends ParsingItem {
        val finish: Option[ParsedSymbol] = None
        val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingLookaheadExcept(elem: LookaheadExcept) extends ParsingItem {
        val finish: Option[ParsedSymbol] = None
        val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingBackup(elem: Backup) extends ParsingItem {
        val finish: Option[ParsedSymbol] = None
        val subs: Set[ParsingItem] = Set()
        def proceed(sym: ParsedSymbol): Option[ParsingItem] = None
    }
}
