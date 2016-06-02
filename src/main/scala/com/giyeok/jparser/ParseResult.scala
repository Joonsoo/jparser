package com.giyeok.jparser

import Symbols._

trait ParseResult {
    // ParseResult는 동일성 비교가 가능해야 한다
}

trait ParseResultFunc[R <: ParseResult] {
    def empty(): R
    def terminal(input: Inputs.Input): R
    def bind(symbol: Symbol, body: R): R
    def join(body: R, constraint: R): R

    // sequence는 Sequence에서만 쓰임
    def sequence(): R
    def append(sequence: R, child: R): R
    def appendWhitespace(sequence: R, whitespace: R): R

    def merge(base: R, merging: R): R
    def merge(results: Iterable[R]): Option[R] =
        if (results.isEmpty) None
        else Some(results.tail.foldLeft(results.head)(merge(_, _)))
}

case class ParseForest(trees: Set[ParseResultTree.Node]) extends ParseResult

object ParseForestFunc extends ParseResultFunc[ParseForest] {
    import ParseResultTree._

    def empty() = ParseForest(Set(EmptyNode))
    def terminal(input: Inputs.Input) = ParseForest(Set(TerminalNode(input)))
    def bind(symbol: Symbol, body: ParseForest) =
        ParseForest(body.trees map { b => BindedNode(symbol, b) })
    def join(body: ParseForest, constraint: ParseForest) = {
        // body와 join의 tree 각각에 대한 조합을 추가한다
        ParseForest(body.trees flatMap { b => constraint.trees map { c => JoinNode(b, c) } })
    }
    def sequence() = ParseForest(Set(SequenceNode(List(), List())))
    def append(sequence: ParseForest, child: ParseForest) = {
        assert(sequence.trees forall { _.isInstanceOf[SequenceNode] })
        // sequence의 tree 각각에 child 각각을 추가한다
        ParseForest(sequence.trees flatMap { s => child.trees map { c => s.asInstanceOf[SequenceNode].append(c) } })
    }
    def appendWhitespace(sequence: ParseForest, whitespace: ParseForest) = {
        assert(sequence.trees forall { _.isInstanceOf[SequenceNode] })
        // sequence의 tree 각각에 whitespace 각각을 추가한다
        ParseForest(sequence.trees flatMap { s => whitespace.trees map { c => s.asInstanceOf[SequenceNode].appendWhitespace(c) } })
    }

    def merge(base: ParseForest, merging: ParseForest) =
        ParseForest(base.trees ++ merging.trees)
}

object ParseResultTree {
    import Inputs._

    sealed trait Node

    case object EmptyNode extends Node
    case class TerminalNode(input: Input) extends Node
    case class BindedNode(symbol: Symbol, body: Node) extends Node
    case class JoinNode(body: Node, join: Node) extends Node
    case class SequenceNode(_childrenWS: List[Node], _childrenIdx: List[Int]) extends Node {
        // childrenIdx: index of childrenWS
        // _childrenIdx: reverse of chidlrenIdx

        val childrenSize = _childrenIdx.length

        lazy val childrenIdx = _childrenIdx.reverse
        lazy val childrenWS = _childrenWS.reverse
        lazy val wsIdx = (0 until childrenWS.length).toSet -- childrenIdx
        lazy val children: List[Node] = {
            def pick(_childrenIdx: List[Int], _childrenWS: List[Node], current: Int, cc: List[Node]): List[Node] =
                if (_childrenIdx.isEmpty) cc else {
                    val dropped = _childrenWS drop (current - _childrenIdx.head)
                    pick(_childrenIdx.tail, dropped.tail, _childrenIdx.head - 1, dropped.head +: cc)
                }
            pick(_childrenIdx, _childrenWS, _childrenWS.length - 1, List())
        }

        def append(child: Node): SequenceNode = {
            SequenceNode(child +: _childrenWS, (_childrenWS.length) +: _childrenIdx)
        }
        def appendWhitespace(wsChild: Node): SequenceNode = {
            SequenceNode(wsChild +: _childrenWS, _childrenIdx)
        }
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
    implicit class ShortString(node: Node) {
        def toShortString: String = node match {
            case EmptyNode => "()"
            case n: TerminalNode => s"Term(${n.input.toShortString})"
            case n: BindedNode => s"${n.symbol.toShortString}(${n.body.toShortString})"
            case n: JoinNode => s"${n.body.toShortString}(&${n.join.toShortString})"
            case s: SequenceNode => (s.children map { _.toShortString } mkString "/")
        }
    }
    implicit class TreePrint(node: Node) {
        def printTree(): Unit = println(toTreeString("", "  "))
        def toTreeString(indent: String, indentUnit: String): String = node match {
            case EmptyNode =>
                indent + s"- empty\n"
            case n: TerminalNode =>
                indent + s"- ${n.input}\n"
            case n: BindedNode =>
                (indent + s"- ${n.symbol}\n") + n.body.toTreeString(indent + indentUnit, indentUnit)
            case n: JoinNode =>
                (indent + s"- \n") + (n.body.toTreeString(indent + indentUnit, indentUnit)) + "\n" +
                    (indent + s"& \n") + (n.join.toTreeString(indent + indentUnit, indentUnit))
            case s: SequenceNode =>
                (indent + "[\n") + (s.children map { _.toTreeString(indent + indentUnit, indentUnit) } mkString "\n") + (indent + "]")
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
                case EmptyNode =>
                    (2, Seq("()"))
                case n: TerminalNode =>
                    val str = n.input.toShortString
                    (str.length, Seq(str))
                case n: BindedNode =>
                    val actual = n.body.toHorizontalHierarchyStringSeq
                    val symbolic = n.symbol.toShortString
                    appendBottom(actual, n.symbol.toShortString)
                case n: JoinNode =>
                    val actual = n.body.toHorizontalHierarchyStringSeq
                    val actualJoin = n.join.toHorizontalHierarchyStringSeq
                    // ignoring actualJoin
                    actual
                case s: SequenceNode =>
                    val body = s.children
                    if (body.isEmpty) {
                        (2, Seq("[]"))
                    } else {
                        HorizontalTreeStringSeqUtil.merge(body map { _.toHorizontalHierarchyStringSeq })
                    }
            }
            result ensuring (result._2.forall(_.length == result._1))
        }

        def toHorizontalHierarchyString(): String =
            toHorizontalHierarchyStringSeq._2 mkString "\n"
    }
}
