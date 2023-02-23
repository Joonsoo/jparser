package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.nparser.Kernel
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}

// naive parser 1과 2의 차이점은 accept condition이 그래프 안에 있냐 밖에 있냐의 차이
// milestone parser 1과 2의 차이점도 비슷
class MilestoneParserGen(val parser: NaiveParser2) {
  def parsingActionFrom(graph: KernelGraph, start: Kernel, toProgress: List[Kernel], currGen: Int): ParsingAction = {
    // graph에서 start로부터 reachable한 node들 중 milestone들을 찾아서 appendingMilestone
    // appendingMilestone과
    ???
  }

  def termActionsFrom(termKernel: KernelTemplate): List[(TermGroupDesc, ParsingAction)] = {
    ???
  }

  def edgeProgressActionsBetween(startKernel: KernelTemplate, endKernel: KernelTemplate): ParsingAction = {
    ???
  }

  case class Jobs(milestones: Set[KernelTemplate], edges: Set[(KernelTemplate, KernelTemplate)])

  private def createParserData(jobs: Jobs, cc: MilestoneParserData): MilestoneParserData = {
    ???
  }

  def parserData(): MilestoneParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    ???
  }
}

object MilestoneParserGen {
  def generateMilestoneParserData(grammar: NGrammar): MilestoneParserData =
    new MilestoneParserGen(new NaiveParser2(grammar)).parserData()
}
