package com.giyeok.jparser.milestone2

import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.{be, convertToAnyShouldWrapper}

class Milstone2Test extends AnyFlatSpec {
  it should "work" in {
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/simple-lookahead.cdg").readAllBytes()))

    //    val naiveParser = new NaiveParser2(analysis.ngrammar)
    //    val ctx1 = naiveParser.parseStep(naiveParser.initialParsingHistoryContext, Inputs.Character('a')).right.get
    //    val ctx2 = naiveParser.parseStep(ctx1, Inputs.Character('b')).right.get
    //    val ctx3 = naiveParser.parseStep(ctx2, Inputs.Character('c')).right.get
    //    Utils.printDotGraph(analysis.ngrammar, ctx3.parsingContext)

    //    val gen = new MilestoneParserGen(new NaiveParser2(analysis.ngrammar))
    //    val s75 = gen.termActionsFor(KernelTemplate(75, 1))
    //    val s72 = gen.termActionsFor(KernelTemplate(72, 2))
    //    println(s75)
    //    println(s72)
    //    val s1 = gen.edgeProgressActionBetween(KernelTemplate(3, 4), KernelTemplate(72, 1))
    //    val s2 = gen.edgeProgressActionBetween(KernelTemplate(72, 1), KernelTemplate(75, 1))
    //    println(s1)
    //    println(s2)

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
      .setVerbose()
    println(parser.initialCtx)

    val inputs = Inputs.fromString("abc124")
    val parsed = parser.parse(inputs) match {
      case Right(value) => value
      case Left(err) => throw new IllegalStateException(err.msg)
    }

    val history = parser.kernelsHistory(parsed)
      .map(_.toList.sortWith((k1, k2) =>
        if (k1.symbolId == k2.symbolId) k1.pointer < k2.pointer else k1.symbolId < k2.symbolId))

    val parseTree = new ParseTreeConstructor2(ParseForestFunc)(parserData.grammar)(
      inputs, history.map(ks => Kernels(ks.toSet))).reconstruct()
    println(parseTree)
    val valuefier = new ValuefyExprSimulator(analysis.ngrammar, analysis.grammar.startSymbol.name, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
    parseTree.get.trees.foreach { tree =>
      println(valuefier.valuefyStart(tree))
    }
    parseTree.get.trees.size should be(1)
  }
}
