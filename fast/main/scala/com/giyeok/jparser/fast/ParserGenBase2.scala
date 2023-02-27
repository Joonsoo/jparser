package com.giyeok.jparser.fast

import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AlwaysTemplate, ExistsTemplate, LongestTemplate, NeverTemplate, NotExistsTemplate, OnlyIfTemplate, UnlessTemplate}
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always, disjunct}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.{DeriveTask, FinishTask, KernelGraph, NaiveParser2, ParsingContext, ParsingTask, ProgressTask}

case class CtxWithTasks(ctx: ParsingContext, tasks: List[ParsingTask], startKernelProgressTasks: List[ProgressTask]) {
  def deriveTasks: List[DeriveTask] =
    tasks.filter(_.isInstanceOf[DeriveTask]).map(_.asInstanceOf[DeriveTask])

  def progressTasks: List[ProgressTask] =
    tasks.filter(_.isInstanceOf[ProgressTask]).map(_.asInstanceOf[ProgressTask])

  def progressConditionsFor(kernel: Kernel): List[AcceptCondition] =
    progressTasks.filter(_.kernel == kernel).map(_.condition)

  def finishTasks: List[FinishTask] =
    tasks.filter(_.isInstanceOf[FinishTask]).map(_.asInstanceOf[FinishTask])

  def conditionToTemplate(condition: AcceptCondition): AcceptConditionTemplate = {
    def cannotExist(kernel: Kernel) =
      progressConditionsFor(kernel).isEmpty && !ctx.graph.nodes.contains(kernel)

    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplate))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplate))
      case AcceptCondition.NotExists(0 | 1, 3, symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        LongestTemplate(symbolId)
      case AcceptCondition.Unless(0 | 1, 2, symbolId) =>
        // except
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(Unless이기 때문) 반환
          AlwaysTemplate
        } else {
          UnlessTemplate(symbolId)
        }
      case AcceptCondition.OnlyIf(0 | 1, 2, symbolId) =>
        // join
        if (cannotExist(Kernel(symbolId, 0, 1, 1))) {
          // ctx를 보고 혹시 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          // 아직 가능성이 있으면 forAcceptConditions에 KernelTemplate(beginGen, 0)에 대한 정보 추가
          OnlyIfTemplate(symbolId)
        }
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead except
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          NotExistsTemplate(symbolId)
        }
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          ExistsTemplate(symbolId)
        }
    }
  }

  def tasksSummary: TasksSummary2 = {
    val progressedStartKernel = startKernelProgressTasks.map(_.kernel).distinct
    assert(progressedStartKernel.size <= 1)
    val addedKernels = progressTasks.map(_.kernel).toSet[Kernel].map { kernel =>
      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2)
    } ++ finishTasks.filter(_.kernel.pointer == 0).map(_.kernel)
    TasksSummary2(
      addedKernels = addedKernels,
      progressedKernels = progressTasks.map(_.kernel).toSet,
      progressedStartKernel = progressedStartKernel.headOption)
  }
}

case class TasksSummary2(
  addedKernels: Set[Kernel],
  progressedKernels: Set[Kernel],
  progressedStartKernel: Option[Kernel],
)

case class ParserGenBase2(parser: NaiveParser2) {
  def runTasksWithProgressBarrier(nextGen: Int, tasks: List[ParsingTask], barrierNode: Kernel, cc: CtxWithTasks): CtxWithTasks = tasks match {
    case (barrierTask@ProgressTask(`barrierNode`, _)) +: rest =>
      val ncc = cc.copy(startKernelProgressTasks = barrierTask +: cc.startKernelProgressTasks)
      runTasksWithProgressBarrier(nextGen, rest, barrierNode, ncc)
    case task +: rest =>
      val (nextCtx, newTasks) = parser.process(nextGen, task, cc.ctx)
      val ncc = CtxWithTasks(nextCtx, task +: cc.tasks, cc.startKernelProgressTasks)
      runTasksWithProgressBarrier(nextGen, rest ++ newTasks, barrierNode, ncc)
    case List() => cc
  }

  def startingCtxFrom(start: KernelTemplate, baseGen: Int): (Kernel, CtxWithTasks) = {
    val startKernel = Kernel(start.symbolId, start.pointer, baseGen, baseGen + 1)
    val startGraph = KernelGraph(Set(startKernel), Set())
    val startCtx = ParsingContext(startGraph, Map(startKernel -> Always))

    val deriveTask = DeriveTask(startKernel)

    val ctx = runTasksWithProgressBarrier(
      baseGen + 1,
      List(deriveTask),
      startKernel,
      CtxWithTasks(startCtx, List(), List()))
    (startKernel, ctx)
  }
}
