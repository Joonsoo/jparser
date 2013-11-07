package com.giyeok.moonparser.parsing

import com.giyeok.moonparser.GrElems._
import com.giyeok.moonparser.ParsedSymbols._
import com.giyeok.moonparser.InputPieces._
import com.giyeok.moonparser.GrammarDefinitionException
import com.giyeok.moonparser.Grammar

trait ParsingItems extends IsNullable {
    this: Parser =>

    trait ParsingItem {
        val elem: GrElem
        val genpoint: ParsingContext

        val isEmpty: Boolean

        val repr: String

        def isValidWithLiftParents(parents: Set[Entry]): Boolean = true
    }
    abstract sealed class EntryData extends ParsingItem {
        def inContext(context: ParsingContext): Entry
    }
    abstract sealed class Entry extends ParsingItem {
        val context: ParsingContext

        val finish: Option[ParsedSymbol]
        val subs: Set[EntryData]
        def proceed(sym: ParsedSymbol): Option[EntryData]
    }

    private def starting(elem: GrElem, genpoint: ParsingContext): Option[EntryData] = elem match {
        case Empty => None
        case EndOfFileElem => Some(EOFInputEntryData(genpoint, None))
        case j: CharacterInputElem => Some(CharacterInputEntryData(genpoint, j, None))
        case j: StringInputElem => Some(StringInputEntryData(genpoint, j, Nil))
        case j: VirtualInputElem => Some(VirtualInputEntryData(genpoint, j, None))
        case j: Nonterminal => Some(NonterminalEntryData(genpoint, j, None))
        case j: OneOf => Some(OneOfEntryData(genpoint, j, None))
        case j: Repeat => Some(RepeatEntryData(genpoint, j, Nil))
        case j: Sequence => Some(SequenceEntryData(genpoint, j, Nil, Nil, Map()))
        case j: Except => Some(ExceptEntryData(genpoint, j, None))
        case j: Backup => Some(BackupEntryData(genpoint, j, None))
        case j: LookaheadExcept =>
            throw GrammarDefinitionException("lookahead_except can only be placed in sequence")
    }

    trait SimpleInput extends ParsingItem {
        val input: Option[ParsedSymbol]
        val isEmpty = input.isEmpty
    }
    trait ListInput extends ParsingItem {
        val input: Seq[ParsedSymbol]
        val isEmpty = input.isEmpty
    }

    trait SimpleRepr extends ParsingItem {
        val input: Option[ParsedSymbol]

        override lazy val repr = if (input.isEmpty) "* " + elem.repr else elem.repr + " *"
    }

