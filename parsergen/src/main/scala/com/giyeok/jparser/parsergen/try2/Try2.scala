package com.giyeok.jparser.parsergen.try2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.metalang3a.generated.ArrayExprAst
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Kernel, Node}
import com.giyeok.jparser.nparser.{AcceptCondition, NaiveParser}
import com.giyeok.jparser.parsergen.try2.Try2.{KernelTemplate, ParsingAction, PrecomputedParserData}
import com.giyeok.jparser.parsergen.utils.TermGrouper
import com.giyeok.jparser.visualize.DotGraphGenerator

import scala.annotation.tailrec

class Try2(val parser: NaiveParser) {

  case class ContWithTasks(tasks: List[parser.Task], cc: parser.Cont)

  def runTasks(nextGen: Int, tasks: List[parser.Task], cc: ContWithTasks): ContWithTasks = tasks match {
    case task +: rest =>
      val (ncc, newTasks) = parser.process(nextGen, task, cc.cc)
      runTasks(nextGen, newTasks ++: rest, ContWithTasks(cc.tasks ++ newTasks, ncc))
    case List() => cc
  }

  def runTasksExceptDerive(nextGen: Int, tasks: List[parser.Task], cc: ContWithTasks): ContWithTasks = tasks match {
    case task +: rest =>
      val (ncc, newTasks0) = parser.process(nextGen, task, cc.cc)
      val newTasks = newTasks0.filterNot(_.isInstanceOf[parser.DeriveTask])
      runTasksExceptDerive(nextGen, newTasks ++: rest, ContWithTasks(cc.tasks ++ newTasks, ncc))
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

  private def startingCtxFrom(template: KernelTemplate): (Node, ContWithTasks) = {
    val startNode = Node(Kernel(template.symbolId, template.pointer, 0, 0)(parser.grammar.symbolOf(template.symbolId)), Always)
    val startGraph = Graph(Set(startNode), Set())

    val deriveTask = parser.DeriveTask(startNode)

    (startNode, runTasksWithProgressBarrier(0, List(deriveTask), startNode,
      ContWithTasks(List(deriveTask), parser.Cont(startGraph, Map()))))
  }

  private implicit class ParsingTasksList(val tasks: List[parser.Task]) {
    def deriveTasks: List[parser.DeriveTask] =
      tasks.filter(_.isInstanceOf[parser.DeriveTask]).map(_.asInstanceOf[parser.DeriveTask])

    def progressTasks: List[parser.ProgressTask] =
      tasks.filter(_.isInstanceOf[parser.ProgressTask]).map(_.asInstanceOf[parser.ProgressTask])

    def finishTask: List[parser.FinishTask] =
      tasks.filter(_.isInstanceOf[parser.FinishTask]).map(_.asInstanceOf[parser.FinishTask])
  }

  def parsingActionFrom(graph: Graph, startNode: Node, progressTasks: List[parser.ProgressTask], currGen: Int): ParsingAction = {
    val nextGen = currGen + 1
    val termProgressResult = runTasksWithProgressBarrier(nextGen, progressTasks, startNode,
      ContWithTasks(progressTasks, parser.Cont(graph, Map())))
    val trimmed = parser.trimGraph(termProgressResult.cc.graph, startNode, nextGen)

    val appendingMilestones = termProgressResult.tasks.deriveTasks
      .filter(task => trimmed.nodes.contains(task.node))
      .filter(_.node.kernel.symbol.isInstanceOf[NSequence])
      .map(_.node)
      .filter(node => node.kernel.beginGen < node.kernel.endGen && node.kernel.endGen == nextGen)
    val startProgressConditions = termProgressResult.tasks.progressTasks.filter(_.node == startNode).map(_.condition)
    println(appendingMilestones)
    println(startProgressConditions)

    // kernelTemplate이 tip pointer일 때 termGroup이 들어오면
    // - rootIsProgressed=true이면 tip pointer가 progress되고
    // - for each milestone in appendingMilestones,
    //   - kernelTemplate->milestone 으로 가는 경로가 추가되고
    //   - kernelTemplate->milestone 사이에는 trimmed가 그래프로 들어감
    ParsingAction(
      appendingMilestones.map(node => (KernelTemplate(node.kernel.symbolId, node.kernel.pointer), node.condition)),
      startProgressConditions,
      trimmed)
  }

  // `kernelTemplate`가 tip에 있을 때 받을 수 있는 터미널들을 찾고, 각 터미널에 대해 KernelTemplateAction 계산
  def termActionsFrom(kernelTemplate: KernelTemplate): Map[TermGroupDesc, ParsingAction] = {
    val (startNode, ContWithTasks(_, parser.Cont(derived, _))) = startingCtxFrom(kernelTemplate)

    new DotGraphGenerator(parser.grammar).addGraph(derived).printDotGraph()

    TermGrouper.termGroupsOf(derived).map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 0, termGroup)
      val termProgressTasks = termNodes.toList.map(parser.ProgressTask(_, AcceptCondition.Always))

      termGroup -> parsingActionFrom(derived, startNode, termProgressTasks, 0)
    }.toMap
  }

  // `simulateEdgeProgress` 바로 다음에 `endTemplate`가 붙어있고 `endTemplate`이 progress되는 경우
  def edgeProgressActionsBetween(startTemplate: KernelTemplate, endTemplate: KernelTemplate): ParsingAction = {
    val (startNode, ContWithTasks(_, parser.Cont(derived, _))) = startingCtxFrom(startTemplate)

    val endKernelInitials = derived.nodes.filter { node =>
      node.kernel.symbolId == endTemplate.symbolId && node.kernel.pointer < endTemplate.pointer
    }

    val fakeEnds = endKernelInitials.map { node =>
      node -> Node(
        Kernel(endTemplate.symbolId, endTemplate.pointer, 0, 1)(parser.grammar.symbolOf(endTemplate.symbolId)),
        node.condition)
    }.toMap
    val derivedWithEnds = fakeEnds.foldLeft(derived) { (graph, end) =>
      graph.edgesByEnd(end._1).foldLeft(graph.addNode(end._2)) { (ngraph, start) =>
        ngraph.addEdge(Edge(start.start, end._2, actual = false))
      }
    }
    val afterDerive = parser.rec(1, fakeEnds.values.map(parser.DeriveTask).toList, derivedWithEnds)
    val afterTrimming = parser.trimGraph(afterDerive.graph, startNode, 1)

    // graphBetween은 startTemplate과 endTemplate 사이의 그래프(endTemplate에서 derive된 것도 포함해야 함)
    //   -> 이게 유일하게 결정될까? (accept condition은?)
    // endNodes는 graphBetween에서 endTemplate에 대응되는 노드(들)
    val (graphBetween: Graph, endNodes: Set[Node]) = (afterTrimming, fakeEnds.values.toSet)

    val progressTasks = endNodes.map(parser.ProgressTask(_, Always)).toList
    val action = parsingActionFrom(graphBetween, startNode, progressTasks, 1)
    println(action)
    action
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  @tailrec
  private def createParserData(jobs: Jobs, cc: PrecomputedParserData): PrecomputedParserData = {
    val withTermActions = jobs.milestones.foldLeft((Jobs(Set(), Set()), cc)) { (m, i) =>
      val (jobs, wcc) = m
      val termActions = termActionsFrom(i)
      val appendables = termActions.values.flatMap(_.appendingMilestones.map(_._1)).toSet
      (Jobs(jobs.milestones ++ appendables, jobs.edges ++ appendables.map((i, _))),
        wcc.copy(termActions = wcc.termActions + (i -> termActions)))
    }
    val withEdgeActions = jobs.edges.foldLeft(withTermActions) { (m, i) =>
      val (jobs, wcc) = m
      val edgeAction = edgeProgressActionsBetween(i._1, i._2)
      val appendables = edgeAction.appendingMilestones.map(_._1)
      val newEdges = appendables.map((i._1, _)).toSet
      (Jobs(jobs.milestones ++ appendables, jobs.edges ++ newEdges),
        wcc.copy(edgeProgressActions = wcc.edgeProgressActions + (i -> edgeAction)))
    }
    val (newJobs, ncc) = withEdgeActions
    val newRemainingJobs = Jobs(
      milestones = newJobs.milestones -- ncc.termActions.keySet,
      edges = newJobs.edges -- ncc.edgeProgressActions.keySet)
    if (newRemainingJobs.milestones.isEmpty && newRemainingJobs.edges.isEmpty) ncc
    else createParserData(newRemainingJobs, ncc)
  }

  def parserData(): PrecomputedParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)

    createParserData(Jobs(Set(start), Set()), PrecomputedParserData(parser.grammar, Map(), Map()))
  }
}

