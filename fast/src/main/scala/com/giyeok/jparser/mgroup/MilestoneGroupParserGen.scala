package com.giyeok.jparser.mgroup

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.metalang3a.MetaLanguage3
import com.giyeok.jparser.milestone.{KernelTemplate, TasksSummary}
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}

import scala.annotation.tailrec

class MilestoneGroupParserGen(val parser: NaiveParser) {
  case class ContWithTasks(tasks: List[parser.Task], cc: parser.Cont)

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

  private def startingCtxFrom(startKernel: Kernel): (Node, ContWithTasks) = {
    val startNode = Node(startKernel, Always)
    val startGraph = Graph(Set(startNode), Set())

    val deriveTask = parser.DeriveTask(startNode)

    (startNode, runTasksWithProgressBarrier(startKernel.endGen, List(deriveTask), startNode,
      ContWithTasks(List(deriveTask), parser.Cont(startGraph, Map()))))
  }

  case class Jobs(milestoneGroups: Set[MilestoneGroup], edges: Set[(MilestoneGroup, MilestoneGroup)])

  private implicit class ParsingTasksList(val tasks: List[parser.Task]) {
    def deriveTasks: List[parser.DeriveTask] =
      tasks.filter(_.isInstanceOf[parser.DeriveTask]).map(_.asInstanceOf[parser.DeriveTask])

    def progressTasks: List[parser.ProgressTask] =
      tasks.filter(_.isInstanceOf[parser.ProgressTask]).map(_.asInstanceOf[parser.ProgressTask])

    def finishTasks: List[parser.FinishTask] =
      tasks.filter(_.isInstanceOf[parser.FinishTask]).map(_.asInstanceOf[parser.FinishTask])
  }

  private def tasksSummaryFrom(tasks: List[parser.Task]) = {
    val progressed = tasks.progressTasks.map(t => (t.node, t.condition))
    val finished = tasks.finishTasks.map(_.node)
    TasksSummary(progressed, finished)
  }

  @tailrec
  private def createParserData(jobs: Jobs, cc: MilestoneGroupParserData): MilestoneGroupParserData = {
    val newRemainingJobs = Jobs(milestoneGroups = Set(), edges = Set())
    if (newRemainingJobs.milestoneGroups.isEmpty && newRemainingJobs.edges.isEmpty) ???
    else createParserData(newRemainingJobs, ???)
  }

  def parserData(): MilestoneGroupParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    val (_, ContWithTasks(tasks, _)) = startingCtxFrom(Kernel(start.symbolId, start.pointer, 1, 1))
    val startingMilestoneGroup0 = MilestoneGroup(Set(start))
    // startingMilestoneGroup은 startingMilestoneGroup0에서 derive된 노드들 중 milestone이 될 수 있는 것들을 모두 찾고,
    // start에서 각 milestone으로 가면서 만날 수 있는 모든 dependent들을 추가한 것
    val startingMilestoneGroup = startingMilestoneGroup0
    val result = createParserData(Jobs(Set(startingMilestoneGroup), Set()),
      MilestoneGroupParserData(parser.grammar, tasksSummaryFrom(tasks), Map(), Map(), Map(), Map()))
    result.copy(derivedGraph = ???)
  }
}

object MilestoneGroupParserGen {
  def generateMilestoneGroupParserData(grammar: NGrammar): MilestoneGroupParserData =
    new MilestoneGroupParserGen(new NaiveParser(grammar)).parserData()

  def main(args: Array[String]): Unit = {
    val grammar = MetaLanguage3.analyzeGrammar(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' WS E (WS ',' WS E)* WS ']' {Arr(elems=[$2]+$3)}
        |WS = ' '*
        |""".stripMargin)
    generateMilestoneGroupParserData(grammar.ngrammar)
  }
}
