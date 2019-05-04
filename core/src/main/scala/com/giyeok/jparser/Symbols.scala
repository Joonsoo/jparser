package com.giyeok.jparser

import com.giyeok.jparser.Inputs.CharsGrouping

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
    sealed trait AtomicSymbol extends Symbol

    // NonAtomicSymbol은 sequence 밖에 없음
    sealed trait NonAtomicSymbol extends Symbol

    sealed trait Terminal extends Symbol with AtomicSymbol {
        def accept(input: Inputs.Input): Boolean

        def accept(termGroup: Inputs.TermGroupDesc): Boolean = accept(Inputs.AbstractInput(termGroup))
    }

    object Terminals {

        import Inputs._

        sealed trait CharacterTerminal extends Terminal

        sealed trait VirtualTerminal extends Terminal

        case object Any extends Terminal {
            def accept(input: Input) = true
        }

        case object AnyChar extends CharacterTerminal {
            def accept(input: Input): Boolean = input match {
                case Character(_) => true
                case AbstractInput(_: CharacterTermGroupDesc) => true
                case _ => false
            }
        }

        case class ExactChar(char: Char) extends CharacterTerminal {
            override val hashCode: Int = char.hashCode

            def accept(input: Input): Boolean = input match {
                case Character(c) if char == c => true
                case AbstractInput(termGroup) =>
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
                case _ => false
            }
        }

        case class Chars(chars: Set[Char]) extends CharacterTerminal {
            override val hashCode: Int = chars.hashCode

            def accept(input: Input): Boolean = input match {
                case Character(c) if chars contains c => true
                case AbstractInput(termGroup) =>
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
                case _ => false
            }
        }

        case class Unicode(categories: Set[Int]) extends CharacterTerminal {
            override val hashCode: Int = categories.hashCode

            def accept(input: Input): Boolean =
                input match {
                    case Character(c) if categories contains c.getType => true
                    case AbstractInput(termGroup) =>
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
                    case _ => false
                }
        }

        case class ExactVirtual(name: String) extends VirtualTerminal {
            def accept(input: Input): Boolean = input match {
                case Virtual(n) if n == name => true
                case AbstractInput(termGroup) =>
                    assert(!termGroup.isEmpty)
                    termGroup match {
                        case VirtualsGroup(virtualNames) if virtualNames == Set(name) => true
                        case VirtualsGroup(virtualNames) if virtualNames contains name => throw new AssertionError("Invalid AbstractInput")
                        case _ => false
                    }
                case _ => false
            }
        }

        case class Virtuals(names: Set[String]) extends VirtualTerminal {
            override val hashCode: Int = names.hashCode

            def accept(input: Input): Boolean = input match {
                case Virtual(n) if names contains n => true
                case AbstractInput(termGroup) =>
                    assert(!termGroup.isEmpty)
                    termGroup match {
                        case VirtualsGroup(virtualNames) if virtualNames subsetOf names => true
                        case VirtualsGroup(virtualNames) if (virtualNames intersect names).nonEmpty =>
                            throw new AssertionError("Invalid AbstractInput")
                        case _ => false
                    }
                case _ => false
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

    sealed trait Nonterm extends Symbol

    sealed trait AtomicNonterm extends Nonterm with AtomicSymbol

    sealed trait NonAtomicNonterm extends Nonterm with NonAtomicSymbol

    val Any = Terminals.Any
    val AnyChar = Terminals.AnyChar
    val ExactChar = Terminals.ExactChar
    val Chars = Terminals.Chars
    val Unicode = Terminals.Unicode

    case object Start extends AtomicNonterm

    case class Nonterminal(name: String) extends AtomicNonterm {
        override val hashCode: Int = (classOf[Nonterminal], name).hashCode
    }

    case class Sequence(seq: Seq[AtomicSymbol], contentIdx: Seq[Int]) extends NonAtomicNonterm {
        override val hashCode: Int = (classOf[Sequence], seq, contentIdx).hashCode
    }

    object Sequence {
        def apply(seq: Seq[AtomicSymbol]): Sequence = Sequence(seq, seq.indices)
    }

    case class OneOf(syms: Set[AtomicSymbol]) extends AtomicNonterm {
        override val hashCode: Int = (classOf[OneOf], syms).hashCode
    }

    case class Repeat(sym: AtomicSymbol, lower: Int) extends AtomicNonterm {
        override val hashCode: Int = (classOf[Repeat], sym, lower).hashCode

        val baseSeq: Symbol = if (lower == 1) sym else Sequence((0 until lower) map { _ => sym })
        val repeatSeq = Sequence(Seq(this, sym))
    }

    case class Except(sym: AtomicSymbol, except: AtomicSymbol) extends AtomicNonterm {
        override val hashCode: Int = (classOf[Except], sym, except).hashCode
    }

    sealed trait Lookahead extends AtomicNonterm

    case class LookaheadIs(lookahead: AtomicSymbol) extends Lookahead {
        override val hashCode: Int = (classOf[LookaheadIs], lookahead).hashCode
    }

    case class LookaheadExcept(except: AtomicSymbol) extends Lookahead {
        override val hashCode: Int = (classOf[LookaheadExcept], except).hashCode
    }

    case class Proxy(sym: Symbol) extends AtomicNonterm {
        override val hashCode: Int = (classOf[Proxy], sym).hashCode
    }

    case class Join(sym: AtomicSymbol, join: AtomicSymbol) extends AtomicNonterm {
        assert(sym != join)
        override val hashCode: Int = (classOf[Join], sym, join).hashCode
    }

    case class Longest(sym: AtomicSymbol) extends AtomicNonterm {
        override val hashCode: Int = (classOf[Longest], sym).hashCode
    }

    implicit class ShortStringSymbols(sym: Symbol) {
        // TODO improve short string
        import com.giyeok.jparser.utils.UnicodeUtil.{categoryCodeToName, toReadable}

        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case ExactChar(c) => s"'${toReadable(c)}'"
            case chars: Terminals.Chars =>
                "{" + chars.chars.groupedString + "}"
            case Unicode(c) => s"<unicode ${
                (c.toSeq.sorted map {
                    categoryCodeToName
                }) mkString ", "
            }>"
            case Start => "<start>"
            case s: Nonterminal => s.name
            case s: Sequence => "[" + (s.seq map {
                _.toShortString
            } mkString " ") + "]"
            case s: OneOf if s.syms.size == 2 && s.syms.contains(Proxy(Sequence(Seq(), Seq()))) =>
                s"${(s.syms - Proxy(Sequence(Seq(), Seq()))).head.toShortString}?"
            case s: OneOf => s.syms map {
                _.toShortString
            } mkString "|"
            case Repeat(sym, 0) => s"${sym.toShortString}*"
            case Repeat(sym, 1) => s"${sym.toShortString}+"
            case Repeat(sym, lower) => s"${sym.toShortString}[$lower-]"
            case s: Except => s"${s.sym.toShortString} except ${s.except.toShortString}"
            case LookaheadIs(lookahead) => s"la_is ${lookahead.toShortString}"
            case LookaheadExcept(except) => s"la_except ${except.toShortString}"
            case Proxy(sym) => s"(${sym.toShortString})"
            case Join(sym, join) => s"${sym.toShortString} joinedwith ${join.toShortString}"
            case Longest(sym) => s"L(${sym.toShortString})"
        }
    }

}
