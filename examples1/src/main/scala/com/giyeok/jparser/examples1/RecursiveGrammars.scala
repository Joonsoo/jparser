package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object RecursiveGrammar1 extends GrammarWithStringSamples {
    val name = "Recursive Grammar 1 (Right Recursion)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars('a' to 'z'), seq(chars('a' to 'z'), n("S"))))
    val start = "S"

    val validInputs = Set(
        "a",
        "aaa")
    val invalidInputs = Set()
}

object RecursiveGrammar1_1 extends GrammarWithStringSamples {
    val name = "Recursive Grammar 1-1 (Left Recursion)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(chars('a' to 'z'), seq(n("S"), chars('a' to 'z'))))
    val start = "S"

    val validInputs = Set(
        "a",
        "aaa")
    val invalidInputs = Set()
}

object RecursiveGrammar1_2 extends GrammarWithStringSamples {
    val name = "Recursive Grammar 1-2 (Left Recursion 2)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(c('s'), seq(n("A"), c('s')), seq(n("S"), c('s'))),
        "A" -> ListSet(c('a'), seq(n("B"), c('a'))),
        "B" -> ListSet(c('b'), seq(n("S"), c('b'))))
    val start = "S"

    val validInputs = Set(
        "as",
        "bas",
        "sssbas")
    val invalidInputs = Set()
}

object RecursiveGrammar2 extends GrammarWithStringSamples {
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
    val start = "S"

    val validInputs = Set(
        "and (random 10) (random 20)",
        "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123)))")
    val invalidInputs = Set(
        "ran dom 10",
        "and (random 12) (and (or (random 10) (random 10)) (or (random 12) (random 123123))")
}

object RecursiveGrammar3 extends GrammarWithStringSamples {
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
    val start = "S"

    // Because of the associativity of binary opeartors, this grammar may not parse expressions like `1+2+3` properly
    val validInputs = Set(
        "123123",
        "1+2",
        "1+2*3",
        "3*2+1",
        "12300+23400*34500",
        "34500*23400+12300")
    val invalidInputs = Set()
}

object RecursiveGrammars extends ExampleGrammarSet {
    val examples = Set(
        RecursiveGrammar1.toPair,
        RecursiveGrammar1_1.toPair,
        RecursiveGrammar1_2.toPair,
        RecursiveGrammar2.toPair,
        RecursiveGrammar3.toPair)
}
