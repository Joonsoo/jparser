package com.giyeok.jparser

object Symbols {
    sealed trait Symbol {
        val id = Symbol.getId(this)
    }
    object Symbol {
        private var cache: Map[Symbol, Int] = Map()
        private var counter = 0
        def getId(sp: Symbol): Int = {
            cache get sp match {
                case Some(i) => i
                case None =>
                    counter += 1
                    cache += ((sp, counter))
                    counter
            }
        }
    }
    // AtomicSymbol은 매칭이 되거나/안되거나 - 한 번 lift된 symbolProgress에서 derive가 되거나 하는 일은 생기지 않음
    sealed trait AtomicSymbol extends Symbol
    // NonAtomicSymbol은 repeat/seq 밖에 없음
    sealed trait NonAtomicSymbol extends Symbol

    sealed trait Terminal extends AtomicSymbol {
        def accept(input: Inputs.Input): Boolean
    }
    object Terminals {
        import Inputs.{ Input, Character, Virtual, AbstractInput, CharsGroup, CharsUnicodeExcluding, AllCharsExcluding, VirtualsGroup }

        sealed trait CharacterTerminal extends Terminal
        sealed trait VirtualTerminal extends Terminal
        case object Any extends Terminal {
            def accept(input: Input) = true
        }
        case object AnyChar extends CharacterTerminal {
            def accept(input: Input) = input match {
                case Character(_, _) => true
                case AbstractInput(CharsGroup(_) | CharsUnicodeExcluding(_, _)) => true
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
                        case CharsGroup(set) if set == Set(char) => true
                        case CharsGroup(set) if set contains char =>
                            throw new AssertionError("Invalid AbstractInput")
                        case CharsUnicodeExcluding(unicodeCategories, excludingChars) if (unicodeCategories contains char.getType) && !(excludingChars contains char) =>
                            throw new AssertionError("Invalid AbstractInput")
                        case AllCharsExcluding(excludingCategories, excludingChars) if !(excludingCategories contains char.getType) || !(excludingChars contains char) =>
                            throw new AssertionError("Invalid AbstractInput")
                        case _ => false
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
                        case CharsGroup(set) if set subsetOf chars => true
                        case CharsGroup(set) if !(set intersect chars).isEmpty =>
                            throw new AssertionError("Invalid AbstractInput")
                        case CharsUnicodeExcluding(unicodeCategories, excludingChars) if !((chars filter { unicodeCategories contains _.getType }) subsetOf excludingChars) =>
                            throw new AssertionError("Invalid AbstractInput")
                        case AllCharsExcluding(excludingCategories, excludingChars) if !(chars forall { c => (excludingCategories contains c.getType) || (excludingChars contains c) }) =>
                            throw new AssertionError("Invalid AbstractInput")
                        case _ => false
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
                            case CharsGroup(chars) if chars forall { categories contains _.getType } => true
                            case CharsGroup(chars) if chars exists { categories contains _.getType } => false
                            case CharsUnicodeExcluding(unicodeCategories, _) if unicodeCategories subsetOf categories =>
                                true
                            case CharsUnicodeExcluding(unicodeCategories, _) if !(unicodeCategories intersect categories).isEmpty =>
                                throw new AssertionError("Invalid AbstractInput")
                            case AllCharsExcluding(excludingCategories, _) if !(categories subsetOf excludingCategories) =>
                                throw new AssertionError("Invalid AbstractInput")
                            case _ => false
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
                        case VirtualsGroup(virtualNames) if !(virtualNames intersect names).isEmpty =>
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

    val Any = Terminals.Any
    val AnyChar = Terminals.AnyChar
    val ExactChar = Terminals.ExactChar
    val Chars = Terminals.Chars
    val Unicode = Terminals.Unicode

    case object Empty extends Nonterm
    case object Start extends Nonterm with AtomicSymbol
    case class Nonterminal(name: String) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[Nonterminal], name).hashCode
    }
    case class Sequence(seq: Seq[Symbol], whitespace: Set[Symbol]) extends Nonterm with NonAtomicSymbol {
        override val hashCode = (classOf[Sequence], seq, whitespace).hashCode
    }
    case class OneOf(syms: Set[Symbol]) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[OneOf], syms).hashCode
    }
    sealed trait Repeat extends Nonterm with NonAtomicSymbol {
        val sym: Symbol
    }
    case class RepeatBounded(sym: Symbol, lower: Int, upper: Int) extends Repeat {
        override val hashCode = (classOf[RepeatBounded], sym, lower, upper).hashCode
    }
    case class RepeatUnbounded(sym: Symbol, lower: Int) extends Repeat {
        override val hashCode = (classOf[RepeatUnbounded], sym, lower).hashCode
    }
    case class Except(sym: Symbol, except: Symbol) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[Except], sym, except).hashCode
    }
    case class LookaheadExcept(except: Symbol) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[LookaheadExcept], except).hashCode
    }
    case class Proxy(val sym: Symbol) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[Proxy], sym).hashCode
    }
    object Proxy {
        def of(sym: Symbol): AtomicSymbol = sym match {
            case sym: AtomicSymbol => sym
            case sym => Proxy(sym)
        }
    }
    case class Backup(sym: AtomicSymbol, backup: AtomicSymbol) extends Nonterm with AtomicSymbol {
        def this(sym: Symbol, backup: Symbol) = this(Proxy.of(sym), Proxy.of(backup))
        override val hashCode = (classOf[Backup], sym, backup).hashCode
    }
    case class Join(sym: AtomicSymbol, join: AtomicSymbol) extends Nonterm with AtomicSymbol {
        assert(sym != join)
        def this(sym: Symbol, join: Symbol) = this(Proxy.of(sym), Proxy.of(join))
        override val hashCode = (classOf[Join], sym, join).hashCode
    }
    case class Longest(sym: Symbol) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[Longest], sym).hashCode
    }
    case class EagerLongest(sym: Symbol) extends Nonterm with AtomicSymbol {
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
        import com.giyeok.jparser.utils.UnicodeUtil.{ toReadable, categoryCodeToName }

        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case ExactChar(c) => toReadable(c)
            case chars: Terminals.Chars =>
                "(" + (chars.groups map { range =>
                    if (range._1 == range._2) s"'${toReadable(range._1)}'"
                    else if (range._1 + 1 == range._2) s"'${toReadable(range._1)}'-'${toReadable(range._2)}'"
                    else s"'${toReadable(range._1)}'-'${toReadable(range._2)}'"
                } mkString "|") + ")"
            case Unicode(c) => s"<unicode ${(c.toSeq.sorted map { categoryCodeToName(_) }) mkString ", "}>"
            case t: Terminal => t.toShortString
            case Empty => "<empty>"
            case Start => "<start>"
            case s: Nonterminal => s.name
            case s: Sequence => "(" + (s.seq map { _.toShortString } mkString " ") + ")"
            case s: OneOf => s.syms map { _.toShortString } mkString "|"
            case RepeatBounded(sym, lower, upper) => s"${sym.toShortString}[$lower-$upper]"
            case RepeatUnbounded(sym, lower) => s"${sym.toShortString}[$lower-]"
            case s: Except => s"${s.sym.toShortString} except ${s.except.toShortString}"
            case LookaheadExcept(except) => s"la_except ${except.toShortString}"
            case Proxy(sym) => s"P(${sym.toShortString})"
            case Backup(sym, backup) => s"${sym.toShortString} backedupby ${backup.toShortString}"
            case Join(sym, join) => s"${sym.toShortString} joinedwith ${join.toShortString}"
            case Longest(sym) => s"L(${sym.toShortString})"
            case EagerLongest(sym) => s"EL(${sym.toShortString})"
        }
    }
}
