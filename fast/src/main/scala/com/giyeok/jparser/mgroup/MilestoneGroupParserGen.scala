package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.{GraphNoIndex, KernelTemplate, ParserGenBase}
import com.giyeok.jparser.metalang3a.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, conjunct}
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}
import com.giyeok.jparser.utils.TermGrouper

import scala.annotation.tailrec

class MilestoneGroupParserGen(val parser: NaiveParser) extends ParserGenBase {
  private var milestoneGroups = Map[MilestoneGroup, Int]()
  private var milestoneGroupsById = Map[Int, MilestoneGroup]()
  private var newMilestoneGroups = List[Int]()

  private def milestoneGroupIdOf(milestoneGroup: MilestoneGroup): Int = milestoneGroups get milestoneGroup match {
    case Some(existingId) => existingId
    case None =>
      val newId = milestoneGroups.size + 1
      milestoneGroups += milestoneGroup -> newId
      milestoneGroupsById += newId -> milestoneGroup
      newMilestoneGroups +:= newId
      newId
  }

  private def milestoneGroupIdOf(milestones: Set[KernelTemplate]): Int = milestoneGroupIdOf(MilestoneGroup(milestones))

  // milestoneGroupIdOf와 동일하지만 newMilestoneGroups에 추가하지 않는다.
  private def milestoneGroupIdOfNoTip(milestoneGroup: MilestoneGroup): Int = milestoneGroups get milestoneGroup match {
    case Some(existingId) => existingId
    case None =>
      val newId = milestoneGroups.size + 1
      milestoneGroups += milestoneGroup -> newId
      milestoneGroupsById += newId -> milestoneGroup
      newId
  }

  private def milestoneGroupIdOfNoTip(milestones: Set[KernelTemplate]): Int = milestoneGroupIdOfNoTip(MilestoneGroup(milestones))

  case class Jobs(milestoneGroups: Set[Int], edges: Set[(Int, Int)])

  def startingCtxFrom(startKernels: Set[KernelTemplate], nextGen: Int): (Map[KernelTemplate, Node], ContWithTasks) = {
    val startNodesMap = startKernels.map(k => k -> Node(Kernel(k.symbolId, k.pointer, 0, nextGen), Always)).toMap
    val startNodes = startNodesMap.values.toSet
    val startGraph = Graph(startNodes, Set())
    val deriveTasks = startNodes.map(parser.DeriveTask).toList

    (startNodesMap, runTasksWithProgressBarriers(nextGen, deriveTasks, startNodes,
      ContWithTasks(deriveTasks, parser.Cont(startGraph, Map()))))
  }

  def trimGraph(graph: Graph, startNodes: Set[Node], nextGen: Int): Graph = {
    val trim1 = parser.trimFinishedTerminalNodes(graph, nextGen)
    val termNodes = parser.termNodes(graph, nextGen)
    // trim1에서 startNodes에서 reachable(accept condition의 ref도 포함)하고 termNodes로 reachable한
    val fromStarts = startNodes.flatMap(parser.reachableNodesFrom(trim1, _))
    val toEnds = parser.reachableNodesTo(trim1, termNodes)
    val removing = trim1.nodes -- (fromStarts intersect toEnds)
    trim1.removeNodes(removing)
  }

  def parsingActionFrom(graph: Graph, startNodes: Set[Node], progressTasks: List[parser.ProgressTask], currGen: Int): ParsingAction = {
    val nextGen = currGen + 1
    val termProgressResult = runTasksWithProgressBarriers(nextGen, progressTasks, startNodes,
      ContWithTasks(List(), parser.Cont(graph, Map())))
    val progressedGraph = termProgressResult.cc.graph
    val trimmed = trimGraph(progressedGraph, startNodes, nextGen)
    val survivingStartNodes = startNodes.intersect(trimmed.nodes)

    val appendingMilestones0 = termProgressResult.tasks.deriveTasks.appendingMilestoneCandidates(trimmed, nextGen)
    val startProgressTasks = termProgressResult.tasks.progressTasks.filter(t => startNodes.contains(t.node))
    val startProgressConditions = startProgressTasks.map(_.condition)

    val (_, actions) = startProgressTasks.foldLeft((termProgressResult.cc, termProgressResult.tasks)) { case ((cc, tasks), nextTask) =>
      val (ncc, newTasks) = parser.process(nextGen, nextTask, cc)
      (ncc, tasks ++ newTasks)
    }

    val appendingMilestones = appendingMilestones0.filter(progressedGraph.reachableBetween(startNodes, _))
    val appendingMilestoneGroup = milestoneGroupIdOf(
      appendingMilestones.map(n => KernelTemplate(n.kernel.symbolId, n.kernel.pointer)).toSet)

    ParsingAction(
      replaceTo = milestoneGroupIdOfNoTip(survivingStartNodes.map(KernelTemplate.fromNode)),
      appendingMilestoneGroups = List(
        AppendingMilestoneGroup(appendingMilestoneGroup,
          // TODO condition, dependent 제대로 반환
          appendingMilestones.map(_.condition).foldLeft[AcceptCondition](Always) { (m, i) => conjunct(m, i) },
          List())),
      tasksSummary = tasksSummaryFrom(actions),
      // TODO startNodeProgressConditions 제대로 반환
      startNodeProgressConditions = Map(),
      // TODO graphBetween 제대로 반환
      graphBetween = GraphNoIndex(Set(), Set()))
  }

