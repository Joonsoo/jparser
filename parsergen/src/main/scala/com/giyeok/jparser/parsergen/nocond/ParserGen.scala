package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

case class Node(nodeId: Int, prevNodeIds: Set[Int], paths: Set[AKernelSetPath]) {

}

sealed trait TermAction

case class AppendAction(appendNodeId: Int, finishable: Boolean) extends TermAction

case class ReplaceAndAppendAction(popCount: Int, replaceNodeId: Int, appendNodeId: Int, finishable: Boolean) extends TermAction

object Finish extends TermAction


sealed trait EdgeAction

case class EdgeReplaceAction(prevNodeId: Int, lastNodeId: Int, finish: Boolean) extends EdgeAction

object EdgeDropLastAction extends EdgeAction


class NoCondParser(val grammar: NGrammar,
                   val nodes: Map[Int, Node],
                   val nodeTermActions: Map[Int, Map[CharacterTermGroupDesc, TermAction]],
                   val edgeFinishActions: Map[(Int, Int), EdgeAction])


class NoCondParserGen(val grammar: NGrammar) {
    def generateParser(): NoCondParser = ???
}
