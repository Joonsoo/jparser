package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.examples.metalang3.MetaLang3ExamplesCatalog
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
    val grammar = MetaLang3ExamplesCatalog.INSTANCE.getBibix2.getGrammarText

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = c()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "ABC = cde.hello()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, new String(new FileInputStream("build.bbx").readAllBytes()))

    MetaLang3ExamplesCatalog.INSTANCE.getBibix2.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "json grammar" should "work" in {
    val json = MetaLang3ExamplesCatalog.INSTANCE.getJson

    val analysis = MetaLanguage3.analyzeGrammar(json.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    json.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "proto3 grammar" should "work" in {
    val proto3 = MetaLang3ExamplesCatalog.INSTANCE.getProto3

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

  "subset of autodb grammar" should "work" in {
    val grammar = MetaLanguage3.analyzeGrammar(
      """EntityViewFieldSelectExpr: EntityViewFieldSelectExpr = EntityViewFieldSelectTerm
        |    | EntityViewFieldSelectTerm WS <"==" {%EQ} | "!=" {%NE}> WS EntityViewFieldSelectExpr
        |      {BinOp(op:%BinOpType=$2, lhs=$0, rhs=$4)}
        |EntityViewFieldSelectTerm: EntityViewFieldSelectTerm = "null"&Tk {NullValue()}
        |    | FieldName ((WS '?')? WS '.' WS DataFieldName {DataTypeValueSelectField(nullable=ispresent($0), fieldName=$4)})* {DataTypeValueSelect(field=$0, selectPath=$1)}
        |    | "@primaryKey" {PrimaryKeySelect()}
        |    | '(' WS EntityViewFieldSelectExpr WS ')' {Paren(expr=$2)}
        |PreDefinedEntityView = "view"&Tk WS FieldRef WS '(' WS PreDefinedEntityViewField (WS ',' WS PreDefinedEntityViewField)* (WS ',')? WS ')'
        |    {PreDefinedEntityView(definition=$2, fields=[$6]+$7)}
        |PreDefinedEntityViewField = FieldName {PreDefinedEntityViewField(originalFieldName=$0, thisEntityFieldName=$0)}
        |    | FieldName WS '=' WS FieldName {PreDefinedEntityViewField(originalFieldName=$0, thisEntityFieldName=$4)}
        |
        |
        |WS = ' '*
        |Tk = <'a-zA-Z0-9_'+> | <'+\-*/!&|=<>'+>
        |
        |FieldName = Name
        |DataTypeName = Name
        |DataFieldName = Name
        |
        |FieldRef = <Name (WS '.' WS Name)* {FieldRef(names=[$0] + $1)}>
        |
        |Name = <'a-zA-Z_' 'a-zA-Z0-9_'* {str($0, $1)}>-Keywords
        |
        |Keywords = "Int" | "Long" | "String" | "Timestamp" | "Duration" | "URI" | "Boolean"
        |  | "Empty" | "Ref" | "List" | "entity"
        |  | "autoincrement" | "sparsegenLong" | "view" | "null"
        |  | "==" | "!="
        |  | "query" | "rows" | "update"
        |  | "true" | "false"
        |""".stripMargin
    ).ngrammar

    val (milestoneParser, mgroupParser) = generateParsers(grammar)

    testEquality(grammar, milestoneParser, mgroupParser, "verifiedIdentity != null")
  }

  "autodb3problem" should "work" in {
    val autodb3 = MetaLang3ExamplesCatalog.INSTANCE.getAutodb3problem

    val analysis = MetaLanguage3.analyzeGrammar(autodb3.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    autodb3.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "j1 mark1" should "work" in {
    val j1mark1 = MetaLang3ExamplesCatalog.INSTANCE.getJ1mark1

    val analysis = MetaLanguage3.analyzeGrammar(j1mark1.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    j1mark1.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }

  "j1 mark1 subset" should "work" in {
    val j1mark1 = MetaLang3ExamplesCatalog.INSTANCE.getJ1mark1subset

    val analysis = MetaLanguage3.analyzeGrammar(j1mark1.getGrammarText)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    j1mark1.getExamples.forEach { example =>
      testEquality(analysis.ngrammar, milestoneParser, mgroupParser, example.getExample)
    }
  }
}
