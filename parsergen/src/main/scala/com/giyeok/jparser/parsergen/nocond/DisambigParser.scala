package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

object DisambigParser {

    // TermAction과 EdgeAction의 기본적인 의미는 SimpleParser와 동일, 다만 PopAndReplace 등의 액션이 추가됨.

    sealed trait TermAction

    case class Finish(replace: Int) extends TermAction

    // stack에서 pop한 후, replace와 append를 순서대로 push
    case class Append(replace: Int, append: List[Int], pendingFinish: Option[Int]) extends TermAction

    // PopAndReplace 액션에도 append가 필요한가?
    case class PopAndReplace(popCount: Int, replace: Int, pendingFinish: Option[Int]) extends TermAction

    sealed trait EdgeAction

    case class DropLast(replace: Int) extends EdgeAction

    case class ReplaceEdge(replacePrev: Int, replaceLast: Int, pendingFinish: Option[Int]) extends EdgeAction

    case class PopAndReplaceEdge(popCount: Int, replace: Int, pendingFinish: Option[Int]) extends EdgeAction

}

// DisambigParser는 SimpleParser의 superset이다.
// grammar, nodes, nodeRelInferer는 참고용 -- 파싱 동작에는 영향을 미치지 않음.
class DisambigParser(val grammar: NGrammar,
                     // simpleParser.nodes, kernelSetNodes들도 모두 DisambigNode로 바뀌어서 nodes에 포함됨
                     val nodes: Map[Int, AKernelSetPathSet],
                     val nodeRelInferer: DisambigNodeRelInferer,
                     val startNodeId: Int,
                     val termActions: Map[(Int, CharacterTermGroupDesc), DisambigParser.TermAction],
                     val edgeActions: Map[(Int, Int), DisambigParser.EdgeAction]) {
    // DisambigParser를 만들다 보면 불필요한 노드가 생길 수 있는데 DisambigParserGen은 이런 불필요한 노드들을 정리해주지 않음
    // 그런 불필요한 노드들을 지워서 경량화한 DisambigParser를 반환한다.
    def trim(): DisambigParser = {
        ???
    }

    lazy val termActionsByNodeId: Map[Int, Map[CharacterTermGroupDesc, DisambigParser.TermAction]] =
        termActions groupBy (_._1._1) mapValues (m => m map (p => p._1._2 -> p._2))

    def acceptableTermsOf(nodeId: Int): Set[CharacterTermGroupDesc] =
        termActionsByNodeId(nodeId).keySet

    def describe(): Unit = {
        nodes.toList.sortBy(_._1).foreach { kv =>
            println(s"Node ${kv._1}:")
            val paths = kv._2.paths
            paths foreach { path =>
                println(path)
            }
        }
        termActions.toList.sortBy(_._1._1).foreach { act =>
            println(s"${act._1._1}, ${act._1._2.toShortString} -> ${act._2}")
        }
        edgeActions.toList.sortBy(_._1).foreach { act =>
            println(s"fin ${act._1} -> ${act._2}")
        }
        nodeRelInferer.adjGraph.adjacencies.adjByFoll.toList.sortBy(_._1).foreach { kv =>
            println(s"${kv._1} follow {${kv._2.toList.sorted map (_.toString) mkString ", "}}")
        }
    }
}
