package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Inputs
import com.giyeok.moonparser.Parser
import org.scalatest.FlatSpec
import com.giyeok.moonparser.Grammar

class BasicParseTest(val grammars: Traversable[Grammar with Samples]) extends FlatSpec {
    private def testCorrect(grammar: Grammar, source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should s"${grammar.name} properly parsed on '${source.toCleanString}'" in {
            if (result.isLeft) {
                assert(result.left.get.resultCandidates.size == 1)
            } else {
                fail(result.right.get.msg)
            }
        }
    }

    private def testIncorrect(grammar: Grammar, source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should s"${grammar.name} failed to parse on '${source.toCleanString}'" in {
            result match {
                case Left(ctx) => assert(ctx.resultCandidates.isEmpty)
                case Right(_) => assert(true)
            }
        }
    }

    grammars foreach { grammar =>
        grammar.correctSampleInputs foreach { input =>
            testCorrect(grammar, input)
        }
        grammar.incorrectSampleInputs foreach { input =>
            testIncorrect(grammar, input)
        }
    }
}