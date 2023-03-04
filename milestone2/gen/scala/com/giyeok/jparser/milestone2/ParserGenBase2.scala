package com.giyeok.jparser.milestone2

import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.AcceptCondition.{AcceptCondition, Always}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel}
import com.giyeok.jparser.nparser2._
import com.giyeok.jparser.nparser2.opt.{MutableParsingContext, OptNaiveParser2}

import scala.annotation.tailrec
import scala.collection.mutable

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
    // 이 시점에는 filter out된 paths만 들고있어서 필터링이 잘 안되는데.. 컨디션으로 필터 아웃 되기 전의 paths들을 전달해주면 할 수 있을까?
    // progressConditionsFor(kernel).isEmpty && !ctx.graph.nodes.contains(kernel)
      false

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
      case AcceptCondition.NotExists(beginGen, endGen, symbolId) if beginGen == endGen && (beginGen == 0 || beginGen == 1) =>
        // lookahead except
        if (cannotExist(Kernel(symbolId, 0, beginGen, beginGen))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          LookaheadNotTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.NotExists(2, 2, symbolId) =>
        // lookahead except
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 AlwaysTemplate(NotExists이기 때문) 반환
          AlwaysTemplate
        } else {
          LookaheadNotTemplate(symbolId, fromNextGen = true)
        }
      case AcceptCondition.Exists(beginGen, endGen, symbolId) if beginGen == endGen && (beginGen == 0 || beginGen == 1) =>
        // lookahead except
        if (cannotExist(Kernel(symbolId, 0, beginGen, beginGen))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate
          NeverTemplate
        } else {
          LookaheadIsTemplate(symbolId, fromNextGen = false)
        }
      case AcceptCondition.Exists(2, 2, symbolId) =>
        // lookahead is
        if (cannotExist(Kernel(symbolId, 0, 2, 2))) {
          // ctx를 보고 이미 가능성이 없는 심볼인 경우 NeverTemplate 반환
          NeverTemplate
        } else {
          LookaheadIsTemplate(symbolId, fromNextGen = true)
        }
    }
  }

  def tasksSummary: TasksSummary2 = {
    val progressedStartKernel = startKernelProgressTasks.map(_.kernel).distinct
    assert(progressedStartKernel.size <= 1)
    val addedByProgresses = progressTasks.groupBy(_.kernel).view.map { pair =>
      val kernel = pair._1
      val condition = AcceptConditionTemplate.disjunct(pair._2.map(_.condition).map(conditionToTemplate).toSet)
      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2) -> condition
    }.toMap.groupMap(_._2)(_._1).view.mapValues(_.toSet).toMap
    val addedByOthers = (deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)).toSet
    val addedKernels = addedByProgresses + (AlwaysTemplate -> (addedByProgresses.getOrElse(AlwaysTemplate, Set()) ++ addedByOthers))
    //    val addedKernels = progressTasks.map(_.kernel).toSet[Kernel].map { kernel =>
    //      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2)
    //    } ++ deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)
    TasksSummary2(
      addedKernels = addedKernels,
      progressedKernels = progressTasks.map(_.kernel).toSet,
      progressedStartKernel = progressedStartKernel.headOption)
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
    val mutStartKernelProgressTasks = mutable.Buffer[ProgressTask]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case (barrierTask@ProgressTask(`barrierNode`, _)) +: rest =>
          mutStartKernelProgressTasks += barrierTask
          recursion(rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)
    CtxWithTasks(mutCtx.toParsingContext, mutTasks.toList, mutStartKernelProgressTasks.toList)
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
}
