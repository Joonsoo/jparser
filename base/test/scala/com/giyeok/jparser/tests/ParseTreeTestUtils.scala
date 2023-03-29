package com.giyeok.jparser.tests

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.{Inputs, ParseResultTree}

object ParseTreeTestUtils {
  def parseTreeToPath(parseNode: Node): String = {
    val builder = new StringBuilder()

    def traverseBindNode(node: ParseResultTree.BindNode, indent: String): String = {
      node.body match {
        case bindNode: ParseResultTree.BindNode =>
          builder.append(node.symbol.id.toString + "(")
          val closingParens = traverseBindNode(bindNode, indent)
          closingParens + ")"
        case _ =>
          traverse(node.body, indent + "  ")
          ""
      }
    }

    def traverse(node: Node, indent: String): Unit = {
      node match {
        case ParseResultTree.TerminalNode(start, input) =>
          val char = input.asInstanceOf[Inputs.Character].char
          if (char == '\n') {
            builder.append("'\\n'")
          } else {
            builder.append("'" + char + "'")
          }
        //          builder.append('\n')
        case node: ParseResultTree.BindNode =>
          //          builder.append(indent)
          val closingParens = traverseBindNode(node, indent)
          builder.append(closingParens)
        //          builder.append(indent + closingParens + "\n")
        case ParseResultTree.CyclicBindNode(start, end, symbol) => ???
        case ParseResultTree.JoinNode(symbol, body, join) =>
          builder.append("join " + symbol.id + "(")
          traverse(body, indent + "  ")
          builder.append(")")
        case seq: ParseResultTree.SequenceNode =>
          builder.append(seq.symbol.id + "[")
          seq.children.zipWithIndex.foreach { case (child, idx) =>
            traverse(child, indent + "  ")
          }
          builder.append("]")
        case ParseResultTree.CyclicSequenceNode(start, end, symbol, pointer, _children) => ???
      }
    }

    traverse(parseNode, "")
    builder.result()
  }
}
