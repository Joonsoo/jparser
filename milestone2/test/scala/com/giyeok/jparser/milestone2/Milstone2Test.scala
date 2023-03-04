package com.giyeok.jparser.milestone2

import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone2.MilestoneParser2ProtobufConverter.toProto
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.{Inputs, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.io._

class Milstone2Test extends AnyFlatSpec {
  it should "work" in {
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))

    //    val oldParserGen = new OldMilestoneParserGen(new NaiveParser(analysis.ngrammar))
    //    val oldParserData = oldParserGen.parserData()
    //    val w = new BufferedOutputStream(new FileOutputStream(new File("bibix2-oldparserdata.pb")))
    //    MilestoneParserProtobufConverter.convertMilestoneParserDataToProto(oldParserData).writeTo(w)
    //    w.close()

    val parserGen = new MilestoneParserGen(new NaiveParser2(analysis.ngrammar))
    val termActions = parserGen.termActionsFor(KernelTemplate(1, 0))
    println(termActions)
    val edgeAction = parserGen.edgeProgressActionBetween(KernelTemplate(229, 4), KernelTemplate(277, 2))
    println(edgeAction)
    //    ???

    val codeWriter = new BufferedWriter(new FileWriter(new File("BibixAst.kt")))
    val codegen = new KotlinOptCodeGen(analysis)
    codeWriter.write(codegen.generate("BibixAst", ""))
    codeWriter.close()

    println(s"interest: ${codegen.symbolsOfInterest.toList.sorted}")
    val diff = (analysis.ngrammar.nsymbols.keySet ++ analysis.ngrammar.nsequences.keySet) -- codegen.symbolsOfInterest
    println(s"diff: ${diff.size} ${diff.toList.size}")

    val parserData0 = parserGen.parserData()
    val parserData = parserData0.trimTasksSummariesForSymbols(codegen.symbolsOfInterest)
    println(s"milestones: ${parserData.termActions.size}, edges=${parserData.edgeProgressActions.keySet.size}")
    // println(parserData.edgeProgressActions.keySet)

    val writer = new BufferedOutputStream(new FileOutputStream(new File("bibix2-parserdata.pb")))
    toProto(parserData).writeTo(writer)
    writer.close()

    val parser = new MilestoneParser(parserData0)
      .setVerbose()
    // println(parser.initialCtx)

    val inputs = Inputs.fromString("a = this")
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
