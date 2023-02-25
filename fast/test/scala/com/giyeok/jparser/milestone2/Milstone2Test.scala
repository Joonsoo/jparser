package com.giyeok.jparser.milestone2

import com.giyeok.jparser.{Inputs, ParseForestFunc}
import com.giyeok.jparser.Inputs.CharsGroup
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.utils.Utils
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.matchers.should.Matchers.be

class Milstone2Test extends AnyFlatSpec {
  it should "work" in {
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))

    //    val naiveParser = new NaiveParser2(analysis.ngrammar)
    //    val ctx1 = naiveParser.parseStep(naiveParser.initialParsingHistoryContext, Inputs.Character('a')).right.get
    //    val ctx2 = naiveParser.parseStep(ctx1, Inputs.Character('b')).right.get
    //    val ctx3 = naiveParser.parseStep(ctx2, Inputs.Character('c')).right.get
    //    Utils.printDotGraph(analysis.ngrammar, ctx3.parsingContext)

    val gen = new MilestoneParserGen(new NaiveParser2(analysis.ngrammar))
    val edgeAction = gen.edgeProgressActionBetween(KernelTemplate(3, 4), KernelTemplate(72, 1))
    println(edgeAction)

    val parserData = MilestoneParserGen.generateMilestoneParserData(analysis.ngrammar)
    println(s"milestones: ${parserData.termActions.size}, edges=${parserData.edgeProgressActions.keySet.size}")
    println(parserData.edgeProgressActions.keySet)
    //    val parserData = MilestoneParserData(
    //      analysis.ngrammar,
    //      TasksSummary(List(), List()),
    //      Map(KernelTemplate(1, 0) -> List(
    //        CharsGroup(Set(), Set(), ('a' to 'z').toSet) -> ParsingAction(
    //          List(
    //            AppendingMilestone(KernelTemplate(14, 1), AlwaysTemplate),
    //            AppendingMilestone(KernelTemplate(3, 1),
    //              AndTemplate(Set(NotExistsTemplate(8, false, true), NotExistsTemplate(23, false, true)))),
    //            AppendingMilestone(KernelTemplate(3, 2),
    //              AndTemplate(Set(NotExistsTemplate(8, false, true), NotExistsTemplate(23, false, true)))),
    //          ),
    //          Map(
    //            KernelTemplate(8, 0) -> List(AppendingMilestone(KernelTemplate(14, 1), AlwaysTemplate)),
    //            KernelTemplate(23, 0) -> List(AppendingMilestone(KernelTemplate(27, 1), AlwaysTemplate)),
    //          ),
    //          Some(AlwaysTemplate),
    //          TasksSummary(List(), List()),
    //          KernelGraph(Set(), Set()),
    //        ))),
    //      Map(),
    //      Map(),
    //      Map(),
    //    )
    val parser = new MilestoneParser(parserData)
    //      .setVerbose()
    println(parser.initialCtx)

    val failed = parser.parse(Inputs.fromString("this = \"$def\""))
    failed should be(Symbol("left"))
    failed should not be (Symbol("right"))

    val inputs = Inputs.fromString("a = \"$def\"")
    val parsed = parser.parse(inputs)
      .getOrElse(throw new IllegalStateException(""))

    val history = parser.kernelsHistory(parsed)
      .map(_.toList.sortWith((k1, k2) =>
        if (k1.symbolId == k2.symbolId) k1.pointer < k2.pointer else k1.symbolId < k2.symbolId))
    println(history)
    println()

    val parseTree = new ParseTreeConstructor2(ParseForestFunc)(parserData.grammar)(
      inputs, history.map(ks => Kernels(ks.toSet))).reconstruct()
    println(parseTree)
    // result.getOrElse(throw new IllegalStateException("")).actionsHistory
  }
}
