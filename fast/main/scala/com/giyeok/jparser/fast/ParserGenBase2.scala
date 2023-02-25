package com.giyeok.jparser.fast

import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.{DeriveTask, FinishTask, KernelGraph, NaiveParser2, ParsingContext, ParsingTask, ProgressTask}

case class CtxWithTasks(ctx: ParsingContext, tasks: List[ParsingTask], startKernelProgressTasks: List[ProgressTask]) {
  def deriveTasks: List[DeriveTask] =
    tasks.filter(_.isInstanceOf[DeriveTask]).map(_.asInstanceOf[DeriveTask])

  def progressTasks: List[ProgressTask] =
    tasks.filter(_.isInstanceOf[ProgressTask]).map(_.asInstanceOf[ProgressTask])

  def finishTasks: List[FinishTask] =
    tasks.filter(_.isInstanceOf[FinishTask]).map(_.asInstanceOf[FinishTask])

  def tasksSummary: TasksSummary2 = {
    val progressedStartKernel = startKernelProgressTasks.map(_.kernel).distinct
    assert(progressedStartKernel.size <= 1)
    TasksSummary2(
      progressedKernels = progressTasks.map(_.kernel).toSet,
      finishedKernels = finishTasks.map(_.kernel).toSet,
      progressedStartKernel = progressedStartKernel.headOption)
  }
}

case class TasksSummary2(
  progressedKernels: Set[Kernel],
  finishedKernels: Set[Kernel],
  progressedStartKernel: Option[Kernel],
)

case class ParserGenBase2(parser: NaiveParser2) {
  def runTasksWithProgressBarrier(nextGen: Int, tasks: List[ParsingTask], barrierNode: Kernel, cc: CtxWithTasks): CtxWithTasks = tasks match {
    case task +: rest =>
      if (task.isInstanceOf[ProgressTask] && barrierNode == task.asInstanceOf[ProgressTask].kernel) {
        val ncc = cc.copy(startKernelProgressTasks = task.asInstanceOf[ProgressTask] +: cc.startKernelProgressTasks)
        runTasksWithProgressBarrier(nextGen, rest, barrierNode, ncc)
      } else {
        val (nextCtx, newTasks) = parser.process(nextGen, task, cc.ctx)
        val ncc = CtxWithTasks(nextCtx, cc.tasks ++ newTasks, cc.startKernelProgressTasks)
        runTasksWithProgressBarrier(nextGen, newTasks ++: rest, barrierNode, ncc)
      }
    case List() => cc
  }

  def startingCtxFrom(start: KernelTemplate, baseGen: Int): (Kernel, CtxWithTasks) = {
    val startKernel = Kernel(start.symbolId, start.pointer, baseGen, baseGen + 1)
    val startGraph = KernelGraph(Set(startKernel), Set())
    val startCtx = ParsingContext(startGraph, Map(startKernel -> Always))

    val deriveTask = DeriveTask(startKernel)

    val ctx = runTasksWithProgressBarrier(
      startKernel.endGen,
      List(deriveTask),
      startKernel,
      CtxWithTasks(startCtx, List(deriveTask), List()))
    (startKernel, ctx)
  }
}
