package com.giyeok.jparser.milestone2

import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2._
import com.giyeok.jparser.nparser2.opt.{MutableParsingContext, OptNaiveParser2}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class CtxWithTasks(
  ctx: ParsingContext,
  tasks: List[ParsingTask],
  startKernelProgressConditions: Map[Kernel, AcceptCondition],
  progressConditions: Map[Kernel, AcceptCondition]
) {
  def deriveTasks: List[DeriveTask] =
    tasks.filter(_.isInstanceOf[DeriveTask]).map(_.asInstanceOf[DeriveTask])

  def progressTasks: List[ProgressTask] =
    tasks.filter(_.isInstanceOf[ProgressTask]).map(_.asInstanceOf[ProgressTask])

  def progressConditionsFor(kernel: Kernel): List[AcceptCondition] =
    progressTasks.filter(_.kernel == kernel).map(_.condition)

  def finishTasks: List[FinishTask] =
    tasks.filter(_.isInstanceOf[FinishTask]).map(_.asInstanceOf[FinishTask])

  object NotExistsMatch {
    def unapply(condition: AcceptCondition.NotExists): Option[((Int, Int), Int)] =
      Some((condition.beginGen, condition.endGen), condition.symbolId)
  }

  object ExistsMatch {
    def unapply(condition: AcceptCondition.Exists): Option[((Int, Int), Int)] =
      Some((condition.beginGen, condition.endGen), condition.symbolId)
  }

  object UnlessMatch {
    def unapply(condition: AcceptCondition.Unless): Option[((Int, Int), Int)] =
      Some((condition.beginGen, condition.endGen), condition.symbolId)
  }

  object OnlyIfMatch {
    def unapply(condition: AcceptCondition.OnlyIf): Option[((Int, Int), Int)] =
      Some((condition.beginGen, condition.endGen), condition.symbolId)
  }

  def conditionToTemplateForTaskSummary(condition: AcceptCondition): AcceptConditionTemplate = {
    condition match {
      case AcceptCondition.Always => AlwaysTemplate
      case AcceptCondition.Never => NeverTemplate
      case AcceptCondition.And(conditions) =>
        AcceptConditionTemplate.conjunct(conditions.map(conditionToTemplateForTaskSummary))
      case AcceptCondition.Or(conditions) =>
        AcceptConditionTemplate.disjunct(conditions.map(conditionToTemplateForTaskSummary))
      /* 여기서부터 progress 이전에 만들어진 컨디션이 살아남은 경우 */
      case NotExistsMatch((0, 1) | (1, 2), symbolId) =>
        LongestTemplate(symbolId, beginFromNextGen = false)
      case ExistsMatch((0, 0) | (1, 1), symbolId) =>
        LookaheadIsTemplate(symbolId, fromNextGen = false)
      case NotExistsMatch((0, 0) | (1, 1), symbolId) =>
        LookaheadNotTemplate(symbolId, fromNextGen = false)
      //      case UnlessMatch((0, 0) | (1, 1), symbolId) =>
      //        UnlessTemplate(symbolId)
      //        ???
      //      case OnlyIfMatch((0, 0) | (1, 1), symbolId) =>
      //        OnlyIfTemplate(symbolId)
      //        ???
      /* 여기서부터 progress task에 의해 새로 추가될 수 있는 컨디션들 */
      case NotExistsMatch((0, 3) | (1, 3), symbolId) =>
        // longest
        // longest는 일단 다음 gen부터 체크되므로 가능성이 없어질 가능성(반환값이 달라지는 경우)은 없음
        LongestTemplate(symbolId, beginFromNextGen = false)
      case AcceptCondition.NotExists(2, 3, symbolId) =>
        // longest with nullable symbol
        LongestTemplate(symbolId, beginFromNextGen = true)
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        LookaheadIsTemplate(symbolId, fromNextGen = true)
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead not
        LookaheadNotTemplate(symbolId, fromNextGen = true)
      case UnlessMatch((0, 2) | (1, 2), symbolId) =>
        UnlessTemplate(symbolId, fromNextGen = false)
      case OnlyIfMatch((0, 2) | (1, 2), symbolId) =>
        OnlyIfTemplate(symbolId, fromNextGen = false)
      case UnlessMatch((2, 2) | (2, 2), symbolId) =>
        UnlessTemplate(symbolId, fromNextGen = true)
      case OnlyIfMatch((2, 2) | (2, 2), symbolId) =>
        OnlyIfTemplate(symbolId, fromNextGen = true)
    }
  }

  def tasksSummary(currGen: Int): TasksSummary2 = {
    val progressedStartKernel = startKernelProgressConditions.keySet
    assert(progressedStartKernel.size <= 1)
    val addedByProgresses: Map[AcceptConditionTemplate, Set[Kernel]] = progressConditions.toList.groupBy(_._2).view.map { pair =>
      val (condition, kernels) = pair
      val addedKernels = kernels.map { case (kernel, _) => Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2) }

      //      val baseCondition = conditionToTemplateForTaskSummary(currGen, ctx.acceptConditions.getOrElse(kernel, Always))
      //      val progressConditions = progressTasks.map(_.condition).map(conditionToTemplateForTaskSummary(currGen, _))
      //      val addedCondition = AcceptConditionTemplate.disjunct(progressConditions.toSet)
      //      val condition = AcceptConditionTemplate.conjunct(Set(baseCondition, addedCondition))
      //      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2) -> condition

      conditionToTemplateForTaskSummary(condition) -> addedKernels.toSet
    }.toMap
    val addedByOthers = (deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)).toSet
    val addedKernels = addedByProgresses + (AlwaysTemplate -> (addedByProgresses.getOrElse(AlwaysTemplate, Set()) ++ addedByOthers))
    //    val addedKernels = progressTasks.map(_.kernel).toSet[Kernel].map { kernel =>
    //      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2)
    //    } ++ deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)
    TasksSummary2(
      addedKernels = addedKernels,
      progressedKernels = progressTasks.map(_.kernel).toSet,
      // progressedStartKernel = progressedStartKernel.headOption,
    )
  }
}

