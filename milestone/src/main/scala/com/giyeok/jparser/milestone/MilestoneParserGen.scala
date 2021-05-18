package com.giyeok.jparser.milestone

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NExcept, NJoin, NLookaheadIs, NLookaheadSymbol, NSequence}
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.ParsingContext.{Edge, Graph, Kernel, Node}
import com.giyeok.jparser.nparser.{AcceptCondition, NaiveParser}
import com.giyeok.jparser.utils.TermGrouper

import scala.annotation.tailrec

class MilestoneParserGen(val parser: NaiveParser) {

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

  private def startingCtxFrom(startKernel: Kernel): (Node, ContWithTasks) = {
    val startNode = Node(startKernel, Always)
    val startGraph = Graph(Set(startNode), Set())

    val deriveTask = parser.DeriveTask(startNode)

    (startNode, runTasksWithProgressBarrier(startKernel.endGen, List(deriveTask), startNode,
      ContWithTasks(List(deriveTask), parser.Cont(startGraph, Map()))))
  }

  private def startingCtxFrom(template: KernelTemplate, nextGen: Int): (Node, ContWithTasks) =
    startingCtxFrom(Kernel(template.symbolId, template.pointer, 0, nextGen))

  private implicit class ParsingTasksList(val tasks: List[parser.Task]) {
    def deriveTasks: List[parser.DeriveTask] =
      tasks.filter(_.isInstanceOf[parser.DeriveTask]).map(_.asInstanceOf[parser.DeriveTask])

    def progressTasks: List[parser.ProgressTask] =
      tasks.filter(_.isInstanceOf[parser.ProgressTask]).map(_.asInstanceOf[parser.ProgressTask])

    def finishTasks: List[parser.FinishTask] =
      tasks.filter(_.isInstanceOf[parser.FinishTask]).map(_.asInstanceOf[parser.FinishTask])
  }

