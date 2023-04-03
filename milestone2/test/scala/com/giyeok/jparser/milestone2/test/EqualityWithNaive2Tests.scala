package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.examples.metalang3.{GrammarTestExample, GrammarWithExamples, MetaLang3ExamplesCatalog}
import com.giyeok.jparser.examples.naive.NaiveExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2._
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.milestone2.test.MilestoneAcceptConditionOrdering.milestoneAcceptConditionOrdering
import com.giyeok.jparser.nparser.AcceptConditionOrdering.acceptConditionOrdering
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel, ParseTreeConstructor2}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc, ParseResultTree}
import org.junit.jupiter.api.Assertions.assertEquals
import org.scalatest.flatspec.AnyFlatSpec

import java.util.zip.GZIPInputStream
import scala.collection.mutable
import scala.util.Using

// milestone2 파서가 naive 파서와 동일하게 동작하는지 테스트
class EqualityWithNaive2Tests extends AnyFlatSpec {
  // graph에서 start 커널로부터 시작해서 나오는 모든 milestone path들과 그 경로에서 커버되는 노드들의 집합을 반환한다.
  private def milestonePathsFrom(grammar: NGrammar, graph: KernelGraph, start: Kernel, gen: Int): (List[List[Milestone]], Set[Kernel]) = {
    assert(start.beginGen == start.endGen)

    val paths = mutable.Set[List[Milestone]]()
    val visitedNodes = mutable.Set[Kernel]()

    def isMilestone(kernel: Kernel): Boolean =
      grammar.symbolOf(kernel.symbolId).isInstanceOf[NSequence] &&
        kernel.pointer > 0 && kernel.beginGen < kernel.endGen

    def traverse(curr: Kernel, visited: List[Kernel], path: List[Milestone]): Unit = {
      visitedNodes += curr
      val nextPath = if (isMilestone(curr)) {
        Milestone(curr.symbolId, curr.pointer, curr.endGen) +: path
      } else path
      if (curr.endGen == gen) {
        paths += nextPath
      }
      val outgoings = graph.edgesByStart(curr).map(_.end) -- visited.toSet
      outgoings.foreach { outgoing =>
        traverse(outgoing, curr +: visited, nextPath)
      }
    }

    traverse(start, List(start), List(Milestone(start.symbolId, start.pointer, start.beginGen)))

    (paths.toList, visitedNodes.toSet)
  }

  def assertEqualCondition(condition: AcceptCondition.AcceptCondition, mcondition: MilestoneAcceptCondition, gen: Int): Unit = {
    (condition, mcondition) match {
      case (AcceptCondition.Always, Always) => // ok. do nothing
      case (AcceptCondition.Never, Never) => // ok. do nothing
      case (AcceptCondition.And(conds1), And(conds2)) =>
        if (conds1.size != conds2.size) {
          println(s"??")
          println(conds1)
          println(conds2)
        }
        assertEquals(conds1.size, conds2.size)
        conds1.toList.sorted.zip(conds2.sorted).foreach { pair =>
          assertEqualCondition(pair._1, pair._2, gen)
        }
      case (AcceptCondition.Or(conds1), Or(conds2)) =>
        assertEquals(conds1.size, conds2.size)
        conds1.toList.sorted.zip(conds2.sorted).foreach { pair =>
          assertEqualCondition(pair._1, pair._2, gen)
        }
      case (AcceptCondition.Exists(beginGen, endGen, symbolId), Exists(milestone, checkFromNextGen)) =>
        assertEquals(symbolId, milestone.symbolId)
        assertEquals(0, milestone.pointer)
        assertEquals(beginGen, milestone.gen)
        // TODO endGen은 어떻게 검증해야되지?
        // checkFromNextGen은 처음 생긴 이후로 evolveAcceptCondition할 때 해제되므로 이 시점에는 항상 false임
        assert(!checkFromNextGen)
      case (AcceptCondition.NotExists(beginGen, endGen, symbolId), NotExists(milestone, checkFromNextGen)) =>
        assertEquals(symbolId, milestone.symbolId)
        assertEquals(0, milestone.pointer)
        assertEquals(beginGen, milestone.gen)
        // TODO endGen은 어떻게 검증해야되지?
        // checkFromNextGen은 처음 생긴 이후로 evolveAcceptCondition할 때 해제되므로 이 시점에는 항상 false임
        assert(!checkFromNextGen)
      case (AcceptCondition.OnlyIf(beginGen, endGen, symbolId), OnlyIf(milestone)) =>
        assertEquals(symbolId, milestone.symbolId)
        assertEquals(0, milestone.pointer)
        assertEquals(beginGen, milestone.gen)
        assertEquals(endGen, gen)
      case (AcceptCondition.Unless(beginGen, endGen, symbolId), Unless(milestone)) =>
        assertEquals(symbolId, milestone.symbolId)
        assertEquals(0, milestone.pointer)
        assertEquals(beginGen, milestone.gen)
        assertEquals(endGen, gen)
      case _ =>
      //        assert(false)
    }
  }

