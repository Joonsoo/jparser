package com.giyeok.jparser

import Symbols._
import com.giyeok.jparser.nparser.NGrammar.{NJoin, NSequence, NSymbol}

case class ParseForest(trees: Seq[ParseResultTree.Node]) extends ParseResult

object ParseForestFunc extends ParseResultFunc[ParseForest] {
    import ParseResultTree._

    def terminal(left: Int, input: Inputs.Input): ParseForest =
        ParseForest(Seq(TerminalNode(input)))
    def bind(left: Int, right: Int, symbol: NSymbol, body: ParseForest): ParseForest =
        ParseForest(body.trees map { b => BindNode(symbol, b) })
    def cyclicBind(left: Int, right: Int, symbol: NSymbol): ParseForest =
        ParseForest(Seq(CyclicBindNode(symbol)))
    def join(left: Int, right: Int, symbol: NJoin, body: ParseForest, constraint: ParseForest): ParseForest = {
        // body와 join의 tree 각각에 대한 조합을 추가한다
        ParseForest(body.trees flatMap { b => constraint.trees map { c => JoinNode(b, c) } })
    }

    def sequence(left: Int, right: Int, symbol: NSequence, pointer: Int): ParseForest =
        if (pointer == 0) {
            ParseForest(Seq(SequenceNode(symbol, List())))
        } else {
            ParseForest(Seq(CyclicSequenceNode(symbol, pointer, List())))
        }
    def append(sequence: ParseForest, child: ParseForest): ParseForest = {
        assert(sequence.trees forall { n => n.isInstanceOf[SequenceNode] || n.isInstanceOf[CyclicSequenceNode] })
        // sequence의 tree 각각에 child 각각을 추가한다
        ParseForest(sequence.trees flatMap { sequenceNode =>
            child.trees map { c =>
                sequenceNode match {
                    case seq: SequenceNode => seq.append(c)
                    case seq: CyclicSequenceNode => seq.append(c)
                    case _ => ??? // cannot happen
                }
            }
        })
    }

    def merge(base: ParseForest, merging: ParseForest) =
        ParseForest(base.trees ++ merging.trees)
}

object ParseResultTree {
    import Inputs._

    sealed trait Node

