package com.giyeok.jparser.fast

import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}

trait ParserGenBase {
  val parser: NaiveParser

  case class ContWithTasks(tasks: List[parser.Task], cc: parser.Cont)

  def runTasks(nextGen: Int, tasks: List[parser.Task], cc: ContWithTasks): ContWithTasks = tasks match {
    case task +: rest =>
      val (ncc, newTasks) = parser.process(nextGen, task, cc.cc)
      runTasks(nextGen, newTasks ++: rest, ContWithTasks(cc.tasks ++ newTasks, ncc))
    case List() => cc
  }

  def runTasksWithProgressBarrier(nextGen: Int, tasks: List[parser.Task], barrierNode: Node, cc: ContWithTasks): ContWithTasks = tasks match {
    case task +: rest =>
      if (task.isInstanceOf[parser.ProgressTask] && task.node == barrierNode) {
        runTasksWithProgressBarrier(nextGen, rest, barrierNode, cc)
      } else {
        val (ncc, newTasks) = parser.process(nextGen, task, cc.cc)
        runTasksWithProgressBarrier(nextGen, newTasks ++: rest, barrierNode, ContWithTasks(cc.tasks ++ newTasks, ncc))
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

  def startingCtxFrom(template: KernelTemplate, nextGen: Int): (Node, ContWithTasks) =
    startingCtxFrom(Kernel(template.symbolId, template.pointer, 0, nextGen))

  implicit class ParsingTasksList(val tasks: List[parser.Task]) {
    def deriveTasks: List[parser.DeriveTask] =
      tasks.filter(_.isInstanceOf[parser.DeriveTask]).map(_.asInstanceOf[parser.DeriveTask])

    def progressTasks: List[parser.ProgressTask] =
      tasks.filter(_.isInstanceOf[parser.ProgressTask]).map(_.asInstanceOf[parser.ProgressTask])

    def finishTasks: List[parser.FinishTask] =
      tasks.filter(_.isInstanceOf[parser.FinishTask]).map(_.asInstanceOf[parser.FinishTask])
  }

  def tasksSummaryFrom(tasks: List[parser.Task]) = {
    val progressed = tasks.progressTasks.map(t => (t.node, t.condition))
    val finished = tasks.finishTasks.map(_.node)
    TasksSummary(progressed, finished)
  }
}