  private def assertEqualCtx(
    naiveParser: NaiveParser2,
    naiveCtx: NaiveParser2.ParsingHistoryContext,
    milestoneParser: MilestoneParser,
    milestoneCtx: ParsingContext
  ): Unit = {
    val gen = naiveCtx.gen
    assertEquals(gen, milestoneCtx.gen)
    // TODO naiveCtx.parsingContext.graph에서 존재할 수 있는 모든 milestone path가 milestoneCtx에 포함되는지 확인
    // TODO naiveCtx 그래프에서 accept condition을 처리하기 위해 필요한 모든 path가 milestoneCtx에 포함되는지 확인
    // TODO genActions도 확인

    val (milestonePaths, coveredKernels) = milestonePathsFrom(naiveParser.grammar, naiveCtx.parsingContext.graph, naiveParser.startKernel, gen)
    // println(milestonePaths)

    // TODO 현재는 (* <start>, 0..0) kernel에서 시작돼서 나올 수 있는 milestone path가 milestoneCtx에 모두 포함되는지만 확인하고 있음
    //  TODO 1. accept condition 때문에 추가되어야 하는 path들도 모두 포함되어 있는지 확인
    //  TODO 2. milestone path에 불필요한 path는 없는지 확인

    val pathsMap0 = milestoneCtx.paths.groupBy(_.path)
    val pathsMap = pathsMap0.view.mapValues { paths =>
      MilestoneAcceptCondition.disjunct(paths.map(_.acceptCondition).toSet)
    }.toMap

    val mainPathsFromNaive = milestonePaths.filter(_.last == milestoneParser.initialMilestone).toSet
    val mainPathsFromMile2 = pathsMap.filter(_._1.last == milestoneParser.initialMilestone)
    if (mainPathsFromNaive != mainPathsFromMile2.keySet) {
      println(s":: $gen")
      mainPathsFromNaive.foreach { path =>
        val h = path.head
        val condition = naiveCtx.parsingContext.acceptConditions(Kernel(h.symbolId, h.pointer, path.drop(1).headOption.map(_.gen).getOrElse(0), gen))
        println(s"$path  $condition")
      }
      println("===")
      mainPathsFromMile2.foreach { pair =>
        println(s"${pair._1}  ${pair._2}")
      }
      println("===")
    }
    assert(mainPathsFromNaive == mainPathsFromMile2.keySet)
    assert(milestonePaths.toSet.subsetOf(pathsMap.keySet))
    milestonePaths.foreach { path =>
      val tip = path.head
      val tipKernel = Kernel(tip.symbolId, tip.pointer, path.drop(1).headOption.map(_.gen).getOrElse(0), tip.gen)
      val tipCondition = naiveCtx.parsingContext.acceptConditions(tipKernel)
      assertEqualCondition(tipCondition, pathsMap(path), gen)
    }
  }

