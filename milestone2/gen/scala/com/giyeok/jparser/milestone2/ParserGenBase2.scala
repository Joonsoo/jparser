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
  newNodes: Map[Kernel, AcceptCondition]
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
    val addedKernels: Map[AcceptConditionTemplate, Set[Kernel]] = newNodes.toList.groupBy(_._2).view.map { pair =>
      val (condition, kernels) = pair

      //      val baseCondition = conditionToTemplateForTaskSummary(currGen, ctx.acceptConditions.getOrElse(kernel, Always))
      //      val progressConditions = progressTasks.map(_.condition).map(conditionToTemplateForTaskSummary(currGen, _))
      //      val addedCondition = AcceptConditionTemplate.disjunct(progressConditions.toSet)
      //      val condition = AcceptConditionTemplate.conjunct(Set(baseCondition, addedCondition))
      //      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2) -> condition

      conditionToTemplateForTaskSummary(condition) -> kernels.map(_._1).toSet
    }.toMap
    //    val addedByOthers = (deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)).toSet
    //    val addedKernels = addedByProgresses + (AlwaysTemplate -> (addedByProgresses.getOrElse(AlwaysTemplate, Set()) ++ addedByOthers))
    //    val addedKernels = progressTasks.map(_.kernel).toSet[Kernel].map { kernel =>
    //      Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, 2)
    //    } ++ deriveTasks.map(_.kernel) ++ finishTasks.map(_.kernel).filter(_.pointer == 0)
    TasksSummary2(
      addedKernels = addedKernels,
      progressedKernels = Set() // progressTasks.map(_.kernel).toSet,
      // progressedStartKernel = progressedStartKernel.headOption,
    )
  }

  // 시작 커널이 같은 세대에서 zero-width로 progress된 경우(nullable start symbol),
  // 그 progressed kernel도 summary에 포함시킨다. 입력이 시작되기 전(gen 0)에
  // start symbol이 매치되는 경우 — 빈 입력 수용 등 — 의 acceptance에 필요하다.
  def tasksSummaryWithStartProgress(currGen: Int, startKernel: Kernel): TasksSummary2 = {
    val base = tasksSummary(currGen)
    startKernelProgressConditions.get(startKernel) match {
      case Some(condition) =>
        val template = conditionToTemplateForTaskSummary(condition)
        val progressed = Kernel(startKernel.symbolId, startKernel.pointer + 1, startKernel.beginGen, startKernel.endGen)
        base.copy(addedKernels =
          base.addedKernels + (template -> (base.addedKernels.getOrElse(template, Set()) + progressed)))
      case None => base
    }
  }
}

class ParserGenBase2(private val parser: OptNaiveParser2) {
  // 시뮬레이션의 각 단계가 끝난 뒤, 현재 세대(gen)에서 zero-width로 결정되는
  // 조건들(beginGen == endGen == gen)만 선택적으로 진화시킨다.
  //
  // nullable한 conditional 심볼의 body가 같은 세대에서 zero-width로 progress되면
  // endGen이 현재 세대인 조건(OnlyIf/Unless/Exists/NotExists)이 만들어지는데,
  // 이들의 피연산자 매치는 zero-width라 입력과 무관하게 정적이므로 시뮬레이션
  // 컨텍스트에서 안전하게 해소할 수 있고, 해소하지 않으면 template 변환이 처리할
  // 수 없는 모양(OnlyIf/Unless with beginGen==endGen)이 살아남는다.
  // (NaiveParser2.initialParsingHistoryContext의 gen 0 평가와 같은 원리)
  //
  // 주의: 그 외의 조건들(이전 세대에서 시작했거나 여러 세대에 걸친 조건)은
  // 진화시키지 않는다. 특히 edge action 시뮬레이션의 컨텍스트는 fake end 같은
  // 합성 구조를 포함하므로, 거기서 전체 진화를 적용하면 지역 정보가 불완전한
  // 조건이 잘못 해소된다 (예: 여러 세대에 걸친 lookahead 대상).
  def evolveZeroWidthCondition(gen: Int, ctx: ParsingContext, condition: AcceptCondition): AcceptCondition =
    condition match {
      case AcceptCondition.Always | AcceptCondition.Never => condition
      case AcceptCondition.And(conds) =>
        AcceptCondition.conjunct(conds.toSeq.map(evolveZeroWidthCondition(gen, ctx, _)): _*)
      case AcceptCondition.Or(conds) =>
        AcceptCondition.disjunct(conds.toSeq.map(evolveZeroWidthCondition(gen, ctx, _)): _*)
      case AcceptCondition.Unless(b, e, symbolId) if b == gen && e == gen =>
        ctx.acceptConditions.get(Kernel(symbolId, 1, gen, gen)) match {
          case Some(matched) => evolveZeroWidthCondition(gen, ctx, matched.neg)
          case None => AcceptCondition.Always
        }
      case AcceptCondition.OnlyIf(b, e, symbolId) if b == gen && e == gen =>
        ctx.acceptConditions.get(Kernel(symbolId, 1, gen, gen)) match {
          case Some(matched) => evolveZeroWidthCondition(gen, ctx, matched)
          case None => AcceptCondition.Never
        }
      case AcceptCondition.Exists(b, e, symbolId) if b == gen && e == gen =>
        ctx.acceptConditions.get(Kernel(symbolId, 1, gen, gen)) match {
          case Some(matched) =>
            val zeroWidth = evolveZeroWidthCondition(gen, ctx, matched)
            if (ctx.graph.nodes.contains(Kernel(symbolId, 0, gen, gen)))
              AcceptCondition.disjunct(zeroWidth, condition)
            else zeroWidth
          case None =>
            if (ctx.graph.nodes.contains(Kernel(symbolId, 0, gen, gen))) condition
            else AcceptCondition.Never
        }
      case AcceptCondition.NotExists(b, e, symbolId) if b == gen && e == gen =>
        ctx.acceptConditions.get(Kernel(symbolId, 1, gen, gen)) match {
          case Some(matched) =>
            val zeroWidth = evolveZeroWidthCondition(gen, ctx, matched.neg)
            if (ctx.graph.nodes.contains(Kernel(symbolId, 0, gen, gen)))
              AcceptCondition.conjunct(zeroWidth, condition)
            else zeroWidth
          case None =>
            if (ctx.graph.nodes.contains(Kernel(symbolId, 0, gen, gen))) condition
            else AcceptCondition.Always
        }
      case _ => condition
    }

