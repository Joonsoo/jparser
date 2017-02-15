package com.giyeok.jparser

object Symbols {
    sealed trait Symbol {
        val id = Symbol.getId(this)
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
            def accept(input: Input) = input match {
                case Character(_, _) => true
                case AbstractInput(_: CharacterTermGroupDesc) => true
                case _ => false
            }
        }
        case class ExactChar(char: Char) extends CharacterTerminal {
            override val hashCode = char.hashCode
            def accept(input: Input) = input match {
                case Character(c, _) if char == c => true
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
            override val hashCode = chars.hashCode
            def accept(input: Input) = input match {
                case Character(c, _) if chars contains c => true
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
            override val hashCode = categories.hashCode
            def accept(input: Input) =
                input match {
                    case Character(c, _) if categories contains c.getType => true
                    case AbstractInput(termGroup) =>
                        assert(!termGroup.isEmpty)
                        termGroup match {
                            case CharsGroup(baseUnicodeCategories, excludingChars, additionalChars) if (baseUnicodeCategories subsetOf categories) && (additionalChars forall { categories contains _.getType }) =>
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
            def accept(input: Input) = input match {
                case Virtual(n, _) if n == name => true
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
            override val hashCode = names.hashCode
            def accept(input: Input) = input match {
                case Virtual(n, _) if names contains n => true
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
        override val hashCode = (classOf[Nonterminal], name).hashCode
    }
    case class Sequence(seq: Seq[AtomicSymbol], contentIdx: Seq[Int]) extends NonAtomicNonterm {
        override val hashCode = (classOf[Sequence], seq, contentIdx).hashCode
    }
    object Sequence {
        def apply(seq: Seq[AtomicSymbol]): Sequence = Sequence(seq, (0 until seq.length).toSeq)
    }
    case class OneOf(syms: Set[Symbol]) extends AtomicNonterm {
        override val hashCode = (classOf[OneOf], syms).hashCode
    }
    case class Repeat(sym: AtomicSymbol, lower: Int) extends AtomicNonterm {
        override val hashCode = (classOf[Repeat], sym, lower).hashCode

        val baseSeq = if (lower == 1) sym else Sequence(((0 until lower) map { _ => sym }).toSeq)
        val repeatSeq = Sequence(Seq(this, sym))
    }
    case class Except(sym: Symbol, except: AtomicSymbol) extends AtomicNonterm {
        override val hashCode = (classOf[Except], sym, except).hashCode
    }
    sealed trait Lookahead extends AtomicNonterm
    case class LookaheadIs(lookahead: AtomicSymbol) extends Lookahead {
        override val hashCode = (classOf[LookaheadIs], lookahead).hashCode
    }
    case class LookaheadExcept(except: AtomicSymbol) extends Lookahead {
        override val hashCode = (classOf[LookaheadExcept], except).hashCode
    }
    case class Proxy(sym: Symbol) extends AtomicNonterm {
        override val hashCode = (classOf[Proxy], sym).hashCode
    }
    object Proxy {
        def of(sym: Symbol): AtomicSymbol = sym match {
            case sym: AtomicSymbol => sym
            case sym => Proxy(sym)
        }
    }
    case class Join(sym: AtomicSymbol, join: AtomicSymbol) extends AtomicNonterm {
        assert(sym != join)
        override val hashCode = (classOf[Join], sym, join).hashCode
    }
    case class Longest(sym: Symbol) extends AtomicNonterm {
        override val hashCode = (classOf[Longest], sym).hashCode
    }
    case class EagerLongest(sym: Symbol) extends AtomicNonterm {
        override val hashCode = (classOf[EagerLongest], sym).hashCode
    }

    implicit class CharsGrouping(sym: Terminals.Chars) {
        def groups: List[(Char, Char)] = {
            def grouping(chars: List[Char], range: Option[(Char, Char)], cc: List[(Char, Char)]): List[(Char, Char)] = {
                (chars, range) match {
                    case (head +: tail, Some(range)) =>
                        if (head == range._2 + 1) grouping(tail, Some(range._1, head), cc)
                        else grouping(tail, Some(head, head), range +: cc)
                    case (head +: tail, None) => grouping(tail, Some(head, head), cc)
                    case (List(), Some(range)) => range +: cc
                    case (List(), None) => cc
                }
            }
            grouping(sym.chars.toList.sorted, None, List()).reverse
        }
    }

    implicit class ShortStringSymbols(sym: Symbol) {
        // TODO improve short string
        import com.giyeok.jparser.utils.UnicodeUtil.categoryCodeToName
        import com.giyeok.jparser.utils.UnicodeUtil.toReadable

        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case ExactChar(c) => toReadable(c)
            case chars: Terminals.Chars =>
                "(" + (chars.groups map { range =>
                    if (range._1 == range._2) s"${toReadable(range._1)}"
                    else if (range._1 + 1 == range._2) s"${toReadable(range._1)}-${toReadable(range._2)}"
                    else s"${toReadable(range._1)}-${toReadable(range._2)}"
                } mkString "|") + ")"
            case Unicode(c) => s"<unicode ${(c.toSeq.sorted map { categoryCodeToName(_) }) mkString ", "}>"
            case t: Terminal => t.toShortString
            case Start => "<start>"
            case s: Nonterminal => s.name
            case s: Sequence => "(" + (s.seq map { _.toShortString } mkString " ") + ")"
            case s: OneOf => s.syms map { _.toShortString } mkString "|"
            case Repeat(sym, lower) => s"${sym.toShortString}[$lower-]"
            case s: Except => s"${s.sym.toShortString} except ${s.except.toShortString}"
            case LookaheadIs(lookahead) => s"la_is ${lookahead.toShortString}"
            case LookaheadExcept(except) => s"la_except ${except.toShortString}"
            case Proxy(sym) => s"P(${sym.toShortString})"
            case Join(sym, join) => s"${sym.toShortString} joinedwith ${join.toShortString}"
            case Longest(sym) => s"L(${sym.toShortString})"
            case EagerLongest(sym) => s"EL(${sym.toShortString})"
        }
    }
}