  def testEqualityBetweenNaive2AndMilestone(naiveParser: NaiveParser2, milestoneParser: MilestoneParser, grammarTestExample: GrammarTestExample): Unit = {
    println(s"naive2-milestone :: ${grammarTestExample.getName} (length=${grammarTestExample.getExample.length})")
    val inputs = Inputs.fromString(grammarTestExample.getExample)

    // milestoneParser.setVerbose()
    var naive2Ctx = naiveParser.initialParsingHistoryContext
    var milestoneCtx = milestoneParser.initialCtx

    // TODO initial ctx는 왜 다르지..?
    // assertEqualCtx(naive1Ctx, naive2Ctx)
    assertEqualCtx(naiveParser, naive2Ctx, milestoneParser, milestoneCtx)

    inputs.foreach { input =>
      if (naive2Ctx.gen % 100 == 0) {
        println(s"${naive2Ctx.gen}/${inputs.size} $input")
      }
      assertEquals(naive2Ctx.gen, milestoneCtx.gen)
      naive2Ctx = naiveParser.parseStep(naive2Ctx, input).getOrElse(throw new IllegalStateException())
      val milestoneCtx1 = milestoneParser.parseStep(milestoneCtx, input)
      milestoneCtx = milestoneCtx1.getOrElse(throw new IllegalStateException())
      assertEqualCtx(naiveParser, naive2Ctx, milestoneParser, milestoneCtx)
    }

    println("Context equality check ok")

    val naive2KernelsHistory = naiveParser.historyKernels(naive2Ctx).map(Kernels)
    val naive2Trees = new ParseTreeConstructor2(ParseForestFunc)(naiveParser.grammar)(inputs, naive2KernelsHistory).reconstruct().get.trees
    assertEquals(1, naive2Trees.size)

    val milestoneKernelsHistory = milestoneParser.kernelsHistory(milestoneCtx).map(Kernels)
    assert(naive2KernelsHistory.size == milestoneKernelsHistory.size)
    // naive2KernelsHistory와 milestoneKernelsHistory는 다를 수 있는데..
    //    if (naive2KernelsHistory != milestoneKernelsHistory) {
    //      naive2KernelsHistory.zip(milestoneKernelsHistory).zipWithIndex.foreach { case ((naive2, milestone), idx) =>
    //        println(s"$idx:")
    //        println(s"n-m:${(naive2.kernels -- milestone.kernels).toList.sorted}")
    //        println(s"m-n:${(milestone.kernels -- naive2.kernels).toList.sorted}")
    //      }
    //      println("??")
    //    }
    //    assertEquals(naive2KernelsHistory, milestoneKernelsHistory)
    val milestoneTrees = new ParseTreeConstructor2(ParseForestFunc)(naiveParser.grammar)(inputs, milestoneKernelsHistory).reconstruct().get.trees
    assertEquals(naive2Trees.size, milestoneTrees.size)
    assertEquals(naive2Trees, milestoneTrees)
    println("Parse tree equality check ok")
  }

  def test(examples: GrammarWithExamples, parserData: MilestoneParserData): Unit = {
    val naiveParser = new NaiveParser2(parserData.grammar)
    val milestoneParser = new MilestoneParser(parserData)

    examples.getExamples.forEach { example =>
      testEqualityBetweenNaive2AndMilestone(naiveParser, milestoneParser, example)
    }
  }

  def exampleFrom(name: String, input: Inputs.ConcreteSource): GrammarTestExample = {
    val inputString = new String(input.map(_.asInstanceOf[Inputs.Character].char).toArray)
    val exampleName = if (name.isEmpty) inputString else name
    new GrammarTestExample(exampleName, inputString, null)
  }

  "naive examples" should "work" in {
    NaiveExamplesCatalog.grammarWithExamples.foreach { example =>
      println(s"::${example.grammar.name}")
      val naiveParser = new NaiveParser2(example.ngrammar)
      val parserData = new MilestoneParserGen(example.ngrammar).parserData()
      val milestoneParser = new MilestoneParser(parserData)
      example.correctExampleInputs.foreach { input =>
        testEqualityBetweenNaive2AndMilestone(naiveParser, milestoneParser, exampleFrom("", input))
      }
    }
  }

  def loadGeneratedParserAndTest(examples: GrammarWithExamples, resourceName: String): Unit = {
    val parserDataProto = Using(new GZIPInputStream(getClass.getResourceAsStream(resourceName))) {
      MilestoneParserDataProto.Milestone2ParserData.parseFrom(_)
    }.get

    test(examples, MilestoneParser2ProtobufConverter.fromProto(parserDataProto))
  }

  def generateParserAndTest(examples: GrammarWithExamples): Unit = {
    val grammar = MetaLanguage3.analyzeGrammar(examples.getGrammarText).ngrammar

    val parserData = new MilestoneParserGen(grammar).parserData()

    examples.getExamples.forEach { example =>
      testEqualityBetweenNaive2AndMilestone(new NaiveParser2(grammar), new MilestoneParser(parserData), example)
    }
  }

  "json grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getJson)
  }

  "bibix2 grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getBibix2)
  }

  "proto3 grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getProto3)
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

    val parserGen = new MilestoneParserGen(grammar)

    // (124, 4) -> (26, 1) 에 (19, 2, 0..2)가 왜 always로 들어있지..?
    val edgeAction = parserGen.edgeProgressActionBetween(KernelTemplate(124, 4), KernelTemplate(26, 1))
    println(edgeAction)

    val parserData = parserGen.parserData()

    val examples = List("verifiedIdentity != null")
    examples.foreach { example =>
      testEqualityBetweenNaive2AndMilestone(new NaiveParser2(grammar), new MilestoneParser(parserData), new GrammarTestExample("q", example, ""))
    }
  }
}
