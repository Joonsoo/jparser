package com.giyeok.jparser.fast

import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.{DeriveTask, FinishTask, KernelGraph, NaiveParser2, ParsingContext, ParsingTask, ProgressTask}

case class CtxWithTasks(ctx: ParsingContext, tasks: List[ParsingTask]) {
  def deriveTasks: List[DeriveTask] =
    tasks.filter(_.isInstanceOf[DeriveTask]).map(_.asInstanceOf[DeriveTask])

  def progressTasks: List[ProgressTask] =
    tasks.filter(_.isInstanceOf[ProgressTask]).map(_.asInstanceOf[ProgressTask])

  def finishTasks: List[FinishTask] =
    tasks.filter(_.isInstanceOf[FinishTask]).map(_.asInstanceOf[FinishTask])

  def tasksSummary: TasksSummary2 = {
    // TODO
    TasksSummary2(List(), List())
  }
}

case class TasksSummary2(
  progressedKernels: List[Kernel],
  finishedKernels: List[Kernel],
)

case class ParserGenBase2(val parser: NaiveParser2) {
  def runTasksWithProgressBarrier(nextGen: Int, tasks: List[ParsingTask], barrierNode: Kernel, cc: CtxWithTasks): CtxWithTasks =
    runTasksWithProgressBarriers(nextGen, tasks, Set(barrierNode), cc)

  def runTasksWithProgressBarriers(nextGen: Int, tasks: List[ParsingTask], barrierNodes: Set[Kernel], cc: CtxWithTasks): CtxWithTasks = tasks match {
    case task +: rest =>
      if (task.isInstanceOf[ProgressTask] && barrierNodes.contains(task.asInstanceOf[ProgressTask].kernel)) {
        runTasksWithProgressBarriers(nextGen, rest, barrierNodes, cc)
      } else {
        val (ncc, newTasks) = parser.process(nextGen, task, cc.ctx)
        runTasksWithProgressBarriers(nextGen, newTasks ++: rest, barrierNodes, CtxWithTasks(ncc, cc.tasks ++ newTasks))
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
      CtxWithTasks(startCtx, List(deriveTask)))
    (startKernel, ctx)
  }
}
