package com.giyeok.moonparser

import com.giyeok.moonparser.utils.UnicodeUtil

object Inputs {
    type Location = Int

    sealed trait Input
    case class Character(char: Char, location: Location) extends Input
    case class Virtual(name: String, location: Location) extends Input
    case class AbstractInput(termGroup: TermGroupDesc) extends Input

    sealed trait TermGroupDesc {
        def toShortString: String
        def isEmpty: Boolean
    }
    sealed trait CharacterTermGroupDesc extends TermGroupDesc {
        def -(other: CharacterTermGroupDesc): CharacterTermGroupDesc
        def intersect(other: CharacterTermGroupDesc): CharacterTermGroupDesc
    }
    sealed trait VirtualTermGroupDesc extends TermGroupDesc {
        def -(other: VirtualTermGroupDesc): VirtualTermGroupDesc
        def intersect(other: VirtualTermGroupDesc): VirtualTermGroupDesc
    }
    case class AllCharsExcluding(excludingUnicodeCategories: Set[Int], excludingChars: Set[Char]) extends CharacterTermGroupDesc {
        // 이미 excludingUnicodeCategories에서 빠진 문자가 excludingChars에 또 들어있으면 안된다
        assert(excludingChars forall { c => !(excludingUnicodeCategories contains c.getType) })

        def -(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case AllCharsExcluding(otherExcludingUnicodeCategories, otherExcludingChars) => ???
            case other: CharsUnicodeExcluding =>
                AllCharsExcluding.of(excludingUnicodeCategories ++ other.unicodeCategories, excludingChars ++ other.excludingChars)
            case CharsGroup(chars) =>
                AllCharsExcluding.of(excludingUnicodeCategories, excludingChars ++ chars)
        }
        def intersect(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case other: AllCharsExcluding =>
                AllCharsExcluding.of(excludingUnicodeCategories ++ other.excludingUnicodeCategories, excludingChars ++ other.excludingChars)
            case other: CharsUnicodeExcluding =>
                CharsUnicodeExcluding.of(other.unicodeCategories, excludingChars ++ other.excludingChars)
            case other: CharsGroup =>
                CharsGroup(other.chars -- excludingChars filterNot { excludingUnicodeCategories contains _.getType })
        }

        def toShortString: String = "AllCharsExcluding(" + (excludingChars.toSeq.sorted map { UnicodeUtil.toReadable _ } mkString "")
        def isEmpty = false
    }
    object AllCharsExcluding {
        def of(excludingCategories: Set[Int], excludingChars: Set[Char]): AllCharsExcluding =
            AllCharsExcluding(excludingCategories, excludingChars filterNot { excludingCategories contains _.getType })
    }
    case class CharsUnicodeExcluding(unicodeCategories: Set[Int], excludingChars: Set[Char]) extends CharacterTermGroupDesc {
        // base가 되는 unicodeCategories에 속하지 않은 excludingChars가 있으면 안된다
        assert(excludingChars forall { unicodeCategories contains _.getType })

        def -(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case AllCharsExcluding(otherExcludingUnicodeCategories, otherExcludingChars) => ???
            case other: CharsUnicodeExcluding =>
                CharsUnicodeExcluding.of(unicodeCategories -- other.unicodeCategories, excludingChars ++ other.excludingChars)
            case other: CharsGroup =>
                CharsUnicodeExcluding.of(unicodeCategories, excludingChars ++ other.chars)
        }
        def intersect(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case AllCharsExcluding(otherExcludingUnicodeCategories, otherExcludingChars) => ???
            case other: CharsUnicodeExcluding =>
                CharsUnicodeExcluding(unicodeCategories intersect other.unicodeCategories, excludingChars ++ other.excludingChars)
            case other: CharsGroup =>
                CharsGroup((other.chars filter { unicodeCategories contains _.getType }) -- excludingChars)
        }

        def contains(char: Char) = (unicodeCategories contains char.getType) && !(excludingChars contains char)

        def toShortString: String = "CharsUnicodeExcluding(" + (unicodeCategories.toSeq.sorted map { UnicodeUtil.categoryCodeToName _ } mkString ",") + "-" + (excludingChars.toSeq.sorted map { UnicodeUtil.toReadable _ } mkString "") + ")"
        def isEmpty = unicodeCategories.isEmpty
    }
    object CharsUnicodeExcluding {
        def of(categories: Set[Int], excludingChars: Set[Char]): CharsUnicodeExcluding =
            // TODO 혹시 excludingChars가 category 전부를 포함하는 경우 categories에서 제외해주기
            CharsUnicodeExcluding(categories, excludingChars filter { categories contains _.getType })
    }
    case class CharsGroup(chars: Set[Char]) extends CharacterTermGroupDesc {
        def -(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case AllCharsExcluding(otherExcludingUnicodeCategories, otherExcludingChars) => ???
            case other: CharsUnicodeExcluding =>
                CharsGroup(chars filterNot { other contains _ })
            case other: CharsGroup =>
                CharsGroup(chars -- other.chars)
        }
        def intersect(other: CharacterTermGroupDesc): CharacterTermGroupDesc = other match {
            case AllCharsExcluding(otherExcludingUnicodeCategories, otherExcludingChars) => ???
            case other: CharsUnicodeExcluding =>
                CharsGroup(chars filter { other contains _ })
            case other: CharsGroup =>
                CharsGroup(chars intersect other.chars)
        }

        def toShortString: String = "CharsGroup(" + (chars.toSeq.sorted map { UnicodeUtil.toReadable _ } mkString "") + ")"
        def isEmpty = chars.isEmpty
    }