object Try2 {

  case class KernelTemplate(symbolId: Int, pointer: Int)

  case class PrecomputedParserData(grammar: NGrammar,
                                   termActions: Map[KernelTemplate, Map[TermGroupDesc, ParsingAction]],
                                   edgeProgressActions: Map[(KernelTemplate, KernelTemplate), ParsingAction])

  case class ParsingAction(appendingMilestones: List[(KernelTemplate, AcceptCondition)],
                           startNodeProgressConditions: List[AcceptCondition],
                           graphBetween: Graph)

  def precomputedParserData(grammar: NGrammar): PrecomputedParserData =
    new Try2(new NaiveParser(grammar)).parserData()

  def main(args: Array[String]): Unit = {
    val grammar = ArrayExprAst.ngrammar
    val try2 = new Try2(new NaiveParser(grammar))

    val parserData = try2.parserData()

    //    val y = try2.termActionsFrom(KernelTemplate(grammar.nsymbols(grammar.startSymbol), 0))
    //    println(y)
    println(parserData)

    val kt1 = KernelTemplate(7, 2)
    try2.termActionsFrom(kt1).foreach { x =>
      println(s"Term: ${x._1.toShortString}")
      x._2.appendingMilestones.foreach { appending =>
        val action = try2.edgeProgressActionsBetween(kt1, appending._1)
        println(action)
      }
    }
  }
}
