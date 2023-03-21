package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.{Inputs, ParseForestFunc}
import com.giyeok.jparser.metalang3.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParserGen}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import org.scalatest.flatspec.AnyFlatSpec

class MilestoneGroupParserTest extends AnyFlatSpec {
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
