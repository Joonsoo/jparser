package com.giyeok.moonparser

object ParseTree {
    import Symbols._
    import Inputs._

    sealed trait ParseNode[+T <: Symbol] {
        val symbol: T
    }

    case object ParsedEmpty extends ParseNode[Empty.type] {
        val symbol = Empty
    }
    case class ParsedTerminal(symbol: Terminal, child: Input) extends ParseNode[Terminal]
    case class ParsedSymbol[T <: Symbol](symbol: T, body: ParseNode[Symbol]) extends ParseNode[T]
    case class ParsedSymbolsSeq[T <: Symbol](symbol: T, body: Seq[ParseNode[Symbol]]) extends ParseNode[T]

    object HorizontalTreeStringSeqUtil {
        def merge(list: Seq[(Int, Seq[String])]): (Int, Seq[String]) = {
            val maxSize: Int = (list maxBy { _._2.size })._2.size
            val fittedList = list map { c =>
                if (c._2.length < maxSize) (c._1, c._2 ++ Seq.fill(maxSize - c._2.size)(" " * c._1))
                else c
            }
            val mergedList = fittedList.tail.foldLeft(fittedList.head) { (memo, e) =>
                (memo._1 + 1 + e._1, memo._2 zip e._2 map { t => t._1 + " " + t._2 })
            }
            mergedList ensuring (mergedList._2.forall(_.length == mergedList._1))
        }
    }
    implicit class TreePrintableParseNode(node: ParseNode[Symbol]) {
        def printTree(): Unit = println(toTreeString("", "  "))
        def toTreeString(indent: String, indentUnit: String): String = node match {
            case ParsedEmpty =>
                indent + "()"
            case ParsedTerminal(sym, child) =>
                indent + s"- $sym('$child')"
            case ParsedSymbol(sym, body) =>
                (indent + s"- $sym\n") + body.toTreeString(indent + indentUnit, indentUnit)
            case ParsedSymbolsSeq(sym, body) =>
                (indent + s"- $sym\n") + (body map { _.toTreeString(indent + indentUnit, indentUnit) } mkString "\n")
        }

        def toHorizontalHierarchyStringSeq(): (Int, Seq[String]) = {
            def centerize(string: String, width: Int): String = {
                if (string.length >= width) string
                else {
                    val prec = (width - string.length) / 2
                    ((" " * prec) + string + (" " * (width - string.length - prec)))
                }
            }
            def appendBottom(top: (Int, Seq[String]), bottom: String) =
                if (top._1 >= bottom.length + 2) {
                    val finlen = top._1
                    val result = (finlen, top._2 :+ ("[" + centerize(bottom, finlen - 2) + "]"))
                    result ensuring (result._2.forall(_.length == result._1))
                } else if (top._1 >= bottom.length) {
                    val finlen = top._1 + 2
                    val result = (finlen, (top._2 map { " " + _ + " " }) :+ ("[" + centerize(bottom, finlen - 2) + "]"))
                    result ensuring (result._2.forall(_.length == result._1))
                } else {
                    val finlen = bottom.length + 2
                    val prec = (bottom.length - top._1) / 2
                    val (p, f) = (" " * prec, " " * (finlen - top._1 - prec))
                    val result = (finlen, (top._2 map { p + _ + f }) :+ ("[" + bottom + "]"))
                    result ensuring (result._2.forall(_.length == result._1))
                }
            val result = node match {
                case ParsedEmpty =>
                    (2, Seq("()"))
                case ParsedTerminal(sym, child) =>
                    val actual = child.toShortString
                    val symbolic = sym.toShortString
                    val finlen = math.max(actual.length, symbolic.length)
                    (finlen, Seq(centerize(actual, finlen), centerize(symbolic, finlen)))
                case ParsedSymbol(sym, body) =>
                    val actual = body.toHorizontalHierarchyStringSeq
                    val symbolic = sym.toShortString
                    appendBottom(actual, sym.toShortString)
                case ParsedSymbolsSeq(sym, body) =>
                    if (body.isEmpty) {
                        val symbolic = sym.toShortString
                        (symbolic.length + 2, Seq("[" + symbolic + "]"))
                    } else {
                        val list: Seq[(Int, Seq[String])] = body map { _.toHorizontalHierarchyStringSeq }
                        appendBottom(HorizontalTreeStringSeqUtil.merge(list), sym.toShortString)
                    }
            }
            result ensuring (result._2.forall(_.length == result._1))
        }

        def toHorizontalHierarchyString(): String =
            toHorizontalHierarchyStringSeq._2 mkString "\n"
    }
}
