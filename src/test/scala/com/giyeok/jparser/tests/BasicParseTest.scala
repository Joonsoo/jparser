package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs
import org.scalatest.FlatSpec
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.NewParser
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.DerivationSliceFunc

class BasicParseTest(val testsSuite: Traversable[GrammarTestCases]) extends FlatSpec {
    def log(s: String): Unit = {
        // println(s)
    }

    private def testCorrect(test: GrammarTestCases)(source: Inputs.ConcreteSource) = {
        val result = test.parser.parse(source)
        it should s"${test.grammar.name} properly parsed on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
                    assert(ctx.result.isDefined)
                    val trees = ctx.result.get.trees
                    if (trees.size != 1) {
                        trees.zipWithIndex foreach { result =>
                            log(s"=== ${result._2} ===")
                            log(result._1.toHorizontalHierarchyString)
                        }
                    }
                    assert(trees.size == 1)
                case Right(error) => fail(error.msg)
            }
        }
    }

    private def testIncorrect(tests: GrammarTestCases)(source: Inputs.ConcreteSource) = {
        val result = tests.parser.parse(source)
        it should s"${tests.grammar.name} failed to parse on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.result.isEmpty)
                case Right(_) => assert(true)
            }
        }
    }

    private def testAmbiguous(tests: GrammarTestCases)(source: Inputs.ConcreteSource) = {
        log(s"Testing ${tests.grammar.name} on '${source.toCleanString}'")
        val result = tests.parser.parse(source)
        log("  - Parsing Done")
        it should s"${tests.grammar.name} is ambiguous on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) =>
                    assert(ctx.result.isDefined)
                    val trees = ctx.result.get.trees
                    if (trees.size != 1) {
                        trees.zipWithIndex foreach { result =>
                            log(s"=== ${result._2} ===")
                            log(result._1.toHorizontalHierarchyString)
                        }
                    }
                    assert(trees.size > 1)
                case Right(_) => assert(false)
            }
        }
    }

    testsSuite foreach { test =>
        test.correctSampleInputs foreach { testCorrect(test) }
        test.incorrectSampleInputs foreach { testIncorrect(test) }
        if (test.isInstanceOf[AmbiguousSamples]) test.asInstanceOf[AmbiguousSamples].ambiguousSampleInputs foreach { testAmbiguous(test) }
    }
}
