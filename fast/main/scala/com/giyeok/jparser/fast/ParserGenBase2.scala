package com.giyeok.jparser.fast

import com.giyeok.jparser.milestone2._
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2.{ParsingContext, _}

case class CtxWithTasks(ctx: ParsingContext, tasks: List[ParsingTask], startKernelProgressTasks: List[ProgressTask]) {
  def deriveTasks: List[DeriveTask] =
    tasks.filter(_.isInstanceOf[DeriveTask]).map(_.asInstanceOf[DeriveTask])

  def progressTasks: List[ProgressTask] =
    tasks.filter(_.isInstanceOf[ProgressTask]).map(_.asInstanceOf[ProgressTask])

  def progressConditionsFor(kernel: Kernel): List[AcceptCondition] =
    progressTasks.filter(_.kernel == kernel).map(_.condition)

  def finishTasks: List[FinishTask] =
    tasks.filter(_.isInstanceOf[FinishTask]).map(_.asInstanceOf[FinishTask])

  def tasksSummary: TasksSummary2 = {
    val progressedStartKernel = startKernelProgressTasks.map(_.kernel).distinct
    assert(progressedStartKernel.size <= 1)
    val addedKernels = progressTasks.map(_.kernel).toSet[Kernel].map { kernel =>
      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2)
    } ++ deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)
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