    case class TerminalNode(input: Input) extends Node
    case class BindNode(symbol: NSymbol, body: Node) extends Node
    case class CyclicBindNode(symbol: NSymbol) extends Node
    case class JoinNode(body: Node, join: Node) extends Node
    case class SequenceNode(symbol: NSequence, _children: List[Node]) extends Node {
        // _children: reverse of all children

        def append(child: Node): SequenceNode = {
            SequenceNode(symbol, child +: _children)
        }
        lazy val childrenAll = _children.reverse
        lazy val children = symbol.symbol.contentIdx filter { _ < childrenAll.length } map { childrenAll(_) }
    }
    case class CyclicSequenceNode(symbol: NSequence, pointer: Int, _children: List[Node]) extends Node {
        def append(child: Node): CyclicSequenceNode = {
            CyclicSequenceNode(symbol, pointer, child +: _children)
        }
        lazy val childrenAll = _children.reverse
        lazy val children = symbol.symbol.contentIdx filter { _ < childrenAll.length } map { childrenAll(_) }
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
            mergedList ensuring (mergedList._2 forall { _.length == mergedList._1 })
        }
    }
    implicit class ShortString(node: Node) {
        def toShortString: String = node match {
            case n: TerminalNode => s"Term(${n.input.toShortString})"
            case n: BindNode => s"${n.symbol.symbol.toShortString}(${n.body.toShortString})"
            case n: CyclicBindNode => s"cyclic(${n.symbol.symbol.toShortString})"
            case n: JoinNode => s"${n.body.toShortString}(&${n.join.toShortString})"
            case s: SequenceNode =>
                if (s.children.isEmpty) "ε" else s.children map { _.toShortString } mkString "/"
            case s: CyclicSequenceNode =>
                s"cyclic(${s.symbol.symbol.toShortString},${s.pointer})"
        }
    }
    implicit class TreePrint(node: Node) {
        def printTree(): Unit = println(toTreeString("", "  "))
        def toTreeString(indent: String, indentUnit: String): String = node match {
            case n: TerminalNode =>
                indent + s"- ${n.input}\n"
            case n: BindNode =>
                (indent + s"- ${n.symbol}\n") + n.body.toTreeString(indent + indentUnit, indentUnit)
            case n: CyclicBindNode =>
                indent + s"- cyclic ${n.symbol}\n"
            case n: JoinNode =>
                (indent + s"- \n") + n.body.toTreeString(indent + indentUnit, indentUnit) + "\n" +
                    (indent + s"& \n") + n.join.toTreeString(indent + indentUnit, indentUnit)
            case s: SequenceNode =>
                (indent + "[\n") + (s.children map { _.toTreeString(indent + indentUnit, indentUnit) } mkString "\n") + (indent + "]")
            case s: CyclicSequenceNode =>
                indent + s"- cyclicSequence(${s.symbol}, ${s.pointer})\n"
        }

        // 가로 길이, 각 줄
        def toHorizontalHierarchyStringSeq: (Int, Seq[String]) = {
            def centerize(string: String, width: Int): String = {
                if (string.length >= width) string
                else {
                    val prec = (width - string.length) / 2
                    s" $prec$string${" " * (width - string.length - prec)}"
                }
            }
            def appendBottom(top: (Int, Seq[String]), bottom: String): (Int, Seq[String]) =
                if (top._1 >= bottom.length + 2) {
                    val finlen = top._1
                    val result = (finlen, top._2 :+ ("[" + centerize(bottom, finlen - 2) + "]"))
                    result ensuring (result._2 forall { _.length == result._1 })
                } else if (top._1 >= bottom.length) {
                    val finlen = top._1 + 2
                    val result = (finlen, (top._2 map { " " + _ + " " }) :+ ("[" + centerize(bottom, finlen - 2) + "]"))
                    result ensuring (result._2 forall { _.length == result._1 })
                } else {
                    val finlen = bottom.length + 2
                    val prec = (finlen - top._1) / 2
                    val (p, f) = (" " * prec, " " * (finlen - top._1 - prec))
                    val result = (finlen, (top._2 map { p + _ + f }) :+ ("[" + bottom + "]"))
                    result ensuring (result._2 forall { _.length == result._1 })
                }
            val result: (Int, Seq[String]) = node match {
                case n: TerminalNode =>
                    val str = n.input.toShortString
                    (str.length, Seq(str))
                case n: BindNode =>
                    val actual = n.body.toHorizontalHierarchyStringSeq
                    val symbolic = n.symbol.symbol.toShortString
                    appendBottom(actual, n.symbol.symbol.toShortString)
                case n: CyclicBindNode =>
                    val str = "cyclic " + n.symbol.symbol.toShortString
                    (str.length, Seq(str))
                case n: JoinNode =>
                    val actual = n.body.toHorizontalHierarchyStringSeq
                    val actualJoin = n.join.toHorizontalHierarchyStringSeq
                    // ignoring actualJoin
                    actual
                case s: SequenceNode =>
                    val body = s.children
                    if (body.isEmpty) {
                        (1, Seq("ε"))
                    } else {
                        HorizontalTreeStringSeqUtil.merge(body map { _.toHorizontalHierarchyStringSeq })
                    }
                case s: CyclicSequenceNode =>
                    val str = s"cyclicSequence ${s.symbol} ${s.pointer}"
                    (str.length, Seq(str))
            }
            result ensuring (result._2 forall { _.length == result._1 })
        }

        def toHorizontalHierarchyString: String =
            toHorizontalHierarchyStringSeq._2 mkString "\n"

        def toOperationsString: String = node match {
            case n: TerminalNode => s"term(${n.input.toShortString})"
            case n: BindNode => s"bind(${n.symbol.symbol.toShortString}, ${n.body.toOperationsString})"
            case n: CyclicBindNode => s"cyclicBind(${n.symbol.symbol.toShortString})"
            case n: JoinNode => s"join(${n.body.toOperationsString}, ${n.join.toOperationsString})"
            case s: SequenceNode => "seq()" + (s.children map { _.toOperationsString } map { s => s".append($s)" } mkString "")
            case s: CyclicSequenceNode => s"cyclicSeq(${s.symbol.symbol.toShortString}, ${s.pointer})"
        }
    }
}
