package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListMap
import com.giyeok.jparser.Inputs._
import org.junit.Assert._
import scala.collection.immutable.ListSet
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import com.giyeok.jparser.tests.GrammarTestCases

object ExceptGrammar1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abc", "abbbc")
    val incorrectSamples = Set("a", "abbc")
}

object ExceptGrammar1_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("C").plus),
        "C" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abc", "abbbc", "abbbcac", "abbbcabbbc")
    val incorrectSamples = Set("a", "abbc", "abbbcabbc")
}

object ExceptGrammar1_2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"), i("abbb")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abc", "abbbbc")
    val incorrectSamples = Set("a", "abbc", "abbbc")
}

object ExceptGrammar1_3 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(seq(i("a"), i("b").repeat(4, 7))))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("ac", "abc", "abbc", "abbbc", "abbbbbbbbc")
    val incorrectSamples = Set("a", "abbbbc", "abbbbbc", "abbbbbbc", "abbbbbbbc")
}

object ExceptGrammar1_4 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")))),
        "A" -> ListSet(seq(c('a'), c('b').repeat(2, 4))),
        "B" -> ListSet(seq(i("a"), i("b").star)))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set("a", "ab", "abb", "abbb", "abbbb", "abbbbb")
}

object ExceptGrammar2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c')), i("abbc")),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abc", "abbbc", "abbc")
    val incorrectSamples = Set("a")
}

object ExceptGrammar3_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 3-1 (except with lookahead_is)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), chars('0' to '9').plus)),
        "A" -> ListSet(chars('a' to 'z').star),
        "B" -> ListSet(seq(i("abc"), lookahead_is(c('0')))))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]("abc0")
}

object ExceptGrammar4_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 4_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(chars('a' to 'z').repeat(0, 5)).except(c('a').star)))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abcd", "aaaab", "baaaa")
    val incorrectSamples = Set("a", "aaaaa", "aaaaaaaaa", "")
}

object ExceptGrammar4_2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "Except Grammar 4_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("N"), i("r")),
            seq(n("N"), i("ch"))),
        "N" -> ListSet(
            n("I").except(n("K"))),
        "I" -> ListSet(
            chars('a' to 'z').star),
        "K" -> ListSet(
            i("for"),
            i("foreach")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set("abbbbr", "aaaabch", "baaaach", "for", "foreach")
    val incorrectSamples = Set[String]()
}

object GrammarWithExcept {
    val tests: Set[GrammarTestCases] = Set(
        ExceptGrammar1,
        ExceptGrammar1_1,
        ExceptGrammar1_2,
        ExceptGrammar1_3,
        ExceptGrammar1_4,
        ExceptGrammar2,
        ExceptGrammar3_1,
        ExceptGrammar4_1,
        ExceptGrammar4_2)
}

class ExceptGrammarTestSuite1 extends BasicParseTest(GrammarWithExcept.tests)
