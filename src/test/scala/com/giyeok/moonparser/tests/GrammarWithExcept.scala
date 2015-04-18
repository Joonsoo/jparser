package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._
import scala.collection.immutable.ListSet

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
        "B" -> ListSet(seq(i("a"), i("b").repeat(4,7))))
    val startSymbol = n("S")

    val correctSamples = Set("ac", "abc", "abbc", "abbbc", "abbbbbbbbc")
    val incorrectSamples = Set("a", "abbbbc", "abbbbbc", "abbbbbbc", "abbbbbbbc")
}

object GrammarWithExcept {
    val grammars: Set[Grammar with Samples] = Set(
        ExceptGrammar1,
        ExceptGrammar1_1,
        ExceptGrammar1_2,
        ExceptGrammar1_3)
}

class GrammarWithExceptTestSuite extends BasicParseTest(GrammarWithExcept.grammars)
