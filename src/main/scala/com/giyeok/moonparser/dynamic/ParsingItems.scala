package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.InputPieces._
import com.giyeok.moonparser.GrammarDefinitionException

trait ParsingItems {
    this: Parser =>

    implicit class GrElemtoParsingItemOpt(self: GrElem) {
        def toParsingItem: ParsingItem = {
            val opt = self.toParsingItemOpt
            assert(opt.isDefined)
            opt.get
        }
        def toParsingItemOpt: Option[ParsingItem] = self match {
            case Empty => None
            case EndOfFileElem => Some(ParsingEOFInput())
            case j: CharacterInputElem => Some(ParsingCharacterInput(j))
            case j: StringInputElem => Some(ParsingStringInput(j))
            case j: VirtualInputElem => Some(ParsingVirtualInput(j))
            case j: Nonterminal => Some(ParsingNonterminal(j))
            case j: OneOf => Some(ParsingOneOf(j))
            case j: Repeat => Some(ParsingRepeat(j))
            case j: Sequence => Some(new ParsingSequence(j))
            case j: Except => Some(ParsingExcept(j))
            case j: Backup => Some(ParsingBackup(j))
            case j: LookaheadExcept =>
                throw GrammarDefinitionException("lookahead_except can only be placed in sequence")
        }
    }

    abstract class ParsingItem {
        val elem: GrElem

        val finish: Option[ParsedSymbol]
        val subs: Set[ParsingItem]
        def proceed(sym: Set[ParsedSymbol]): Set[ParsingItem]

        override def toString: String = repr
        lazy val repr = super.toString

        // FUTURE expecting will be used to make more informative parse error message
        // `expecting` is the set of grammar elements that are expected to be appeared at the point
        // val expecting: Set[GrElem]
    }

    trait SimpleRepr extends ParsingItem {
        val input: Option[ParsedSymbol]

