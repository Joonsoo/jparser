package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.NGrammar.{NExcept, NJoin, NLookaheadSymbol, NTerminal}
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.AcceptCondition.Always
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2._
import com.giyeok.jparser.utils.TermGrouper
import com.giyeok.jparser.{Inputs, NGrammar}

import scala.annotation.tailrec

// mgroup과 milestone의 가장 큰 차이점은 mgroup은 accept condition을 위해 트래킹해야 하는 심볼들도 mgroup에 포함시킨다는 것.
// mgroup의 수가 너무 많아지는 경우도 생길 수 있을듯..
// 그래서 milestone 파서를 그냥 쓰게 되는 경우가 생길것 같음
class MGroupParserGen(val grammar: NGrammar) {
  val parsingTaskImpl: ParsingTaskImpl = new ParsingTaskImpl(grammar)
  val startKernelTmpl: KernelTemplate = KernelTemplate(grammar.startSymbol, 0)

  def deriveMGroup(nextGen: Int, milestones: Set[KernelTemplate]): (ParsingContext, MilestoneGroup) = {
    @tailrec
    def rec(cc: (ParsingContext, Set[KernelTemplate]), tasks: List[ParsingTask]): (ParsingContext, Set[KernelTemplate]) = tasks match {
      case List() => cc
      case head +: rest =>
        val (newCtx, newTasks) = parsingTaskImpl.process(nextGen, head, cc._1)
        val newShadowKernel: Set[KernelTemplate] = head match {
          case DeriveTask(kernel) =>
            grammar.symbolOf(kernel.symbolId) match {
              case NExcept(_, _, _, except) =>
                Set(KernelTemplate(except, 0))
              case NJoin(_, _, _, join) =>
                Set(KernelTemplate(join, 0))
              case lookahead: NLookaheadSymbol =>
                Set(KernelTemplate(lookahead.lookahead, 0))
              case _ =>
                // Longest는 그 자체가 조건에 붙기 때문에 shadowMilestone으로 추가할 필요 없음
                Set()
            }
          case _ => Set()
        }
        val x: Set[KernelTemplate] = cc._2 ++ newShadowKernel
        rec((newCtx, x), rest ++ newTasks)
    }

    val milestoneKernels = milestones.map(kt => Kernel(kt.symbolId, kt.pointer, 0, 0))
    val initialGraph = KernelGraph(milestoneKernels, Set())
    val deriveTasks = milestoneKernels.map(DeriveTask)

    val (ctx, shadowMilestones) =
      rec((ParsingContext(initialGraph, milestoneKernels.map(_ -> Always).toMap), Set()), deriveTasks.toList)
    (ctx, MilestoneGroup(milestones, shadowMilestones))
  }

  def acceptableTermGroups(graph: KernelGraph, gen: Int): List[Inputs.TermGroupDesc] = {
    val terminalNodes = graph.nodes.filter {
      case Kernel(symbolId, 0, `gen`, `gen`) =>
        grammar.symbolOf(symbolId) match {
          case _: NTerminal => true
          case _ => false
        }
      case _ => false
    }
    val terminals = terminalNodes.map(node => grammar.symbolOf(node.symbolId).asInstanceOf[NTerminal])
    TermGrouper.termGroupsOf(terminals.map(_.symbol))
  }

  case class ParsingAction0(prevReplacing: MilestoneGroup, appending: MilestoneGroup)

  def termActionsFrom(gen: Int, mgroup: MilestoneGroup, parsingCtx: ParsingContext): List[(Inputs.TermGroupDesc, ParsingAction0)] = {
    val termGroups = acceptableTermGroups(parsingCtx.graph, gen)
    termGroups.map { termGroup =>
      val terminalNodes = parsingCtx.graph.nodes.filter {
        case Kernel(symbolId, 0, `gen`, `gen`) =>
          grammar.symbolOf(symbolId) match {
            case NTerminal(_, terminal) => terminal.acceptTermGroup(termGroup)
            case _ => false
          }
        case _ => false
      }
      // TODO mgroup.milestones에서 도달 가능한지 확인 - mgroup.milestones에서 도달 가능한게 하나도 없으면
      val nextCtx = parsingTaskImpl.rec(2, terminalNodes.toList.map(ProgressTask(_, Always)), parsingCtx)
      println(nextCtx)
      // TODO
      termGroup -> ParsingAction0(MilestoneGroup(Set(), Set()), MilestoneGroup(Set(), Set()))
    }
  }
}
