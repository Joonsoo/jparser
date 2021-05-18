package com.giyeok.jparser.milestone

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Node}

case class KernelTemplate(symbolId: Int, pointer: Int) extends Ordered[KernelTemplate] {
  override def compare(that: KernelTemplate): Int = if (symbolId == that.symbolId) pointer - that.pointer else symbolId - that.symbolId
}

case class TasksSummary(progressedKernels: List[(Node, AcceptCondition)], finishedKernels: List[Node])

// TODO derivedGraph와 ParsingAction.graphBetween은 커널 그래프만 저장하면 됨.
case class MilestoneParserData(grammar: NGrammar,
                               byStart: TasksSummary,
                               termActions: Map[KernelTemplate, List[(TermGroupDesc, ParsingAction)]],
                               edgeProgressActions: Map[(KernelTemplate, KernelTemplate), ParsingAction],
                               derivedGraph: Map[KernelTemplate, GraphNoIndex])

// progressedKernels와 finishedKernels는 이 parsing action으로 인해 progress된 커널과 finish된 커널들.
// -> 이들은 parse tree reconstruction을 위해 사용되는 것이기 때문에 여기에는 accept condition이 필요 없음
case class ParsingAction(appendingMilestones: List[AppendingMilestone],
                         tasksSummary: TasksSummary,
                         startNodeProgressConditions: List[AcceptCondition],
                         graphBetween: GraphNoIndex)

case class AppendingMilestone(milestone: KernelTemplate, acceptCondition: AcceptCondition,
                              dependents: List[(KernelTemplate, KernelTemplate, AcceptCondition)])

case class GraphNoIndex(nodes: Set[Node], edges: Set[Edge])

object GraphNoIndex {
  def fromGraph(graph: Graph): GraphNoIndex = GraphNoIndex(graph.nodes, graph.edges)
}
