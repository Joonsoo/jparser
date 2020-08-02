package com.giyeok.jparser.tests.metalang3

import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.examples.metalang3.AllMetaLang3Examples
import com.giyeok.jparser.metalang3a.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3a.{AnalysisPrinter, MetaLanguage3, ValuefyExprSimulator}
import org.scalatest.FlatSpec

class AllExamplesTest extends FlatSpec {
    AllMetaLang3Examples.examples foreach { example =>
        var analysis: ProcessedGrammar = null
        var valuefySimulator: ValuefyExprSimulator = null
        var analysisPrinter: AnalysisPrinter = null
        example.name should "be correctly analyzed" in {
            analysis = MetaLanguage3.analyzeGrammar(example.grammar, example.name)
            assert(analysis.errors.isClear, analysis.errors)

            valuefySimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
            analysisPrinter = new AnalysisPrinter(valuefySimulator.startValuefyExpr, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)

            val classHierarchy = analysis.classRelations.toHierarchy
            analysisPrinter.printClassHierarchy(classHierarchy)
            println(s"Enum Values: ${analysis.enumValuesMap}")
            println(s"Shortened enums: ${analysis.shortenedEnumTypesMap}")
            analysis.classParamTypes.foreach(pair =>
                // TODO supers
                analysisPrinter.printClassDef(classHierarchy, pair._1, pair._2)
            )
            analysisPrinter.printValuefyStructure()
        }
        example.correctExamplesWithResults foreach { inputExample =>
            val CorrectExample(input, expectedResult) = inputExample
            example.name should s"correctly valuefy input $input" in {
                val parsed = valuefySimulator.parse(inputExample.input)
                assert(parsed.isLeft)
                println(s"== Input: $input")
                val parsedNode = parsed.left.get
                analysisPrinter.printNodeStructure(parsedNode)
                val valuefied = valuefySimulator.valuefy(parsedNode)
                println(valuefied.prettyPrint())
                expectedResult.foreach(someExpectedResult =>
                    assert(valuefied.prettyPrint() == someExpectedResult))
            }
        }
    }
}
