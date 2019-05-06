package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

case class DisambigNode(paths: Seq[NodePath]) {
}

object DisambigParser {

    // TermAction과 EdgeAction의 기본적인 의미는 SimpleParser와 동일.
    // 하지만 아마도? pop 액션과 pushToRestore 액션이 추가될듯?

    sealed trait TermAction

    case class Finish(replace: Int) extends TermAction

    case class Append(replace: Int, append: Int, pendingFinish: Option[Int]) extends TermAction

    sealed trait EdgeAction

    case class DropLast(replace: Int) extends EdgeAction

    case class ReplaceEdge(replacePrev: Int, replaceLast: Int, pendingFinish: Option[Int]) extends EdgeAction

}

class DisambigParser(val grammar: NGrammar,
                     val simpleParser: SimpleParser,
                     val disambigNodes: Map[Int, DisambigNode],
                     val nodeRelInferer: NodeRelInferer,
                     val startNodeId: Int,
                     val termActions: Map[(Int, CharacterTermGroupDesc), DisambigParser.TermAction],
                     val edgeActions: Map[(Int, CharacterTermGroupDesc), DisambigParser.EdgeAction]) {
    val baseNodes: Map[Int, AKernelSet] = simpleParser.nodes
}

class DisambigParserGen(val grammar: NGrammar) {
    def generateParser(): DisambigParser = ???
}
