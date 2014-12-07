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

    val correctSamples = Set("abc")
    val incorrectSamples = Set("a")
}

object GrammarWithExcept {
    val grammars: Set[Grammar with Samples] = Set(
        ExceptGrammar1)
}
