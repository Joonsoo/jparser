package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import com.giyeok.moonparser.Symbols.Symbol
import com.giyeok.moonparser.Inputs
import org.scalatest.FlatSpec

object SimpleGrammar1 extends Grammar with StringSamples {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A")),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("abc")
    val incorrectSamples = Set("a")
}

object SimpleGrammar1_1 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A"), n("B"))),
        "A" -> Set(chars("abc").repeat(2)),
        "B" -> Set(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")

    val correctSamples = Set("abcabcddfefefes")
    val incorrectSamples = Set("abcdefs")
}

object SimpleGrammar1_2 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> Set(i("abc")),
        "B" -> Set(i("bc")))
    val startSymbol = n("S")

    val correctSamples = Set("bcbcbc", "abcbcbcabc")
    val incorrectSamples = Set("abc")
}

object SimpleGrammar1_3 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c'))))
    val startSymbol = n("S")

    val correctSamples = Set("ac", "abc", "abbbbbbbc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_3_2 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_3_2"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c').opt)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abb")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_3_3 extends Grammar with StringSamples {
    // ambiguous language
    val name = "Simple Grammar 1_3_3"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c').opt, c('b').star)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abbbcbbb")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_4 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(Seq[Symbol](i("ab"), i("qt").opt, i("cd")), Set[Symbol](chars(" \t\n")))))
    val startSymbol = n("S")

    val correctSamples = Set("ab   \tqt\t  cd", "abcd", "ab  cd", "abqtcd")
    val incorrectSamples = Set("a  bcd", "abc  d")
}

object SimpleGrammar1_5 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_5"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> Set(i("abc")),
        "B" -> Set(i("ab")))
    val startSymbol = n("S")

    val correctSamples = Set("abcabababc", "abcabababcabc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_6 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_6"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A"), e),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("", "abc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_7 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_7"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A").opt),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("", "abc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_8 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_8"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), n("A").opt)),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("a", "aabc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammarSet1 {
    val grammars: Set[Grammar with Samples] = Set(
        SimpleGrammar1,
        SimpleGrammar1_1,
        SimpleGrammar1_2,
        SimpleGrammar1_3,
        SimpleGrammar1_3_2,
        SimpleGrammar1_3_3,
        SimpleGrammar1_4,
        SimpleGrammar1_5,
        SimpleGrammar1_6,
        SimpleGrammar1_7,
        SimpleGrammar1_8)
}

class SimpleGrammar1TestSuite extends FlatSpec {
    private def testCorrect(grammar: Grammar, source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should "Properly parsed" in {
            assert(result.isLeft)
        }
        val ctx = result.left.get
        it should "Not ambiguous" in {
            assert(ctx.resultCandidates.size == 1)
        }
    }

    private def testIncorrect(grammar: Grammar, source: Inputs.Source) = {
        val result = new Parser(grammar).parse(source)
        it should "Failed to parse" in {
            assert(result.isRight)
        }
    }

    SimpleGrammarSet1.grammars foreach { grammar =>
        grammar.correctSampleInputs foreach { input =>
            testCorrect(grammar, input)
        }
        grammar.incorrectSampleInputs foreach { input =>
            testIncorrect(grammar, input)
        }
    }
}
