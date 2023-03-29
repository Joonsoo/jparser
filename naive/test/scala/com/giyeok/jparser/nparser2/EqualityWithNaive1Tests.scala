package com.giyeok.jparser.nparser2

import com.giyeok.jparser.examples.metalang3.{GrammarTestExample, GrammarWithExamples, MetaLang3ExamplesCatalog}
import com.giyeok.jparser.examples.naive.NaiveExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser._
import com.giyeok.jparser.tests.ParseTreeTestUtils.parseTreeToPath
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.junit.jupiter.api.Assertions.assertEquals
import org.scalatest.flatspec.AnyFlatSpec

// milestone2 파서가 naive 파서와 동일하게 동작하는지 테스트
class EqualityWithNaive2Tests extends AnyFlatSpec {
  private def assertEqualCtx(
    naive1Ctx: Parser.Context,
    naive2Ctx: NaiveParser2.ParsingHistoryContext,
  ): Unit = {
    val kernels = naive1Ctx.nextGraph.nodes.groupBy(_.kernel).view.mapValues { nodes =>
      val conditions = nodes.map(_.condition).toArray
      AcceptCondition.disjunct(conditions: _*)
    }.toMap

    if (kernels.keySet != naive2Ctx.parsingContext.graph.nodes) {
      println(s"nodes mismatch at gen: ${naive1Ctx.gen}")
      println(s"1-2: ${(kernels.keySet -- naive2Ctx.parsingContext.graph.nodes).toList.sorted}")
      println(s"2-1: ${(naive2Ctx.parsingContext.graph.nodes -- kernels.keySet).toList.sorted}")
      println()
    }
    assertEquals(kernels.keySet, naive2Ctx.parsingContext.graph.nodes)
    if (kernels != naive2Ctx.parsingContext.acceptConditions) {
      println(s"condition mismatch at gen: ${naive1Ctx.gen}")
      println(s"1-2: ${(kernels.keySet -- naive2Ctx.parsingContext.acceptConditions.keySet).toList.sorted}")
      println(s"2-1: ${(naive2Ctx.parsingContext.acceptConditions.keySet -- kernels.keySet).toList.sorted}")
      (kernels.keySet ++ naive2Ctx.parsingContext.acceptConditions.keySet).foreach { kernel =>
        val cond1 = kernels(kernel)
        val cond2 = naive2Ctx.parsingContext.acceptConditions(kernel)
        if (cond1 != cond2) {
          println(s"$kernel:")
          println(s"1: $cond1")
          println(s"2: $cond2")
        }
      }
      println()
    }
    assertEquals(kernels, naive2Ctx.parsingContext.acceptConditions)
  }

  // naive1 parser, naive2 parser 동일성 확인
  def testEqualityBetweenNaive1And2(grammar: NGrammar, grammarTestExample: GrammarTestExample): Unit = {
    println(s"naive1-naive2 :: ${grammarTestExample.getName} (length=${grammarTestExample.getExample.length})")
    val inputs = Inputs.fromString(grammarTestExample.getExample)

    val naive1Parser = new NaiveParser(grammar)
    val naive2Parser = new NaiveParser2(grammar)
    var naive1Ctx = naive1Parser.initialContext
    var naive2Ctx = naive2Parser.initialParsingHistoryContext

    // TODO initial ctx는 왜 다르지..?
    // assertEqualCtx(naive1Ctx, naive2Ctx)

    inputs.foreach { input =>
      if (naive2Ctx.gen % 100 == 0) {
        println(s"${naive2Ctx.gen}/${inputs.size} $input")
      }
      assertEquals(naive1Ctx.gen, naive2Ctx.gen)
      naive1Ctx = naive1Parser.proceed(naive1Ctx, input).swap.getOrElse(throw new IllegalStateException())
      naive2Ctx = naive2Parser.parseStep(naive2Ctx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naive1Ctx, naive2Ctx)
    }

    val naive1Trees = new ParseTreeConstructor(ParseForestFunc)(naive2Parser.grammar)(inputs, naive1Ctx.history, naive1Ctx.conditionFinal).reconstruct().get.trees
    val naive2Trees = new ParseTreeConstructor2(ParseForestFunc)(naive2Parser.grammar)(inputs, naive2Parser.historyKernels(naive2Ctx).map(Kernels)).reconstruct().get.trees
    assertEquals(1, naive1Trees.size)
    if (naive1Trees != naive2Trees) {
      val naive1Tree = naive1Trees.head
      naive2Trees.foreach { tree =>
        println(s"${parseTreeToPath(tree)} ${tree == naive1Tree}")
      }
    }
    assertEquals(naive1Trees, naive2Trees)
  }

  def exampleFrom(name: String, input: Inputs.ConcreteSource): GrammarTestExample = {
    val inputString = new String(input.map(_.asInstanceOf[Inputs.Character].char).toArray)
    val exampleName = if (name.isEmpty) inputString else name
    new GrammarTestExample(exampleName, inputString, null)
  }

  "naive examples" should "work" in {
    NaiveExamplesCatalog.grammarWithExamples.foreach { example =>
      example.correctExampleInputs.foreach { input =>
        testEqualityBetweenNaive1And2(example.ngrammar, exampleFrom("", input))
      }
    }
  }

  def test(examples: GrammarWithExamples): Unit = {
    val grammar = MetaLanguage3.analyzeGrammar(examples.getGrammarText).ngrammar

    examples.getExamples.forEach { example =>
      testEqualityBetweenNaive1And2(grammar, example)
    }
  }

  "json grammar" should "work" in {
    test(MetaLang3ExamplesCatalog.INSTANCE.getJson)
  }

  "bibix2 grammar" should "work" in {
    test(MetaLang3ExamplesCatalog.INSTANCE.getBibix2)
  }

  "proto3 grammar" should "work" in {
    test(MetaLang3ExamplesCatalog.INSTANCE.getProto3)
  }
}
