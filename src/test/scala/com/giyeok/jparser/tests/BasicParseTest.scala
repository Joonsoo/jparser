package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs
import org.scalatest.FlatSpec
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.NewParser

class BasicParseTest(val grammars: Traversable[Grammar with Samples]) extends FlatSpec {
    def log(s: String): Unit = {
        // println(s)
    }

    private def testCorrect(grammar: Grammar)(source: Inputs.ConcreteSource) = {
        val result = new NewParser(grammar).parse(source)
        it should s"${grammar.name} properly parsed on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
                    if (ctx.results.size != 1) {
                        ctx.results.zipWithIndex foreach { result =>
                            log(s"=== ${result._2} ===")
                            log(result._1.toHorizontalHierarchyString)
                        }
                    }
                    assert(ctx.results.size == 1)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def testIncorrect(grammar: Grammar)(source: Inputs.ConcreteSource) = {
        val result = new NewParser(grammar).parse(source)
        it should s"${grammar.name} failed to parse on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.results.isEmpty)
                case Right(_) => assert(true)
            }
        }
    }

    private def testAmbiguous(grammar: Grammar)(source: Inputs.ConcreteSource) = {
        log(s"Testing ${grammar.name} on '${source.toCleanString}'")
        val result = new NewParser(grammar).parse(source)
        log("  - Parsing Done")
        it should s"${grammar.name} is ambiguous on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.results.size > 1)
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
