package com.giyeok.jparser.parsergen.nocond

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.nparser.NGrammar

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

    case class PrevNode(nodeId: Int, finishable: Boolean)

}

// grammar, prevNodes는 디버깅을 위한 목적
class SimpleParser(val grammar: NGrammar,
                   val nodes: Map[Int, AKernelSet],
                   val nodeRelGraph: NodeRelGraph,
                   val startNodeId: Int,
                   // TODO empty string을 accept하는 경우를 위해 pendingFinish 초기값 필요
                   val termActions: Map[(Int, CharacterTermGroupDesc), SimpleParser.TermAction],
                   val edgeActions: Map[(Int, Int), SimpleParser.EdgeAction]) {
    def describe(): Unit = {
        nodes.toList.sortBy(_._1).foreach { kv =>
            println(s"${kv._1} -> ${kv._2.items} ${kv._2.items map (_.toReadableString(grammar)) mkString " | "}")
        }
        termActions.toList.sortBy(_._1._1).foreach { act =>
            println(s"${act._1._1}, ${act._1._2.toShortString} -> ${act._2}")
        }
        edgeActions.toList.sortBy(_._1).foreach { act =>
            println(s"fin ${act._1} -> ${act._2}")
        }
        nodeRelGraph.adjacents.toList.sorted.foreach { kv =>
            println(s"${kv._1} -> ${kv._2}")
        }
    }
}
