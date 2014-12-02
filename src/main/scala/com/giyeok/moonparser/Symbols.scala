package com.giyeok.moonparser

object Symbols {
    sealed abstract class Symbol

    abstract class Terminal extends Symbol {
        def accept(input: Inputs.Input): Boolean
    }
    object Terminals {
        import Inputs.{ Input, Character, Virtual, EOF }
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
        case object EndOfFile extends Terminal {
            def accept(input: Input) =
                input.isInstanceOf[EOF]
        }
    }
    val Any = Terminals.Any
    val AnyChar = Terminals.AnyChar
    val FuncChar = Terminals.FuncChar
    val ExactChar = Terminals.ExactChar
    val Chars = Terminals.Chars
    val Unicode = Terminals.Unicode
    val EndOfFile = Terminals.EndOfFile

    case object Empty extends Symbol
    case class Nonterminal(name: String) extends Symbol {
        override val hashCode = name.hashCode
    }
    case class Sequence(seq: Seq[Symbol], whitespace: Set[Symbol]) extends Symbol {
        override val hashCode = (seq, whitespace).hashCode
    }
    case class OneOf(syms: Set[Symbol]) extends Symbol {
        override val hashCode = syms.hashCode
    }
    case class Except(sym: Symbol, except: Symbol) extends Symbol {
        override val hashCode = (sym, except).hashCode
    }
    case class Repeat(sym: Symbol, range: Repeat.Range) extends Symbol {
        override val hashCode = (sym, range).hashCode
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
    case class LookaheadExcept(except: Symbol) extends Symbol {
        override val hashCode = except.hashCode
    }
    case class Backup(sym: Symbol, backup: Symbol) extends Symbol {
        override val hashCode = (sym, backup).hashCode
    }

    implicit class ShortStringSymbol(sym: Symbol) {
        // TODO improve short string
        def toShortString: String = sym match {
            case Any => "<any>"
            case AnyChar => "<any>"
            case FuncChar => "<func>"
            case ExactChar(c) => s"'$c'"
            case Chars(cs) =>
                def generateCharSetShortRepr(chars: List[Char], latestRange: Option[(Char, Char)]): String = {
                    def makeRepr(range: (Char, Char)): String =
                        if (range._1 == range._2) s"'${range._1}'"
                        else if (range._1 + 1 == range._2) s"'${range._1}','${range._2}'"
                        else s"'${range._1}'-'${range._2}'"
                    (chars, latestRange) match {
                        case (head +: tail, Some(range)) =>
                            if (head == range._2 + 1) generateCharSetShortRepr(tail, Some(range._1, head))
                            else makeRepr(range) + "," + generateCharSetShortRepr(tail, Some(head, head))
                        case (head +: tail, None) => generateCharSetShortRepr(tail, Some(head, head))
                        case (List(), Some(range)) => makeRepr(range)
                        case (List(), None) => ""
                    }
                }
                s"{${generateCharSetShortRepr(cs.toList.sorted, None)}}"
            case Unicode(c) => s"<unicode>"
            case EndOfFile => "<eof>"
            case s: Nonterminal => s.name
            case s: Sequence => s.seq map { _.toShortString } mkString " "
            case s: OneOf => s.syms map { _.toShortString } mkString "|"
            case s: Except => s"${s.sym.toShortString}-${s.except.toShortString}"
            case s: Repeat => s"${s.sym.toShortString}[${s.range.toShortString}]"
            case s =>
                println(s)
                s.toString
        }
    }
}
