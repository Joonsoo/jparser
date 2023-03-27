package com.giyeok.jparser.tests.metalang3

import com.giyeok.jparser.examples.MetaLang3Example
import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.metalang3.MetaLanguage3.{IllegalGrammar, analyzeGrammar, check}
import com.giyeok.jparser.metalang3.{AnalysisPrinter, Type, ValuefyExprSimulator}

object Tester {

  def testExample(example: MetaLang3Example): Unit = {
    println(example.grammar)
    val analysis = analyzeGrammar(example.grammar, example.name)
    if (!analysis.errors.isClear) {
      throw new Exception(analysis.errors.toString)
    }
    val valuefyExprSimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
    val analysisPrinter = new AnalysisPrinter(analysis.ngrammar, valuefyExprSimulator.startValuefyExpr, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)

    analysis.nonterminalTypes.foreach { p =>
      println(s"Nonterm `${p._1}` = ${Type.readableNameOf(p._2)}")
    }
    AnalysisPrinter.printClassHierarchy(analysis.rawClassRelations.toHierarchy)
    val classHierarchy = analysis.classRelations.toHierarchy
    AnalysisPrinter.printClassHierarchy(classHierarchy)
    println(s"Enum Values: ${analysis.enumValuesMap}")
    println(s"Shortened enums: ${analysis.shortenedEnumTypesMap}")
    analysis.classParamTypes.foreach(pair =>
      // TODO supers
      AnalysisPrinter.printClassDef(classHierarchy, pair._1, pair._2)
    )
    analysisPrinter.printValuefyStructure()

    if (!analysis.errors.isClear) {
      throw IllegalGrammar(s"Errors: ${analysis.errors.errors}")
    }
    example.correctExamplesWithResults.foreach { example =>
      val CorrectExample(input, expectedResult) = example
      valuefyExprSimulator.parse(input) match {
        case Left(parsed) =>
          println(s"== Input: $input")
          AnalysisPrinter.printNodeStructure(parsed)
          val valuefied = valuefyExprSimulator.valuefyStart(parsed)
          println(s"Value: ${valuefied.prettyPrint()}")
          // println(valuefied.detailPrint())
          expectedResult.foreach(someExpectedResult =>
            check(valuefied.prettyPrint() == someExpectedResult,
              s"Valuefy result mismatch, expected=$someExpectedResult, actual=${valuefied.prettyPrint()}"))
        case Right(error) =>
          println(error)
      }
    }
  }
}
