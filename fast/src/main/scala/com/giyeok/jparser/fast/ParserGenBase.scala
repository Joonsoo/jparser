package com.giyeok.jparser.fast

import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}

trait ParserGenBase {
  val parser: NaiveParser

  case class ContWithTasks(tasks: List[parser.Task], cc: parser.Cont)

  implicit class ParsingTasksList(val tasks: List[parser.Task]) {
    def deriveTasks: List[parser.DeriveTask] =
      tasks.filter(_.isInstanceOf[parser.DeriveTask]).map(_.asInstanceOf[parser.DeriveTask])

    def progressTasks: List[parser.ProgressTask] =
      tasks.filter(_.isInstanceOf[parser.ProgressTask]).map(_.asInstanceOf[parser.ProgressTask])

    def finishTasks: List[parser.FinishTask] =
      tasks.filter(_.isInstanceOf[parser.FinishTask]).map(_.asInstanceOf[parser.FinishTask])
  }

  implicit class DeriveTasksList(val tasks: List[parser.DeriveTask]) {
    def appendingMilestoneCandidates(trimmedGraph: Graph, nextGen: Int): List[Node] =
      tasks.filter(task => trimmedGraph.nodes.contains(task.node))
        .filter(task => parser.grammar.symbolOf(task.node.kernel.symbolId).isInstanceOf[NSequence])
        .map(_.node)
        .filter(node => node.kernel.beginGen < node.kernel.endGen && node.kernel.endGen == nextGen)

  }

  def runTasksWithProgressBarrier(nextGen: Int, tasks: List[parser.Task], barrierNode: Node, cc: ContWithTasks): ContWithTasks =
    runTasksWithProgressBarriers(nextGen, tasks, Set(barrierNode), cc)

  def runTasksWithProgressBarriers(nextGen: Int, tasks: List[parser.Task], barrierNodes: Set[Node], cc: ContWithTasks): ContWithTasks = tasks match {
    case task +: rest =>
      if (task.isInstanceOf[parser.ProgressTask] && barrierNodes.contains(task.node)) {
        runTasksWithProgressBarriers(nextGen, rest, barrierNodes, cc)
      } else {
        val (ncc, newTasks) = parser.process(nextGen, task, cc.cc)
        runTasksWithProgressBarriers(nextGen, newTasks ++: rest, barrierNodes, ContWithTasks(cc.tasks ++ newTasks, ncc))
      }
    case List() => cc
  }

  def startingCtxFrom(startKernel: Kernel): (Node, ContWithTasks) = {
    val startNode = Node(startKernel, Always)
    val startGraph = Graph(Set(startNode), Set())

    val deriveTask = parser.DeriveTask(startNode)

    (startNode, runTasksWithProgressBarrier(startKernel.endGen, List(deriveTask), startNode,
      ContWithTasks(List(deriveTask), parser.Cont(startGraph, Map()))))
  }

  def tasksSummaryFrom(tasks: List[parser.Task]): TasksSummary = {
    val progressed = tasks.progressTasks.map(t => (t.node, t.condition))
    val finished = tasks.finishTasks.map(_.node)
    TasksSummary(progressed, finished)
  }
}
