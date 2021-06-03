package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.ParserGenBase
import com.giyeok.jparser.metalang3a.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptConditionSlot, Always}
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Graph, Kernel, Node}
import com.giyeok.jparser.utils.TermGrouper

class MilestoneGroupParserGen(val parser: NaiveParser) extends ParserGenBase {
  // 처음에는 accept condition slot의 수를 milestone의 수와 동일하게 하고
  // 나중에 slot 수를 줄이는 알고리즘을 별도로 실행
  private var milestoneGroups: Map[List[Milestone], Int] = Map()
  private var milestoneGroupsById: Map[Int, List[Milestone]] = Map()

  private def mgroupIdOf(milestones: List[Milestone]): Int = milestoneGroups.get(milestones.sorted) match {
    case Some(mgroupId) => mgroupId
    case None =>
      val newId = milestoneGroups.size + 1
      milestoneGroups += milestones -> newId
      milestoneGroupsById += newId -> milestones
      newId
  }

  private def mgroupIdOf(milestones: Iterable[Milestone]): Int = mgroupIdOf(milestones.toList)

  private def milestonesFromNodes(nodes: Iterable[Node]): (List[Milestone], List[Node]) = {
    val pairs = nodes.toList.zipWithIndex
      .map(p => Milestone(p._1.kernel.symbolId, p._1.kernel.pointer, p._2) -> p._1)
      .sortBy(_._1)
    (pairs.map(_._1), pairs.map(_._2))
  }

  private def derivedFrom(milestones: List[Milestone], nextGen: Int): (List[Node], ContWithTasks) = {
    val startNodes = milestones.map(m => Node(Kernel(m.symbolId, m.pointer, 0, nextGen), Always))
    val startGraph = Graph(startNodes.toSet, Set())
    val deriveTasks = startNodes.map(parser.DeriveTask)

    val result = runTasksWithProgressBarriers(nextGen, deriveTasks, startNodes.toSet,
      ContWithTasks(deriveTasks, parser.Cont(startGraph, Map())))
    (startNodes, result)
  }

  private def stepReplacement(from: List[Milestone], to: List[Milestone]): StepReplacement = {
    assert(to.toSet.subsetOf(from.toSet))
    val sortedFrom = from.sorted.zipWithIndex.map(f => f._1 -> f._2).toMap
    val sortedTo = to.sorted
    StepReplacement(mgroupIdOf(to), sortedTo.map(sortedFrom))
  }

  private def termActionsOf(mgroupId: Int): List[(TermGroupDesc, TermAction)] = {
    val milestones = milestoneGroupsById(mgroupId)
    val (startNodes, ContWithTasks(_, parser.Cont(derived, _))) = derivedFrom(milestones, 1)
    val termGroups = TermGrouper.termGroupsOf(parser.grammar, derived)

    termGroups.map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 1, termGroup)
      val progressTasks = termNodes.toList.zipWithIndex.map(n => parser.ProgressTask(n._1, AcceptConditionSlot(n._2)))

      val progressResults = runTasksWithProgressBarriers(2, progressTasks, startNodes.toSet,
        ContWithTasks(List(), parser.Cont(derived, Map())))
      val progressedGraph = progressResults.cc.graph

      val trimmed = parser.trimGraph(progressedGraph, startNodes.toSet, 2)

      // milestone이 될 조건을 만족하는 것들을 우선 추리고
      val appendingMilestones0 = progressResults.tasks.deriveTasks.appendingMilestoneCandidates(trimmed, 2)
      // startNodes 중 한 개 이상에서 도달 가능한 milestone만 추리면 appending milestones
      val appendingMilestones1 = appendingMilestones0.filter(progressedGraph.reachableBetween(startNodes.toSet, _))
      val appendAction = if (appendingMilestones1.isEmpty) None else {
        val survivingMilestones = milestones.zip(startNodes)
          .filter(pair => progressedGraph.reachableBetween(pair._2, appendingMilestones1.toSet))
          .map(_._1)
        val (appendingMilestones, appendingConditions) = milestonesFromNodes(appendingMilestones1)
        Some(TermActionAppendingAction(
          if (survivingMilestones.size == startNodes.size) None else Some(stepReplacement(milestones, survivingMilestones)),
          mgroupIdOf(appendingMilestones),
          appendingConditions.map(_.condition)
        ))
      }

      val startProgressTasks = progressResults.tasks.progressTasks.filter(t => startNodes.contains(t.node))
        .groupBy(_.node)
      val tipProgress = if (startProgressTasks.isEmpty) None else {
        val (progressingMilestones, progressingConditions) = milestonesFromNodes(startProgressTasks.keySet)
        // TODO progressingConditions로부터 TipProgress.acceptConditions 얻어내기
        Some(TipProgress(mgroupIdOf(progressingMilestones), ???))
      }

      termGroup -> TermAction(appendAction, tipProgress)
    }
  }

  def parserData(): MilestoneGroupParserData = {
    // TODO start에 대해서 Start 에서 도달 가능한 accept condition generating symbol들이 가리키는 symbol도 추가해야할 수도
    val startMgroup = mgroupIdOf(Set(Milestone(parser.grammar.startSymbol, 0, 1)))
    termActionsOf(startMgroup)
  }
}

object MilestoneGroupParserGen {
  def generateMilestoneGroupParserData(grammar: NGrammar): MilestoneGroupParserData =
    new MilestoneGroupParserGen(new NaiveParser(grammar)).parserData()

  def main(args: Array[String]): Unit = {
    val grammar1 =
      """Elem = 'a' {Literal()} | Array
        |Array = '[' WS Elem (WS ',' WS Elem)* WS ']' {Array(elems=[$2] + $3)}
        |      | '[' WS ']' {Array(elems=[])}
        |WS = ' '*
        |""".stripMargin
    val grammar2 =
      """Tokens = Tk (WS Tk)* {[$0] + $1}
        |Tk = Id | Kw
        |Id = Word-Kw {Identifier(name=$0)}
        |Kw = ("if" {%IF} | "int" {%INT})&Word {Keyword(value:%Keywords=$0)}
        |Word = <'a-zA-Z0-9_'+> {str($0)}
        |WS = ' '*
        |""".stripMargin
    val grammar = MetaLanguage3.analyzeGrammar(grammar2)
    val sourceText = "[[[a]]]"
    val valuefySimulator = ValuefyExprSimulator(grammar)

    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)
    val milestoneParser = new MilestoneParser(milestoneParserData, verbose = true)
    val milestoneParseForest = milestoneParser.parseAndReconstructToForest(sourceText).left.get
    val milestoneAst = milestoneParseForest.trees.map(valuefySimulator.valuefyStart)
    println(milestoneAst.map(_.prettyPrint()))

    val mgroupParserData = MilestoneGroupParserGen.generateMilestoneGroupParserData(grammar.ngrammar)
    val mgroupParser = new MilestoneGroupParser(mgroupParserData)
    //    val mgroupParseForest = mgroupParser.parseAndReconstructToForest(sourceText).left.get
    //    val mgroupAst = mgroupParseForest.trees.map(valuefySimulator.valuefyStart)
    //    println(mgroupAst)

    //    println(milestoneParserData)
    //    println(mgroupParserData)
  }
}
