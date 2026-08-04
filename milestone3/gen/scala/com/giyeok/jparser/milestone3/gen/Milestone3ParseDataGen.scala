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
    val kernel = Kernel(kernelTmpl.symbolId, kernelTmpl.pointer, 0, 0)
    val initial = ParsingContext(KernelGraph(Set(kernel), Set()), Map(kernel -> Always))
    val (derived, derivedTasks) = naiveParser.deriveTask(0, DeriveTask(kernel), initial)

    watcherRootSymbols(kernelTmpl) = symbolsToWatch(derived)

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
      val progress = targetNodes.map(ProgressTask(_, Always)).toList
      // TODO runTasks 에서 task들도 받아서 TasksSummary 계산에 사용
      val (derived, tipAcceptConditions) = runTasks(1, progress, kernel, derived)

      val appendMilestones = derived.graph.nodes
        .filter(kernel => grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence])
        .filter(kernel => kernel.pointer > 0 && kernel.beginGen < kernel.endGen)
      assert(appendMilestones.forall(m => m.beginGen == 0 && m.endGen == 1 && isReachable(derived, kernel, m)))

      val appends = appendMilestones.toList.sorted.map { m =>
        // TODO appendAcceptCondition to template
        val appendAcceptCondition = derived.acceptConditions(m)
        // TODO edgeAcceptConditionsBetween가 Parsing context 뿐만 아니라 task들도 반환해야 함 - 그걸로 tip_progress_condition 추출 가능
        val edgeAcceptCondition = edgeAcceptConditionsBetween(derived, kernel, m)

        AppendMilestone(
          KernelTemplate(m.symbolId, m.pointer),
          // appendAcceptCondition의 템플릿 형태
          ???,
          // edgeAcceptCondition의 템플릿 형태
          ???,
        )
      }

      // TODO tipProgressCondition to template
      val tipProgressCondition = tipAcceptConditions.get(kernel)

      appends.foreach { append =>
        addPendingEdge(kernelTmpl, append.appendKernelTemplate)
      }

      ParsingAction(appends,
        // tipProgressCondition의 템플릿 형태
        ???)
    }
  }

  def addPendingEdge(start: KernelTemplate, end: KernelTemplate) = {
    val edge = start -> end
    if (!processedEdges.contains(edge)) {
      pendingEdges.add(edge)
    }
  }

  def isReachable(ctx: ParsingContext, start: Kernel, end: Kernel): Boolean = {
    ???
  }

  def edgeAcceptConditionsBetween(ctx: ParsingContext, start: Kernel, end: Kernel): AcceptCondition = {
    // ctx에서 start node - end node 사이의 accept condition 중 start의 derive 과정에서 발생한 것들(endGen == 0인 것들)을 Or-Of-Ands(같은 경로 내에선 And, 서로 다른 경로들끼린 Or)로 묶은 것
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