  // TODO accept condition 때문에 생기는 derivation에 대한 별도 처리 필요. (MilestoneParserTest의 마지막 테스트 참고)
  // -> 다른 milestone path에 dependent한 milestone path가 존재할 수 있음.
  //    예를 들어 ("elem"&Tk) 커널에서는 "elem" 커널로 가는 엣지밖에 없지만 Tk 커널에 대한 파싱도 파악할 필요가 있음
  //    Naive parser에서는 trimGraph할 때 reachablesFromStart에 의해 ("elem"&Tk)가 살아있을 때만 Tk도 살아남게 되는데
  //    Milestone parser에서는 그런 구조를 표현할 방법이 없음.
  //    또 이런 경우 Tk 밑에 붙는 milestone이 바로 start에서 붙는 형태가 되는데 이러면 제대로 구조 파악이 안됨.
  //    -> 즉, Start를 거치지 않고 (Tk -> ...)의 마일스톤 path가 되어야 하므로 별도 처리가 필요
  def parsingActionFrom(graph: Graph, startNode: Node, progressTasks: List[parser.ProgressTask], currGen: Int): ParsingAction = {
    val nextGen = currGen + 1
    val termProgressResult = runTasksWithProgressBarrier(nextGen, progressTasks, startNode,
      ContWithTasks(List(), parser.Cont(graph, Map())))
    val progressedGraph = termProgressResult.cc.graph
    val trimmed = parser.trimGraph(progressedGraph, startNode, nextGen)

    val appendingMilestones0 = termProgressResult.tasks.deriveTasks
      .filter(task => trimmed.nodes.contains(task.node))
      .filter(task => parser.grammar.symbolOf(task.node.kernel.symbolId).isInstanceOf[NSequence])
      .map(_.node)
      .filter(node => node.kernel.beginGen < node.kernel.endGen && node.kernel.endGen == nextGen)
    val startProgressTasks = termProgressResult.tasks.progressTasks.filter(_.node == startNode)
    val startProgressConditions = startProgressTasks.map(_.condition)

    val (_, actions) = startProgressTasks.foldLeft((termProgressResult.cc, termProgressResult.tasks)) { case ((cc, tasks), nextTask) =>
      val (ncc, newTasks) = parser.process(nextGen, nextTask, cc)
      (ncc, tasks ++ newTasks)
    }

    // kernelTemplate이 tip pointer일 때 termGroup이 들어오면
    // - rootIsProgressed=true이면 tip pointer가 progress되고
    // - for each milestone in appendingMilestones,
    //   - kernelTemplate->milestone 으로 가는 경로가 추가되고
    //   - kernelTemplate->milestone 사이에는 trimmed가 그래프로 들어감
    // appendingMilestones0 중 startNode에서 직접 edge를 통해 reachable한 것들만 appendingMilestones로 픽스.
    // -> 그 외에는 dependent로 추가될 것들
    // TODO 각 appendingMilestones에 대해(let say AM), (startNode -> AM)으로 가는 경로들만 포함하는 subgraph를 계산하고,
    //      그 그래프에 join, except, lookahead 심볼이 있으면 이들의 (accept condition symbol) SYM 각각에 대해,
    //      list of dependent를 만들어 AM의 dependent로 추가하는데,
    //      이 dependent는 ((SYM, 0, currGen, currGen), Always)에서 reachable한 milestone(in appendingMilestones0) M의 리스트에 대해,
    //      ((SYM, 0), M.kernelTemplate, M.condition)가 된다.
    val appendingMilestones = appendingMilestones0.filter(progressedGraph.reachableBetween(startNode, _))
    val appendingMilestoneActions = appendingMilestones.map { node =>
      def dependentStartsBetween(from: Node, ends: Set[Node]): Set[Node] = {
        val subgraphBetween = progressedGraph.reachableGraphBetween(from, ends)
        subgraphBetween.nodes
          .filter(_.kernel.endGen == currGen)
          .flatMap(n => parser.grammar.symbolOf(n.kernel.symbolId) match {
            case NJoin(_, _, _, join) => Some(join)
            case NExcept(_, _, _, except) => Some(except)
            case lookahead: NLookaheadSymbol => Some(lookahead.lookahead)
            case _ => None
          })
          .map(symbolId => Node(Kernel(symbolId, 0, currGen, currGen), Always))
      }

      val starts0 = dependentStartsBetween(startNode, Set(node)).toList.sortBy(_.kernel.tuple)

      @tailrec
      def allDependents(newStarts: Set[Node], allStarts: Set[Node]): List[Node] = {
        val nextStarts = newStarts.flatMap { start =>
          val ends = appendingMilestones0.filter(progressedGraph.reachableBetween(start, _))
          dependentStartsBetween(start, ends.toSet)
        } -- allStarts
        if (nextStarts.nonEmpty) allDependents(nextStarts, allStarts ++ nextStarts)
        else (allStarts ++ nextStarts).toList.sortBy(_.kernel.tuple)
      }

      val dependents = allDependents(starts0.toSet, starts0.toSet).flatMap { start =>
        val ends = appendingMilestones0.filter(progressedGraph.reachableBetween(start, _))
        ends map { end =>
          (KernelTemplate(start.kernel.symbolId, start.kernel.pointer), KernelTemplate(end.kernel.symbolId, end.kernel.pointer), end.condition)
        }
      }
      AppendingMilestone(KernelTemplate(node.kernel.symbolId, node.kernel.pointer), node.condition, dependents)
    }
    ParsingAction(
      appendingMilestoneActions,
      tasksSummaryFrom(actions),
      startProgressConditions,
      GraphNoIndex.fromGraph(trimmed))
  }

  private def tasksSummaryFrom(tasks: List[parser.Task]) = {
    val progressed = tasks.progressTasks.map(t => (t.node, t.condition))
    val finished = tasks.finishTasks.map(_.node)
    TasksSummary(progressed, finished)
  }

  // `kernelTemplate`가 tip에 있을 때 받을 수 있는 터미널들을 찾고, 각 터미널에 대해 KernelTemplateAction 계산
  // 반환되는 pair에서 _1는 각각 disjoint해야 함(같은 터미널을 포함할 수 없음)
  def termActionsFrom(kernelTemplate: KernelTemplate): List[(TermGroupDesc, ParsingAction)] = {
    val (startNode, ContWithTasks(_, parser.Cont(derived, _))) = startingCtxFrom(kernelTemplate, 1)

    // new DotGraphGenerator(parser.grammar).addGraph(derived).printDotGraph()

    TermGrouper.termGroupsOf(parser.grammar, derived).map { termGroup =>
      val termNodes = parser.finishableTermNodes(derived, 1, termGroup)
      val termProgressTasks = termNodes.toList.map(parser.ProgressTask(_, AcceptCondition.Always))

      termGroup -> parsingActionFrom(derived, startNode, termProgressTasks, 1)
    }.toList.sortBy(_._1.toString)
  }

