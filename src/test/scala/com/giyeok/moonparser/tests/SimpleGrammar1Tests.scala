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
import scala.collection.immutable.ListSet

object SimpleGrammar1 extends Grammar with StringSamples {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A")),
        "A" -> ListSet(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("abc")
    val incorrectSamples = Set("a")
}

object SimpleGrammar1_1 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("B"))),
        "A" -> ListSet(chars("abc").repeat(2)),
        "B" -> ListSet(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")

    val correctSamples = Set("abcabcddfefefes")
    val incorrectSamples = Set("abds")
}

object SimpleGrammar1_2 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> ListSet(i("abc")),
        "B" -> ListSet(i("bc")))
    val startSymbol = n("S")

    val correctSamples = Set("bcbcbc", "abcbcbcabc")
    val incorrectSamples = Set("abc")
}

object SimpleGrammar1_3 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c'))))
    val startSymbol = n("S")

    val correctSamples = Set("ac", "abc", "abbbbbbbc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_3_2 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_3_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c').opt)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abb")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_3_3 extends Grammar with StringSamples {
    // ambiguous language
    val name = "Simple Grammar 1_3_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c').opt, c('b').star)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abbbcbbb")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_4 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(Seq[Symbol](i("ab"), i("qt").opt, i("cd")), Set[Symbol](chars(" \t\n")))))
    val startSymbol = n("S")

    val correctSamples = Set("ab   \tqt\t  cd", "abcd", "ab  cd", "abqtcd")
    val incorrectSamples = Set("a  bcd", "abc  d")
}

object SimpleGrammar1_5 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> ListSet(i("abc")),
        "B" -> ListSet(i("ab")))
    val startSymbol = n("S")

    val correctSamples = Set("abcabababc", "abcabababcabc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_6 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A"), e),
        "A" -> ListSet(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("", "abc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_7 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_7"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").opt),
        "A" -> ListSet(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("", "abc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_8 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_8"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), n("A").opt)),
        "A" -> ListSet(i("abc")))
    val startSymbol = n("S")

    val correctSamples = Set("a", "aabc")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_9 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_9"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(chars("abcefgijkxyz")).opt))
    val startSymbol = n("S")

    val correctSamples = Set("a", "x")
    val incorrectSamples = Set[String]()
}

object SimpleGrammar1_10 extends Grammar with StringSamples {
    val name = "Simple Grammar 1_10"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(seq(chars('a' to 'z').plus), chars(" ")))
    val startSymbol = n("S")

    val correctSamples = Set[String]("asdfasdf   asdfasdf")
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
        SimpleGrammar1_8,
        SimpleGrammar1_9)
    // SimpleGrammar1_10        // Infinite loop
}

class SimpleGrammar1TestSuite extends BasicParseTest(SimpleGrammarSet1.grammars)
