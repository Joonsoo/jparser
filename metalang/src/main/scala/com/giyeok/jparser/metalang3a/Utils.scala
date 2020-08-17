package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}

object Utils {
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
        val BindNode(repeat: NGrammar.NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) =>
                assert(symbol.id == repeat.repeatSeq)
                val s = repeating.children(1)
                val r = unrollRepeat0(repeating.children.head)
                r :+ s
            case SequenceNode(_, _, symbol, emptySeq) =>
                assert(symbol.id == repeat.baseSeq)
                assert(emptySeq.isEmpty)
                List()
        }
    }
}
