package com.giyeok.jparser.mgroup

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.fast.{KernelTemplate, ParserGenBase}
import com.giyeok.jparser.metalang3a.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParsingContext.Kernel

import scala.annotation.tailrec

class MilestoneGroupParserGen(val parser: NaiveParser) extends ParserGenBase {
  case class Jobs(milestoneGroups: Set[MilestoneGroup], edges: Set[(MilestoneGroup, MilestoneGroup)])

  def termActionsFrom(start: MilestoneGroup): List[(TermGroupDesc, ParsingAction)] = {
    ???
  }

  @tailrec
  private def createParserData(jobs: Jobs, cc: MilestoneGroupParserData): MilestoneGroupParserData = {
    val newRemainingJobs = Jobs(milestoneGroups = Set(), edges = Set())
    if (newRemainingJobs.milestoneGroups.isEmpty && newRemainingJobs.edges.isEmpty) ???
    else createParserData(newRemainingJobs, ???)
  }

  def parserData(): MilestoneGroupParserData = {
    val start = KernelTemplate(parser.grammar.startSymbol, 0)
    val (_, ContWithTasks(tasks, _)) = startingCtxFrom(Kernel(start.symbolId, start.pointer, 1, 1))
    val startingMilestoneGroup0 = MilestoneGroup(Set(start))
    // startingMilestoneGroup은 startingMilestoneGroup0에서 derive된 노드들 중 milestone이 될 수 있는 것들을 모두 찾고,
    // start에서 각 milestone으로 가면서 만날 수 있는 모든 dependent들을 추가한 것
    val startingMilestoneGroup = startingMilestoneGroup0
    val result = createParserData(Jobs(Set(startingMilestoneGroup), Set()),
      MilestoneGroupParserData(parser.grammar, tasksSummaryFrom(tasks), Map(), Map(), Map(), Map()))
    result.copy(derivedGraph = ???)
  }
}

object MilestoneGroupParserGen {
  def generateMilestoneGroupParserData(grammar: NGrammar): MilestoneGroupParserData =
    new MilestoneGroupParserGen(new NaiveParser(grammar)).parserData()

  def main(args: Array[String]): Unit = {
    val grammar = MetaLanguage3.analyzeGrammar(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' (WS E (WS ',' WS E)*)? WS ']' {Arr(elems=$1{[$1]+$2} ?: [])}
        |WS = ' '*
        |""".stripMargin)
    val sourceText = "[]"
    val valuefySimulator = ValuefyExprSimulator(grammar)

    val milestoneParserData = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)
    val milestoneParser = new MilestoneParser(milestoneParserData)
    val milestoneParseForest = milestoneParser.parseAndReconstructToForest(sourceText).left.get
    val milestoneAst = milestoneParseForest.trees.map(valuefySimulator.valuefyStart)
    println(milestoneAst)

    val mgroupParserData = MilestoneGroupParserGen.generateMilestoneGroupParserData(grammar.ngrammar)
    val mgroupParser = new MilestoneGroupParser(mgroupParserData)
    val mgroupParseForest = mgroupParser.parseAndReconstructToForest(sourceText).left.get
    val mgroupAst = mgroupParseForest.trees.map(valuefySimulator.valuefyStart)
    println(mgroupAst)
  }
}
