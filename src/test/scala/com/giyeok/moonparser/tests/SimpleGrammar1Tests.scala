package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._
import com.giyeok.moonparser.Symbols.Symbol

object SimpleGrammar1 extends Grammar {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A")),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")

    val samples = Set("abc")
}

object SimpleGrammar1_1 extends Grammar {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A"), n("B"))),
        "A" -> Set(chars("abc").repeat(2)),
        "B" -> Set(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")

    val samples = Set("abcabcddfefefes")
}

object SimpleGrammar1_2 extends Grammar {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> Set(i("abc")),
        "B" -> Set(i("bc")))
    val startSymbol = n("S")
}

object SimpleGrammar1_3 extends Grammar {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c'))))
    val startSymbol = n("S")

    val correctSamples = Set("ac", "abc", "abbbbbbbc")
}

object SimpleGrammar1_3_2 extends Grammar {
    val name = "Simple Grammar 1_3_2"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c').opt)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abb")
}

object SimpleGrammar1_3_3 extends Grammar {
    // ambiguous language
    val name = "Simple Grammar 1_3_3"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c').opt, c('b').star)))
    val startSymbol = n("S")

    val correctSamples = Set("a", "abc", "abbbcbbb")
}

object SimpleGrammar1_4 extends Grammar {
    val name = "Simple Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(Seq[Symbol](i("ab"), i("qt").opt, i("cd")), Set[Symbol](chars(" \t\n")))))
    val startSymbol = n("S")

    val correctSamples = Set("ab   \tqt\t  cd", "abcd", "ab  cd", "abqtcd")
    val incorrectSamples = Set("a  bcd", "abc  d")
}

object SimpleGrammar1_5 extends Grammar {
    val name = "Simple Grammar 1_5"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> Set(i("abc")),
        "B" -> Set(i("ab")))
    val startSymbol = n("S")

    val correctSamples = Set("abcabababc", "abcabababcabc")
}
