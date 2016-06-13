package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.tests.AmbiguousSamples

object MyPaper1 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
    val name = "MyPaper Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("N").plus),
        "N" -> ListSet(
            n("A"),
            n("B")),
        "A" -> ListSet(
            c('a').plus),
        "B" -> ListSet(
            c('b')))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("aab")
}

object Earley1970AE extends Grammar with GrammarTestCases with StringSamples {
    val name = "Earley 1970 Grammar AE"
    val rules: RuleMap = ListMap(
        "E" -> ListSet(
            n("T"),
            seq(n("E"), c('+'), n("T"))),
        "T" -> ListSet(
            n("P"),
            seq(n("T"), c('*'), n("P"))),
        "P" -> ListSet(
            c('a')))
    val startSymbol = n("E")

    val grammar = this
    val correctSamples = Set[String](
        "a+a*a")
    val incorrectSamples = Set[String]()
}

object PaperTests {
    val tests: Set[GrammarTestCases] = Set(
        MyPaper1,
        Earley1970AE)
}

class PaperTestSuite extends BasicParseTest(PaperTests.tests)

// grammars in Parsing Techniques book (http://dickgrune.com/Books/PTAPG_1st_Edition/BookBody.pdf)

object Fig7_4 extends Grammar with GrammarTestCases with StringSamples with AmbiguousSamples {
    val name = "Parsing Techniques Grammar in Figure 7.4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a'), n("S"), c('b')),
            seq(n("S"), c('a'), c('b')),
            seq(i("aaa"))))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]("aaaab")
}

object Fig7_8 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.8"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("E")),
        "E" -> ListSet(
            seq(n("E"), n("Q"), n("F")),
            n("F")),
        "F" -> ListSet(
            c('a')),
        "Q" -> ListSet(
            c('+'),
            c('-')))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "a+a",
        "a-a+a")
    val incorrectSamples = Set[String]()
}

object Fig7_19 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Parsing Techniques Grammar in Figure 7.19"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A"),
            seq(n("A"), n("B")),
            n("B")),
        "A" -> ListSet(
            n("C")),
        "B" -> ListSet(
            n("D")),
        "C" -> ListSet(
            c('p')),
        "D" -> ListSet(
            c('q')))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "p",
        "q",
        "pq")
    val incorrectSamples = Set[String]()
}

object ParsingTechniquesTests {
    val tests: Set[GrammarTestCases] = Set(
        Fig7_4,
        Fig7_8,
        Fig7_19)
}

class ParsingTechniquesTestSuite extends BasicParseTest(ParsingTechniquesTests.tests)
