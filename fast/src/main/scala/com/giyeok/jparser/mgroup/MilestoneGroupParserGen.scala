package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.ParserGenBase
import com.giyeok.jparser.metalang3a.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, AcceptConditionSlot, Always, disjunct}
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Kernel, Node}
import com.giyeok.jparser.utils.TermGrouper

import scala.annotation.tailrec

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

  private def milestonesFromNodes(nodes: Iterable[Node]): (List[Milestone], List[AcceptCondition]) = {
    val milestoneTmpls = nodes.toList.groupBy(n => Milestone(n.kernel.symbolId, n.kernel.pointer, -1)).toList.sortBy(_._1)
    val milestones = milestoneTmpls.zipWithIndex
      .map(p => p._1._1.copy(acceptConditionSlot = p._2) -> disjunct(p._1._2.map(_.condition): _*))
    (milestones.map(_._1), milestones.map(_._2))
  }

  private var positionsGraph = MilestoneGroupPositionGraph.empty
  private var tipGroups = Set[Int]()
  private var edgeTriggerGroups = Set[Int]()

  /**
   *
   * @param milestones
   * @param nextGen
   * @return (startNodes, Cont)
   */
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

  private def parsingAction(graph: Graph, startGroupId: Int, startMilestoneAndNodes: List[(Milestone, Node)], progressTasks: List[parser.ProgressTask], currGen: Int): ParsingAction = {
    val nextGen = currGen + 1
    val startNodes = startMilestoneAndNodes.map(_._2).toSet
    val progressResults = runTasksWithProgressBarriers(nextGen, progressTasks, startNodes,
      ContWithTasks(List(), parser.Cont(graph, Map())))
    val progressedGraph = progressResults.cc.graph

    val trimmed = parser.trimGraph(progressedGraph, startNodes, nextGen)

    // milestone이 될 조건을 만족하는 것들을 우선 추리고
    val appendingMilestones0 = progressResults.tasks.deriveTasks.appendingMilestoneCandidates(trimmed, nextGen)
    // startNodes 중 한 개 이상에서 도달 가능한 milestone만 추리면 appending milestones
    val appendingMilestones1 = appendingMilestones0.filter(progressedGraph.reachableBetween(startNodes, _))
    val appendAction = if (appendingMilestones1.isEmpty) None else {
      val survivingMilestones = startMilestoneAndNodes
        .filter(pair => progressedGraph.reachableBetween(pair._2, appendingMilestones1.toSet))
        .map(_._1)
      val (appendingMilestones, appendingConditions) = milestonesFromNodes(appendingMilestones1)
      // appendingConditions.map(_.condition)에는 AcceptConditionSlot이 없어야 함
      // TODO 위 조건 assert 추가?
      val startMilestones = startMilestoneAndNodes.map(_._1)

      val replace = if (survivingMilestones.size == startNodes.size) None else Some(stepReplacement(startMilestones, survivingMilestones))
      val appendingGroup = mgroupIdOf(appendingMilestones)
      if (replace.isDefined) {
        positionsGraph = positionsGraph.addEdgeSafe(ReplacedBy(replace.get.mgroup, startGroupId))
      }
      positionsGraph = positionsGraph.addEdgeSafe(FollowedBy(appendingGroup, if (replace.isDefined) replace.get.mgroup else startGroupId))
      tipGroups += appendingGroup
      Some(AppendingAction(replace, appendingGroup, appendingConditions))
    }

    val startProgressTasks = progressResults.tasks.progressTasks.filter(t => startNodes.contains(t.node))
      .groupBy(_.node)
    val tipProgress = if (startProgressTasks.isEmpty) None else {
      val (progressingMilestones, progressingConditions) = milestonesFromNodes(startProgressTasks.keySet)
      // Tip Progress는 새로 생기는 accept condition밖에 없어서 succession이 필요 없을듯
      val edgeTriggerGroup = mgroupIdOf(progressingMilestones)
      if (startGroupId != edgeTriggerGroup) {
        positionsGraph = positionsGraph.addEdgeSafe(ReplacedBy(edgeTriggerGroup, startGroupId))
      }
      edgeTriggerGroups += edgeTriggerGroup
      Some(StepProgress(edgeTriggerGroup, progressingConditions))
    }

    // TODO milestone parser의 dependent 처리

    ParsingAction(appendAction, tipProgress)
  }

  private def termActionsOf(mgroupId: Int): List[(TermGroupDesc, ParsingAction)] = {
    val milestones = milestoneGroupsById(mgroupId)
    val (startNodes, ContWithTasks(_, parser.Cont(derived, _))) = derivedFrom(milestones, 1)
    val termGroups = TermGrouper.termGroupsOf(parser.grammar, derived)

    termGroups.map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 1, termGroup)
      // terminal node는 항상 Always로 progress됨
      val progressTasks = termNodes.toList.map(parser.ProgressTask(_, Always))

      termGroup -> parsingAction(derived, mgroupId, milestones.zip(startNodes), progressTasks, 1)
    }
  }

  private def edgeActionBetween(parentMGroupId: Int, tipMGroupId: Int): ParsingAction = {
    // parent -> tip 엣지에서, parent mgroup의 마일스톤 중 "한 개 이상의 tip으로 도달 가능한 milestone들만" 추린 다음(survivingParentMilestones),
    // - tip의 milestone은 모두 parent에서 도달 가능해야 함
    val originalParentMilestones = milestoneGroupsById(parentMGroupId)
    val tipMilestones = milestoneGroupsById(tipMGroupId)
    val tipNodes = tipMilestones.map(m => Node(Kernel(m.symbolId, 0, 1, 1), Always)).toSet
    val (startNodes, ContWithTasks(_, parser.Cont(derivedGraph, _))) = derivedFrom(originalParentMilestones, 1)
    assert(tipNodes.forall(derivedGraph.reachableBetween(startNodes.toSet, _)))
    val survivingParentMilestones = startNodes.zip(originalParentMilestones).collect {
      case (startNode, parentMilestone) if derivedGraph.reachableBetween(startNode, tipNodes) =>
        parentMilestone -> startNode
    }
    // tip group의 milestone 노드들의 ProgressTask(with SlotCondition)을 실행한 다음
    // - tip group의 milestone으로 fake end들을 만들어서 엣지와 함께 추가하고
    // - 다음 gen에 살아남는 노드로 추려서 사용
    val fakeEnds = tipMilestones.zipWithIndex.map { pair =>
      val (milestone, slotIdx) = pair
      val prevNode = Node(Kernel(milestone.symbolId, 0, 1, 1), Always)
      val newNode = Node(Kernel(milestone.symbolId, milestone.pointer, 1, 2), AcceptConditionSlot(slotIdx))
      prevNode -> newNode
    }
    val graphWithFakeEnds = fakeEnds.foldLeft(derivedGraph) { (graph, fakeEnd) =>
      val (prevNode, newNode) = fakeEnd
      graph.edgesByEnd(prevNode).foldLeft(graph.addNode(newNode)) { (ngraph, edge) =>
        ngraph.addEdge(Edge(edge.start, newNode))
      }
    }
    val afterDerive = parser.rec(2, fakeEnds.map(e => parser.DeriveTask(e._2)), graphWithFakeEnds)
    val afterTrimming = parser.trimGraph(afterDerive.graph, startNodes.toSet, 2)

    // parent에서 도달 가능한 milestone들이 있으면 parentReplacement + appending으로 설정하고
    // parent 중 progress되는 것들이 있으면 parentProgress
    // - 전 단계에서 만들어진 AC들은 이미 slot에 반영되어 있으므로 여기서는 Always로 progress하면 될듯
    val progressTasks = fakeEnds.map(_._2).toSet.intersect(afterTrimming.nodes).toList.map(parser.ProgressTask(_, Always))

    parsingAction(afterTrimming, parentMGroupId, survivingParentMilestones, progressTasks, 2)
  }

  private var termActions = Map[Int, List[(TermGroupDesc, ParsingAction)]]()
  private var edgeActions = Map[(Int, Int), ParsingAction]()

  case class Jobs(tips: Set[Int], edges: Set[(Int, Int)])

  private def remainingJobs(): Jobs = {
    def traversePrevs(mgroupId: Int): Set[Int] = {
      positionsGraph.edgesByStart(mgroupId).collect {
        case ReplacedBy(_, end) =>
          traversePrevs(end)
        case FollowedBy(_, end) =>
          Set(end)
      }.flatten
    }

    val possibleEdges = edgeTriggerGroups.flatMap(g => traversePrevs(g).map(_ -> g))
    Jobs(tipGroups -- termActions.keySet, possibleEdges -- edgeActions.keySet)
  }

  def parserData(): MilestoneGroupParserData = {
    // TODO start에 대해서 Start 에서 도달 가능한 accept condition generating symbol들이 가리키는 symbol도 추가해야할 수도
    val startMGroup = mgroupIdOf(Set(Milestone(parser.grammar.startSymbol, 0, 0)))
    val emptyMGroup = mgroupIdOf(Set())

    tipGroups += startMGroup

    @tailrec
    def generateData(): MilestoneGroupParserData = {
      val jobs = remainingJobs()
      // println(s"remaining jobs: tips=${jobs.tips.size}, edges=${jobs.edges.size}")
      println(jobs)
      if (jobs.tips.isEmpty && jobs.edges.isEmpty) {
        // Done
        milestoneGroupsById.toList.sortBy(_._1).foreach { mgroup =>
          println(s"** ${mgroup._1}")
          mgroup._2.foreach(println)
        }
        // TODO drop action 계산
        // TODO AC slot들 사이의 관계 그래프 계산
        MilestoneGroupParserData(parser.grammar,
          startMGroup,
          emptyMGroup,
          ???,
          ???,
          termActions,
          edgeActions,
          ???,
          ???)
      } else {
        jobs.tips.foreach { tip =>
          termActions += tip -> termActionsOf(tip)
        }
        jobs.edges.foreach { edge =>
          edgeActions += edge -> edgeActionBetween(edge._1, edge._2)
        }
        generateData()
      }
    }

    // term action과 edge action을 모두 구한 다음,
    // 각 mgroup의 accept condition slot의 관계를 알아내서
    // - 각 mgroup에서 불필요한 slot을 제거해서 slot 수를 줄이고
    // - 필요한 drop actions만 계산해서 drop actions를 넣는다.
    //   - drop action 계산시 불필요한걸 추릴 수 없으면 그냥 2^n개 생성
    val data0 = generateData()

    // TODO data0에서 accept condition slot optimization
    data0
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
    val grammar = MetaLanguage3.analyzeGrammar(grammar1)
    val sourceText = "[[[a]]]"
    val valuefySimulator = ValuefyExprSimulator(grammar)

    //    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)
    //    val milestoneParser = new MilestoneParser(milestoneParserData, verbose = true)
    //    val milestoneParseForest = milestoneParser.parseAndReconstructToForest(sourceText).left.get
    //    val milestoneAst = milestoneParseForest.trees.map(valuefySimulator.valuefyStart)
    //    println(milestoneAst.map(_.prettyPrint()))

    val mgroupParserData = MilestoneGroupParserGen.generateMilestoneGroupParserData(grammar.ngrammar)
    val mgroupParser = new MilestoneGroupParser(mgroupParserData)
    //    val mgroupParseForest = mgroupParser.parseAndReconstructToForest(sourceText).left.get
    //    val mgroupAst = mgroupParseForest.trees.map(valuefySimulator.valuefyStart)
    //    println(mgroupAst)

    //    println(milestoneParserData)
    //    println(mgroupParserData)
  }
}
