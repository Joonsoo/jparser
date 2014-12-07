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
import scala.collection.immutable.ListSet

object RecursiveGrammar1 extends Grammar with StringSamples {
    val name = "Recursive Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars("a"), seq(chars("a"), n("S"))))
    val startSymbol = n("S")

    val correctSamples = Set("a", "aaa")
    val incorrectSamples = Set[String]()
}

object RecursiveGrammar1_1 extends Grammar with StringSamples {
    val name = "Recursive Grammar 1-1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars("a"), seq(n("S"), chars("a"))))
    val startSymbol = n("S")

    val correctSamples = Set("a", "aaa")
    val incorrectSamples = Set[String]()
}

object RecursiveGrammar2 extends Grammar with StringSamples {
    val name = "Recursive Grammar 2"
    def expr(s: Symbol*) = seq(s.toSeq, Set[Symbol](chars(" \t\n")))
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("And"),
            n("Or"),
            n("Random"),
            seq(i("("), n("S"), i(")"))),
        "And" -> ListSet(expr(i("and"), n("S"), n("S"))),
        "Or" -> ListSet(expr(i("or"), n("S"), n("S"))),
        "Random" -> ListSet(expr(i("random"), n("Number"))),
        "Number" -> ListSet(seq(chars('1' to '9'), chars('0' to '9').star)))
    val startSymbol = n("S")

    val correctSamples = Set[String]("and (random 10) (random 20)", "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123)))")
    val incorrectSamples = Set[String]("ran dom 10", "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123))")
}

object RecursiveGrammar3 extends Grammar with StringSamples {
    val name = "Recursive Grammar 3"
    def expr(s: Symbol*) = seq(s.toSeq, Set[Symbol](chars(" \t\n")))
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("Prior1")),
        "Prior1" -> ListSet(
            expr(n("Prior1"), c('+'), n("Prior1")),
            expr(n("Prior1"), c('-'), n("Prior1")),
            n("Prior2")),
        "Prior2" -> ListSet(
            expr(n("Prior2"), c('*'), n("Prior2")),
            expr(n("Prior2"), c('/'), n("Prior2")),
            expr(i("("), n("Prior1"), i(")")),
            n("Number")),
        "Number" -> ListSet(seq(chars('1' to '9'), chars('0' to '9').star)))
    val startSymbol = n("S")

    // Because of the associativity of binary opeartors, this grammar may not parse expressions like `1+2+3` properly
    val correctSamples = Set[String]("123123", "1+2", "1+2*3", "3*2+1", "12300+23400*34500", "34500*23400+12300")
    val incorrectSamples = Set[String]()
}

object RecursiveGrammarSet1 {
    val grammars: Set[Grammar with Samples] = Set(
        RecursiveGrammar1,
        RecursiveGrammar1_1,
        RecursiveGrammar2,
        RecursiveGrammar3)
}

class RecursiveGrammar1TestSuite extends BasicParseTest(RecursiveGrammarSet1.grammars)