  // `simulateEdgeProgress` 바로 다음에 `endTemplate`가 붙어있고 `endTemplate`이 progress되는 경우
  def edgeProgressActionsBetween(startTemplate: KernelTemplate, endTemplate: KernelTemplate): ParsingAction = {
    val (startNode, ContWithTasks(_, parser.Cont(derived, _))) =
      if (startTemplate.pointer == 0) startingCtxFrom(Kernel(startTemplate.symbolId, startTemplate.pointer, 1, 1))
      else startingCtxFrom(Kernel(startTemplate.symbolId, startTemplate.pointer, 0, 1))

    val endKernelInitials = derived.nodes.filter { node =>
      node.kernel.symbolId == endTemplate.symbolId && node.kernel.pointer < endTemplate.pointer
    }

    val fakeEnds = endKernelInitials.map { node =>
      node -> Node(Kernel(endTemplate.symbolId, endTemplate.pointer, 1, 2), node.condition)
    }.toMap
    val derivedWithEnds = fakeEnds.foldLeft(derived) { (graph, end) =>
      graph.edgesByEnd(end._1).foldLeft(graph.addNode(end._2)) { (ngraph, start) =>
        ngraph.addEdge(Edge(start.start, end._2))
      }
    }
    val afterDerive = parser.rec(2, fakeEnds.values.map(parser.DeriveTask).toList, derivedWithEnds)
    val afterTrimming = parser.trimGraph(afterDerive.graph, startNode, 2)

    // graphBetween은 startTemplate과 endTemplate 사이의 그래프(endTemplate에서 derive된 것도 포함해야 함)
    //   -> 이게 유일하게 결정될까? (accept condition은?)
    // endNodes는 graphBetween에서 endTemplate에 대응되는 노드(들)
    val (graphBetween: Graph, endNodes: Set[Node]) = (afterTrimming, fakeEnds.values.toSet)

    val progressTasks = endNodes.map(parser.ProgressTask(_, Always)).toList
    parsingActionFrom(graphBetween, startNode, progressTasks, 2)
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  @tailrec
  private def createParserData(jobs: Jobs, cc: MilestoneParserData): MilestoneParserData = {
    val withTermActions = jobs.milestones.foldLeft((Jobs(Set(), Set()), cc)) { (m, i) =>
      val (jobs, wcc) = m
      val termActions = termActionsFrom(i)
      val appendables = termActions.flatMap(_._2.appendingMilestones.map(_.milestone)).toSet
      val dependents = termActions.flatMap(_._2.appendingMilestones.flatMap { appending =>
        appending.dependents.map(dep => (dep._1, dep._2))
      }).toSet
      (Jobs(jobs.milestones ++ appendables ++ dependents.map(_._2),
        jobs.edges ++ appendables.map((i, _)) ++ dependents),
        wcc.copy(termActions = wcc.termActions + (i -> termActions)))
    }
    val withEdgeActions = jobs.edges.foldLeft(withTermActions) { (m, i) =>
      val (jobs, wcc) = m
      val edgeAction = edgeProgressActionsBetween(i._1, i._2)
      val appendables = edgeAction.appendingMilestones.map(_.milestone)
      val newEdges = appendables.map((i._1, _)).toSet
      val dependents = edgeAction.appendingMilestones.flatMap { appending =>
        appending.dependents.map(dep => (dep._1, dep._2))
      }.toSet
      (Jobs(jobs.milestones ++ appendables ++ dependents.map(_._2),
        jobs.edges ++ newEdges ++ dependents),
        wcc.copy(edgeProgressActions = wcc.edgeProgressActions + (i -> edgeAction)))
    }
    val (newJobs, ncc) = withEdgeActions
    val newRemainingJobs = Jobs(
      milestones = newJobs.milestones -- ncc.termActions.keySet,
      edges = newJobs.edges -- ncc.edgeProgressActions.keySet)
    println(s"Remaining jobs: milestones=${newRemainingJobs.milestones.size}, edges=${newRemainingJobs.edges.size}")
    if (newRemainingJobs.milestones.isEmpty && newRemainingJobs.edges.isEmpty) ncc
    else createParserData(newRemainingJobs, ncc)
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    val (_, ContWithTasks(tasks, _)) = startingCtxFrom(Kernel(start.symbolId, start.pointer, 1, 1))

    val result = createParserData(Jobs(Set(start), Set()),
      MilestoneParserData(parser.grammar, tasksSummaryFrom(tasks), Map(), Map(), Map()))
    result.copy(derivedGraph = result.termActions.keySet.map { kernelTemplate =>
      val startNode = Node(Kernel(kernelTemplate.symbolId, kernelTemplate.pointer, 0, 0), Always)
      kernelTemplate -> GraphNoIndex.fromGraph(parser.rec(0, List(parser.DeriveTask(startNode)), Graph(Set(startNode), Set())).graph)
    }.toMap)
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser(grammar)).parserData()
}
