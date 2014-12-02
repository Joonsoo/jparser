package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._

object SimpleGrammar1_1 extends Grammar {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A"), n("B"))),
        "A" -> Set(chars("abc").repeat(2)),
        "B" -> Set(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")
}

object SimpleGrammar1_2 extends Grammar {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B")).repeat(3, 5)),
        "A" -> Set(i("abc")),
        "B" -> Set(i("bc")))
    val startSymbol = n("S")
}

object SimpleGrammar1_3 extends Grammar {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c('a'), c('b').star, c('c'))))
    val startSymbol = n("S")
}
