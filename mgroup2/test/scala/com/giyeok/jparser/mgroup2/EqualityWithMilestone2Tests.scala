package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.examples.metalang3.Catalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{Milestone, MilestoneParser, MilestoneParser2ProtobufConverter, MilestoneParserGen, MilestonePath, ParsingContext => MilestoneParsingContext}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.FileInputStream
import java.time.{Duration, Instant}

// mgroup2 파서가 milestone2 파서와 동일하게 동작하는지 테스트
class EqualityWithMilestone2Tests extends AnyFlatSpec {
  def assertEqualCtx(milestoneCtx: MilestoneParsingContext, mgroupCtx: ParsingContext, mgroupData: MilestoneGroupParserData): Unit = {
    val flattenMgroupPaths = mgroupCtx.paths.flatMap { path =>
      val group = mgroupData.milestoneGroups(path.tip.groupId)
      group.map { kernelTemplate =>
        new MilestonePath(path.first, Milestone(kernelTemplate, path.tip.gen) +: path.path, path.acceptCondition)
      }
    }
    if (mgroupCtx.paths.size > 100) {
      println(s"????? ${mgroupCtx.paths.size}")
    }
    // TODO 이렇게 비교하면 같은 컨텍스트인데도 다르다고 판정되는 경우가 없을까?
    assert(milestoneCtx.paths.size == flattenMgroupPaths.size)
    assert(milestoneCtx.paths.toSet == flattenMgroupPaths.toSet)
  }

  def testEquality(grammar: NGrammar, milestoneParser: MilestoneParser, mgroupParser: MilestoneGroupParser, example: String): Unit = {
    val inputs = Inputs.fromString(example)

    println(s"::: \"$example\"")

    var milestoneCtx = milestoneParser.initialCtx
    var mgroupCtx = mgroupParser.initialCtx
    assertEqualCtx(milestoneCtx, mgroupCtx, mgroupParser.parserData)
    inputs.foreach { input =>
      milestoneCtx = milestoneParser.parseStep(milestoneCtx, input).getOrElse(throw new IllegalStateException())
      mgroupCtx = mgroupParser.parseStep(mgroupCtx, input).getOrElse(throw new IllegalStateException())
      assertEqualCtx(milestoneCtx, mgroupCtx, mgroupParser.parserData)
    }

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
    val start1 = Instant.now()
    println(s"milestone::: start=$start1")
    val milestoneParserData = new MilestoneParserGen(grammar).parserData()
    val termActionsSize = milestoneParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"terms=${milestoneParserData.termActions.size}, termActions=$termActionsSize, edgeActions=${milestoneParserData.edgeProgressActions.size}")
    println(s"elapsed: ${Duration.between(start1, Instant.now())}")
    val milestoneParser = new MilestoneParser(milestoneParserData) //.setVerbose()

    val start2 = Instant.now()
    println(s"mgroup::: start=$start2")
    val mgroupParserData = new MilestoneGroupParserGen(grammar).parserData()
    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, prTipEdges=${mgroupParserData.tipEdgeProgressActions.size}, exTipEdges=${mgroupParserData.tipEdgeRequiredSymbols.size}, midEdges=${mgroupParserData.midEdgeProgressActions.size}")
    println(s"elapsed: ${Duration.between(start2, Instant.now())}")
    val mgroupParser = new MilestoneGroupParser(mgroupParserData) //.setVerbose()

    val milestoneParserProto = MilestoneParser2ProtobufConverter.toProto(milestoneParserData)
    val mgroupParserProto = MilestoneGroupParserDataProtobufConverter.toProto(mgroupParserData)
    println(s"milestone size=${milestoneParserProto.getSerializedSize}, mgroup size=${mgroupParserProto.getSerializedSize}, ratio=${mgroupParserProto.getSerializedSize.toDouble / milestoneParserProto.getSerializedSize}")
    (milestoneParser, mgroupParser)
  }

  "metalang3 grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/metalang3/grammar.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = 'a'+")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "Abc = 'a'+ {str($0)}")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, new String(getClass.getResourceAsStream("/metalang3/grammar.cdg").readAllBytes()))
  }

  "bibix grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix2/grammar.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = c()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "ABC = cde.hello()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, new String(new FileInputStream("build.bbx").readAllBytes()))

    Catalog.INSTANCE.getBibix2.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "json grammar" should "work" in {
    val json = Catalog.INSTANCE.getJson

    val analysis = MetaLanguage3.analyzeGrammar(json.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    json.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "proto3 grammar" should "work" in {
    val proto3 = Catalog.INSTANCE.getProto3

    val analysis = MetaLanguage3.analyzeGrammar(proto3.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    proto3.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
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
  //    val grammar = new String(getClass.getResourceAsStream("/j1/grammar.cdg").readAllBytes())
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
  //
  //  "j1 grammar load" should "work" in {
  //    val parserData = Using(getClass.getResourceAsStream("/j1-mg2-parserdata.pb.gz")) { inputStream =>
  //      Using(new GZIPInputStream(inputStream)) { gzipStream =>
  //        MilestoneGroupParserDataProtobufConverter.fromProto(
  //          MilestoneGroupParserDataProto.MilestoneGroupParserData.parseFrom(gzipStream)
  //        )
  //      }.get
  //    }.get
  //
  //    val parser = new MilestoneGroupParser(parserData).setVerbose()
  //    val input = Inputs.fromString("class A {\n  String xx = \"\\22\";\n}")
  //    val result = parser.parseOrThrow(input)
  //    val history = parser.kernelsHistory(result)
  //
  //    val parseForest = new ParseTreeConstructor2(ParseForestFunc)(parserData.grammar)(input, history.map(Kernels)).reconstruct().get
  //    assert(parseForest.trees.size == 1)
  //    println(parseForest.trees.head)
  //  }
}
