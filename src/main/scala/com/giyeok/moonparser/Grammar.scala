package com.giyeok.moonparser

abstract class Grammar {
    import scala.collection.immutable._
    import com.giyeok.moonparser.GrElems._

    type RuleMap = Map[String, Set[GrElem]]

    val name: String
    val rules: RuleMap
    val startSymbol: String

    def e = Empty
    def eof = EndOfFileElem
    def n(name: String) = Nonterminal(name)
    def i(string: String) = StringInputElem(string)
    def c = AnyCharacterInputElem
    def c(chars: Char*) = SetCharacterInputElem(chars.toSet)
    def c(chars: Set[Char]) = SetCharacterInputElem(chars)
    def c(chars: String) = SetCharacterInputElem(chars.toCharArray toSet)
    def c(from: Char, to: Char) = CharacterRangeInputElem(from, to)
    def unicode(categories: String*): UnicodeCategoryCharacterInputElem = unicode(categories toSet)
    def unicode(categories: Set[String]) = UnicodeCategoryCharacterInputElem(UnicodeUtil.translateCategoryNamesToByte(categories))
    def virtual(name: String) = VirtualInputElem(name)
    def seq(seq: GrElem*) = Sequence(seq, Set())
    def seq(whitespace: Set[GrElem], seq: GrElem*) = Sequence(seq, whitespace)
    def ws(set: GrElem*): Set[GrElem] = Set[GrElem](set: _*)
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
        def backup(backups: GrElem*): Backup = self match {
            case _: Nonterminal | _: InputElem =>
                // NOTE consider which elements are deserved to be backed up
                Backup(self, backups.toSet)
            case _ => throw new Exception("Applied backup to the items that cannot be")
        }
    }
}

case class GrammarDefinitionException(msg: String) extends Exception(msg)
case class AmbiguousGrammarException(msg: String) extends Exception(msg)

object GrElems {
    abstract class AbsGrElem
    sealed abstract class GrElem extends AbsGrElem

    case object Empty extends GrElem

    case class Nonterminal(name: String) extends GrElem {
        override lazy val hashCode = name.hashCode
    }

    sealed abstract class InputElem extends GrElem
    case class StringInputElem(string: String) extends InputElem {
        override lazy val hashCode = string.hashCode
    }
    sealed abstract class CharacterInputElem extends InputElem {
        def accept(char: Char): Boolean
    }
    case object AnyCharacterInputElem extends CharacterInputElem {
        def accept(char: Char) = true
    }
    case class SetCharacterInputElem(chars: Set[Char]) extends CharacterInputElem {
        def accept(char: Char) = (chars contains char)

        override lazy val hashCode = chars.hashCode
    }
    case class UnicodeCategoryCharacterInputElem(categories: Set[Byte]) extends CharacterInputElem {
        def accept(char: Char) = categories contains char.getType.toByte

        override lazy val hashCode = categories.hashCode
    }
    case class CharacterRangeInputElem(from: Char, to: Char) extends CharacterInputElem {
        def accept(char: Char) = (from <= char && char <= to)

        override lazy val hashCode = (from, to).hashCode
    }
    case class VirtualInputElem(name: String) extends InputElem {
        override lazy val hashCode = name.hashCode
    }
    case object EndOfFileElem extends InputElem

    case class Sequence(seq: Seq[GrElem], whitespace: Set[GrElem]) extends GrElem {
        override lazy val hashCode = (seq, whitespace).hashCode
    }
    case class OneOf(elems: Set[GrElem]) extends GrElem {
        override lazy val hashCode = elems.hashCode
    }
    case class Except(elem: GrElem, except: Set[GrElem]) extends GrElem {
        override lazy val hashCode = (elem, except).hashCode
    }
    case class LookaheadExcept(except: Set[GrElem]) extends GrElem {
        override lazy val hashCode = except.hashCode
    }
    case class Repeat(elem: GrElem, range: Repeat.Range) extends GrElem {
        override lazy val hashCode = (elem, range).hashCode
    }
    object Repeat {
        sealed abstract class Range {
            val from: Int

            def contains(v: Int): Boolean
            def canProceed(x: Int): Boolean

            def canEqual(other: Any): Boolean

            val repr: String
        }
        case class RangeFrom(val from: Int) extends Range {
            def contains(v: Int) = from <= v
            override def canProceed(x: Int): Boolean = true

            override lazy val hashCode = from

            lazy val repr = s"$from-"
        }
        case class RangeTo(val from: Int, val to: Int) extends Range {
            override def contains(v: Int) = from <= v && v <= to
            override def canProceed(x: Int): Boolean = x < to

            override lazy val hashCode = (from, to).hashCode

            lazy val repr = s"$from-$to"
        }
    }
    case class Backup(elem: GrElem, backups: Set[GrElem]) extends GrElem {
        override lazy val hashCode = (elem, backups).hashCode
    }
    // ActDef will be considered later!

    implicit class GrElemRepr(elem: GrElem) {
        def repr: String = elem match {
            case Empty => "<e>"
            case Nonterminal(name) => name
            case StringInputElem(string) => "\"" + string + "\""
            case AnyCharacterInputElem => "<anychar>"
            case SetCharacterInputElem(chars) => "{" + (chars mkString "|") + "}"
            case UnicodeCategoryCharacterInputElem(categories) =>
                "{" + UnicodeUtil.translateToString(categories) mkString "|" + "}"
            case CharacterRangeInputElem(from, to) => s"<$from-$to>"
            case VirtualInputElem(name) => s"_${name}_"
            case EndOfFileElem => "<eof>"
            case Sequence(seq, _) => seq map { _.repr } mkString " "
            case OneOf(elems) => elems map { _.repr } mkString " | "
            case Except(elem, except) =>
                s"(${elem.repr}) except (${except map { _.repr } mkString "|"})"
            case LookaheadExcept(except) =>
                s"<lookahead not in (${except map { _.repr } mkString "|"})>"
            case Repeat(elem, range) => s"${elem.repr}[${range.repr}]"
            case Backup(elem, backup) => ???
        }
    }
}
