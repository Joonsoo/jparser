package com.giyeok.jparser.examples.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.{AmbiguousExamples, GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object ExceptGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")), c('c'))),
        "A" -> List(seq(c('a'), c('b').star)),
        "B" -> List(i("abb"))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abc", "abbbc")
    val incorrectExamples = Set("a", "abbc")
}

object ExceptGrammar1_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> List(n("C").plus),
        "C" -> List(seq(n("A").except(n("B")), c('c'))),
        "A" -> List(seq(c('a'), c('b').star)),
        "B" -> List(i("abb"))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abc", "abbbc", "abbbcac", "abbbcabbbc")
    val incorrectExamples = Set("a", "abbc", "abbbcabbc")
}

object ExceptGrammar1_2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")), c('c'), c('c'))),
        "A" -> List(seq(c('a'), c('b').star)),
        "B" -> List(i("abb"), i("abbb"))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abcc", "abbbbcc")
    val incorrectExamples = Set("a", "abbcc", "abbbcc")
}

object ExceptGrammar1_3 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")), c('c'))),
        "A" -> List(seq(c('a'), c('b').star)),
        "B" -> List(seq(i("a"), i("b").repeat(4, 7)))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("ac", "abc", "abbc", "abbbc", "abbbbbbbbc")
    val incorrectExamples = Set("a", "abbbbc", "abbbbbc", "abbbbbbc", "abbbbbbbc")
}

object ExceptGrammar1_4 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")))),
        "A" -> List(seq(c('a'), c('b').repeat(2, 4))),
        "B" -> List(seq(i("a"), i("b").star))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]()
    val incorrectExamples = Set("a", "ab", "abb", "abbb", "abbbb", "abbbbb")
}

object ExceptGrammar2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")), c('c')), i("abbc")),
        "A" -> List(seq(c('a'), c('b').star)),
        "B" -> List(i("abb"))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abc", "abbbc", "abbc")
    val incorrectExamples = Set("a")
}

object ExceptGrammar3_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 3-1 (except with lookahead_is)"
    val rules: RuleMap = ListMap(
        "S" -> List(seq(n("A").except(n("B")), chars('0' to '9').plus)),
        "A" -> List(chars('a' to 'z').star),
        "B" -> List(seq(i("abc"), lookahead_is(c('0'))))
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]()
    val incorrectExamples = Set[String]("abc0")
}

object ExceptGrammar4_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 4_1"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(chars('a' to 'z').repeat(0, 5)).except(c('a').star)
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abcd", "aaaab", "baaaa")
    val incorrectExamples = Set("a", "aaaaa", "aaaaaaaaa", "")
}

object ExceptGrammar4_2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "Except Grammar 4_2"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(n("N"), i("r")),
            seq(n("N"), i("ch"))
        ),
        "N" -> List(
            n("I").except(n("K"))
        ),
        "I" -> List(
            chars('a' to 'z').star
        ),
        "K" -> List(
            i("for"),
            i("foreach")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("abbbbr", "aaaabch", "baaaach", "for", "foreach")
    val incorrectExamples = Set[String]()
}

object ExceptGrammar4_3 extends Grammar with GrammarWithExamples with StringExamples {
    // 4.2에서 for->x, ea->y, ch->z 로 바꾼 버젼
    val name = "Except Grammar 4_3"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(n("N"), i("z"))
        ),
        "N" -> List(
            chars('a' to 'z').star.except(n("K"))
        ),
        "K" -> List(
            i("x"), i("xyz")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set("xyz")
    val incorrectExamples = Set[String]()
}

object ExceptGrammar5_1 extends Grammar with GrammarWithExamples with StringExamples with AmbiguousExamples {
    val name = "Except Grammar 5_1"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(n("B1"), n("B2"))
        ),
        "B1" -> List(
            c('b').plus.except(i("bb"))
        ),
        "B2" -> List(
            c('b').plus
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set()
    val incorrectExamples = Set()
    val ambiguousExamples = Set("bbbb")
}

object GrammarWithExcept {
    val tests: Set[GrammarWithExamples] = Set(
        ExceptGrammar1,
        ExceptGrammar1_1,
        ExceptGrammar1_2,
        ExceptGrammar1_3,
        ExceptGrammar1_4,
        ExceptGrammar2,
        ExceptGrammar3_1,
        ExceptGrammar4_1,
        ExceptGrammar4_2,
        ExceptGrammar4_3,
        ExceptGrammar5_1
    )
}
