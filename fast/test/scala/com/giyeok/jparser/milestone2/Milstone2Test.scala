package com.giyeok.jparser.milestone2

import com.giyeok.jparser.fast.KernelTemplate
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.NaiveParser2
import com.giyeok.jparser.proto.MilestoneParser2ProtobufConverter.toProto
import com.giyeok.jparser.{Inputs, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.{be, convertToAnyShouldWrapper}

import java.io._

class Milstone2Test extends AnyFlatSpec {
  it should "work" in {
    val analysis = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))

    val parserGen = new MilestoneParserGen(new NaiveParser2(analysis.ngrammar))
    val parserData = parserGen.parserData()
    println(s"milestones: ${parserData.termActions.size}, edges=${parserData.edgeProgressActions.keySet.size}")
    // println(parserData.edgeProgressActions.keySet)

    val codeWriter = new BufferedWriter(new FileWriter(new File("BibixAst.kt")))
    val codegen = new KotlinOptCodeGen(analysis)
    codeWriter.write(codegen.generate())
    codeWriter.close()

    val writer = new BufferedOutputStream(new FileOutputStream(new File("bibix2-parserdata.pb")))
    toProto(parserData).writeTo(writer)
    writer.close()

    val parser = new MilestoneParser(parserData)
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
