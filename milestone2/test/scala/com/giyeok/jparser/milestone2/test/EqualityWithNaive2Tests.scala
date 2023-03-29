package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.examples.metalang3.{GrammarWithExamples, MetaLang3ExamplesCatalog}
import com.giyeok.jparser.examples.naive.NaiveExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2._
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.milestone2.test.AcceptConditionOrderings.{mileestoneAcceptConditionOrdering, naiveAcceptConditionOrdering}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser.{ParsingContext, _}
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
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
    naive1Ctx: Parser.Context,
    naiveCtx: NaiveParser2.ParsingHistoryContext,
  ): Unit = {
    val kernels = naive1Ctx.nextGraph.nodes.groupBy(_.kernel).view.mapValues { nodes =>
      val conditions = nodes.map(_.condition).toArray
      AcceptCondition.disjunct(conditions: _*)
    }.toMap

    if (kernels.keySet != naiveCtx.parsingContext.graph.nodes) {
      println(s"1-2: ${(kernels.keySet -- naiveCtx.parsingContext.graph.nodes).toList.sorted}")
      println(s"2-1: ${(naiveCtx.parsingContext.graph.nodes -- kernels.keySet).toList.sorted}")
      println()
    }
    assertEquals(kernels.keySet, naiveCtx.parsingContext.graph.nodes)
    assertEquals(kernels, naiveCtx.parsingContext.acceptConditions)
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
    val pathsMap = pathsMap0.view.mapValues(paths => MilestoneAcceptCondition.disjunct(paths.map(_.acceptCondition).toSet)).toMap

    assert(milestonePaths.toSet.subsetOf(pathsMap.keySet))
    milestonePaths.foreach { path =>
      val tip = path.head
      val tipKernel = Kernel(tip.symbolId, tip.pointer, path.drop(1).headOption.map(_.gen).getOrElse(0), tip.gen)
      val tipCondition = naiveCtx.parsingContext.acceptConditions(tipKernel)
      assertEqualCondition(tipCondition, pathsMap(path), gen)
    }
  }

  // naive1 parser, naive parser, milestone parser의 동일성 확인
  def testEquality(naiveParser: NaiveParser2, milestoneParser: MilestoneParser, inputs: List[Inputs.Input]): Unit = {
    val naive1Parser = new NaiveParser(naiveParser.grammar)
    var naive1Ctx = naive1Parser.initialContext
    var naiveCtx = naiveParser.initialParsingHistoryContext
    var milestoneCtx = milestoneParser.initialCtx

    assertEqualCtx(naiveParser, naiveCtx, milestoneParser, milestoneCtx)

    inputs.foreach { input =>
      // println(s"${naiveCtx.gen}/${inputs.size} $input")
      assertEquals(naive1Ctx.gen, naiveCtx.gen)
      assertEquals(naiveCtx.gen, milestoneCtx.gen)
      naive1Ctx = naive1Parser.proceed(naive1Ctx, input).swap.getOrElse(throw new IllegalStateException())
      naiveCtx = naiveParser.parseStep(naiveCtx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naive1Ctx, naiveCtx)

      milestoneCtx = milestoneParser.parseStep(milestoneCtx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naiveParser, naiveCtx, milestoneParser, milestoneCtx)
    }

    val naive1Trees = new ParseTreeConstructor(ParseForestFunc)(naiveParser.grammar)(inputs, naive1Ctx.history, naive1Ctx.conditionFinal).reconstruct().get.trees
    val naive2KernelsHistory = naiveCtx.historyKernels.map(Kernels).toVector
    val naive2Trees = new ParseTreeConstructor2(ParseForestFunc)(naiveParser.grammar)(inputs, naive2KernelsHistory).reconstruct().get.trees
    assertEquals(1, naive1Trees.size)
    assertEquals(naive1Trees, naive2Trees)

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
    assertEquals(naive2Trees, milestoneTrees)
  }

  def test(examples: GrammarWithExamples, parserData: MilestoneParserData): Unit = {
    val naiveParser = new NaiveParser2(parserData.grammar)
    val milestoneParser = new MilestoneParser(parserData)

    examples.getExamples.forEach { example =>
      println(":: " + example.getName)
      testEquality(naiveParser, milestoneParser, Inputs.fromString(example.getExample))
    }
  }

  "naive examples" should "work" in {
    NaiveExamplesCatalog.grammarWithExamples.foreach { example =>
      val parserData = new MilestoneParserGen(example.ngrammar).parserData()
      val milestoneParser = new MilestoneParser(parserData)

      val naiveParser = new NaiveParser2(example.ngrammar)

      example.correctExampleInputs.foreach { input =>
        testEquality(naiveParser, milestoneParser, input.toList)
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
    val analysis = MetaLanguage3.analyzeGrammar(examples.getGrammarText)
    val parserData = new MilestoneParserGen(analysis.ngrammar).parserData()

    test(examples, parserData)
  }

  "json grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getJson)
  }

  "bibix2 grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getBibix2) //, "/bibix2-m2-parserdata.pb.gz")
  }

  "proto3 grammar" should "work" in {
    generateParserAndTest(MetaLang3ExamplesCatalog.INSTANCE.getProto3)
  }
}
