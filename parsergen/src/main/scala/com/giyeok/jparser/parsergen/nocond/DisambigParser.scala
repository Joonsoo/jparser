package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

case class DisambigNode(paths: Seq[NodePath]) {
}

object DisambigParser {

    // TermAction과 EdgeAction의 기본적인 의미는 SimpleParser와 동일, 다만 PopAndReplace 등의 액션이 추가됨.

    sealed trait TermAction

    case class Finish(replace: Int) extends TermAction

    case class Append(replace: Int, append: Int, pendingFinish: Option[Int]) extends TermAction

    // PopAndReplace 액션에도 append가 필요한가?
    case class PopAndReplace(popCount: Int, replace: Int, pendingFinish: Option[Int]) extends TermAction

    sealed trait EdgeAction

    case class DropLast(replace: Int) extends EdgeAction

    case class ReplaceEdge(replacePrev: Int, replaceLast: Int, pendingFinish: Option[Int]) extends EdgeAction

}

// DisambigParser는 NodePathSet이 노드
// grammar, simpleParser, kernelSetNodeRelInferer, disambigNodeRelInferer는 모두 참고용
class DisambigParser(val grammar: NGrammar,
                     val simpleParser: SimpleParser,
                     // simpleParser.nodes 외에 추가적인 kernelSet 노드들
                     val kernelSetNodes: Map[Int, AKernelSet],
                     // simpleParser.nodes, kernelSetNodes들도 모두 DisambigNode로 바뀌어서 nodes에 포함됨
                     val nodes: Map[Int, DisambigNode],
                     // kernelSetNodeRelInferer는 simpleParser.nodelRelInferer의 내용을 포함함
                     val kernelSetNodeRelInferer: SimpleNodeRelInferer,
                     val disambigNodeRelInferer: DisambigNodeRelInferer,
                     val startNodeId: Int,
                     val termActions: Map[(Int, CharacterTermGroupDesc), DisambigParser.TermAction],
                     val edgeActions: Map[(Int, CharacterTermGroupDesc), DisambigParser.EdgeAction]) {
    val baseKernelSetNodes: Map[Int, AKernelSet] = simpleParser.nodes

    // SimpleParser에 새로운 노드를 추가해서 DisambigParser를 만들다 보면 불필요한 노드가 남아있을 수 있음.
    // 그런 불필요한 노드들을 지워서 경량화한 DisambigParser를 반환한다. (단, simpleParser의 내용은 바꾸지 않음)
    def trim(): DisambigParser = {
        ???
    }
}

class DisambigParserGen(val simpleParser: SimpleParser) {
    def this(grammar: NGrammar) = this(new SimpleParserGen(grammar).generateParser())

    def generateParser(): DisambigParser = ???
}
