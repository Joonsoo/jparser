package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.tests.GrammarTestCases

object LookaheadIsGrammar1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "LookaheadIsGrammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(longest(chars('a' to 'z').plus), lookahead_is(c(' '))),
            chars(" ")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("abc ", "abc def ")
    val incorrectSamples = Set[String]("abc")
}

object LookaheadIsGrammar2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "LookaheadIsGrammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> ListSet(
            seq(i("abc"), lookahead_is(i("def")))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("abcdef")
    val incorrectSamples = Set[String]("abcdex")
}

object LookaheadIsGrammar2_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "LookaheadIsGrammar 2_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> ListSet(
            seq(i("a"), lookahead_is(i("a")))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String]("aa", "aabbbbb")
    val incorrectSamples = Set[String]("a", "ax", "axxxx")
}

object FollowedByGrammar3 extends Grammar with GrammarTestCases with StringSamples {
    val name = "FollowedBy Grammar 3 (a^n b^n c^n)"

    val grammarText: String =
        """S = $P 'a'* B
          |P = A 'c'
          |A = 'a' A 'b' | 'a' 'b'
          |B = 'b' B 'c' | 'b' 'c'
          |""".stripMargin('|')

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(lookahead_is(n("P")), c('a').star, n("B"))
        ),
        "P" -> ListSet(
            seq(n("A"), c('c'))
        ),
        "A" -> ListSet(
            seq(c('a'), n("A"), c('b')),
            i("ab")
        ),
        "B" -> ListSet(
            seq(c('b'), n("B"), c('c')),
            i("bc")
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectSamples = Set[String](
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
    val tests: Set[GrammarTestCases] = Set(
        LookaheadIsGrammar1,
        LookaheadIsGrammar2,
        LookaheadIsGrammar2_1,
        FollowedByGrammar3
    )
}

class LookaheadIsTestSuite1 extends BasicParseTest(GrammarWithLookaheadIs.tests)
