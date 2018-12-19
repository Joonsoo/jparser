package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.ListMap
import com.giyeok.jparser.examples1.{ExampleGrammarSet, StringSamples}

import scala.collection.immutable.ListSet

object ExceptGrammar1 extends GrammarWithStringSamples {
    val name = "Except Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"))
    )
    val start = "S"

    val validInputs = Set("abc", "abbbc")
    val invalidInputs = Set("a", "abbc")
}

object ExceptGrammar1_1 extends GrammarWithStringSamples {
    val name = "Except Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("C").plus),
        "C" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"))
    )
    val start = "S"

    val validInputs = Set("abc", "abbbc", "abbbcac", "abbbcabbbc")
    val invalidInputs = Set("a", "abbc", "abbbcabbc")
}

object ExceptGrammar1_2 extends GrammarWithStringSamples {
    val name = "Except Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"), i("abbb"))
    )
    val start = "S"

    val validInputs = Set("abcc", "abbbbcc")
    val invalidInputs = Set("a", "abbcc", "abbbcc")
}

object ExceptGrammar1_3 extends GrammarWithStringSamples {
    val name = "Except Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c'))),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(seq(i("a"), i("b").repeat(4, 7)))
    )
    val start = "S"

    val validInputs = Set("ac", "abc", "abbc", "abbbc", "abbbbbbbbc")
    val invalidInputs = Set("a", "abbbbc", "abbbbbc", "abbbbbbc", "abbbbbbbc")
}

object ExceptGrammar1_4 extends GrammarWithStringSamples {
    val name = "Except Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")))),
        "A" -> ListSet(seq(c('a'), c('b').repeat(2, 4))),
        "B" -> ListSet(seq(i("a"), i("b").star))
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set("a", "ab", "abb", "abbb", "abbbb", "abbbbb")
}

object ExceptGrammar2 extends GrammarWithStringSamples {
    val name = "Except Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), c('c')), i("abbc")),
        "A" -> ListSet(seq(c('a'), c('b').star)),
        "B" -> ListSet(i("abb"))
    )
    val start = "S"

    val validInputs = Set("abc", "abbbc", "abbc")
    val invalidInputs = Set("a")
}

object ExceptGrammar3_1 extends GrammarWithStringSamples {
    val name = "Except Grammar 3-1 (except with lookahead_is)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A").except(n("B")), chars('0' to '9').plus)),
        "A" -> ListSet(chars('a' to 'z').star),
        "B" -> ListSet(seq(i("abc"), lookahead_is(c('0'))))
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set("abc0")
}

object ExceptGrammar4_1 extends GrammarWithStringSamples {
    val name = "Except Grammar 4_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(chars('a' to 'z').repeat(0, 5)).except(c('a').star)
        )
    )
    val start = "S"

    val validInputs = Set("abcd", "aaaab", "baaaa")
    val invalidInputs = Set("a", "aaaaa", "aaaaaaaaa", "")
}

object ExceptGrammar4_2 extends GrammarWithStringSamples {
    val name = "Except Grammar 4_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("N"), i("r")),
            seq(n("N"), i("ch"))
        ),
        "N" -> ListSet(
            n("I").except(n("K"))
        ),
        "I" -> ListSet(
            chars('a' to 'z').star
        ),
        "K" -> ListSet(
            i("for"),
            i("foreach")
        )
    )
    val start = "S"

    val validInputs = Set("abbbbr", "aaaabch", "baaaach", "for", "foreach")
    val invalidInputs = Set[String]()
}

object ExceptGrammar4_3 extends GrammarWithStringSamples {
    // 4.2에서 for->x, ea->y, ch->z 로 바꾼 버젼
    val name = "Except Grammar 4_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("N"), i("z"))
        ),
        "N" -> ListSet(
            chars('a' to 'z').star.except(n("K"))
        ),
        "K" -> ListSet(
            i("x"), i("xyz")
        )
    )
    val start = "S"

    val validInputs = Set("xyz")
    val invalidInputs = Set()
}

object ExceptGrammar5_1 extends GrammarWithStringSamples {
    val name = "Except Grammar 5_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("B1"), n("B2"))
        ),
        "B1" -> ListSet(
            c('b').plus.except(i("bb"))
        ),
        "B2" -> ListSet(
            c('b').plus
        )
    )
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("bbbb")
}

object ExceptGrammars1 extends ExampleGrammarSet {
    val examples = Set(
        ExceptGrammar1.toPair,
        ExceptGrammar1_1.toPair,
        ExceptGrammar1_2.toPair,
        ExceptGrammar1_3.toPair,
        ExceptGrammar1_4.toPair,
        ExceptGrammar2.toPair,
        ExceptGrammar3_1.toPair,
        ExceptGrammar4_1.toPair,
        ExceptGrammar4_2.toPair,
        ExceptGrammar4_3.toPair,
        ExceptGrammar5_1.toPair
    )
}
