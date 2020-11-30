package com.giyeok.jparser.metalang3a

import java.io.{BufferedWriter, FileWriter}

import com.giyeok.jparser.examples.MetaLang3Example
import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.examples.metalang3.{MetaLang3Grammar, OptionalExamples, PExprExamples, SimpleExamples}
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3a.Type._
import com.giyeok.jparser.metalang3a.ValuefyExpr.UnrollChoices
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen
import com.giyeok.jparser.{Grammar, NGrammar}

object MetaLanguage3 {

  case class IllegalGrammar(msg: String) extends Exception(msg)

  def check(cond: Boolean, errorMessage: String): Unit = {
    if (!cond) throw IllegalGrammar(errorMessage)
  }

  def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = MetaGrammar3Ast.parseAst(grammar) match {
    case Left(value) => value
    case Right(value) => throw IllegalGrammar(value.msg)
  }

  case class ProcessedGrammar(grammar: Grammar, ngrammar: NGrammar, startNonterminalName: String,
                              nonterminalTypes: Map[String, Type], nonterminalValuefyExprs: Map[String, UnrollChoices],
                              rawClassRelations: ClassRelationCollector, classParamTypes: Map[String, List[(String, Type)]],
                              shortenedEnumTypesMap: Map[Int, String], enumValuesMap: Map[String, Set[String]],
                              errors: CollectedErrors) {
    val typeInferer = new TypeInferer(startNonterminalName, nonterminalTypes)
    val classRelations: ClassRelationCollector =
      if (errors.isClear) rawClassRelations.removeDuplicateEdges() else null

    def isSubtypeOf(superType: Type, subType: Type): Boolean = (superType, subType) match {
      case (ClassType(superClass), ClassType(subClass)) => classRelations.reachableBetween(superClass, subClass)
      case (OptionalOf(superOpt), OptionalOf(subOpt)) => isSubtypeOf(superOpt, subOpt)
      case (ArrayOf(superElem), ArrayOf(subElem)) => isSubtypeOf(superElem, subElem)
      case (EnumType(superEnumName), EnumType(subEnumName)) => superEnumName == subEnumName
      case (EnumType(enumName), UnspecifiedEnumType(uniqueId)) => shortenedEnumTypesMap(uniqueId) == enumName
      case (UnspecifiedEnumType(uniqueId), EnumType(enumName)) => shortenedEnumTypesMap(uniqueId) == enumName
      case (UnspecifiedEnumType(superEnumId), UnspecifiedEnumType(subEnumId)) =>
        shortenedEnumTypesMap(superEnumId) == shortenedEnumTypesMap(subEnumId)
      case (Type.AnyType, _) => true
      case (_, Type.NothingType) => true
      case (superType, subType) => superType == subType
    }

    // TODO UnionOf(UnspecifiedEnumType(1), UnspecifiedEnumType(2))가 들어왔는데 UnspecifiedEnumType(1)과 2가 같은 enum인 경우 Nothing이 반환됨. 고쳐야 함
    def reduceUnionType(unionType: UnionOf): Type = {
      val types = unionType.types
      val reducedTypes = types.filterNot { subType =>
        // (types - subType)에 subType보다 super type인 타입이 존재하면 subType은 없어도 됨
        (types - subType).exists { superType => isSubtypeOf(superType, subType) }
      }
      Type.unifyTypes(reducedTypes)
    }

    def validated(): ProcessedGrammar = if (!errors.isClear) this else {
      // validate하기 전에 이미 오류가 있었으면 더이상 validate할 필요가 없음
      // 여기까지 얻어진 정보에서 오류를 찾아서 오류가 있으면 `errors`에 추가해서 반환. 오류가 없으면 그대로 반환
      // - function call에서
      //   - ispresent의 인자는 하나여야 하고, 그 타입은 array, optional, string, node 중 하나여야 함
      //   - isempty도 마찬가지
      //   - str은 각 인자가 node, bool, char, string, 혹은 이 네가지 타입의 어레이(혹은 그 어레이의 어레이)여야 함
      // - 연산자에서
      //   - !expr에서 expr은 bool 타입이어야 함
      //   - A&&B와 A||B에서 A와 B는 bool 타입이어야 함
      //   - A==B와 A!=B에서 A와 B의 타입이 consistent해야 함(A가 B의 서브타입이거나 B가 A의 서브타입이면 될듯?)
      //   - A?B:C 에서 A는 bool이어야 하고 B와 C의 타입은 consistent(B가 C의 서브타입이거나 C가 B의 서브타입이거나)
      this
    }
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

    ProcessedGrammar(grammar, ngrammar, transformer.startNonterminalName(),
      inferredTypeCollector.nonterminalTypes, transformer.nonterminalValuefyExprs,
      inferredTypeCollector.typeRelations.classRelations, classParamTypes,
      enumsMap, enumValues,
      errorCollector.collectedErrors
    ).validated()
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
            val valuefied = valuefyExprSimulator.valuefy(parsed)
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

    // testExample(SimpleExamples.ex12a)
    // testExample(MetaLang3Grammar.inMetaLang3)

    def generateParser(grammar: String, grammarName: String,
                       mainFuncExamples: Option[List[String]] = None,
                       printClassHierarchy: Boolean = false): Unit = {
      val analysis = analyzeGrammar(grammar, grammarName)
      if (!analysis.errors.isClear) {
        throw new Exception(analysis.errors.toString)
      }

      println(analysis.ngrammar.startSymbol)
      analysis.ngrammar.nsymbols.foreach(println)
      analysis.ngrammar.nsequences.foreach(println)

      if (printClassHierarchy) {
        AnalysisPrinter.printClassHierarchy(analysis.classRelations.toHierarchy)
      }

      val codegen = new ScalaCodeGen(analysis)

      val packageName = "com.giyeok.jparser.metalang3a.generated"

      val filePath = s"./metalang/src/main/scala/${packageName.split('.').mkString("/")}/$grammarName.scala"
      val generatedCode = s"package $packageName\n\n" + codegen.generateParser(grammarName, mainFuncExamples)

      val writer = new BufferedWriter(new FileWriter(filePath))
      writer.write(generatedCode)
      writer.close()
    }

    // testExample(OptionalExamples.withShortEnum)

    //    testExample(MetaLang3Example("A",
    //      """MyClass<SuperClass>(value: string)
    //        |A: AnotherClass = # {MyClass("123")}
    //        |""".stripMargin).example(""))
    //    testExample(MetaLang3Example("B",
    //      """MyClass<SuperClass, AnohterClass>(value: string)
    //        |A: AnotherClass = # {MyClass("123")}
    //        |""".stripMargin).example(""))
    //    testExample(MetaLang3Example("C",
    //      """MyClass(value: string)
    //        |A: AnotherClass = # {MyClass("123")}
    //        |""".stripMargin).example(""))
    //    testExample(MetaLang3Example("D",
    //      """SuperClass<GrandSuper> {
    //        |  SomeClass,
    //        |  SubSuperClass<AnotherClass> {
    //        |    IndirectSubConcreteClass<>(param1:string)
    //        |  },
    //        |  DirectSubConcreteClass<>()
    //        |}
    //        |A = 'a'
    //        |""".stripMargin))

    //    generateParser(SimpleExamples.ex3.grammar, "Simple3", Some(List("a b")))
    //    testExample(PExprExamples.ex3)
    //    testExample(PExprExamples.ex3a)
    //    testExample(PExprExamples.ex3b)
    //    generateParser(PExprExamples.ex1.grammar, "BindPExpr", Some(PExprExamples.ex1.correctExamples))
    //    generateParser(SimpleExamples.ex12a.grammar, "Simple12a", Some(List("ac", "abbbbc")))
    //    generateParser(SimpleExamples.repeat.grammar, "RepeatExample", Some(List("b", "aaaabbbbb")))
    //    generateParser(OptionalExamples.simple.grammar, "OptionalExample", Some(List("abc", "d")))
    //    generateParser(OptionalExamples.withShortEnum.grammar, "OptionalWithShortEnumExample", Some(OptionalExamples.withShortEnum.correctExamples))
    generateParser(
      """A = B&C {$>0}
        |B = 'a-z'+ {B(text=str($0))}
        |C = 'b-y'+ {C(text=str($0))}
        |""".stripMargin, "JoinTest", printClassHierarchy = true,
      mainFuncExamples = Some(List("b")))
  }
}
