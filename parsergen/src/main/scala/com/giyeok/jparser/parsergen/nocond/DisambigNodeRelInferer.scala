package com.giyeok.jparser.parsergen.nocond

// DisambigNodeRelInferer는 SimpleNodeRelInferer에 pop, restore 등의 추가적인 action 종류를 추가로 지원함
class DisambigNodeRelInferer(private val termActions: Map[Int, Set[DisambigParser.TermAction]],
                             private val edgeActions: Map[(Int, Int), DisambigParser.EdgeAction],
                             val adjGraph: NodeAdjacencyGraph) {
    // adjacency는 SimpleNodeRelInferer꺼 그대로 쓸 수 있을듯?
}

object DisambigNodeRelInferer {
    val emptyInferer = new DisambigNodeRelInferer(Map(), Map(), NodeAdjacencyGraph.emptyGraph)
}