    trait ParsingCharacterInput extends ParsingItem with SimpleInput with SimpleRepr {
        val elem: CharacterInputElem
        val input: Option[TermSymbol[CharInput]]
    }
    case class CharacterInputEntryData(genpoint: ParsingContext, elem: CharacterInputElem, input: Option[TermSymbol[CharInput]])
            extends EntryData with ParsingCharacterInput {
        def inContext(context: ParsingContext) = CharacterInputEntry(genpoint, elem, input, context)
    }
    case class CharacterInputEntry(genpoint: ParsingContext, elem: CharacterInputElem, input: Option[TermSymbol[CharInput]], context: ParsingContext)
            extends Entry with ParsingCharacterInput {
        val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        val subs: Set[EntryData] = Set()
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case term @ TermSymbol(CharInput(c), _) if elem accept c =>
                        // NOTE term will always be TermSymbol[CharInput] here, but scala infers term as TermSymbol[Input]
                        // This happens many times in this file
                        Some(CharacterInputEntryData(genpoint, elem, Some(term.asInstanceOf[TermSymbol[CharInput]])))
                    case _ => None
                }
            }
    }

    trait ParsingStringInput extends ParsingItem with ListInput {
        val elem: StringInputElem
        val input: List[TermSymbol[CharInput]]
        lazy val repr = {
            val (first, second) = elem.string splitAt input.length
            "\"" + first + "*" + second + "\""
        }
    }
    case class StringInputEntryData(genpoint: ParsingContext, elem: StringInputElem, input: List[TermSymbol[CharInput]])
            extends EntryData with ParsingStringInput {
        def inContext(context: ParsingContext) = StringInputEntry(genpoint, elem, input, context)
    }
    case class StringInputEntry(genpoint: ParsingContext, elem: StringInputElem, input: List[TermSymbol[CharInput]], context: ParsingContext)
            extends Entry with ParsingStringInput {
        lazy val isFinished = input.length >= elem.string.length
        lazy val finish: Option[NontermSymbol] =
            if (isFinished) Some(NontermSymbolSeq(elem, input.reverse)) else None
        lazy val subs: Set[EntryData] = Set()
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (isFinished) None else {
                sym match {
                    case term @ TermSymbol(CharInput(c), _) if elem.string.charAt(input.length) == c =>
                        Some(StringInputEntryData(genpoint, elem, term.asInstanceOf[TermSymbol[CharInput]] +: input))
                    case _ => None
                }
            }
    }

    trait ParsingVirtualInput extends ParsingItem with SimpleInput with SimpleRepr {
        val elem: VirtualInputElem
        val input: Option[TermSymbol[VirtInput]]
    }
    case class VirtualInputEntryData(genpoint: ParsingContext, elem: VirtualInputElem, input: Option[TermSymbol[VirtInput]])
            extends EntryData with ParsingVirtualInput {
        def inContext(context: ParsingContext) = VirtualInputEntry(genpoint, elem, input, context)
    }
    case class VirtualInputEntry(genpoint: ParsingContext, elem: VirtualInputElem, input: Option[TermSymbol[VirtInput]], context: ParsingContext)
            extends Entry with ParsingVirtualInput {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[EntryData] = Set()
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case term @ TermSymbol(VirtInput(v), _) =>
                        Some(VirtualInputEntryData(genpoint, elem, Some(term.asInstanceOf[TermSymbol[VirtInput]])))
                    case _ => None
                }
            }
    }

    trait ParsingEOFInput extends ParsingItem with SimpleInput with SimpleRepr {
        val elem = EndOfFileElem
        val input: Option[TermSymbol[EndOfFile.type]]
    }
    case class EOFInputEntryData(genpoint: ParsingContext, input: Option[TermSymbol[EndOfFile.type]])
            extends EntryData with ParsingEOFInput {
        def inContext(context: ParsingContext) = EOFInputEntry(genpoint, input, context)
    }
    case class EOFInputEntry(genpoint: ParsingContext, input: Option[TermSymbol[EndOfFile.type]], context: ParsingContext)
            extends Entry with ParsingEOFInput {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(EndOfFileElem, input.get)) else None
        lazy val subs: Set[EntryData] = Set()
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case term @ TermSymbol(EndOfFile, _) =>
                        Some(EOFInputEntryData(genpoint, Some(term.asInstanceOf[TermSymbol[EndOfFile.type]])))
                    case _ => None
                }
            }
    }

    trait ParsingNonterminal extends ParsingItem with SimpleInput with SimpleRepr {
        val elem: Nonterminal
        val input: Option[ParsedSymbol]
    }
    case class NonterminalEntryData(genpoint: ParsingContext, elem: Nonterminal, input: Option[ParsedSymbol])
            extends EntryData with ParsingNonterminal {
        def inContext(context: ParsingContext) = NonterminalEntry(genpoint, elem, input, context)
    }
    case class NonterminalEntry(genpoint: ParsingContext, elem: Nonterminal, input: Option[ParsedSymbol], context: ParsingContext)
            extends Entry with ParsingNonterminal {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[EntryData] =
            if (input.isDefined) Set() else (grammar.rules(elem.name) flatMap { starting(_, context) })
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case ns: NontermSymbol if grammar.rules(elem.name) contains ns.elem =>
                        Some(NonterminalEntryData(genpoint, elem, Some(ns)))
                    case _ => None
                }
            }
    }

    trait ParsingOneOf extends ParsingItem with SimpleInput with SimpleRepr {
        val elem: OneOf
        val input: Option[ParsedSymbol]
    }
    case class OneOfEntryData(genpoint: ParsingContext, elem: OneOf, input: Option[ParsedSymbol])
            extends EntryData with ParsingOneOf {
        def inContext(context: ParsingContext) = OneOfEntry(genpoint, elem, input, context)
    }
    case class OneOfEntry(genpoint: ParsingContext, elem: OneOf, input: Option[ParsedSymbol], context: ParsingContext)
            extends Entry with ParsingOneOf {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[EntryData] =
            if (input.isDefined) Set() else (elem.elems flatMap { starting(_, context) })
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case ns: NontermSymbol if elem.elems contains ns.elem =>
                        Some(OneOfEntryData(genpoint, elem, Some(ns)))
                    case _ => None
                }
            }
    }

    trait ParsingRepeat extends ParsingItem with ListInput {
        val elem: Repeat
        val input: List[ParsedSymbol]
        override lazy val repr = s"${elem.repr}*${input.length}"
    }
    case class RepeatEntryData(genpoint: ParsingContext, elem: Repeat, input: List[ParsedSymbol])
            extends EntryData with ParsingRepeat {
        def inContext(context: ParsingContext) = new RepeatEntry(genpoint, elem, input, context)
    }
    case class RepeatEntry(genpoint: ParsingContext, elem: Repeat, input: List[ParsedSymbol], context: ParsingContext)
            extends Entry with ParsingRepeat {
        lazy val finish: Option[ParsedSymbol] =
            if (elem.range contains input.length) Some(NontermSymbolSeq(elem, input.reverse)) else None
        lazy val canProceed = elem.range canProceed input.length
        lazy val subs: Set[EntryData] = if (canProceed) starting(elem.elem, context).toSet else Set()
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (!canProceed) None else {
                sym match {
                    case ns: NontermSymbol if elem.elem == ns.elem =>
                        Some(RepeatEntryData(genpoint, elem, ns +: input))
                    case _ => None
                }
            }
    }

    trait ParsingSequence extends ParsingItem {
        val elem: Sequence
        val input: List[ParsedSymbol]
        val inputWS: List[ParsedSymbol]
        val mappings: Map[Int, Int]
        // input: list of symbols WITHOUT white spaces (and with Empty symbols) -- indices sync with elem.seq
        // inputWS: list of symbols WITH white spaces (and without Empty symbols) -- indices not sync with elem.seq
        // mappings: mapping from the index of actual child item of `elem: Sequence` -> index of `inputWS`, 
        //             i.e. index of `input` -> index of `inputWS`
        // NOTE !!! `input` and `inputWS` are reversed !!!
        val isEmpty = inputWS.isEmpty

        val pointer = input.length
        lazy val canFinish: Boolean = if (input.isEmpty) false else {
            val leftItemsAreAllNullable = elem.seq drop pointer forall { _.isNullable }
            val lastIsNotWhitespace = (mappings maxBy { _._1 })._2 + 1 == inputWS.length
            leftItemsAreAllNullable && lastIsNotWhitespace
        }

        override lazy val repr = {
            val (first, second) = elem.seq splitAt pointer
            "[" + (((first map { _.repr }) ++ Seq("*") ++ (second map { _.repr })) mkString " ") + "]"
        }
    }
    case class SequenceEntryData(genpoint: ParsingContext, elem: Sequence, input: List[ParsedSymbol], inputWS: List[ParsedSymbol], mappings: Map[Int, Int])
            extends EntryData with ParsingSequence {
        def inContext(context: ParsingContext) = SequenceEntry(genpoint, elem, input, inputWS, mappings, context)
    }
    case class SequenceEntry(genpoint: ParsingContext, elem: Sequence, input: List[ParsedSymbol], inputWS: List[ParsedSymbol], mappings: Map[Int, Int], context: ParsingContext)
            extends Entry with ParsingSequence {
        lazy val finish: Option[ParsedSymbol] = {
            if (!canFinish) None else {
                val rest = (elem.seq drop pointer).toList map (EmptySymbol(_))
                Some(new NontermSymbolSeqWS(elem, input.reverse ++ rest, inputWS.reverse, mappings))
            }
        }
        val subs: Set[EntryData] = {
            def prop(seq: Seq[GrElem]): Set[EntryData] = seq match {
                case LookaheadExcept(except) +: rest =>
                    // TODO check lookahead
                    Set()
                case item +: rest =>
                    item.isNullable match {
                        case true => prop(rest) ++ starting(item, context).toSet
                        case false => starting(item, context).toSet
                    }
                case Nil => Set()
            }
            prop(elem.seq drop pointer) ++ (elem.whitespace flatMap { starting(_, context) })
        }
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (pointer >= elem.seq.length) None else {
                sym match {
                    case sym: NamedSymbol =>
                        def prop(seq: Seq[GrElem], acc: List[ParsedSymbol] = Nil): List[ParsedSymbol] = seq match {
                            case item +: rest if item == sym.elem => sym +: acc
                            case item +: rest if item.isNullable => prop(rest, EmptySymbol(item) +: acc)
                            case _ => Nil
                        }
                        val p = prop(elem.seq drop pointer)
                        if (!p.isEmpty)
                            Some(SequenceEntryData(genpoint, elem, p ++ input, sym +: inputWS, mappings + ((pointer + p.length - 1) -> inputWS.length)))
                        else {
                            if ((!input.isEmpty) && (elem.whitespace contains sym.elem)) Some(SequenceEntryData(genpoint, elem, input, sym +: inputWS, mappings))
                            else None
                        }
                    case _ => None
                }
            }
    }

    trait ParsingExcept extends ParsingItem with SimpleInput with SimpleRepr {
        val elem: Except
        val input: Option[ParsedSymbol]
    }
    case class ExceptEntryData(genpoint: ParsingContext, elem: Except, input: Option[ParsedSymbol])
            extends EntryData with ParsingExcept {
        def inContext(context: ParsingContext) = ExceptEntry(genpoint, elem, input, context)
    }
    case class ExceptEntry(genpoint: ParsingContext, elem: Except, input: Option[ParsedSymbol], context: ParsingContext)
            extends Entry with ParsingExcept {
        val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        val subs: Set[EntryData] =
            if (input.isDefined) Set()
            else (elem.except + elem.elem) flatMap { starting(_, context) }
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case sym: NamedSymbol if elem.elem == sym.elem =>
                        Some(ExceptEntryData(genpoint, elem, Some(sym)))
                    case _ => None
                }
            }
        override def isValidWithLiftParents(parents: Set[Entry]): Boolean =
            !(parents exists { elem.except contains _.elem })
    }

    trait ParsingBackup extends ParsingItem with SimpleInput {
        val elem: Backup
        val input: Option[ParsedSymbol]
        override lazy val repr = s"_${elem.repr}_"
    }
    case class BackupEntryData(genpoint: ParsingContext, elem: Backup, input: Option[ParsedSymbol])
            extends EntryData with ParsingBackup {
        def inContext(context: ParsingContext) = BackupEntry(genpoint, elem, input, context)
    }
    case class BackupEntry(genpoint: ParsingContext, elem: Backup, input: Option[ParsedSymbol], context: ParsingContext)
            extends Entry with ParsingBackup {
        lazy val finish: Option[ParsedSymbol] =
            if (input.isDefined) Some(NontermSymbolElem(elem, input.get)) else None
        lazy val subs: Set[EntryData] =
            if (input.isDefined) Set() else starting(elem.elem, context).toSet
        def proceed(sym: ParsedSymbol): Option[EntryData] =
            if (input.isDefined) None else {
                sym match {
                    case sym: NamedSymbol if elem.elem == sym.elem =>
                        Some(BackupEntryData(genpoint, elem, Some(sym)))
                    case _ => None
                }
            }
    }
}