        override lazy val repr = if (input.isEmpty) "* " + elem.repr else elem.repr + " *"
    }
    case class ParsingCharacterInput(elem: CharacterInputElem, input: Option[TermSymbol[CharInput]] = None)
            extends ParsingItem with SimpleRepr {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        // dumb code: if (input.isDefined) Some(input.get) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case term @ TermSymbol(CharInput(c), _) if elem accept c =>
                        // NOTE term will always be TermSymbol[CharInput] here, but scala infers term as TermSymbol[Input]
                        // This happens many times in this file
                        Some(ParsingCharacterInput(elem, Some(term.asInstanceOf[TermSymbol[CharInput]])))
                    case _ => None
                }
            }

        override lazy val hashCode = (elem, input).hashCode
    }
    trait TokenCompatibles extends ParsingItem { self: ParsingItem =>
        case class GotToken(token: TermSymbol[TokenInput]) extends ParsingItem {
            val elem = self.elem
            val finish: Option[NontermSymbol] = Some(NontermSymbolElem(elem, token))
            val subs: Set[ParsingItem] = Set()
            def proceed(syms: Set[ParsedSymbol]) = Set()
        }
    }
    case class ParsingStringInput(elem: StringInputElem, input: List[TermSymbol[CharInput]] = Nil)
            extends ParsingItem with TokenCompatibles {
        lazy val isFinished = input.length >= elem.string.length
        lazy val finish: Option[NontermSymbol] =
            if (isFinished) Some(NontermSymbolSeq(elem, input.reverse)) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (isFinished) Set() else {
                syms flatMap {
                    case term @ TermSymbol(CharInput(c), _) if elem.string.charAt(input.length) == c =>
                        Some(ParsingStringInput(elem, term.asInstanceOf[TermSymbol[CharInput]] +: input))
                    case term @ TermSymbol(TokenInput(t), _) if input.isEmpty && (t compat elem) =>
                        Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                    case _ => None
                }
            }

        override lazy val repr = {
            val (first, second) = elem.string splitAt input.length
            "\"" + first + "*" + second + "\""
        }
        override lazy val hashCode = (elem, input).hashCode
    }
    case class ParsingVirtualInput(elem: VirtualInputElem, input: Option[TermSymbol[VirtInput]] = None)
            extends ParsingItem with TokenCompatibles with SimpleRepr {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case term @ TermSymbol(VirtInput(v), _) =>
                        Some(ParsingVirtualInput(elem, Some(term.asInstanceOf[TermSymbol[VirtInput]])))
                    case term @ TermSymbol(TokenInput(t), _) =>
                        Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                    case _ => None
                }
            }

        override lazy val hashCode = (elem, input).hashCode
    }
    case class ParsingEOFInput(input: Option[TermSymbol[EndOfFile.type]] = None)
            extends ParsingItem with SimpleRepr {
        val elem = EndOfFileElem
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(EndOfFileElem, input.get)) else None
        lazy val subs: Set[ParsingItem] = Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case term @ TermSymbol(EndOfFile, _) =>
                        Some(ParsingEOFInput(Some(term.asInstanceOf[TermSymbol[EndOfFile.type]])))
                    case _ => None
                }
            }

        override lazy val hashCode = (input).hashCode
    }
    case class ParsingNonterminal(elem: Nonterminal, input: Option[ParsedSymbol] = None)
            extends ParsingItem with TokenCompatibles with SimpleRepr {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[ParsingItem] =
            if (input.isDefined) Set() else (grammar.rules(elem.name) flatMap { _.toParsingItemOpt })
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case ns: NontermSymbol if grammar.rules(elem.name) contains ns.elem =>
                        Some(ParsingNonterminal(elem, Some(ns)))
                    case term @ TermSymbol(TokenInput(t), _) if t compat elem =>
                        Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                    case _ => None
                }
            }

        override lazy val hashCode = (input).hashCode
    }
    case class ParsingOneOf(elem: OneOf, input: Option[ParsedSymbol] = None)
            extends ParsingItem with TokenCompatibles with SimpleRepr {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[ParsingItem] =
            if (input.isDefined) Set() else (elem.elems flatMap { _.toParsingItemOpt })
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case ns: NontermSymbol if elem.elems contains ns.elem =>
                        Some(ParsingOneOf(elem, Some(ns)))
                    case term @ TermSymbol(TokenInput(t), _) if t compat elem =>
                        Some(new GotToken(term.asInstanceOf[TermSymbol[TokenInput]]))
                    case _ => None
                }
            }

        override lazy val hashCode = (elem, input).hashCode
    }
    case class ParsingRepeat(elem: Repeat, input: List[ParsedSymbol] = Nil) extends ParsingItem {
        lazy val finish: Option[ParsedSymbol] =
            if (elem.range contains input.length) Some(NontermSymbolSeq(elem, input.reverse)) else None
        lazy val canProceed = elem.range canProceed input.length
        lazy val subs: Set[ParsingItem] = if (canProceed) elem.elem.toParsingItemOpt.toSet else Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (!canProceed) Set() else {
                syms flatMap {
                    case ns: NontermSymbol if elem.elem == ns.elem =>
                        Some(ParsingRepeat(elem, ns +: input))
                    // NOTE needs token proceed?
                    case _ => None
                }
            }

        override lazy val repr = s"${elem.repr}[${elem.range.repr},${input.length}]"
        override lazy val hashCode = (elem, input).hashCode
    }
    case class ParsingSequence(elem: Sequence, input: List[ParsedSymbol] = Nil, inputWS: List[ParsedSymbol] = Nil, mappings: Map[Int, Int] = Map())
            extends ParsingItem {
        // input: list of symbols WITHOUT white spaces (and with Empty symbols) -- indices sync with elem.seq
        // inputWS: list of symbols WITH white spaces (and without Empty symbols) -- indices not sync with elem.seq
        // mappings: mapping from the index of actual child item of `elem: Sequence` -> index of `inputWS`, 
        //             i.e. index of `input` -> index of `inputWS`
        // NOTE !!! `input` and `inputWS` are reversed !!!
        val pointer = input.length
        // TODO implement lookahead_except
        lazy val canFinish: Boolean = if (input.isEmpty) false else {
            val leftItemsAreAllNullable = elem.seq drop pointer forall { _.isNullable }
            val lastIsNotWhitespace = (mappings maxBy { _._1 })._2 + 1 == inputWS.length
            leftItemsAreAllNullable && lastIsNotWhitespace
        }
        lazy val finish: Option[ParsedSymbol] = {
            if (!canFinish) None else {
                val rest = (elem.seq drop pointer).toList map (EmptySymbol(_))
                Some(new NontermSymbolSeqWS(elem, input.reverse ++ rest, inputWS.reverse, mappings))
            }
        }
        val subs: Set[ParsingItem] = {
            def prop(seq: Seq[GrElem]): Set[ParsingItem] = seq match {
                case item +: rest if item.isNullable => prop(rest) ++ item.toParsingItemOpt.toSet
                case item +: rest => item.toParsingItemOpt.toSet
                case Nil => Set()
            }
            prop(elem.seq drop pointer) ++ (elem.whitespace flatMap { _.toParsingItemOpt })
        }
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (pointer >= elem.seq.length) Set() else {
                syms flatMap {
                    case tok: Token =>
                        ??? // NOTE needs token proceed?
                    case sym: NamedSymbol =>
                        def prop(seq: Seq[GrElem], acc: List[ParsedSymbol] = Nil): List[ParsedSymbol] = seq match {
                            case item +: rest if item == sym.elem => sym +: acc
                            case item +: rest if item.isNullable => prop(rest, EmptySymbol(item) +: acc)
                            case _ => Nil
                        }
                        val p = prop(elem.seq drop pointer)
                        if (!p.isEmpty)
                            Some(ParsingSequence(elem, p ++ input, sym +: inputWS, mappings + ((pointer + p.length - 1) -> inputWS.length)))
                        else {
                            if ((!input.isEmpty) && (elem.whitespace contains sym.elem)) Some(ParsingSequence(elem, input, sym +: inputWS, mappings))
                            else None
                        }
                    case _ => None
                }
            }

        override lazy val repr = {
            val (first, second) = elem.seq splitAt pointer
            "[" + (((first map { _.repr }) ++ Seq("*") ++ (second map { _.repr })) mkString " ") + "]"
        }
        override lazy val hashCode = (elem, input, inputWS, mappings).hashCode
    }
    case class ParsingBackup(elem: Backup, input: Option[ParsedSymbol] = None) extends ParsingItem {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[ParsingItem] =
            if (input.isDefined) Set()
            else ((elem.backups + elem.elem) flatMap { _.toParsingItemOpt })
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                syms flatMap {
                    case sym: NamedSymbol if (elem.elem == sym.elem) || (elem.backups contains sym.elem) =>
                        Some(ParsingBackup(elem, Some(sym)))
                    case _ => None
                }
            }

        override lazy val repr = s"_${elem.repr}_"
        override lazy val hashCode = (elem, input).hashCode
    }
    // TODO implement the rest
    case class ParsingExcept(elem: Except, input: Option[ParsedSymbol] = None) extends ParsingItem {
        val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        val subs: Set[ParsingItem] =
            if (input.isDefined) Set()
            else (elem.except + elem.elem) flatMap { _.toParsingItemOpt }
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] =
            if (input.isDefined) Set() else {
                val except = syms exists {
                    case ns: NontermSymbol if elem.except contains ns.elem => true
                    case _ => false
                }
                if (except) {
                    val i = syms find {
                        case ns: NontermSymbol if ns.elem == elem.elem => true
                        case _ => false
                    }
                    i match {
                        case Some(input) => Set(ParsingExcept(elem, i))
                        case None => Set()
                    }
                } else Set()
            }
    }
    case class ParsingLookaheadExcept(elem: LookaheadExcept) extends ParsingItem {
        val finish: Option[ParsedSymbol] = None
        val subs: Set[ParsingItem] = Set()
        def proceed(syms: Set[ParsedSymbol]): Set[ParsingItem] = Set()
    }
}
