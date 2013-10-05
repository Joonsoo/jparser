package com.giyeok.moonparser

abstract class Grammar {
    import com.giyeok.moonparser.GrElems._

    type RuleMap = Map[String, Set[GrElem]]

    val name: String
    val rules: RuleMap
    val startSymbol: String

    def e = Empty
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

    implicit class GrammarElementExcludable(self: GrElem) {
        def except(e: GrElem*) = self match {
            case _: Nonterminal | _: InputElem | _: OneOf | _: Except | _: Repeat =>
                Except(self, e toSet)
            case _ => throw GrammarDefinitionException("Applied repeat to the items that cannot be")
        }
        def butnot(e: GrElem*) = except(e: _*)
    }
    implicit class GrammarElementRepeatable(self: GrElem) {
        def repeat(range: Repeat.Range): Repeat = self match {
            case _: Nonterminal | _: InputElem | _: OneOf | _: Repeat =>
                Repeat(self, range)
            case _ => throw new Exception("Applied repeat to the items that cannot be")
        }
        def repeat(from: Int, to: Int): Repeat = repeat(Repeat.RangeTo(from, to))
        def repeat(from: Int): Repeat = repeat(Repeat.RangeFrom(from))

        // optional
        def opt = repeat(0, 1)
        def question = opt

        // more than zero
        def asterisk = repeat(0)
        def star = asterisk

        // more than once
        def plus = repeat(1)
    }
    implicit class GrammarElementBackupable(self: GrElem) {
        def backup(backup: GrElem): Backup = self match {
            case _: Nonterminal => // NOTE consider which elements are deserved to be backed up
                Backup(self, backup)
            case _ => throw new Exception("Applied backup to the items that cannot be")
        }
    }
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)

object GrElems {
    abstract class AbsGrElem
    sealed abstract class GrElem extends AbsGrElem

    case object Empty extends GrElem

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
    case class OneOf(elems: Set[GrElem]) extends GrElem {
        override lazy val hashCode = elems.hashCode
        override def equals(other: Any) = other match {
            case that: OneOf => (that canEqual this) && (that.elems == elems)
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
    case class Repeat(elem: GrElem, range: Repeat.Range) extends GrElem {
        override lazy val hashCode = (elem, range).hashCode
        override def equals(other: Any) = other match {
            case that: Repeat => (that canEqual this) && (that.elem == elem) && (that.range == range)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Repeat]
    }
    object Repeat {
        sealed abstract class Range {
            val from: Int

            def contains(v: Int): Boolean
            def canProceed(x: Int): Boolean

            def canEqual(other: Any): Boolean
        }
        case class RangeFrom(val from: Int) extends Range {
            def contains(v: Int) = from <= v
            override def canProceed(x: Int): Boolean = true

            override lazy val hashCode = from
            override def equals(other: Any) = other match {
                case that: RangeFrom => (that canEqual this) && (that.from == from)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[RangeFrom]
        }
        case class RangeTo(val from: Int, val to: Int) extends Range {
            override def contains(v: Int) = from <= v && v <= to
            override def canProceed(x: Int): Boolean = x < to

            override lazy val hashCode = (from, to).hashCode
            override def equals(other: Any) = other match {
                case that: RangeTo => (that canEqual this) && (that.from == from) && (that.to == to)
                case _ => false
            }
            override def canEqual(other: Any) = other.isInstanceOf[RangeTo]
        }
    }
    case class Backup(elem: GrElem, backup: GrElem) extends GrElem {
        override lazy val hashCode = (elem, backup).hashCode
        override def equals(other: Any) = other match {
            case that: Backup => (that canEqual this) && (that.elem == elem) && (that.backup == backup)
            case _ => false
        }
        override def canEqual(other: Any) = other.isInstanceOf[Backup]
    }
    // ActDef will be considered later!
}