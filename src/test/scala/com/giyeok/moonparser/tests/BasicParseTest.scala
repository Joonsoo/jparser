package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Parser
import org.scalatest.FlatSpec
import com.giyeok.moonparser.Grammar

class BasicParseTest(val grammars: Traversable[Grammar with Samples]) extends FlatSpec {
    private def testCorrect(grammar: Grammar)(source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should s"${grammar.name} properly parsed on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
//                    if (ctx.resultCandidates.size != 1) {
//                        ctx.resultCandidates.zipWithIndex foreach { result =>
//                            println(s"=== ${result._2} ===")
//                            println(result._1.parsed.get.toHorizontalHierarchyString)
//                        }
//                    }
                    assert(ctx.resultCandidates.size == 1)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def testIncorrect(grammar: Grammar)(source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should s"${grammar.name} failed to parse on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.resultCandidates.isEmpty)
                case Right(_) => assert(true)
            }
        }
    }

    private def testAmbiguous(grammar: Grammar)(source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should s"${grammar.name} is ambiguous on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.resultCandidates.size > 1)
                case Right(_) => assert(false)
            }
        }
    }

    grammars foreach { grammar =>
        grammar.correctSampleInputs foreach { testCorrect(grammar) }
        grammar.incorrectSampleInputs foreach { testIncorrect(grammar) }
        if (grammar.isInstanceOf[AmbiguousSamples]) grammar.asInstanceOf[AmbiguousSamples].ambiguousSampleInputs foreach { testAmbiguous(grammar) }
    }
}
