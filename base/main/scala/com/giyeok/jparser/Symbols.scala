package com.giyeok.jparser

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.unicode.UnicodeUtil

import scala.collection.immutable.ListSet

object Symbols {

    sealed trait Symbol {
        val id: Int = Symbol.getId(this)
    }

    object Symbol {
        private var symbolsMap: Map[Symbol, Int] = Map()
        private var counter = 0

        def getId(sp: Symbol): Int = {
            symbolsMap get sp match {
                case Some(i) => i
                case None =>
                    counter += 1
                    symbolsMap += ((sp, counter))
                    counter
            }
        }
    }

    // AtomicSymbol은 매칭이 되거나/안되거나 - 한 번 lift된 symbolProgress에서 derive가 되거나 하는 일은 생기지 않음
    // AtomicSymbol은 Sequence가 아닌 모든 심볼
    sealed trait AtomicSymbol extends Symbol

    // PlainAtomicSymbol은 직접적으로 새로운 accept condition을 추가시키지 않는 심볼
    sealed trait PlainAtomicSymbol extends AtomicSymbol

    sealed trait Terminal extends PlainAtomicSymbol {
        def accept(input: Inputs.Input): Boolean

        def acceptTermGroup(termGroup: Inputs.TermGroupDesc): Boolean
    }

    object Terminals {

        import Inputs._

        sealed trait CharacterTerminal extends Terminal

        sealed trait VirtualTerminal extends Terminal

        case object Any extends Terminal {
            def accept(input: Input) = true

            def acceptTermGroup(termGroup: TermGroupDesc) = true
        }

        case object AnyChar extends CharacterTerminal {
            def accept(input: Input): Boolean = input match {
                case Character(_) => true
                case _ => false
            }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = termGroup match {
                case _: CharacterTermGroupDesc => true
                case _ => false
            }
        }

        case class ExactChar(char: Char) extends CharacterTerminal {
            override val hashCode: Int = char.hashCode

            def accept(input: Input): Boolean = input match {
                case Character(c) if char == c => true
                case _ => false
            }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = {
                assert(!termGroup.isEmpty)
                termGroup match {
                    case CharsGroup(baseUnicodeCategories, excludingChars, additionalChars) if baseUnicodeCategories.isEmpty && excludingChars.isEmpty && (additionalChars == Set(char)) =>
                        true
                    case group: CharacterTermGroupDesc =>
                        assert({
                            val intersect = group intersect CharsGroup(Set(), Set(), Set(char))
                            intersect.isEmpty || intersect == group
                        })
                        false
                    case _: VirtualTermGroupDesc => false
                }
            }
        }

        case class Chars(chars: Set[Char]) extends CharacterTerminal {
            override val hashCode: Int = chars.hashCode

            def accept(input: Input): Boolean = input match {
                case Character(c) if chars contains c => true
                case _ => false
            }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = {
                assert(!termGroup.isEmpty)
                termGroup match {
                    case CharsGroup(baseUnicodeCategories, excludingChars, additionalChars) if baseUnicodeCategories.isEmpty && excludingChars.isEmpty && (additionalChars subsetOf chars) =>
                        // 사실 subsetOf일 필요도 없고 additionalChars contains chars.head 로만 해도 상관 없음
                        true
                    case group: CharacterTermGroupDesc =>
                        assert({
                            val intersect = group intersect CharsGroup(Set(), Set(), chars)
                            intersect.isEmpty || intersect == group
                        })
                        false
                    case _: VirtualTermGroupDesc => false
                }

            }
        }

        case class Unicode(categories: Set[Int]) extends CharacterTerminal {
            override val hashCode: Int = categories.hashCode

            def accept(input: Input): Boolean =
                input match {
                    case Character(c) if categories contains c.getType => true
                    case _ => false
                }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = {
                assert(!termGroup.isEmpty)
                termGroup match {
                    case CharsGroup(baseUnicodeCategories, excludingChars, additionalChars) if (baseUnicodeCategories subsetOf categories) && (additionalChars forall {
                        categories contains _.getType
                    }) =>
                        true
                    case group: CharacterTermGroupDesc =>
                        assert({
                            val intersect = group intersect CharsGroup(categories, Set(), Set())
                            intersect.isEmpty || intersect == group
                        })
                        false
                    case _: VirtualTermGroupDesc => false
                }
            }
        }

        case class ExactVirtual(name: String) extends VirtualTerminal {
            def accept(input: Input): Boolean = input match {
                case Virtual(n) if n == name => true
                case _ => false
            }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = {
                assert(!termGroup.isEmpty)
                termGroup match {
                    case VirtualsGroup(virtualNames) if virtualNames == Set(name) => true
                    case VirtualsGroup(virtualNames) if virtualNames contains name => throw new AssertionError("Invalid AbstractInput")
                    case _ => false
                }
            }
        }

