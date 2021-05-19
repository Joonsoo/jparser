package com.giyeok.jparser.parsergen.deprecated.nocond

import com.giyeok.jparser.{Inputs, NGrammar}
import com.giyeok.jparser.Inputs.CharacterTermGroupDesc

object SimpleParser {

    // TermAction과 EdgeAction은 거의 비슷하지만,
    // TermAction은 terminal node들이 그래프상에 등장하지 않기 때문에 replace될 것들이 append의 형태로 나타난다는 차이가 있음

    sealed trait TermAction

    // stack top을 replace로 치환하고 edge finish 시작
    case class Finish(replace: Int) extends TermAction

    // stack top을 replace로 치환하고 append를 붙임.
    // If pendingFinish != None, append된 노드에서 처리 불가한 터미널이 오면 append된 노드를 지우고 replace된 노드를 다시 pendingFinish로 변경하고 finish 시작
    case class Append(replace: Int, append: Int, pendingFinish: Option[Int]) extends TermAction

    sealed trait EdgeAction

    // stack top을 제거하고 새로운 top을 replace로 치환
    case class DropLast(replace: Int) extends EdgeAction

    // stack top의 노드 2개를 제거하고 replacePrev, replaceLast로 치환.
    case class ReplaceEdge(replacePrev: Int, replaceLast: Int, pendingFinish: Option[Int]) extends EdgeAction

}

// grammar, nodes, nodeRelInferer는 참고용 -- 파싱 동작에는 영향을 미치지 않음.
class SimpleParser(val grammar: NGrammar,
                   val nodes: Map[Int, AKernelSet],
                   val nodeRelInferer: SimpleNodeRelInferer,
                   val startNodeId: Int,
                   // TODO empty string을 accept하는 경우를 위해 pendingFinish 초기값 필요
                   val termActions: Map[(Int, CharacterTermGroupDesc), SimpleParser.TermAction],
                   val edgeActions: Map[(Int, Int), SimpleParser.EdgeAction]) {

    lazy val termActionsByNodeId: Map[Int, Map[CharacterTermGroupDesc, SimpleParser.TermAction]] =
        termActions.groupBy(_._1._1).view.mapValues { m =>
            m.map {
                p: ((Int, Inputs.CharacterTermGroupDesc), SimpleParser.TermAction) => p._1._2 -> p._2
            }
        }.toMap

    def acceptableTermsOf(nodeId: Int): Set[CharacterTermGroupDesc] =
        termActionsByNodeId(nodeId).keySet

    def describe(): Unit = {
        nodes.toList.sortBy(_._1).foreach { kv =>
            val kernelIds = kv._2.sortedItems map (k => s"(${k.symbolId}, ${k.pointer})") mkString " "
            println(s"${kv._1} -> {$kernelIds} {${kv._2.toReadableString(grammar)}}")
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
