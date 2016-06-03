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

object ExceptGrammar1 extends Grammar with StringSamples {
    val name = "Except Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val correctSamples = Set("abc", "abbbc")
    val incorrectSamples = Set("a", "abbc")
}

object ExceptGrammar1_1 extends Grammar with StringSamples {
    val name = "Except Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("C").plus),
        "C" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val correctSamples = Set("abc", "abbbc", "abbbcac", "abbbcabbbc")
    val incorrectSamples = Set("a", "abbc", "abbbcabbc")
}

object ExceptGrammar1_2 extends Grammar with StringSamples {
    val name = "Except Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"), i("abbb")))
    val startSymbol = n("S")

    val correctSamples = Set("abc", "abbbbc")
    val incorrectSamples = Set("a", "abbc", "abbbc")
}

object ExceptGrammar1_3 extends Grammar with StringSamples {
    val name = "Except Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(seq(i("a"), i("b").repeat(4, 7))))
    val startSymbol = n("S")

    val correctSamples = Set("ac", "abc", "abbc", "abbbc", "abbbbbbbbc")
    val incorrectSamples = Set("a", "abbbbc", "abbbbbc", "abbbbbbc", "abbbbbbbc")
}

object ExceptGrammar2 extends Grammar with StringSamples {
    val name = "Except Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c')), i("abbc")),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb")))
    val startSymbol = n("S")

    val correctSamples = Set("abc", "abbbc", "abbc")
    val incorrectSamples = Set("a")
}

object ExceptGrammar3_1 extends Grammar with StringSamples {
    val name = "Except Grammar 3-1 (except with lookahead_is)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), chars('0' to '9'))),
        "A" -> ListSet(chars('a' to 'z').star),
        "B" -> ListSet(seq(i("abc"), lookahead_is(c('0')))))
    val startSymbol = n("S")

    val correctSamples = Set[String]("abc0")
    val incorrectSamples = Set[String]()
}

object ExceptGrammar3_2 extends Grammar with StringSamples {
    val name = "Except Grammar 3-2 (except with lookahead_except)"
    val rules: RuleMap = ListMap("S" -> ListSet() /* TODO */ )
    val startSymbol = n("S")

    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object ExceptGrammar3_3 extends Grammar with StringSamples {
    val name = "Except Grammar 3-3 (except with longest match)"
    val rules: RuleMap = ListMap("S" -> ListSet() /* TODO */ )
    val startSymbol = n("S")

    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object ExceptGrammar3_4 extends Grammar with StringSamples {
    val name = "Except Grammar 3-4 (except with eager longest match)"
    val rules: RuleMap = ListMap("S" -> ListSet() /* TODO */ )
    val startSymbol = n("S")

    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object GrammarWithExcept {
    val grammars: Set[Grammar with Samples] = Set(
        ExceptGrammar1,
        ExceptGrammar1_1,
        ExceptGrammar1_2,
        ExceptGrammar1_3,
        ExceptGrammar2,
        ExceptGrammar3_1,
        ExceptGrammar3_2,
        ExceptGrammar3_3,
        ExceptGrammar3_4)
}

class ExceptGrammarTestSuite1 extends BasicParseTest(GrammarWithExcept.grammars)