class ParserGenBase2(private val parser: OptNaiveParser2) {
  //  def runTasksWithProgressBarrierByNaive(nextGen: Int, tasks: List[ParsingTask], barrierNode: Kernel, cc: CtxWithTasks): CtxWithTasks = tasks match {
  //    case (barrierTask@ProgressTask(`barrierNode`, _)) +: rest =>
  //      val ncc = cc.copy(startKernelProgressTasks = barrierTask +: cc.startKernelProgressTasks)
  //      runTasksWithProgressBarrierByNaive(nextGen, rest, barrierNode, ncc)
  //    case task +: rest =>
  //      val (nextCtx, newTasks) = new nparser2.NaiveParser2(parser.grammar).process(nextGen, task, cc.ctx)
  //      val ncc = CtxWithTasks(nextCtx, task +: cc.tasks, cc.startKernelProgressTasks)
  //      runTasksWithProgressBarrierByNaive(nextGen, rest ++ newTasks, barrierNode, ncc)
  //    case List() => cc
  //  }

  def runTasksWithProgressBarrier(
    nextGen: Int,
    tasks: List[ParsingTask],
    barrierNode: Kernel,
    ctx: ParsingContext
  ): CtxWithTasks = {
    val mutCtx = MutableParsingContext(ctx)
    val mutTasks = mutable.Buffer[ParsingTask]()
    val mutStartKernelProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()
    val mutProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case ProgressTask(`barrierNode`, condition) +: rest =>
          mutStartKernelProgressConds.getOrElseUpdate(barrierNode, ListBuffer()) += condition
          recursion(rest)
        case (task@ProgressTask(kernel, condition)) +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          mutProgressConds.getOrElseUpdate(kernel, ListBuffer()) += condition
          recursion(newTasks ++ rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)
    CtxWithTasks(
      mutCtx.toParsingContext,
      mutTasks.toList,
      mutStartKernelProgressConds.toMap.view.mapValues(conds => AcceptCondition.disjunct(conds.toList: _*)).toMap,
      mutProgressConds.toMap.view.mapValues(conds => AcceptCondition.disjunct(conds.toList: _*)).toMap)
  }

  def runTasksWithProgressBarriers(
    nextGen: Int,
    tasks: List[ParsingTask],
    barrierNodes: Set[Kernel],
    ctx: ParsingContext
  ): CtxWithTasks = {
    val mutCtx = MutableParsingContext(ctx)
    val mutTasks = mutable.Buffer[ParsingTask]()
    val mutStartKernelProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()
    val mutProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case ProgressTask(progressNode, condition) +: rest if barrierNodes.contains(progressNode) =>
          mutStartKernelProgressConds.getOrElseUpdate(progressNode, ListBuffer()) += condition
          recursion(rest)
        case (task@ProgressTask(kernel, condition)) +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          mutProgressConds.getOrElseUpdate(kernel, ListBuffer()) += condition
          recursion(newTasks ++ rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)
    CtxWithTasks(
      mutCtx.toParsingContext,
      mutTasks.toList,
      mutStartKernelProgressConds.toMap.view.mapValues(conds => AcceptCondition.disjunct(conds.toList: _*)).toMap,
      mutProgressConds.toMap.view.mapValues(conds => AcceptCondition.disjunct(conds.toList: _*)).toMap)
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
      startCtx)
    (startKernel, ctx)
  }

  def startingCtxFrom(starts: Set[KernelTemplate], baseGen: Int): (Map[KernelTemplate, Kernel], CtxWithTasks) = {
    val startKernelsMap = starts.map(start =>
      start -> Kernel(start.symbolId, start.pointer, baseGen, baseGen + 1)).toMap
    val startKernels = startKernelsMap.values.toSet
    val startGraph = KernelGraph(startKernels, Set())
    val startCtx = ParsingContext(startGraph, startKernels.map(_ -> Always).toMap)

    val deriveTasks = startKernels.map(DeriveTask)

    val ctx = runTasksWithProgressBarriers(
      baseGen + 1,
      deriveTasks.toList,
      startKernels,
      startCtx
    )
    (startKernelsMap, ctx)
  }
}
