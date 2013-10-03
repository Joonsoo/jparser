package com.giyeok.moonparser.dynamic

import com.giyeok.moonparser.GrElem
import com.giyeok.moonparser.GrElem._
import com.giyeok.moonparser.ParsedSymbol

trait ParsingItems {
    this: Parser =>

    def defItemToState(i: GrElem) = i match {
        case j: CharacterInput => ParsingCharacterInput(j)
        case j: StringInput => ParsingStringInput(j)
        case j: VirtualInput => ParsingVirtualInput(j)
        case j: Nonterminal => ParsingNonterminal(j)
        case j: OneOf => ParsingOneOf(j)
        case j: Repeat => ParsingRepeat(j)
        case j: Sequence => new ParsingSequence(j)
        case j: Except => ParsingExcept(j)
        case j: LookaheadExcept => ParsingLookaheadExcept(j)
    }

    // TODO implement this
    case class ParsingCharacterInput(elem: CharacterInput) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingStringInput(elem: StringInput) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingVirtualInput(elem: VirtualInput) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingNonterminal(elem: Nonterminal) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingOneOf(elem: OneOf) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingRepeat(elem: Repeat) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingSequence(elem: Sequence) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingExcept(elem: Except) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
    case class ParsingLookaheadExcept(elem: LookaheadExcept) extends ParsingItem {
        val subs: Seq[ParsingItem] = Nil
        val finish: Option[ParsedSymbol] = None
        def proceed(sub: ParsedSymbol): Option[ParsingItem] = None
    }
}
