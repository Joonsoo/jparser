package com.giyeok.jparser.examples.naive.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.naive.{AmbiguousExamples, GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object SimpleGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> List(n("A")),
        "A" -> List(i("abc")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abc")
    val incorrectExamples = Set("a")
}

object SimpleGrammar1_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A"), n("B"))),
        "A" -> List(chars("abc").repeat(2)),
        "B" -> List(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abcabcddfefefes")
    val incorrectExamples = Set("abds")
}

object SimpleGrammar1_2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> List(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> List(i("abc")),
        "B" -> List(i("bc")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("bcbcbc", "abcbcbcabc")
    val incorrectExamples = Set("abc")
}

object SimpleGrammar1_3 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(c('a'), c('b').star, c('c'))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("ac", "abc", "abbbbbbbc")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_3_2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_3_2"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(c('a'), c('b').star, c('c').opt)))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "abc", "abb")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_3_3 extends Grammar with GrammarWithExamples with StringExamples {
    // ambiguous language
    val name = "Simple Grammar 1_3_3"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(c('a'), c('b').star, c('c').opt, c('b').star)))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "abc", "abbbcbbb")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_4 extends Grammar with GrammarWithExamples with StringExamples with AmbiguousExamples {
    val name = "Simple Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> List(seqWS(chars(" \t\n").star, i("ab"), i("qt").opt, i("cd"))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("ab   \tqt\t  cd", "abcd", "abqtcd")
    val incorrectExamples = Set("a  bcd", "abc  d")
    val ambiguousExamples = Set("ab cd", "ab  cd")
}

object SimpleGrammar1_5 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_5"
    val rules: RuleMap = ListMap(
        "S" -> List(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> List(i("abc")),
        "B" -> List(i("ab")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abcabababc", "abcabababcabc")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_6 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_6"
    val rules: RuleMap = ListMap(
        "S" -> List(n("A"), empty),
        "A" -> List(i("abc")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("", "abc")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_7 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_7"
    val rules: RuleMap = ListMap(
        "S" -> List(n("A").opt),
        "A" -> List(i("abc")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("", "abc")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_8 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_8"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(c('a'), n("A").opt)),
        "A" -> List(i("abc")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "aabc")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_9 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_9"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(chars("abcefgijkxyz")).opt))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("a", "x")
    val incorrectExamples = Set[String]()
}

object SimpleGrammar1_10 extends Grammar with GrammarWithExamples with StringExamples with AmbiguousExamples {
    val name = "Simple Grammar 1_10 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> List(n("A").star),
        "A" -> List(chars('a' to 'z').plus, chars(" ")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]()
    val incorrectExamples = Set[String]()
    val ambiguousExamples = Set[String]("asdf")
}

object SimpleGrammar1_11 extends Grammar with GrammarWithExamples with StringExamples with AmbiguousExamples {
    val name = "Simple Grammar 1_11 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> List(
            oneof(chars(" "), n("A")).star),
        "A" -> List(
            chars('a' to 'z'),
            seq(chars('a' to 'z'), n("A"))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]()
    val incorrectExamples = Set[String]()
    val ambiguousExamples = Set[String]("abcd")
}

object SimpleGrammar1_12 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Simple Grammar 1_12"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(c('a').opt, n("B").opt, c('c').opt, c('d').opt, c('e').opt, c('f').opt)),
        "B" -> List(
            seq(n("B").opt, c('b'))))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "",
        "abcdef",
        "abbbbbbbbbbcdef")
    val incorrectExamples = Set[String]()
}

object SimpleGrammarSet1 {
    val tests: Set[GrammarWithExamples] = Set(
        SimpleGrammar1,
        SimpleGrammar1_1,
        SimpleGrammar1_2,
        SimpleGrammar1_3,
        SimpleGrammar1_3_2,
        SimpleGrammar1_3_3,
        SimpleGrammar1_4,
        SimpleGrammar1_5,
        SimpleGrammar1_6,
        SimpleGrammar1_7,
        SimpleGrammar1_8,
        SimpleGrammar1_9,
        SimpleGrammar1_10,
        SimpleGrammar1_11,
        SimpleGrammar1_12)
}
