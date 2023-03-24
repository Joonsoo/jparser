package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParser2ProtobufConverter, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.FileInputStream
import java.time.{Duration, Instant}
import scala.reflect.io.File
import scala.util.Using

class MilestoneGroupParserTest extends AnyFlatSpec {
  def testEquality(grammar: NGrammar, milestoneParser: MilestoneParser, mgroupParser: MilestoneGroupParser, example: String): Unit = {
    val inputs = Inputs.fromString(example)

    println(s"::: \"$example\"")

    val startTime1 = System.currentTimeMillis()
    val milestoneResult = milestoneParser.parseOrThrow(inputs)
    println(s"milestone parse time: ${System.currentTimeMillis() - startTime1} ms")
    val milestoneHistory = milestoneParser.kernelsHistory(milestoneResult)

    val startTime2 = System.currentTimeMillis()
    val mgroupResult = mgroupParser.parseOrThrow(inputs)
    println(s"mgroup parse time: ${System.currentTimeMillis() - startTime2} ms")
    val mgroupHistory = mgroupParser.kernelsHistory(mgroupResult)

    val milestoneParseForest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, milestoneHistory.map(Kernels)).reconstruct().get
    assert(milestoneParseForest.trees.size == 1)

    milestoneHistory.zip(mgroupHistory).zipWithIndex.foreach { case (pair, index) =>
      if (pair._1 != pair._2) {
        println(s"$index:")
        println(s"mile: ${pair._1.toList.sorted}")
        println(s"mgrp: ${pair._2.toList.sorted}")
        println(s"mile-mgrp: ${(pair._1 -- pair._2).toList.sorted}")
        println(s"mgrp-mile: ${(pair._2 -- pair._1).toList.sorted}")
      }
      assert(pair._1 == pair._2)
    }

    val mgroupParseForest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, mgroupHistory.map(Kernels)).reconstruct().get
    assert(mgroupParseForest.trees == milestoneParseForest.trees)
  }

  def generateParsers(grammar: NGrammar): (MilestoneParser, MilestoneGroupParser) = {
    println("milestone:::")
    val milestoneParserData = new MilestoneParserGen(grammar).parserData()
    val termActionsSize = milestoneParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"terms=${milestoneParserData.termActions.size}, termActions=$termActionsSize, edgeActions=${milestoneParserData.edgeProgressActions.size}")
    val milestoneParser = new MilestoneParser(milestoneParserData) //.setVerbose()

    println("mgroup:::")
    val mgroupParserData = new MilestoneGroupParserGen(grammar).parserData()
    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, prTipEdges=${mgroupParserData.tipEdgeProgressActions.size}, exTipEdges=${mgroupParserData.tipEdgeRequiredSymbols.size}, midEdges=${mgroupParserData.midEdgeProgressActions.size}")
    val mgroupParser = new MilestoneGroupParser(mgroupParserData) //.setVerbose()

    val milestoneParserProto = MilestoneParser2ProtobufConverter.toProto(milestoneParserData)
    val mgroupParserProto = MilestoneGroupParserDataProtobufConverter.toProto(mgroupParserData)
    println(s"milestone size=${milestoneParserProto.getSerializedSize}, mgroup size=${mgroupParserProto.getSerializedSize}, ratio=${mgroupParserProto.getSerializedSize.toDouble / milestoneParserProto.getSerializedSize}")
    (milestoneParser, mgroupParser)
  }

  "metalang3 grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/cdglang3.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = 'a'+")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "Abc = 'a'+ {str($0)}")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))
  }

  "bibix grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = c()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "ABC = cde.hello()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, new String(new FileInputStream("build.bbx").readAllBytes()))
  }

  "simple grammar" should "work" in {
    val grammar = new String(getClass.getResourceAsStream("/simple.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "aaa")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "a")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "a  ")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "aaa  ")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "aaa  a")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "a a")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "aaa  aaaa")
  }

  "bibix simple grammar" should "work" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix-simple.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A=c()")
  }

//  "j1 grammar" should "work" in {
//    val grammar = new String(getClass.getResourceAsStream("/j1-grammar.cdg").readAllBytes())
//
//    val analysis = MetaLanguage3.analyzeGrammar(grammar)
//
//    println("mgroup:::")
//    val start = Instant.now()
//    println(s"start: $start")
//    val mgroupParserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()
//    val end = Instant.now()
//    println(s"end: $end, ${Duration.between(start, end)}")
//    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
//    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, prTipEdges=${mgroupParserData.tipEdgeProgressActions.size}, exTipEdges=${mgroupParserData.tipEdgeRequiredSymbols.size}, midEdges=${mgroupParserData.midEdgeProgressActions.size}")
//    val mgroupParser = new MilestoneGroupParser(mgroupParserData) //.setVerbose()
//
//    val mgroupParserProto = MilestoneGroupParserDataProtobufConverter.toProto(mgroupParserData)
//    Using(File("j1-mg2-parserdata.pb").outputStream()) { output =>
//      mgroupParserProto.writeTo(output)
//    }
//
//    val input = Inputs.fromString("class Abc {}")
//    val parseResult = mgroupParser.parseOrThrow(input)
//    val kernelsHistory = mgroupParser.kernelsHistory(parseResult)
//
//    val parseForest = new ParseTreeConstructor2(ParseForestFunc)(analysis.ngrammar)(input, kernelsHistory.map(Kernels)).reconstruct().get
//    assert(parseForest.trees.size == 1)
//    println(parseForest.trees.head)
//  }
}
