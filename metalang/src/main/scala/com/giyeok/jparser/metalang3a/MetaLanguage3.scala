package com.giyeok.jparser.metalang3a

import java.io.{BufferedWriter, FileWriter}

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.MetaLang3Example
import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.examples.metalang3.{MetaLang3Grammar, SimpleExamples}
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3a.ValuefyExpr.UnrollChoices
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen

object MetaLanguage3 {

    case class IllegalGrammar(msg: String) extends Exception(msg)

    def check(cond: Boolean, errorMessage: String): Unit = {
        if (!cond) throw IllegalGrammar(errorMessage)
    }

    def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = MetaGrammar3Ast.parseAst(grammar) match {
        case Left(value) => value
        case Right(value) => throw IllegalGrammar(value.msg)
    }

    case class ProcessedGrammar(ngrammar: NGrammar, startNonterminalName: String,
                                nonterminalTypes: Map[String, Type], nonterminalValuefyExprs: Map[String, UnrollChoices],
                                rawClassRelations: ClassRelationCollector, classParamTypes: Map[String, List[(String, Type)]],
                                shortenedEnumTypesMap: Map[Int, String], enumValuesMap: Map[String, Set[String]],
                                errors: CollectedErrors) {
        val typeInferer = new TypeInferer(startNonterminalName, nonterminalTypes)
        val classRelations: ClassRelationCollector =
            if (errors.isClear) rawClassRelations.removeDuplicateEdges() else null
    }

    def analyzeGrammar(grammarDef: MetaGrammar3Ast.Grammar, grammarName: String): ProcessedGrammar = {
        val errorCollector = new ErrorCollector()
        val transformer = new GrammarTransformer(grammarDef, errorCollector)
        val grammar = transformer.grammar(grammarName)

        val inferredTypeCollector = new InferredTypeCollector(
            transformer.startNonterminalName(), transformer.classInfo, grammar.rules.keySet, transformer.nonterminalInfo)(errorCollector)

        var counter = 0
        while (errorCollector.isClear && inferredTypeCollector.tryInference()) {
            counter += 1
            if (counter > 5) {
                println(s"try inference for $counter times...")
            }
        }

        if (!inferredTypeCollector.isComplete) {
            errorCollector.addError("Incomplete type info")
        }

        inferredTypeCollector.typeRelations.classRelations.checkCycle(errorCollector)

        val classParamTypes = inferredTypeCollector.classParamTypes.map { pair =>
            val (className, paramTypes) = pair
            val paramNames = transformer.classInfo.classParamSpecs(className).params.map(_.name)
            className -> paramNames.zip(paramTypes)
        }.toMap

        val ngrammar = NGrammar.fromGrammar(grammar)

        val enumsMap = inferredTypeCollector.typeRelations.enumRelations.toUnspecifiedEnumMap(errorCollector)
        var enumValues = transformer.classInfo.canonicalEnumValues
        enumsMap.foreach(pair =>
            enumValues += (pair._2 -> (enumValues.getOrElse(pair._2, Set()) ++
                transformer.classInfo.shortenedEnumValues.getOrElse(pair._1, Set())))
        )

        ProcessedGrammar(ngrammar, transformer.startNonterminalName(),
            inferredTypeCollector.nonterminalTypes, transformer.nonterminalValuefyExprs,
            inferredTypeCollector.typeRelations.classRelations, classParamTypes,
            enumsMap, enumValues,
            errorCollector.collectedErrors
        )
    }

    def analyzeGrammar(grammarDefinition: String, grammarName: String = "GeneratedGrammar"): ProcessedGrammar =
        analyzeGrammar(parseGrammar(grammarDefinition), grammarName)

    def main(args: Array[String]): Unit = {
        def testExample(example: MetaLang3Example): Unit = {
            println(example.grammar)
            val analysis = analyzeGrammar(example.grammar, example.name)
            if (!analysis.errors.isClear) {
                throw new Exception(analysis.errors.toString)
            }
            val valuefyExprSimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
            val analysisPrinter = new AnalysisPrinter(valuefyExprSimulator.startValuefyExpr, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)

            analysis.nonterminalTypes.foreach { p =>
                println(s"Nonterm `${p._1}` = ${Type.readableNameOf(p._2)}")
            }
            analysisPrinter.printClassHierarchy(analysis.rawClassRelations.toHierarchy)
            val classHierarchy = analysis.classRelations.toHierarchy
            analysisPrinter.printClassHierarchy(classHierarchy)
            println(s"Enum Values: ${analysis.enumValuesMap}")
            println(s"Shortened enums: ${analysis.shortenedEnumTypesMap}")
            analysis.classParamTypes.foreach(pair =>
                // TODO supers
                analysisPrinter.printClassDef(classHierarchy, pair._1, pair._2)
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
                        analysisPrinter.printNodeStructure(parsed)
                        val valuefied = valuefyExprSimulator.valuefy(parsed)
                        println(valuefied.prettyPrint())
                        // println(valuefied.detailPrint())
                        expectedResult.foreach(someExpectedResult =>
                            check(valuefied.prettyPrint() == someExpectedResult,
                                s"Valuefy result mismatch, expected=$someExpectedResult, actual=${valuefied.prettyPrint()}"))
                    case Right(error) =>
                        println(error)
                }
            }
        }

        // testExample(SimpleExamples.ex12a)
        // testExample(MetaLang3Grammar.inMetaLang3)

        def generateParser(grammar: String, grammarName: String): Unit = {
            val analysis = analyzeGrammar(grammar, grammarName)
            if (!analysis.errors.isClear) {
                throw new Exception(analysis.errors.toString)
            }
            val codegen = new ScalaCodeGen(analysis)

            val packageName = "com.giyeok.jparser.metalang3a.generated"

            val filePath = s"./metalang/src/main/scala/${packageName.split('.').mkString("/")}/$grammarName.scala"
            val generatedCode = s"package $packageName\n\n" + codegen.generateParser(grammarName)

            val writer = new BufferedWriter(new FileWriter(filePath))
            writer.write(generatedCode)
            writer.close()
        }

        generateParser(SimpleExamples.ex3.grammar, "Simple3")
        // generateParser(MetaLang3Grammar.inMetaLang3.grammar, "MetaLang3")
    }
}
