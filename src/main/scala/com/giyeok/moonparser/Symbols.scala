package com.giyeok.moonparser

object Symbols {
    sealed trait Symbol
    // AtomicSymbol은 매칭이 되거나/안되거나 - 한 번 lift된 symbolProgress에서 derive가 되거나 하는 일은 생기지 않음
    sealed trait AtomicSymbol extends Symbol

    sealed trait Terminal extends AtomicSymbol {
        def accept(input: Inputs.Input): Boolean
    }
    object Terminals {
        import Inputs.{ Input, Character, Virtual }
        case object Any extends Terminal {
            def accept(input: Input) = true
        }
        case object AnyChar extends Terminal {
            def accept(input: Input) =
                input.isInstanceOf[Character]
        }
        /**
         * `func` must be referential transparent function
         * if not, the behavior of the parser is unpredictable
         */
        case class FuncChar(func: Char => Boolean) extends Terminal {
            def accept(input: Input) = input match {
                case Character(c, _) if func(c) => true
                case _ => false
            }
        }
        case class ExactChar(char: Char) extends Terminal {
            override val hashCode = char.hashCode
            def accept(input: Input) = input match {
                case Character(c, _) if char == c => true
                case _ => false
            }
        }
        case class Chars(chars: Set[Char]) extends Terminal {
            override val hashCode = chars.hashCode
            def accept(input: Input) = input match {
                case Character(c, _) if chars contains c => true
                case _ => false
            }
        }
        case class Unicode(categories: Set[Byte]) extends Terminal {
            override val hashCode = categories.hashCode
            def accept(input: Input) =
                input match {
                    case Character(c, _) if categories contains c.getType.toByte => true
                    case _ => false
                }
        }
        case class ExactVirtual(name: String) extends Terminal {
            def accept(input: Input) = input match {
                case Virtual(n, _) if n == name => true
                case _ => false
            }
        }
        case class Virtuals(names: Set[String]) extends Terminal {
            override val hashCode = names.hashCode
            def accept(input: Input) = input match {
                case Virtual(n, _) if names contains n => true
                case _ => false
            }
        }
    }
    sealed trait Nonterm extends Symbol

    val Any = Terminals.Any
    val AnyChar = Terminals.AnyChar
    val FuncChar = Terminals.FuncChar
    val ExactChar = Terminals.ExactChar
    val Chars = Terminals.Chars
    val Unicode = Terminals.Unicode

    case object Empty extends Nonterm
    case class Nonterminal(name: String) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[Nonterminal], name).hashCode
    }
    case class Sequence(seq: Seq[Symbol], whitespace: Set[Symbol]) extends Nonterm {
        override val hashCode = (classOf[Sequence], seq, whitespace).hashCode
    }
    case class OneOf(syms: Set[Symbol]) extends Nonterm with AtomicSymbol {
        override val hashCode = (classOf[OneOf], syms).hashCode
    }
    case class Repeat(sym: Symbol, range: Repeat.Range) extends Nonterm {
        override val hashCode = (classOf[Repeat], sym, range).hashCode
    }
    object Repeat {
        trait Range {
            def contains(v: Int): Boolean
            def canProceed(x: Int): Boolean
            val isNullable: Boolean
            def toShortString: String
        }
        case class RangeFrom(val from: Int) extends Range {
            override def contains(v: Int) = from <= v
            override def canProceed(x: Int): Boolean = true
            override val isNullable = from == 0

            override val hashCode = from

            def toShortString = s"$from-"
        }
        case class RangeTo(val from: Int, val to: Int) extends Range {
            override def contains(v: Int) = from <= v && v <= to
            override def canProceed(x: Int): Boolean = x < to
            override val isNullable = from == 0

            override val hashCode = (from, to).hashCode

            def toShortString = s"$from-$to"
        }
    }
    case class Except(sym: Symbol, except: Symbol) extends Nonterm {
        override val hashCode = (classOf[Except], sym, except).hashCode
    }
    case class LookaheadExcept(except: Symbol) extends Nonterm {
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
    case class Backup(sym: AtomicSymbol, backup: AtomicSymbol) extends Nonterm {
        def this(sym: Symbol, backup: Symbol) = this(Proxy.of(sym), Proxy.of(backup))
        override val hashCode = (classOf[Backup], sym, backup).hashCode
    }
    case class Join(sym: AtomicSymbol, join: AtomicSymbol) extends Nonterm {
        def this(sym: Symbol, join: Symbol) = this(Proxy.of(sym), Proxy.of(join))
        override val hashCode = (classOf[Join], sym, join).hashCode
    }
    case class Longest(sym: Symbol) extends Nonterm {
        override val hashCode = (classOf[Longest], sym).hashCode
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
        def toReadable(c: Char): String = c match {
            case '\n' => "\\n"
            case '\t' => "\\t"
            case '\\' => "\\\\"
            case _ => c.toString
        }
        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case FuncChar => "<func>"
            case ExactChar(c) => toReadable(c)
            case chars: Terminals.Chars =>
                "(" + (chars.groups map { range =>
                    if (range._1 == range._2) s"'${toReadable(range._1)}'"
                    else if (range._1 + 1 == range._2) s"'${toReadable(range._1)}'-'${toReadable(range._2)}'"
                    else s"'${toReadable(range._1)}'-'${toReadable(range._2)}'"
                } mkString "|") + ")"
            case Unicode(c) => s"<unicode ${(c.toSeq map { cid => cid }).sorted mkString ", "}>"
            case t: Terminal => t.toShortString
            case Empty => "<empty>"
            case s: Nonterminal => s.name
            case s: Sequence => "(" + (s.seq map { _.toShortString } mkString " ") + ")"
            case s: OneOf => s.syms map { _.toShortString } mkString "|"
            case s: Repeat => s"${s.sym.toShortString}[${s.range.toShortString}]"
            case s: Except => s"${s.sym.toShortString} except ${s.except.toShortString}"
            case LookaheadExcept(except) => s"la_except ${except.toShortString}"
            case Proxy(sym) => s"P(${sym.toShortString})"
            case Backup(sym, backup) => s"${sym.toShortString} backedupby ${backup.toShortString}"
            case Join(sym, join) => s"${sym.toShortString} joinedwith ${join.toShortString}"
            case Longest(sym) => s"L(${sym.toShortString})"
        }
    }
}
