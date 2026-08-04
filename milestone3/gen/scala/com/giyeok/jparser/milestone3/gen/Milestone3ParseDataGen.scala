package com.giyeok.jparser.milestone3.gen

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.NGrammar.{NSequence, NTerminal}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2._
import com.giyeok.jparser.nparser2.opt.{MutableParsingContext, OptNaiveParser2}
import com.giyeok.jparser.utils.TermGrouper

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class TermAction(parsingAction: ParsingAction)

case class ParsingAction(
  appends: List[AppendMilestone],
  tipProgressAcceptCondition: Option[AcceptConditionTemplate],
  // TODO tasks summary
)

case class AppendMilestone(
  appendKernelTemplate: KernelTemplate,
  appendAcceptCondition: AcceptConditionTemplate,
  edgeAcceptCondition: AcceptConditionTemplate,
)

case class KernelTemplate(symbolId: Int, pointer: Int)


sealed class AcceptConditionTemplate

case object ACTAlways extends AcceptConditionTemplate

case object ACTNever extends AcceptConditionTemplate

case class ACTAnd(conds: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class ACTOr(conds: List[AcceptConditionTemplate]) extends AcceptConditionTemplate

case class ACTLookahead(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

case class ACTLookaheadNot(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

case class ACTLongest(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

case class ACTOnlyIf(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

case class ACTUnless(symbolId: Int, fromNextGen: Boolean) extends AcceptConditionTemplate

class Milestone3ParseDataGen(
  val grammar: NGrammar
) {
  val naiveParser = new NaiveParser2(grammar)
  val optParser = new OptNaiveParser2(grammar)

  val pendingTips = mutable.Set[KernelTemplate]()
  val pendingEdges = mutable.Set[(KernelTemplate, KernelTemplate)]()

  val processedTips = mutable.Map[KernelTemplate, List[(TermGroupDesc, TermAction)]]()
  val processedEdges = mutable.Map[(KernelTemplate, KernelTemplate), ParsingAction]()
  val watcherRootSymbols = mutable.Map[KernelTemplate, Set[Int]]()
  val watcherRootNullableFinishConditions = mutable.Map[Int, AcceptConditionTemplate]()

  def x() = {
    pendingTips.add(KernelTemplate(grammar.startSymbol, 0))

    // derived
    val nextKernel = pendingTips.head
    processKernel(nextKernel)
  }

  def runTasks(
    nextGen: Int,
    tasks: List[ParsingTask],
    barrierNode: Kernel,
    ctx: ParsingContext
  ): (ParsingContext, Map[Kernel, AcceptCondition]) = {
    val mutCtx = MutableParsingContext(ctx)
    val mutTasks = mutable.ListBuffer[ParsingTask]()
    val mutBarrierProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case ProgressTask(`barrierNode`, condition) +: rest =>
          mutBarrierProgressConds.getOrElseUpdate(barrierNode, ListBuffer()) += condition
          recursion(rest)
        case (task@ProgressTask(kernel, condition)) +: rest =>
          mutTasks += task
          val newTasks = optParser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = optParser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)

    val acceptConditions = mutBarrierProgressConds.map { pair =>
      pair._1 -> AcceptCondition.disjunct(pair._2.toList: _*)
    }.toMap

    // TODO return tasks

    (mutCtx.toParsingContext, acceptConditions)
  }

  def processKernel(kernelTmpl: KernelTemplate): Unit = {
    def templatify(condition: AcceptCondition): AcceptConditionTemplate = condition match {
      // 추가된 accept condition 분류
      // - beginGen == 0 -> 존재 불가
      // - beginGen == 1 -> ???
      // - beginGen == 2 -> ???
      case AcceptCondition.Always => ACTAlways
      case AcceptCondition.Never => ACTNever
      case AcceptCondition.And(conditions) => ACTAnd(conditions.map(templatify).toList)
      case AcceptCondition.Or(conditions) => ACTOr(conditions.map(templatify).toList)
      case AcceptCondition.NotExists(1, 1, symbolId) => ???
      case AcceptCondition.NotExists(1, 2, symbolId) => ???
      case AcceptCondition.NotExists(2, 2, symbolId) => ???
      case AcceptCondition.Exists(beginGen, endGen, symbolId) => ???
      case AcceptCondition.Unless(beginGen, endGen, symbolId) => ???
      case AcceptCondition.OnlyIf(beginGen, endGen, symbolId) => ???
    }

    val tip = Kernel(kernelTmpl.symbolId, kernelTmpl.pointer, 0, 1)
    val initial = ParsingContext(KernelGraph(Set(tip), Set()), Map(tip -> Always))
    val (derived, tipAcceptConditions) = runTasks(2, List(DeriveTask(tip)), tip, initial)

    watcherRootSymbols(kernelTmpl) = symbolsToWatch(derived)

    if (kernelTmpl.pointer == 0 && tipAcceptConditions.contains(tip)) {
      watcherRootNullableFinishConditions(kernelTmpl.symbolId) = templatify(tipAcceptConditions(tip))
    }

    val termNodes = derived.graph.nodes
      .filter { node => grammar.symbolOf(node.symbolId).isInstanceOf[NTerminal] }
    assert(termNodes.forall(_.pointer == 0))
    val termSymbols = termNodes
      .map { node => grammar.symbolOf(node.symbolId).asInstanceOf[NTerminal] }
    val termGroups = TermGrouper.termGroupsOf(termSymbols.map(_.symbol))
    termGroups.foreach { tg =>
      val targetNodes = termNodes.filter { kernel =>
        val symbol = grammar.symbolOf(kernel.symbolId)
        symbol.asInstanceOf[NTerminal].symbol.acceptTermGroup(tg)
      }
      val progressList = targetNodes.map(ProgressTask(_, Always)).toList
      // TODO runTasks 에서 task들도 받아서 TasksSummary 계산에 사용
      val (derived, tipAcceptConditions) = runTasks(1, progressList, tip, derived)

      // parsingActionFrom(tip, derived, tipAcceptConditions.get(tip), templatify)

      val appendMilestones = derived.graph.nodes
        .filter(kernel => grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
        .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen && kernel.endGen == 2)
        .filter(kernel => kernel != tip && derived.graph.reachableBetween(tip, kernel))
      assert(appendMilestones.forall(m => m.beginGen == 1 && m.endGen == 2 && derived.graph.reachableBetween(tip, m)))

      val appends = appendMilestones.toList.sorted.map { m =>
        val appendAcceptCondition = derived.acceptConditions(m)
        val edgeAcceptCondition = edgeAcceptConditionsBetween(derived, tip, m)

        AppendMilestone(
          KernelTemplate(m.symbolId, m.pointer),
          // appendAcceptCondition의 템플릿 형태
          templatify(appendAcceptCondition),
          // edgeAcceptCondition의 템플릿 형태
          templatify(edgeAcceptCondition),
        )
      }

      // TODO tipProgressCondition to template
      val tipProgressCondition = tipAcceptConditions.get(tip).map(templatify)

      ParsingAction(appends, tipProgressCondition)
    }
  }

  def processEdge(startTmpl: KernelTemplate, endTmpl: KernelTemplate): Unit = {
    // endTmpl은 root가 아닌 milestone이어야 하므로 pointer > 0
    assert(endTmpl.pointer > 0)

    val start = Kernel(startTmpl.symbolId, startTmpl.pointer, 0, 1)
    val startOnly = ParsingContext(KernelGraph(Set(start), Set()), Map(start -> Always))
    val (initial, _) = runTasks(1, List(DeriveTask(start)), start, startOnly)

    val endInit = Kernel(endTmpl.symbolId, 0, 1, 1)
    assert(initial.graph.nodes.contains(endInit))

    // initial 에 end 강제로 추가(endInit의 incoming nodes -> end 노드 엣지 추가)
    val end = Kernel(endTmpl.symbolId, endTmpl.pointer, 1, 2)

    val ctxBuilder = MutableParsingContext(initial)

    ctxBuilder.graph.addNode(end)
    ctxBuilder.acceptConditions(end) = Always
    initial.graph.edgesByEnd(endInit).foreach { incoming =>
      ctxBuilder.graph.addEdge(Edge(incoming.start, end))
    }

    val withEnd = ctxBuilder.toParsingContext

    // 그 상태에서 end에 대한 ProgressTask 실행하고 그 결과로부터 ParsingAction 만들기
    val (progressed, startAcceptConditions) = runTasks(3, List(ProgressTask(end, Always)), start, withEnd)

    // 추가된 accept condition 분류:
    // - beginGen == 0, 2 -> 존재 불가. end.beginGen(1) 혹은 이번 wave gen(3) 만 가능.
    // - beginGen == 1 ->
    // - beginGen == 3 ->

    def templatify(condition: AcceptCondition): AcceptConditionTemplate = condition match {
      case AcceptCondition.Always => ACTAlways
      case AcceptCondition.Never => ACTNever
      case AcceptCondition.And(conditions) => ACTAnd(conditions.map(templatify).toList)
      case AcceptCondition.Or(conditions) => ACTOr(conditions.map(templatify).toList)
      case AcceptCondition.NotExists(1, 1, symbolId) => ???
      case AcceptCondition.NotExists(1, 2, symbolId) => ???
      case AcceptCondition.NotExists(1, 3, symbolId) => ???
      case AcceptCondition.NotExists(3, 3, symbolId) => ???
      case AcceptCondition.Exists(beginGen, endGen, symbolId) => ???
      case AcceptCondition.Unless(beginGen, endGen, symbolId) => ???
      case AcceptCondition.OnlyIf(beginGen, endGen, symbolId) => ???
      case _ => throw new IllegalArgumentException()
    }

    parsingActionFrom(start, progressed, 3, startAcceptConditions.get(start), templatify)
  }

  def parsingActionFrom(
    stem: Kernel,
    nextCtx: ParsingContext,
    waveGen: Int,
    tipProgressCondition: Option[AcceptCondition],
    acceptConditionTemplify: AcceptCondition => AcceptConditionTemplate
  ): ParsingAction = {
    val appendMilestones = nextCtx.graph.nodes
      .filter(kernel => grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
      .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen && kernel.endGen == waveGen)
      .filter(kernel => kernel != stem && nextCtx.graph.reachableBetween(stem, kernel))
    // term action only: assert(appendMilestones.forall(m => m.beginGen == 1 && m.endGen == 2))

    val appends = appendMilestones.toList.sorted.map { m =>
      // TODO appendAcceptCondition to template
      val appendAcceptCondition = acceptConditionTemplify(nextCtx.acceptConditions(m))
      // TODO edgeAcceptConditionsBetween가 Parsing context 뿐만 아니라 task들도 반환해야 함 - 그걸로 tip_progress_condition 추출 가능
      val edgeAcceptCondition = acceptConditionTemplify(edgeAcceptConditionsBetween(nextCtx, stem, m))

      AppendMilestone(
        KernelTemplate(m.symbolId, m.pointer),
        appendAcceptCondition,
        edgeAcceptCondition,
      )
    }

    appends.foreach { append =>
      addPendingEdge(KernelTemplate(stem.symbolId, stem.pointer), append.appendKernelTemplate)
    }

    // TODO tipProgressCondition to template
    val tipProgressCond = tipProgressCondition.map(acceptConditionTemplify)
    ParsingAction(appends, tipProgressCond)
  }

  def addPendingEdge(start: KernelTemplate, end: KernelTemplate) = {
    val edge = start -> end
    if (!processedEdges.contains(edge)) {
      pendingEdges.add(edge)
    }
  }

  def edgeAcceptConditionsBetween(ctx: ParsingContext, start: Kernel, end: Kernel): AcceptCondition = {
    // ctx에서 start node - end node 사이의 accept condition들을 Or-Of-Ands(같은 경로 내에선 And, 서로 다른 경로들끼린 Or)로 묶은 것
    ???
  }

  def symbolsToWatch(ctx: ParsingContext): Set[Int] = {
    // ctx.acceptConditions.values 에서 언급되는 심볼 ID 전체 + longest, except, join 에서 언급되는 심볼 ID 전체
    ???
  }

  //  def termGroupsOf(ctx: ParsingContext): List[TermGroupDesc] = {
  //    val terms = ctx.graph.nodes.map { node => grammar.symbolOf(node.symbolId) }
  //      .map { case term: NTerminal => term.symbol }
  //    TermGrouper.termGroupsOf(terms)
  //  }
}
