package com.giyeok.jparser.examples.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object RecursiveGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Recursive Grammar 1 (Right Recursion)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars('a' to 'z'), seq(chars('a' to 'z'), n("S"))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "aaa")
    val incorrectExamples = Set[String]()
}

object RecursiveGrammar1_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Recursive Grammar 1-1 (Left Recursion)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars('a' to 'z'), seq(n("S"), chars('a' to 'z'))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "aaa")
    val incorrectExamples = Set[String]()
}

object RecursiveGrammar1_2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Recursive Grammar 1-2 (Left Recursion 2)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(c('s'), seq(n("A"), c('s')), seq(n("S"), c('s'))),
        "A" -> ListSet(c('a'), seq(n("B"), c('a'))),
        "B" -> ListSet(c('b'), seq(n("S"), c('b'))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("as", "bas", "sssbas")
    val incorrectExamples = Set[String]()
}

object RecursiveGrammar2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Recursive Grammar 2"
    def expr(s: Symbol*) = seqWS(chars(" \t\n").star, s: _*)
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

    val grammar = this
    val correctExamples = Set[String]("and (random 10) (random 20)", "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123)))")
    val incorrectExamples = Set[String]("ran dom 10", "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123))")
}

object RecursiveGrammar3 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Recursive Grammar 3"
    def expr(s: Symbol*) = seqWS(chars(" \t\n").star, s: _*)
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
    val grammar = this
    val correctExamples = Set[String]("123123", "1+2", "1+2*3", "3*2+1", "12300+23400*34500", "34500*23400+12300")
    val incorrectExamples = Set[String]()
}

object RecursiveGrammarSet1 {
    val tests: Set[GrammarWithExamples] = Set(
        RecursiveGrammar1,
        RecursiveGrammar1_1,
        RecursiveGrammar1_2,
        RecursiveGrammar2,
        RecursiveGrammar3)
}
