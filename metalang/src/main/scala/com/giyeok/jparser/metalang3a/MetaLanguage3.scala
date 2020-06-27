package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.MetaLang3Example
import com.giyeok.jparser.examples.metalang3.SimpleExamples
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3a.ValuefyExpr.UnrollChoices

object MetaLanguage3 {

    case class IllegalGrammar(msg: String) extends Exception(msg)

    def check(cond: Boolean, errorMessage: String): Unit = {
        if (!cond) throw IllegalGrammar(errorMessage)
    }

    def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = MetaGrammar3Ast.parseAst(grammar) match {
        case Left(value) => value
        case Right(value) => throw IllegalGrammar(value.msg)
    }

    case class ProcessedGrammar(ngrammar: NGrammar, startNonterminalName: String, nonterminalValuefyExprs: Map[String, UnrollChoices],
                                classRelations: ClassRelationCollector, classParamTypes: Map[String, List[(String, Type)]],
                                shortenedEnumTypesMap: Map[Int, String], enumValuesMap: Map[String, Set[String]],
                                errors: CollectedErrors)

    def analyzeGrammar(grammarDef: MetaGrammar3Ast.Grammar, grammarName: String): ProcessedGrammar = {
        val errorCollector = new ErrorCollector()
        val transformer = new GrammarTransformer(grammarDef, errorCollector)
        val grammar = transformer.grammar(grammarName)

        val inferredTypeCollector = new InferredTypeCollector(
            transformer.startNonterminalName(), transformer.classInfo, grammar.rules.keySet, transformer.nonterminalInfo)(errorCollector)
        while (errorCollector.isClear && inferredTypeCollector.tryInference()) {}

        if (!inferredTypeCollector.isComplete) {
            errorCollector.addError("Incomplete type info")
        }

        if (inferredTypeCollector.typeRelations.classRelations.hasCycle) {
            errorCollector.addError("Cyclic class relation")
        }

        val classParamTypes = inferredTypeCollector.classParamTypes.map { pair =>
            val (className, paramTypes) = pair
            val paramNames = transformer.classInfo.classParamSpecs(className).params.map(_.name)
            className -> paramNames.zip(paramTypes)
        }.toMap

        // TODO A가 B의 super class일 때, class C extends A, B 이면 C는 B만 상속받으면 되고, union(A, B)는 A로 바꾸면 됨

        val ngrammar = NGrammar.fromGrammar(grammar)

        val enumsMap = inferredTypeCollector.typeRelations.enumRelations.toUnspecifiedEnumMap(errorCollector)
        var enumValues = transformer.classInfo.canonicalEnumValues
        enumsMap.foreach(pair =>
            enumValues += (pair._2 -> (enumValues.getOrElse(pair._2, Set()) ++
                transformer.classInfo.shortenedEnumValues.getOrElse(pair._1, Set())))
        )

        ProcessedGrammar(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs,
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
            val valuefyExprSimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs, analysis.shortenedEnumTypesMap)
            val analysisPrinter = new AnalysisPrinter(valuefyExprSimulator.startValuefyExpr, analysis.nonterminalValuefyExprs)

            val classHierarchy = analysis.classRelations.toHierarchy
            analysisPrinter.printClassHierarchy(classHierarchy)
            analysis.classParamTypes.foreach(pair =>
                // TODO supers
                analysisPrinter.printClassDef(pair._1, pair._2)
            )
            analysisPrinter.printValuefyStructure()

            println(analysis.enumValuesMap)
            example.correctExamples.foreach { exampleSource =>
                val parsed = valuefyExprSimulator.parse(exampleSource).left.get
                println(s"== Input: $exampleSource")
                analysisPrinter.printNodeStructure(parsed)
                println(valuefyExprSimulator.valuefy(parsed).prettyPrint())
            }
        }

        testExample(SimpleExamples.ex7)
        // testExample(SimpleExamples.ex6a)
        // testExample(SimpleExamples.ex6b)
        // testExample(SimpleExamples.ex6c)
    }
}
