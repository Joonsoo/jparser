package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.examples.metalang3.{Catalog, GrammarWithExamples}
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{MilestoneParser, MilestoneParserGen, ParsingContext}
import com.giyeok.jparser.nparser2.NaiveParser2
import org.scalatest.flatspec.AnyFlatSpec

// milestone2 파서가 naive 파서와 동일하게 동작하는지 테스트
class EqualityWithNaive2Tests extends AnyFlatSpec {
  private def assertEqualCtx(naiveCtx: NaiveParser2.ParsingHistoryContext, milestoneCtx: ParsingContext): Unit = {
    // TODO
  }

  def testEquality(naiveParser: NaiveParser2, milestoneParser: MilestoneParser, inputs: List[Inputs.Input]): Unit = {
    var naiveCtx = naiveParser.initialParsingHistoryContext
    var milestoneCtx = milestoneParser.initialCtx

    assertEqualCtx(naiveCtx, milestoneCtx)

    inputs.foreach { input =>
      naiveCtx = naiveParser.parseStep(naiveCtx, input).getOrElse(throw new IllegalStateException())
      milestoneCtx = milestoneParser.parseStep(milestoneCtx, input).getOrElse(throw new IllegalStateException())

      assertEqualCtx(naiveCtx, milestoneCtx)
    }
  }

  def test(examples: GrammarWithExamples): Unit = {

    val analysis = MetaLanguage3.analyzeGrammar(examples.getGrammarText)
    val parserData = new MilestoneParserGen(analysis.ngrammar).parserData()
    val milestoneParser = new MilestoneParser(parserData)

    val naiveParser = new NaiveParser2(analysis.ngrammar)

    examples.getExamples.forEach { example =>
      testEquality(naiveParser, milestoneParser, Inputs.fromString(example.getExample))
    }
  }

  "json grammar" should "work" in {
    test(Catalog.INSTANCE.getJson)
  }

  "proto3 grammar" should "work" in {
    test(Catalog.INSTANCE.getProto3)
  }
}
