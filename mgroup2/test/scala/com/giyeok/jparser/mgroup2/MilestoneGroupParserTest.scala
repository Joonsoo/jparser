package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParserData, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import org.scalatest.flatspec.AnyFlatSpec

class MilestoneGroupParserTest extends AnyFlatSpec {
  def testEquality(grammar: NGrammar, mgroupParserData: MilestoneGroupParserData, milestoneParserData: MilestoneParserData, example: String): Unit = {
    val inputs = Inputs.fromString(example)

    //    println("milestone:::")
    val mileParser = new MilestoneParser(milestoneParserData).setVerbose()
    val mileResult = mileParser.parseOrThrow(inputs)
    //    println(mileResult)
    val mileHistory = mileParser.kernelsHistory(mileResult)

    //    println("mgroup:::")
    val mgroupParser = new MilestoneGroupParser(mgroupParserData).setVerbose()
    val mgroupResult = mgroupParser.parseOrThrow(inputs)
    //    println(mgroupResult)
    val mgroupHistory = mgroupParser.kernelsHistory(mgroupResult)

    mileHistory.zip(mgroupHistory).zipWithIndex.foreach { case (pair, index) =>
      if (pair._1 != pair._2) {
        println(s"$index:")
        println(s"mile: ${pair._1.toList.sorted}")
        println(s"mgrp: ${pair._2.toList.sorted}")
        println(s"mile-mgrp: ${(pair._1 -- pair._2).toList.sorted}")
        println(s"mgrp-mile: ${(pair._2 -- pair._1).toList.sorted}")
      }
      assert(pair._1 == pair._2)
    }

    val mileParseForest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, mileHistory.map(Kernels)).reconstruct().get
    val mgroupParseForest = new ParseTreeConstructor2(ParseForestFunc)(grammar)(inputs, mgroupHistory.map(Kernels)).reconstruct().get
    assert(mgroupParseForest.trees == mileParseForest.trees)
    assert(mgroupParseForest.trees.size == 1)
  }

  "metalang3 grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/cdglang3.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    println("milestone:::")
    val milestoneParserData = new MilestoneParserGen(analysis.ngrammar).parserData()
    val termActionsSize = milestoneParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"terms=${milestoneParserData.termActions.size}, termActions=$termActionsSize, edgeActions=${milestoneParserData.edgeProgressActions.size}")
    println("mgroup:::")
    val mgroupParserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()
    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, tipEdgeActions=${mgroupParserData.tipEdgeProgressActions.size}, midEdgeActions=${mgroupParserData.midEdgeProgressActions.size}")

    testEquality(analysis.ngrammar, mgroupParserData, milestoneParserData, "A = 'a'+")
  }

  "bibix grammar" should "be parsed" in {
    val grammar = new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes())

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    println("milestone:::")
    val milestoneParserData = new MilestoneParserGen(analysis.ngrammar).parserData()
    val termActionsSize = milestoneParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"terms=${milestoneParserData.termActions.size}, termActions=$termActionsSize, edgeActions=${milestoneParserData.edgeProgressActions.size}")

    println("mgroup:::")
    val mgroupParserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()
    val mgroupTermActionsSize = mgroupParserData.termActions.foldLeft(0)(_ + _._2.size)
    println(s"groups=${mgroupParserData.milestoneGroups.size}, terms=${mgroupParserData.termActions.size}, termActions=$mgroupTermActionsSize, tipEdgeActions=${mgroupParserData.tipEdgeProgressActions.size}, midEdgeActions=${mgroupParserData.midEdgeProgressActions.size}")

    testEquality(analysis.ngrammar, mgroupParserData, milestoneParserData, "A = c()")
  }

  it should "work" in {
    val grammar =
      """A = B+
        |B: string? = <'a'+ {str($0)} | ' '+ {null}>
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    val mgroupParserData = new MilestoneGroupParserGen(analysis.ngrammar).parserData()
    val mileParserData = new MilestoneParserGen(analysis.ngrammar).parserData()

    val inputs = Inputs.fromString("aaa")

    println("mgroup:::")
    val mgroupParser = new MilestoneGroupParser(mgroupParserData).setVerbose()
    val mgroupResult = mgroupParser.parseOrThrow(inputs)
    println(mgroupResult)
    val mgroupHistory = mgroupParser.kernelsHistory(mgroupResult)

    println("milestone:::")
    val mileParser = new MilestoneParser(mileParserData).setVerbose()
    val mileResult = mileParser.parseOrThrow(inputs)
    println(mileResult)
    val mileHistory = mileParser.kernelsHistory(mileResult)
    mileHistory.zip(mgroupHistory).zipWithIndex.foreach { case (pair, index) =>
      if (pair._1 == pair._2) {
        println(s"$index: identical")
      } else {
        println(s"$index:")
        println(s"mile: ${pair._1.toList.sorted}")
        println(s"mgrp: ${pair._2.toList.sorted}")
        println(s"mile-mgrp: ${(pair._1 -- pair._2).toList.sorted}")
        println(s"mgrp-mile: ${(pair._2 -- pair._1).toList.sorted}")
      }
    }

    println()

    val mgroupParseForest = new ParseTreeConstructor2(ParseForestFunc)(analysis.ngrammar)(inputs, mgroupHistory.map(Kernels)).reconstruct().get
    val mileParseForest = new ParseTreeConstructor2(ParseForestFunc)(analysis.ngrammar)(inputs, mileHistory.map(Kernels)).reconstruct().get
    assert(mgroupParseForest.trees == mileParseForest.trees)
    assert(mgroupParseForest.trees.size == 1)

    val valuefy = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
    println(valuefy.valuefyStart(mgroupParseForest.trees.head))
  }
}