  def evolveParsingContext(gen: Int, ctx: ParsingContext): ParsingContext = {
    val evolvedConds = ctx.acceptConditions.view.mapValues(evolveZeroWidthCondition(gen, ctx, _)).toMap
    val dropped = evolvedConds.filter(_._2 == AcceptCondition.Never).keySet
    ParsingContext(ctx.graph.removeNodes(dropped), evolvedConds -- dropped)
  }

  def evolveCtxWithTasks(gen: Int, ctxWithTasks: CtxWithTasks): CtxWithTasks = {
    val ctx = ctxWithTasks.ctx
    val evolvedConds = ctx.acceptConditions.view.mapValues(evolveZeroWidthCondition(gen, ctx, _)).toMap
    val dropped = evolvedConds.filter(_._2 == AcceptCondition.Never).keySet
    val newCtx = ParsingContext(ctx.graph.removeNodes(dropped), evolvedConds -- dropped)
    CtxWithTasks(
      newCtx,
      ctxWithTasks.tasks,
      ctxWithTasks.startKernelProgressConditions.view.mapValues(evolveZeroWidthCondition(gen, ctx, _)).toMap,
      ctxWithTasks.newNodes.map { case (kernel, condition) =>
        kernel -> evolvedConds.getOrElse(kernel, evolveZeroWidthCondition(gen, ctx, condition))
      })
  }

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
    val mutTasks = mutable.ListBuffer[ParsingTask]()
    val mutStartKernelProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case ProgressTask(`barrierNode`, condition) +: rest =>
          mutStartKernelProgressConds.getOrElseUpdate(barrierNode, ListBuffer()) += condition
          recursion(rest)
        case (task@ProgressTask(kernel, condition)) +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)

    ctxWithTasksFrom(nextGen, mutCtx.toParsingContext, mutTasks.toList, mutStartKernelProgressConds.toMap)
  }

  def runTasksWithProgressBarriers(
    nextGen: Int,
    tasks: List[ParsingTask],
    barrierNodes: Set[Kernel],
    ctx: ParsingContext
  ): CtxWithTasks = {
    val mutCtx = MutableParsingContext(ctx)
    val mutTasks = mutable.ListBuffer[ParsingTask]()
    val mutStartKernelProgressConds = mutable.Map[Kernel, mutable.ListBuffer[AcceptCondition]]()

    @tailrec def recursion(tasks: List[ParsingTask]): Unit =
      tasks match {
        case ProgressTask(progressNode, condition) +: rest if barrierNodes.contains(progressNode) =>
          mutStartKernelProgressConds.getOrElseUpdate(progressNode, ListBuffer()) += condition
          recursion(rest)
        case (task@ProgressTask(kernel, condition)) +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case task +: rest =>
          mutTasks += task
          val newTasks = parser.process(nextGen, task, mutCtx)
          recursion(newTasks ++ rest)
        case List() =>
      }

    recursion(tasks)

    ctxWithTasksFrom(nextGen, mutCtx.toParsingContext, mutTasks.toList, mutStartKernelProgressConds.toMap)
  }

  def ctxWithTasksFrom(
    nextGen: Int,
    ctx: ParsingContext,
    tasks: List[ParsingTask],
    startKernelProgressConds: Map[Kernel, ListBuffer[AcceptCondition]]): CtxWithTasks = {
    val newKernels: Set[Kernel] = tasks.map {
      case DeriveTask(kernel) => Set(kernel)
      case FinishTask(kernel) => Set(kernel)
      case ProgressTask(kernel, _) =>
        val nextKernel = Kernel(kernel.symbolId, kernel.pointer + 1, kernel.beginGen, nextGen)
        // 일반적으로는 progress task에 의해 생긴 노드는 Derive나 Finish로 가기 때문에 nextKernel을 여기서 처리할 필요는 없는데,
        // start kernel들에 대해서는 derive가 실행되지 않기 때문에 nextKernel도 추가해줘야 할 듯
        Set(kernel, nextKernel)
    }.flatten.toSet

    CtxWithTasks(
      ctx,
      tasks,
      startKernelProgressConds.view.mapValues(conds => AcceptCondition.disjunct((conds.toList): _*)).toMap,
      newKernels.map(kernel => kernel -> ctx.acceptConditions(kernel)).toMap)
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
    (startKernel, evolveCtxWithTasks(baseGen + 1, ctx))
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
    (startKernelsMap, evolveCtxWithTasks(baseGen + 1, ctx))
  }
}