    case class VirtualsGroup(virtualNames: Set[String]) extends VirtualTermGroupDesc {
        def -(other: VirtualTermGroupDesc): VirtualsGroup = other match {
            case VirtualsGroup(otherVirtualNames) => VirtualsGroup(virtualNames -- otherVirtualNames)
        }
        def intersect(other: VirtualTermGroupDesc): VirtualsGroup = other match {
            case VirtualsGroup(otherVirtualNames) => VirtualsGroup(virtualNames intersect otherVirtualNames)
        }

        def toShortString: String = virtualNames.toSeq.sorted mkString ","
        def isEmpty = virtualNames.isEmpty
    }

    object TermGroupDesc {
        import Symbols.Terminals._

        def descOf(term: CharacterTerminal): CharacterTermGroupDesc = term match {
            case AnyChar => AllCharsExcluding(Set(), Set())
            case ExactChar(char) => CharsGroup(Set(char))
            case Chars(chars) => CharsGroup(chars)
            case Unicode(categories) => CharsUnicodeExcluding(categories, Set())
        }
        def descOf(term: VirtualTerminal): VirtualTermGroupDesc = term match {
            case ExactVirtual(name) => VirtualsGroup(Set(name))
            case Virtuals(names) => VirtualsGroup(names)
        }
    }

    type Source = Iterable[Input]

    implicit class InputToShortString(input: Input) {
        def toShortString: String = input match {
            case Character(char, _) =>
                char match {
                    case '\n' => "\\n"
                    case '\t' => "\\t"
                    case '\\' => "\\\\"
                    case _ => s"$char"
                }
            case Virtual(name, _) => s"<$name>"
            case AbstractInput(chars) => s"{${chars.toShortString}}"
        }

        def toCleanString: String = input match {
            case Character(char, _) =>
                char match {
                    case '\n' => "\\n"
                    case '\r' => "\\r"
                    case '\t' => "\\t"
                    case c => c.toString
                }
            case Virtual(name, _) => s"{$name}"
            case AbstractInput(chars) => s"{${chars.toShortString}}"
        }
    }
    implicit class SourceToCleanString(source: Source) {
        def toCleanString: String = (source map { _.toCleanString }).mkString
    }

    def fromString(source: String): Seq[Input] =
        source.toCharArray.zipWithIndex map { ci => Character(ci._1, ci._2) }
}
