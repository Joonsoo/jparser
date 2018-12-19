package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object LookaheadIsGrammar1 extends GrammarWithStringSamples {
    val name = "LookaheadIsGrammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(longest(chars('a' to 'z').plus), lookahead_is(c(' '))),
            chars(" ")
        )
    )
    val start = "S"

    val validInputs = Set("abc ", "abc def ")
    val invalidInputs = Set("abc")
}

object LookaheadIsGrammar2 extends GrammarWithStringSamples {
    val name = "LookaheadIsGrammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> ListSet(
            seq(i("abc"), lookahead_is(i("def")))
        )
    )
    val start = "S"

    val validInputs = Set("abcdef")
    val invalidInputs = Set("abcdex")
}

object LookaheadIsGrammar2_1 extends GrammarWithStringSamples {
    val name = "LookaheadIsGrammar 2_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("B"), chars('a' to 'z').star)
        ),
        "B" -> ListSet(
            seq(i("a"), lookahead_is(i("a")))
        )
    )
    val start = "S"

    val validInputs = Set("aa", "aabbbbb")
    val invalidInputs = Set("a", "ax", "axxxx")
}

object FollowedByGrammar3 extends GrammarWithStringSamples {
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
    val start = "S"

    val validInputs = Set(
        "abc", "aabbcc", "aaabbbccc"
    )
    val invalidInputs = Set(
        "aaabbb",
        ("a" * 4) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 4) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 4),
        ("a" * 2) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 2) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 2)
    )
}

object LookaheadIsGrammars extends ExampleGrammarSet {
    // Grammar 1, 2, 7 are double-* ambiguous language
    val examples = Set(
        LookaheadIsGrammar1.toPair,
        LookaheadIsGrammar2.toPair,
        LookaheadIsGrammar2_1.toPair,
        FollowedByGrammar3.toPair
    )
}
