package com.giyeok.moonparser

object ParseTree {
    import Symbols._
    import Inputs._

    sealed trait ParseNode[+T <: Symbol] {
        val symbol: T
    }

    case class ParsedEmpty[T <: Symbol](symbol: T) extends ParseNode[T]
    case class ParsedTerminal(symbol: Terminal, child: Input) extends ParseNode[Terminal]
    case class ParsedSymbol[T <: AtomicSymbol](symbol: T, body: ParseNode[Symbol]) extends ParseNode[T]
    case class ParsedSymbolsSeq[T <: NonAtomicSymbol](symbol: T, _childrenWS: List[ParseNode[Symbol]], _childrenIdx: List[Int]) extends ParseNode[T] {
        // childrenIdx: index of childrenWS
        // _childrenIdx: reverse of chidlrenIdx

        val childrenSize = _childrenIdx.length

        lazy val childrenIdx = _childrenIdx.reverse
        lazy val childrenWS = _childrenWS.reverse
        lazy val children: List[ParseNode[Symbol]] = {
            def pick(_childrenIdx: List[Int], _childrenWS: List[ParseNode[Symbol]], current: Int, cc: List[ParseNode[Symbol]]): List[ParseNode[Symbol]] =
                if (_childrenIdx.isEmpty) cc else {
                    val dropped = _childrenWS drop (current - _childrenIdx.head)
                    pick(_childrenIdx.tail, dropped.tail, _childrenIdx.head - 1, dropped.head +: cc)
                }
            pick(_childrenIdx, _childrenWS, _childrenWS.length - 1, List())
        }

        def appendWhitespace(wsChild: ParseNode[Symbol]): ParsedSymbolsSeq[T] =
            ParsedSymbolsSeq[T](symbol, wsChild +: _childrenWS, _childrenIdx)
        def appendContent(child: ParseNode[Symbol]): ParsedSymbolsSeq[T] =
            ParsedSymbolsSeq[T](symbol, child +: _childrenWS, (_childrenWS.length) +: _childrenIdx)
    }
    class ParsedSymbolJoin(symbol: Join, body: ParseNode[Symbol], val constraint: ParseNode[Symbol]) extends ParsedSymbol[Join](symbol, body)
    object ParsedSymbolJoin {
        def unapply(j: ParsedSymbolJoin): Option[(Join, ParseNode[Symbol], ParseNode[Symbol])] =
            Some((j.symbol, j.body, j.constraint))
    }

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
    implicit class ShortString(node: ParseNode[Symbol]) {
        def toShortString: String = node match {
            case ParsedEmpty(sym) => s"${sym.toShortString}()"
            case ParsedTerminal(sym, child) => s"${sym.toShortString}(${child.toShortString})"
            case ParsedSymbol(sym, body) => s"${sym.toShortString}(${body.toShortString})"
            case s @ ParsedSymbolsSeq(_, _, _) => s"${s.symbol.toShortString}(${s.children map { _.toShortString } mkString "/"})"
            case ParsedSymbolJoin(sym, body, join) => s"${sym.toShortString}(${body.toShortString})"
        }
    }
    implicit class TreePrintableParseNode(node: ParseNode[Symbol]) {
        def printTree(): Unit = println(toTreeString("", "  "))
        def toTreeString(indent: String, indentUnit: String): String = node match {
            case ParsedEmpty(sym) =>
                indent + s"- $sym"
            case ParsedTerminal(sym, child) =>
                indent + s"- $sym('$child')"
            case ParsedSymbol(sym, body) =>
                (indent + s"- $sym\n") + body.toTreeString(indent + indentUnit, indentUnit)
            case s @ ParsedSymbolsSeq(_, _, _) =>
                (indent + s"- ${s.symbol}\n") + (s.children map { _.toTreeString(indent + indentUnit, indentUnit) } mkString "\n")
            case ParsedSymbolJoin(sym, body, join) =>
                (indent + s"- $sym\n") + (body.toTreeString(indent + indentUnit, indentUnit)) + "\n" +
                    (indent + s"- $sym\n") + (join.toTreeString(indent + indentUnit, indentUnit))
        }

        def toHorizontalHierarchyStringSeq(): (Int, Seq[String]) = {
            def centerize(string: String, width: Int): String = {
                if (string.length >= width) string
                else {
                    val prec = (width - string.length) / 2
                    ((" " * prec) + string + (" " * (width - string.length - prec)))
                }
            }
            def appendBottom(top: (Int, Seq[String]), bottom: String): (Int, Seq[String]) =
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
                    val prec = (finlen - top._1) / 2
                    val (p, f) = (" " * prec, " " * (finlen - top._1 - prec))
                    val result = (finlen, (top._2 map { p + _ + f }) :+ ("[" + bottom + "]"))
                    result ensuring (result._2.forall(_.length == result._1))
                }
            val result: (Int, Seq[String]) = node match {
                case ParsedEmpty(sym) =>
                    val symbolic = sym.toShortString
                    val finlen = math.max(2, symbolic.length)
                    (finlen, Seq(centerize("()", finlen), centerize(symbolic, finlen)))
                case ParsedTerminal(sym, child) =>
                    val actual = child.toShortString
                    val symbolic = sym.toShortString
                    val finlen = math.max(actual.length, symbolic.length)
                    (finlen, Seq(centerize(actual, finlen), centerize(symbolic, finlen)))
                case ParsedSymbolJoin(sym, body, join) =>
                    val actual = body.toHorizontalHierarchyStringSeq
                    val actualJoin = join.toHorizontalHierarchyStringSeq
                    val symbolic = sym.toShortString
                    appendBottom(actual, sym.toShortString)
                case ParsedSymbol(sym, body) =>
                    val actual = body.toHorizontalHierarchyStringSeq
                    val symbolic = sym.toShortString
                    appendBottom(actual, sym.toShortString)
                case s @ ParsedSymbolsSeq(_, _, _) =>
                    val (sym, body) = (s.symbol, s.children)
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