        case class Virtuals(names: Set[String]) extends VirtualTerminal {
            override val hashCode: Int = names.hashCode

            def accept(input: Input): Boolean = input match {
                case Virtual(n) if names contains n => true
                case _ => false
            }

            def acceptTermGroup(termGroup: TermGroupDesc): Boolean = {
                assert(!termGroup.isEmpty)
                termGroup match {
                    case VirtualsGroup(virtualNames) if virtualNames subsetOf names => true
                    case VirtualsGroup(virtualNames) if (virtualNames intersect names).nonEmpty =>
                        throw new AssertionError("Invalid AbstractInput")
                    case _ => false
                }
            }
        }

        def compare(t1: Terminal, t2: Terminal): Int = (t1, t2) match {
            case (Any, _) => -1
            case (_, Any) => 1
            case (AnyChar, _) => -1
            case (_, AnyChar) => -1
            case (Unicode(c1), Unicode(c2)) => if (c1.min != c2.min) c1.min - c2.min else c1.max - c2.max
            case (Unicode(_), _) => -1
            case (_, Unicode(_)) => 1
            case (ExactChar(c1), ExactChar(c2)) => c1 - c2
            case (Chars(c1), ExactChar(c2)) => c1.min - c2
            case (ExactChar(c1), Chars(c2)) => c1 - c2.min
            case (Chars(c1), Chars(c2)) => if (c1.min != c2.min) c1.min - c2.min else c1.max - c2.max
            case _ => -1
        }
    }

    val Any = Terminals.Any
    val AnyChar = Terminals.AnyChar
    val ExactChar = Terminals.ExactChar
    val Chars = Terminals.Chars
    val Unicode = Terminals.Unicode

    case object Start extends PlainAtomicSymbol

    case class Nonterminal(name: String) extends PlainAtomicSymbol {
        override val hashCode: Int = (classOf[Nonterminal], name).hashCode
    }

    case class Sequence(seq: Seq[PlainAtomicSymbol]) extends Symbol {
        override val hashCode: Int = (classOf[Sequence], seq).hashCode
    }

    def Sequence(elems: PlainAtomicSymbol*): Sequence = Sequence(elems)

    case class OneOf(syms: ListSet[AtomicSymbol]) extends PlainAtomicSymbol {
        override val hashCode: Int = (classOf[OneOf], syms).hashCode
    }

    case class Repeat(sym: PlainAtomicSymbol, lower: Int) extends PlainAtomicSymbol {
        override val hashCode: Int = (classOf[Repeat], sym, lower).hashCode

        val baseSeq: Symbol = if (lower == 1) sym else Sequence((0 until lower) map { _ => sym })
        val repeatSeq = Sequence(Seq(this, sym))
    }

    case class Except(sym: AtomicSymbol, except: AtomicSymbol) extends AtomicSymbol {
        override val hashCode: Int = (classOf[Except], sym, except).hashCode
    }

    sealed trait Lookahead extends AtomicSymbol

    case class LookaheadIs(lookahead: PlainAtomicSymbol) extends Lookahead {
        override val hashCode: Int = (classOf[LookaheadIs], lookahead).hashCode
    }

    case class LookaheadExcept(except: PlainAtomicSymbol) extends Lookahead {
        override val hashCode: Int = (classOf[LookaheadExcept], except).hashCode
    }

    case class Proxy(sym: Symbol) extends PlainAtomicSymbol {
        override val hashCode: Int = (classOf[Proxy], sym).hashCode
    }

    case class Join(sym: PlainAtomicSymbol, join: PlainAtomicSymbol) extends AtomicSymbol {
        assert(sym != join)
        override val hashCode: Int = (classOf[Join], sym, join).hashCode
    }

    case class Longest(sym: PlainAtomicSymbol) extends AtomicSymbol {
        override val hashCode: Int = (classOf[Longest], sym).hashCode
    }

    implicit class ShortStringSymbols(sym: Symbol) {
        // TODO improve short string

        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case ExactChar(c) => s"'${UnicodeUtil.toReadable(c)}'"
            case chars: Terminals.Chars => "{" + chars.chars.groupedString + "}"
            case Unicode(c) => s"<unicode ${(c.toSeq.sorted map UnicodeUtil.categoryCodeToName) mkString ", "}>"
            case Start => "<start>"
            case s: Nonterminal => s.name
            case s: Sequence => "seq [" + (s.seq map (_.toShortString) mkString " ") + "]"
            case s: OneOf if s.syms.size == 2 && s.syms.contains(Proxy(Sequence(Seq()))) =>
                s"${(s.syms - Proxy(Sequence(Seq()))).head.toShortString}?"
            case s: OneOf => "{" + (s.syms map (_.toShortString) mkString "|") + "}"
            case Repeat(sym, 0) => s"${sym.toShortString}*"
            case Repeat(sym, 1) => s"${sym.toShortString}+"
            case Repeat(sym, lower) => s"${sym.toShortString}[$lower-]"
            case s: Except => s"${s.sym.toShortString}-${s.except.toShortString}"
            case LookaheadIs(lookahead) => s"la_is ${lookahead.toShortString}"
            case LookaheadExcept(except) => s"la_except ${except.toShortString}"
            case Proxy(sym) => s"(${sym.toShortString})"
            case Join(sym, join) => s"${sym.toShortString}&${join.toShortString}"
            case Longest(sym) => s"<${sym.toShortString}>"
        }
    }

}
