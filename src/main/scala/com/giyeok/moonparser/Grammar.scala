package com.giyeok.moonparser

import com.giyeok.moonparser.GrElem._

abstract class Grammar {
    type RuleMap = Map[String, Set[GrElem]]

    val name: String
    val rules: RuleMap
    val startSymbol: String

    def n(name: String) = Nonterminal(name)
    def i(string: String) = StringInput(string)
    def c = AnyCharacterInput
    def c(chars: Set[Char]) = SetCharacterInput(chars)
    def c(chars: String) = SetCharacterInput(chars.toCharArray toSet)
    def c(from: Char, to: Char) = CharacterRangeInput(from, to)
    def unicode(categories: String*): UnicodeCategoryCharacterInput = unicode(categories toSet)
    def unicode(categories: Set[String]) = UnicodeCategoryCharacterInput(UnicodeUtil.translateCategoryNamesToByte(categories))
    def virtual(name: String) = VirtualInput(name)
    def seq(seq: GrElem*) = Sequence(seq, Set())
    def seq(whitespace: Set[GrElem], seq: GrElem*) = Sequence(seq, whitespace)
    def oneof(items: GrElem*) = OneOf(items toSet)
    def oneof(items: Set[GrElem]) = OneOf(items)
    def lookahead_except(except: GrElem*) = LookaheadExcept(except toSet)

    case class GrammarDefinitionException(msg: String) extends Exception(msg)
    implicit class GrammarElementExcludable(self: GrElem) {
        def except(e: GrElem*) = self match {
            case _: Nonterminal | _: InputElem | _: OneOf | _: Except | _: Repeat =>
                Except(self, e toSet)
            case _ => throw GrammarDefinitionException("Applied repeat to the items that cannot be")
        }
        def butnot(e: GrElem*) = except(e: _*)
    }
    implicit class GrammarElementRepeatable(self: GrElem) {
        def repeat(range: RepeatRange): Repeat = self match {
            case _: Nonterminal | _: InputElem | _: OneOf | _: Repeat =>
                Repeat(self, range)
            case _ => throw new Exception("Applied repeat to the items that cannot be")
        }
        def repeat(from: Int, to: Int): Repeat = repeat(RepeatRangeTo(from, to))
        def repeat(from: Int): Repeat = repeat(RepeatRangeFrom(from))

        // optional
        def opt = repeat(0, 1)
        def question = opt

        // more than zero
        def asterisk = repeat(0)
        def star = asterisk

        // more than once
        def plus = repeat(1)
    }
}

sealed abstract class GrElem
object GrElem {
    case class Nonterminal(name: String) extends GrElem {
        override lazy val hashCode = name.hashCode
        override def equals(other: Any) = other match {
            case that: Nonterminal => (that canEqual this) && (that.name == name)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Nonterminal]
    }

    sealed abstract class InputElem extends GrElem
    case class StringInput(string: String) extends InputElem {
        override lazy val hashCode = string.hashCode
        override def equals(other: Any) = other match {
            case that: StringInput => (that canEqual this) && (that.string == string)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[StringInput]
    }
    sealed abstract class CharacterInput extends InputElem {
        def accept(char: Char): Boolean
    }
    case object AnyCharacterInput extends CharacterInput {
        def accept(char: Char) = true
    }
    case class SetCharacterInput(chars: Set[Char]) extends CharacterInput {
        def accept(char: Char) = (chars contains char)

        override lazy val hashCode = chars.hashCode
        override def equals(other: Any) = other match {
            case that: SetCharacterInput => (that canEqual this) && (that.chars == chars)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[SetCharacterInput]
    }
    case class UnicodeCategoryCharacterInput(categories: Set[Byte]) extends CharacterInput {
        def accept(char: Char) = categories contains char.getType.toByte

        override lazy val hashCode = categories.hashCode
        override def equals(other: Any) = other match {
            case that: UnicodeCategoryCharacterInput => (that canEqual this) && (that.categories == categories)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[UnicodeCategoryCharacterInput]
    }
    case class CharacterRangeInput(from: Char, to: Char) extends CharacterInput {
        def accept(char: Char) = (from <= char && char <= to)

        override lazy val hashCode = (from, to).hashCode
        override def equals(other: Any) = other match {
            case that: CharacterRangeInput => (that canEqual this) && (that.from == from) && (that.to == to)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[CharacterRangeInput]
    }
    case class VirtualInput(name: String) extends InputElem {
        override lazy val hashCode = name.hashCode
        override def equals(other: Any) = other match {
            case that: VirtualInput => (that canEqual this) && (that.name == name)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[VirtualInput]
    }

    case class Sequence(seq: Seq[GrElem], whitespace: Set[GrElem]) extends GrElem {
        override lazy val hashCode = (seq, whitespace).hashCode
        override def equals(other: Any) = other match {
            case that: Sequence => (that canEqual this) && (that.seq == seq) && (that.whitespace == whitespace)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Sequence]
    }
    case class OneOf(items: Set[GrElem]) extends GrElem {
        override lazy val hashCode = items.hashCode
        override def equals(other: Any) = other match {
            case that: OneOf => (that canEqual this) && (that.items == items)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[OneOf]
    }
    case class Except(item: GrElem, except: Set[GrElem]) extends GrElem {
        override lazy val hashCode = (item, except).hashCode
        override def equals(other: Any) = other match {
            case that: Except => (that canEqual this) && (that.item == item) && (that.except == except)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Except]
    }
    case class LookaheadExcept(except: Set[GrElem]) extends GrElem {
        override lazy val hashCode = except.hashCode
        override def equals(other: Any) = other match {
            case that: LookaheadExcept => (that canEqual this) && (that.except == except)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[LookaheadExcept]
    }
    case class Repeat(item: GrElem, range: RepeatRange) extends GrElem {
        override lazy val hashCode = (item, range).hashCode
        override def equals(other: Any) = other match {
            case that: Repeat => (that canEqual this) && (that.item == item) && (that.range == range)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Repeat]
    }

    sealed abstract class RepeatRange {
        def contains(v: Int): Boolean
        def canEqual(other: Any): Boolean
        def canProceed(x: Int): Boolean
        val from: Int
    }
    case class RepeatRangeFrom(val from: Int) extends RepeatRange {
        def contains(v: Int) = from <= v
        override def canProceed(x: Int): Boolean = true

        override lazy val hashCode = from
        override def equals(other: Any) = other match {
            case that: RepeatRangeFrom => (that canEqual this) && (that.from == from)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[RepeatRangeFrom]
    }
    case class RepeatRangeTo(val from: Int, val to: Int) extends RepeatRange {
        override def contains(v: Int) = from <= v && v <= to
        override def canProceed(x: Int): Boolean = x < to

        override lazy val hashCode = (from, to).hashCode
        override def equals(other: Any) = other match {
            case that: RepeatRangeTo => (that canEqual this) && (that.from == from) && (that.to == to)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[RepeatRangeTo]
    }
    // ActDef will be considered later!
}