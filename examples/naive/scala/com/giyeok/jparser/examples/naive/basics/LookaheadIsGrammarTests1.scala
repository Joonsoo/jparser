package com.giyeok.jparser.examples.naive.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.naive.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object LookaheadIsGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "LookaheadIsGrammar 1"
    val rules: RuleMap = ListMap(
        "S" -> List(n("A").star),
        "A" -> List(
            seq(longest(chars('a' to 'z').plus), lookahead_is(c(' '))),
            chars(" ")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]("abc ", "abc def ")
    val incorrectExamples = Set[String]("abc")
}

object LookaheadIsGrammar2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "LookaheadIsGrammar 2"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> List(
            seq(i("abc"), lookahead_is(i("def")))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]("abcdef")
    val incorrectExamples = Set[String]("abcdex")
}

object LookaheadIsGrammar2_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "LookaheadIsGrammar 2_1"
    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> List(
            seq(i("a"), lookahead_is(i("a")))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String]("aa", "aabbbbb")
    val incorrectExamples = Set[String]("a", "ax", "axxxx")
}

object FollowedByGrammar3 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "FollowedBy Grammar 3 (a^n b^n c^n)"

    val grammarText: String =
        """S = $P 'a'* B
          |P = A 'c'
          |A = 'a' A 'b' | 'a' 'b'
          |B = 'b' B 'c' | 'b' 'c'
          |""".stripMargin('|')

    val rules: RuleMap = ListMap(
        "S" -> List(
            seq(lookahead_is(n("P")), c('a').star, n("B"))
        ),
        "P" -> List(
            seq(n("A"), c('c'))
        ),
        "A" -> List(
            seq(c('a'), n("A"), c('b')),
            i("ab")
        ),
        "B" -> List(
            seq(c('b'), n("B"), c('c')),
            i("bc")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectExamples = Set[String](
        "aaabbb",
        ("a" * 4) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 4) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 4),
        ("a" * 2) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 2) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 2)
    )
}

object GrammarWithLookaheadIs {
    // Grammar 1, 2, 7 are double-* ambiguous language
    val tests: Set[GrammarWithExamples] = Set(
        LookaheadIsGrammar1,
        LookaheadIsGrammar2,
        LookaheadIsGrammar2_1,
        FollowedByGrammar3
    )
}
