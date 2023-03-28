package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.NGrammar.NSequence
import com.giyeok.jparser.{Inputs, NGrammar}
import com.giyeok.jparser.examples.metalang3.{GrammarWithExamples, MetaLang3ExamplesCatalog}
import com.giyeok.jparser.examples.naive.NaiveExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.mgroup2.proto.MilestoneGroupParserDataProto
import com.giyeok.jparser.milestone2.proto.MilestoneParserDataProto
import com.giyeok.jparser.milestone2.{Always, And, Exists, Milestone, MilestoneAcceptCondition, MilestoneParser, MilestoneParser2ProtobufConverter, MilestoneParserGen, MilestonePath, Never, NotExists, OnlyIf, Or, ParsingContext, Unless}
import com.giyeok.jparser.nparser.{AcceptCondition, Kernel, NaiveParser, Parser}
import com.giyeok.jparser.nparser2.{KernelGraph, NaiveParser2}
import org.junit.jupiter.api.Assertions.assertEquals
import org.scalatest.flatspec.AnyFlatSpec
import scala.math.Ordering.comparatorToOrdering

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

  // condition 정렬 기준 구현. assertEqualCondition에서 비교하기 위한 것
  implicit val naiveAcceptConditionOrdering: Ordering[AcceptCondition.AcceptCondition] = comparatorToOrdering {
    (o1: AcceptCondition.AcceptCondition, o2: AcceptCondition.AcceptCondition) => ???
  }

  implicit val mileestoneAcceptConditionOrdering: Ordering[MilestoneAcceptCondition] = comparatorToOrdering {
    (o1: MilestoneAcceptCondition, o2: MilestoneAcceptCondition) => ???
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
      println(s"${naiveCtx.gen}/${inputs.size} $input")
      assertEquals(naive1Ctx.gen, naiveCtx.gen)
      assertEquals(naiveCtx.gen, milestoneCtx.gen)
      naive1Ctx = naive1Parser.proceed(naive1Ctx, input).swap.getOrElse(throw new IllegalStateException())
      naiveCtx = naiveParser.parseStep(naiveCtx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naive1Ctx, naiveCtx)

      milestoneCtx = milestoneParser.parseStep(milestoneCtx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naiveParser, naiveCtx, milestoneParser, milestoneCtx)
    }
  }

  def test(examples: GrammarWithExamples, parserDataOpt: Option[MilestoneParserDataProto.Milestone2ParserData] = None): Unit = {
    val (naiveParser: NaiveParser2, milestoneParser: MilestoneParser) =
      parserDataOpt match {
        case Some(parserDataProto) =>
          val parserData = MilestoneParser2ProtobufConverter.fromProto(parserDataProto)
          (new NaiveParser2(parserData.grammar), new MilestoneParser(parserData))
        case None =>
          val analysis = MetaLanguage3.analyzeGrammar(examples.getGrammarText)
          val parserData = new MilestoneParserGen(analysis.ngrammar).parserData()
          (new NaiveParser2(analysis.ngrammar), new MilestoneParser(parserData))
      }

    examples.getExamples.forEach { example =>
      println(":: " + example.getName)
      if (example.getName.contains("jparser-small.bbx")) {
        testEquality(naiveParser, milestoneParser, Inputs.fromString(example.getExample))
      }
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

  "json grammar" should "work" in {
    test(MetaLang3ExamplesCatalog.INSTANCE.getJson)
  }

  "bibix2 grammar" should "work" in {
    val parserData = Using(new GZIPInputStream(getClass.getResourceAsStream("/bibix2-m2-parserdata.pb.gz"))) {
      MilestoneParserDataProto.Milestone2ParserData.parseFrom(_)
    }.get
    test(MetaLang3ExamplesCatalog.INSTANCE.getBibix2, Some(parserData))
  }

  "proto3 grammar" should "work" in {
    test(MetaLang3ExamplesCatalog.INSTANCE.getProto3)
  }
}
