package com.giyeok.jparser.nparser

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}

object RepeatUtils {
  def unrollRepeat1(node: Node): List[Node] = {
    val BindNode(repeat: NGrammar.NRepeat, body) = node
    body match {
      case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
        assert(symbol.id == repeat.repeatSeq)
        val s = repeating.children(1)
        val r = unrollRepeat1(repeating.children.head)
        r :+ s
      case base =>
        List(base)
    }
  }

  def unrollRepeat0(node: Node): List[Node] = {
    val BindNode(repeat: NGrammar.NRepeat, BindNode(bodySymbol, bodyNode)) = node
    if (bodySymbol.id == repeat.repeatSeq) {
      val bodySeq = bodyNode.asInstanceOf[SequenceNode]
      unrollRepeat0(bodySeq.children.head) :+ bodySeq.children(1)
    } else {
      assert(bodySymbol.id == repeat.baseSeq)
      List()
    }
  }
}
