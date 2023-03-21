package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.ParserGenBase
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.mgroup.MilestoneGroupParserData._
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.AcceptCondition._
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Node}
import com.giyeok.jparser.nparser.{Kernel, NaiveParser}
import com.giyeok.jparser.utils.TermGrouperUtil

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
   * @param endGen
   * @return (startNodes, Cont)
   */
  private def derivedFrom(milestones: List[Milestone], startGen: Int, endGen: Int): (List[Node], ContWithTasks) = {
    val startNodes = milestones.map(m => Node(Kernel(m.symbolId, m.pointer, startGen, endGen), Always))
    val startGraph = Graph(startNodes.toSet, Set())
    val deriveTasks = startNodes.map(parser.DeriveTask)

    val result = runTasksWithProgressBarriers(endGen, deriveTasks, startNodes.toSet,
      ContWithTasks(deriveTasks, parser.Cont(startGraph, Map())))
    (startNodes, result)
  }

  private def stepReplacement(fromMilestones: List[Milestone], toMilestones: List[Milestone]): StepReplacement = {
    val from = fromMilestones.map(_.tmpl)
    val to = toMilestones.map(_.tmpl)
    assert(to.toSet.subsetOf(from.toSet))
    val sortedFrom = from.sorted.zipWithIndex.map(f => f._1 -> f._2).toMap
    val sortedTo = to.sorted
    val newTo = sortedTo.zipWithIndex.map(p => Milestone(p._1.symbolId, p._1.pointer, p._2))
    StepReplacement(mgroupIdOf(newTo), sortedTo.map(sortedFrom))
  }

  private def parsingAction(graph: Graph, startGroupId: Int, startMilestoneAndNodes: List[(Milestone, Node)], progressTasks: List[parser.ProgressTask], currGen: Int,
    isTermAction: Boolean): ParsingAction = {
    val nextGen = currGen + 1
    val startNodes = startMilestoneAndNodes.map(_._2).toSet
    val progressResults = runTasksWithProgressBarriers(nextGen, progressTasks, startNodes,
      ContWithTasks(List(), parser.Cont(graph, Map())))
    val progressedGraph = progressResults.cc.graph

    val trimmed = parser.trimGraph(progressedGraph, startNodes, nextGen)

    // milestone이 될 조건을 만족하는 것들을 우선 추리고
    val appendingMilestones0 = progressResults.tasks.deriveTasks.appendingMilestoneCandidates(trimmed, nextGen)
    // startNodes 중 한 개 이상에서 도달 가능한 milestone만 추리면 appending milestones
    val appendingMilestonesWithReachable = appendingMilestones0
      .map(m => m -> progressedGraph.reachableNodesBetween(startNodes, m))
      .filter(_._2.nonEmpty)
    val appendingMilestones1 = appendingMilestonesWithReachable.map(_._1)
    val startMilestones = startMilestoneAndNodes.map(_._1)
    val appendAction = if (appendingMilestones1.isEmpty) None else {
      val survivingMilestones = startMilestoneAndNodes
        .filter(pair => progressedGraph.reachableBetween(pair._2, appendingMilestones1.toSet))
        .map(_._1)
      val (appendingMilestones, appendingConditions) = milestonesFromNodes(appendingMilestones1)
      // appendingConditions.map(_.condition)에는 AcceptConditionSlot이 없어야 함
      // TODO 위 조건 assert 추가?

      val conditions = if (isTermAction) {
        // append할 때 startGroup의 AC를 승계받는다.
        // 실제 naive 파싱과는 다른 동작이지만, path의 중간에서 AC evaluation을 하지 않도록 하기 위해 이렇게 처리한다.
        val reachables = appendingMilestonesWithReachable.map(p => (p._1.kernel.symbolId, p._1.kernel.pointer) -> p._2).toMap
        val succeedingConditions = appendingMilestones.map { appendingMilestone =>
          // appendingMilestone 에 도달 가능한 startNodes
          val startsReachableToMilestone = reachables(appendingMilestone.symbolId -> appendingMilestone.pointer)
          val slotIdx = startsReachableToMilestone.map(m =>
            survivingMilestones.zipWithIndex.find(
              parent => (parent._1.symbolId, parent._1.pointer) == (m.kernel.symbolId, m.kernel.pointer)).get._2)
          val slots = slotIdx.map(AcceptConditionSlot).toSeq
          disjunct(slots: _*)
        }
        succeedingConditions.zip(appendingConditions).map(p => conjunct(p._1, p._2))
      } else {
        appendingConditions
      }

      val replace = if (survivingMilestones.size == startNodes.size) None else Some(stepReplacement(startMilestones, survivingMilestones))
      val appendingGroup = mgroupIdOf(appendingMilestones)
      if (replace.isDefined) {
        positionsGraph = positionsGraph.addEdgeSafe(ReplacedBy(replace.get.mgroup, startGroupId))
      }
      positionsGraph = positionsGraph.addEdgeSafe(FollowedBy(appendingGroup, if (replace.isDefined) replace.get.mgroup else startGroupId))
      tipGroups += appendingGroup
      Some(AppendingAction(replace, appendingGroup, conditions))
    }

    val startProgressTasks = progressResults.tasks.progressTasks.filter(t => startNodes.contains(t.node))
    val tipProgress = if (progressResults.tasks.progressTasks.isEmpty) None else {
      val tasks = startProgressTasks.map(task =>
        Milestone(task.node.kernel.symbolId, task.node.kernel.pointer, -1) -> conjunct(task.node.condition, task.condition)
      )
      val milestoneTmpls = tasks.groupBy(_._1).view.mapValues(g => disjunct(g.map(_._2): _*)).toList.sortBy(_._1)
      val progressingMilestones = milestoneTmpls.map(_._1)
      val progressingConditions = milestoneTmpls.map(_._2)
      val replacement = stepReplacement(startMilestones, progressingMilestones)
      // Tip Progress는 새로 생기는 accept condition밖에 없어서 succession이 필요 없을듯
      if (startGroupId != replacement.mgroup) {
        positionsGraph = positionsGraph.addEdgeSafe(ReplacedBy(replacement.mgroup, startGroupId))
      }
      edgeTriggerGroups += replacement.mgroup
      Some(StepProgress(replacement, progressingConditions))
    }

    // TODO milestone parser의 dependent 처리

    ParsingAction(appendAction, tipProgress)
  }

  private def termActionsOf(mgroupId: Int): List[(TermGroupDesc, ParsingAction)] = {
    val milestones = milestoneGroupsById(mgroupId)
    val (startNodes, ContWithTasks(_, parser.Cont(derived, _))) = derivedFrom(milestones, TERM_START_GEN, TERM_END_GEN)
    val termGroups = TermGrouperUtil.termGroupsOf(parser.grammar, derived)

    termGroups.map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, TERM_END_GEN, termGroup)
      // terminal node는 항상 Always로 progress됨
      val progressTasks = termNodes.toList.map(parser.ProgressTask(_, Always))

      termGroup -> parsingAction(derived, mgroupId, milestones.zip(startNodes), progressTasks, TERM_CURRENT_GEN, isTermAction = true)
    }
  }

  private def edgeActionBetween(parentMGroupId: Int, tipMGroupId: Int): ParsingAction = {
    // parent -> tip 엣지에서, parent mgroup의 마일스톤 중 "한 개 이상의 tip으로 도달 가능한 milestone들만" 추린 다음(survivingParentMilestones),
    // - tip의 milestone은 모두 parent에서 도달 가능해야 함
    val originalParentMilestones = milestoneGroupsById(parentMGroupId)
    val tipMilestones = milestoneGroupsById(tipMGroupId)
    val (startNodes, ContWithTasks(_, parser.Cont(derivedGraph, _))) =
      derivedFrom(originalParentMilestones, EDGE_PARENT_GEN, EDGE_START_GEN)
    val tipNodes = tipMilestones.map(m => Node(Kernel(m.symbolId, 0, EDGE_START_GEN, EDGE_START_GEN), Always)).toSet
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
      val prevNode = Node(Kernel(milestone.symbolId, 0, EDGE_START_GEN, EDGE_START_GEN), Always)
      val newNode = Node(Kernel(milestone.symbolId, milestone.pointer, EDGE_START_GEN, EDGE_END_GEN), AcceptConditionSlot(slotIdx))
      prevNode -> newNode
    }
    val graphWithFakeEnds = fakeEnds.foldLeft(derivedGraph) { (graph, fakeEnd) =>
      val (prevNode, newNode) = fakeEnd
      graph.edgesByEnd(prevNode).foldLeft(graph.addNode(newNode)) { (ngraph, edge) =>
        ngraph.addEdge(Edge(edge.start, newNode))
      }
    }
    val afterDerive = parser.rec(EDGE_END_GEN, fakeEnds.map(e => parser.DeriveTask(e._2)), graphWithFakeEnds)
    val afterTrimming = parser.trimGraph(afterDerive.graph, startNodes.toSet, EDGE_END_GEN)

    // parent에서 도달 가능한 milestone들이 있으면 parentReplacement + appending으로 설정하고
    // parent 중 progress되는 것들이 있으면 parentProgress
    // - 전 단계에서 만들어진 AC들은 이미 slot에 반영되어 있으므로 여기서는 Always로 progress하면 될듯
    val progressTasks = fakeEnds.map(_._2).toSet.intersect(afterTrimming.nodes).toList.map(parser.ProgressTask(_, Always))

    parsingAction(afterTrimming, parentMGroupId, survivingParentMilestones, progressTasks, EDGE_CURRENT_GEN, isTermAction = false)
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
          tasksSummaryFrom(List()),
          /* TODO */ milestoneGroupsById.view.mapValues(list => MilestoneGroup(list, list.size)).toMap,
          termActions,
          edgeActions,
          /* TODO */ Map(),
          /* TODO */ Map())
      } else {
        jobs.tips.toList.sorted.foreach { tip =>
          termActions += tip -> termActionsOf(tip)
        }
        jobs.edges.toList.sorted.foreach { edge =>
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
    val grammar3 =
      """expression = term
        |    | expression WS '+' WS term
        |term = factor
        |    | term WS '*' WS factor
        |factor = 'a-z'+ | '1-9'+
        |    | '(' WS expression WS ')'
        |WS = ' '*
        |""".stripMargin
    val (grammarDef, sourceText) = (grammar1, """[a, a, a]""")
    //    val (grammarDef, sourceText) = (grammar2, """if  int""")
    //    val (grammarDef, sourceText) = (grammar3, """1 + 234  * abc""")
    val grammar = MetaLanguage3.analyzeGrammar(grammarDef)
    val valuefySimulator = ValuefyExprSimulator(grammar)

    println("**** Milestone Parser")
    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)
    val milestoneParser = new MilestoneParser(milestoneParserData, verbose = true)
    val milestoneParseForest = milestoneParser.parseAndReconstructToForest(sourceText).left.get
    val milestoneAst = milestoneParseForest.trees.map(valuefySimulator.valuefyStart)
    println(milestoneAst.map(_.prettyPrint()))

    println()
    println("**** Milestone Group Parser")
    val mgroupParserData = MilestoneGroupParserGen.generateMilestoneGroupParserData(grammar.ngrammar)
    val mgroupParser = new MilestoneGroupParser(mgroupParserData, verbose = true)
    val ctx = mgroupParser.parse(sourceText)
    println(ctx)
    //    val mgroupParseForest = mgroupParser.parseAndReconstructToForest(sourceText).left.get
    //    val mgroupAst = mgroupParseForest.trees.map(valuefySimulator.valuefyStart)
    //    println(mgroupAst)

    //    println(milestoneParserData)
    //    println(mgroupParserData)
  }
}
