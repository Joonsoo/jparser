package com.giyeok.jparser.milestone2

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Inputs.CharsGroup
import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.nparser2.utils.Utils
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}
import org.scalatest.flatspec.AnyFlatSpec

class Milstone2Test extends AnyFlatSpec {
  it should "work" in {
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))

//    val naiveParser = new NaiveParser2(analysis.ngrammar)
//    val ctx1 = naiveParser.parseStep(naiveParser.initialParsingHistoryContext, Inputs.Character('a')).right.get
//    val ctx2 = naiveParser.parseStep(ctx1, Inputs.Character('b')).right.get
//    val ctx3 = naiveParser.parseStep(ctx2, Inputs.Character('c')).right.get
//    Utils.printDotGraph(analysis.ngrammar, ctx3.parsingContext)

    val parserData = MilestoneParserGen.generateMilestoneParserData(analysis.ngrammar)
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
    println(parser.initialCtx)
    //    val ctx1 = parser.parseStep(parser.initialCtx, 1, Inputs.Character('s'))
    //    ctx1.toOption.get.paths.foreach { path =>
    //      println(path.prettyString)
    //    }
  }
}
