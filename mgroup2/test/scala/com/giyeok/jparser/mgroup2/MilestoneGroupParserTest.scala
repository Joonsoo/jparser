package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParser2ProtobufConverter, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.flatspec.AnyFlatSpec

class MilestoneGroupParserTest extends AnyFlatSpec {
  def testEquality(grammar: NGrammar, milestoneParser: MilestoneParser, mgroupParser: MilestoneGroupParser, example: String): Unit = {
    val inputs = Inputs.fromString(example)

    println(s"::: \"$example\"")

    var milestoneCtx = milestoneParser.initialCtx
    var mgroupCtx = mgroupParser.initialCtx
    inputs.foreach { input =>
      milestoneCtx = milestoneParser.parseStep(milestoneCtx, input).getOrElse(throw new IllegalStateException(""))
      mgroupCtx = mgroupParser.parseStep(mgroupCtx, input).getOrElse(throw new IllegalStateException(""))
    }

    val milestoneResult = milestoneCtx
    val milestoneHistory = milestoneParser.kernelsHistory(milestoneResult)

    val mgroupResult = mgroupCtx
    val mgroupHistory = mgroupParser.kernelsHistory(mgroupResult)

    val milestoneParseForest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, milestoneHistory.map(Kernels)).reconstruct().get
    assert(milestoneParseForest.trees.size == 1)

    milestoneHistory.zip(mgroupHistory).zipWithIndex.foreach { case (pair, index) =>
      if (pair._1 == pair._2) {
        println(s"$index: identical")
      } else {
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
    val milestoneParser = new MilestoneParser(milestoneParserData).setVerbose()

    println("mgroup:::")
    val mgroupParserData = new MilestoneGroupParserGen(grammar).parserData()
    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, prTipEdges=${mgroupParserData.tipEdgeProgressActions.size}, exTipEdges=${mgroupParserData.tipEdgeRequiredSymbols.size}, midEdges=${mgroupParserData.midEdgeProgressActions.size}")
    val mgroupParser = new MilestoneGroupParser(mgroupParserData).setVerbose()

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
  }

  "bibix grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A = c()")
    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "ABC = cde.hello()")
  }

  "simple grammar" should "work" in {
    val grammar =
      """A = B+
        |B: string? = <'a'+ {str($0)} | ' '+ {null}>
        |""".stripMargin

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
    val grammar =
      """Defs = Def (WS Def)* {[$0] + $1}
        |
        |Def: Def = TargetDef
        |
        |TargetDef = SimpleName WS '=' WS Expr {TargetDef(name=$0, value=$4)}
        |
        |SimpleName = <('a-zA-z' 'a-zA-Z0-9_'* {str($0, $1)})&Tk> $0
        |
        |Expr: Expr = CallExpr
        |
        |CallExpr = SimpleName WS CallParams {CallExpr(name=$0, params=$2)}
        |CallParams = '(' WS ')' {CallParams(posParams=[], namedParams=[])}
        |
        |Tk = <'a-zA-Z0-9_'+> | <'+\-*/!&|=<>'+>
        |
        |WS = WS_*
        |WS_ = ' \n\r\t'
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)
    val (milestoneParser, mgroupParser) = generateParsers(analysis.ngrammar)

    testEquality(analysis.ngrammar, milestoneParser, mgroupParser, "A=c()")
  }
}
