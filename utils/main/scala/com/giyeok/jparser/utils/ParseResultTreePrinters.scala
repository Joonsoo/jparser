package com.giyeok.jparser.utils

import com.giyeok.jparser.NGrammar.{NSequence, NStart, NTerminal}
import com.giyeok.jparser.ParseResultTree.{BindNode, CyclicBindNode, CyclicSequenceNode, HorizontalTreeStringSeqUtil, JoinNode, Node, SequenceNode, TerminalNode}

object ParseResultTreePrinters {
  implicit class ShortString(node: Node) {
    def toShortString: String = node match {
      case n: TerminalNode => s"Term(${n.input.toShortString})"
      case n: BindNode => s"${n.symbol.symbol.toShortString}(${n.body.toShortString})"
      case n: CyclicBindNode => s"cyclic(${n.symbol.symbol.toShortString})"
      case n: JoinNode => s"${n.body.toShortString}(&${n.join.toShortString})"
      case s: SequenceNode =>
        if (s.children.isEmpty) "ε" else s.children map {
          _.toShortString
        } mkString "/"
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
        (indent + "[\n") + (s.children map {
          _.toTreeString(indent + indentUnit, indentUnit)
        } mkString "\n") + (indent + "]")
      case s: CyclicSequenceNode =>
        indent + s"- cyclicSequence(${s.symbol}, ${s.pointer})\n"
    }

    // 가로 길이, 각 줄
    def toHorizontalHierarchyStringSeq: (Int, Seq[String]) = {
      def centerize(string: String, width: Int): String = {
        if (string.length >= width) string
        else {
          val prec = (width - string.length) / 2
          s"${" " * prec}$string${" " * (width - string.length - prec)}"
        }
      }

      def appendBottom(top: (Int, Seq[String]), bottom: String): (Int, Seq[String]) =
        if (top._1 >= bottom.length + 2) {
          val finlen = top._1
          val result = (finlen, top._2 :+ ("[" + centerize(bottom, finlen - 2) + "]"))
          result ensuring (result._2 forall {
            _.length == result._1
          })
        } else if (top._1 >= bottom.length) {
          val finlen = top._1 + 2
          val result = (finlen, (top._2 map {
            " " + _ + " "
          }) :+ ("[" + centerize(bottom, finlen - 2) + "]"))
          result ensuring (result._2 forall {
            _.length == result._1
          })
        } else {
          val finlen = bottom.length + 2
          val prec = (finlen - top._1) / 2
          val (p, f) = (" " * prec, " " * (finlen - top._1 - prec))
          val result = (finlen, (top._2 map {
            p + _ + f
          }) :+ ("[" + bottom + "]"))
          result ensuring (result._2 forall {
            _.length == result._1
          })
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
            HorizontalTreeStringSeqUtil.merge(body map {
              _.toHorizontalHierarchyStringSeq
            })
          }
        case s: CyclicSequenceNode =>
          val str = s"cyclicSequence ${s.symbol} ${s.pointer}"
          (str.length, Seq(str))
      }
      result ensuring (result._2 forall {
        _.length == result._1
      })
    }

    def toHorizontalHierarchyString: String =
      toHorizontalHierarchyStringSeq._2 mkString "\n"

    def toOperationsString: String = node match {
      case n: TerminalNode => s"term(${n.input.toShortString})"
      case n: BindNode => s"bind(${n.symbol.symbol.toShortString}, ${n.body.toOperationsString})"
      case n: CyclicBindNode => s"cyclicBind(${n.symbol.symbol.toShortString})"
      case n: JoinNode => s"join(${n.body.toOperationsString}, ${n.join.toOperationsString})"
      case s: SequenceNode => "seq()" + (s.children map {
        _.toOperationsString
      } map { s => s".append($s)" } mkString "")
      case s: CyclicSequenceNode => s"cyclicSeq(${s.symbol.symbol.toShortString}, ${s.pointer})"
    }
  }

  implicit class TreeDotGraph(node: Node) {
    class DotGraphGen(private var idCounter: Int = 0, private val builder: StringBuilder = new StringBuilder()) {
      def nextId(): String = {
        idCounter += 1
        f"node$idCounter%03d"
      }

      def traverseNode(node: Node): String = node match {
        case TerminalNode(start, input) =>
          val nodeId = nextId()
          builder.append(nodeId + "[label=\"" + node.sourceText + "\", shape=rectangle];\n")
          nodeId
        case BindNode(symbol, body) =>
          if (symbol.isInstanceOf[NSequence] || symbol.isInstanceOf[NTerminal] || symbol.isInstanceOf[NStart]) {
            traverseNode(body)
          } else {
            val nodeId = nextId()
            builder.append(nodeId + "[label=\"" + symbol.symbol.toShortString + "\"];\n")
            val child = traverseNode(body)
            builder.append(s"$nodeId -> $child;\n")
            nodeId
          }
        case CyclicBindNode(start, end, symbol) => ???
        case JoinNode(symbol, body, join) => ???
        case seq: SequenceNode =>
          if (seq.children.size == 1) {
            traverseNode(seq.children.head)
          } else {
            val nodeId = nextId()
            builder.append(nodeId + "[label=\"" + seq.symbol.symbol.toShortString + "\"];\n")
            val children = seq.children.map { child => traverseNode(child) }
            children.foreach { child =>
              builder.append(s"$nodeId -> $child;\n")
            }
            nodeId
          }
        case CyclicSequenceNode(start, end, symbol, pointer, _children) => ???
      }

      def gen(): String = {
        "digraph tree {\n" + builder.toString() + "}\n"
      }
    }

    def printDotGraph(): Unit = {
      val gen = new DotGraphGen()
      gen.traverseNode(node)
      println(gen.gen())
    }
  }
}
