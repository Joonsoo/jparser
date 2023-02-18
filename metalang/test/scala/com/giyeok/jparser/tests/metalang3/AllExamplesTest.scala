package com.giyeok.jparser.tests.metalang3

import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.examples.metalang3.AllMetaLang3Examples
import com.giyeok.jparser.metalang3.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3.{AnalysisPrinter, MetaLanguage3, ValuefyExprSimulator}
import org.scalatest.flatspec.AnyFlatSpec

class AllExamplesTest extends AnyFlatSpec {
  AllMetaLang3Examples.examples foreach { example =>
    var analysis: ProcessedGrammar = null
    var valuefySimulator: ValuefyExprSimulator = null
    var analysisPrinter: AnalysisPrinter = null
    example.name should "be correctly analyzed" in {
      analysis = MetaLanguage3.analyzeGrammar(example.grammar, example.name)
      assert(analysis.errors.isClear, analysis.errors)

      valuefySimulator = ValuefyExprSimulator(analysis)
      analysisPrinter = new AnalysisPrinter(analysis.ngrammar, valuefySimulator.startValuefyExpr, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)

      //            val classHierarchy = analysis.classRelations.toHierarchy
      //            AnalysisPrinter.printClassHierarchy(classHierarchy)
      //            println(s"Enum Values: ${analysis.enumValuesMap}")
      //            println(s"Shortened enums: ${analysis.shortenedEnumTypesMap}")
      //            analysis.classParamTypes.foreach(pair =>
      //                // TODO supers
      //                AnalysisPrinter.printClassDef(classHierarchy, pair._1, pair._2)
      //            )
      //            analysisPrinter.printValuefyStructure()
    }
    example.correctExamplesWithResults foreach { inputExample =>
      val CorrectExample(input, expectedResult) = inputExample
      example.name should s"correctly valuefy input $input" in {
        val parsed = valuefySimulator.parse(inputExample.input)
        assert(parsed.isLeft)
        //        println(s"== Input: $input")
        val parsedNode = parsed.left.get
        //        AnalysisPrinter.printNodeStructure(parsedNode)
        val valuefied = valuefySimulator.valuefyStart(parsedNode)
        //        println(valuefied.prettyPrint())
        expectedResult.foreach(someExpectedResult =>
          assert(valuefied.prettyPrint() == someExpectedResult))
      }
    }
  }
}