  def termActionsFrom(start: MilestoneGroup): List[(TermGroupDesc, ParsingAction)] = {
    val (startNodesMap, ContWithTasks(_, parser.Cont(derived, _))) = startingCtxFrom(start.milestones, 1)
    val startNodes = startNodesMap.values.toSet

    val termGroups = TermGrouper.termGroupsOf(parser.grammar, derived)

    termGroups.map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 1, termGroup)
      val termProgressTasks = termNodes.toList.map(parser.ProgressTask(_, Always))

      termGroup -> parsingActionFrom(derived, startNodes, termProgressTasks, 1)
    }.toList.sortBy(_._1.toString)
  }

  def edgeActionFrom(start: MilestoneGroup, end: MilestoneGroup): ParsingAction = {
    // TODO
    ParsingAction(1, List(), tasksSummaryFrom(List()), Map(), GraphNoIndex(Set(), Set()))
  }

  @tailrec
  private def createParserData(jobs: Jobs, cc: MilestoneGroupParserData): MilestoneGroupParserData = {
    val newTermActions = jobs.milestoneGroups.map(g => g -> termActionsFrom(milestoneGroupsById(g)))
    val ncc0 = cc.copy(termActions = cc.termActions ++ newTermActions)
    val possibleEdgesFromTermActions = newTermActions.flatMap(_._2.map(_._2)).flatMap { action =>
      action.appendingMilestoneGroups.map(appending => action.replaceTo -> appending.appendingMilestoneGroup)
    }
    val newEdgeActions = jobs.edges.map { e =>
      e -> edgeActionFrom(milestoneGroupsById(e._1), milestoneGroupsById(e._2))
    }
    val ncc = ncc0.copy(edgeProgressActions = ncc0.edgeProgressActions ++ newEdgeActions)
    val milestoneGroupsToProcess = newMilestoneGroups.toSet -- cc.termActions.keySet
    newMilestoneGroups = List()
    val newRemainingEdges: Set[(Int, Int)] = possibleEdgesFromTermActions -- cc.edgeProgressActions.keySet
    println(s"Remaining jobs: mgroups=${milestoneGroupsToProcess.size}, edges=${newRemainingEdges.size}")
    if (milestoneGroupsToProcess.isEmpty && newRemainingEdges.isEmpty) ncc
    else createParserData(Jobs(milestoneGroupsToProcess, newRemainingEdges), ncc)
  }

  def parserData(): MilestoneGroupParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    val (_, ContWithTasks(tasks, _)) = startingCtxFrom(Set(KernelTemplate(start.symbolId, start.pointer)), 1)
    val startingMilestoneGroup0 = MilestoneGroup(Set(start))
    // startingMilestoneGroup은 startingMilestoneGroup0에서 derive된 노드들 중 milestone이 될 수 있는 것들을 모두 찾고,
    // start에서 각 milestone으로 가면서 만날 수 있는 모든 dependent들을 추가한 것
    val startingMilestoneGroup = milestoneGroupIdOf(startingMilestoneGroup0)
    val result = createParserData(Jobs(Set(startingMilestoneGroup), Set()),
      MilestoneGroupParserData(parser.grammar, tasksSummaryFrom(tasks), Map(), Map(), Map(), Map()))
    result.copy(
      milestoneGroups = milestoneGroupsById,
      // derivedGraph = ???
    )
  }
}

object MilestoneGroupParserGen {
  def generateMilestoneGroupParserData(grammar: NGrammar): MilestoneGroupParserData =
    new MilestoneGroupParserGen(new NaiveParser(grammar)).parserData()

  def main(args: Array[String]): Unit = {
    val grammar = MetaLanguage3.analyzeGrammar(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' (WS E (WS ',' WS E)*)? WS ']' {Arr(elems=$1{[$1]+$2} ?: [])}
        |WS = ' '*
        |""".stripMargin)
    val sourceText = "[]"
    val valuefySimulator = ValuefyExprSimulator(grammar)

    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)
    val milestoneParser = new MilestoneParser(milestoneParserData)
    val milestoneParseForest = milestoneParser.parseAndReconstructToForest(sourceText).left.get
    val milestoneAst = milestoneParseForest.trees.map(valuefySimulator.valuefyStart)
    println(milestoneAst)

    val mgroupParserData = MilestoneGroupParserGen.generateMilestoneGroupParserData(grammar.ngrammar)
    //    val mgroupParser = new MilestoneGroupParser(mgroupParserData)
    //    val mgroupParseForest = mgroupParser.parseAndReconstructToForest(sourceText).left.get
    //    val mgroupAst = mgroupParseForest.trees.map(valuefySimulator.valuefyStart)
    //    println(mgroupAst)

    println(milestoneParserData)
    println(mgroupParserData)
  }
}
