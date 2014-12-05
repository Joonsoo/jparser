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

object RecursiveGrammar1 extends Grammar {
    val name = "Recursive Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> Set(chars("a"), seq(chars("a"), n("S"))))
    val startSymbol = n("S")

    val samples = Set("a", "aaa")
}

object RecursiveGrammar2 extends Grammar {
    val name = "Recursive Grammar 2"
    def expr(s: Symbol*) = seq(s.toSeq, Set[Symbol](chars(" \t\n")))
    val rules: RuleMap = ListMap(
        "S" -> Set(
            n("And"),
            n("Or"),
            n("Random"),
            seq(i("("), n("S"), i(")"))),
        "And" -> Set(expr(i("and"), n("S"), n("S"))),
        "Or" -> Set(expr(i("or"), n("S"), n("S"))),
        "Random" -> Set(expr(i("random"), n("Number"))),
        "Number" -> Set(seq(chars('1' to '9'), chars('0' to '9').star)))
    val startSymbol = n("S")
}

object RecursiveGrammar3 extends Grammar {
    val name = "Recursive Grammar 3"
    def expr(s: Symbol*) = seq(s.toSeq, Set[Symbol](chars(" \t\n")))
    val rules: RuleMap = ListMap(
        "S" -> Set(
            n("Prior1"),
            seq(i("("), n("S"), i(")"))),
        "Prior1" -> Set(
            expr(n("Prior1"), c('+'), n("Prior1")),
            expr(n("Prior1"), c('-'), n("Prior1")),
            n("Prior2")),
        "Prior2" -> Set(
            expr(n("Prior2"), c('*'), n("Prior2")),
            expr(n("Prior2"), c('/'), n("Prior2")),
            n("Prior2"),
            n("Number")),
        "Number" -> Set(seq(chars('1' to '9'), chars('0' to '9').star)))
    val startSymbol = n("S")
}
