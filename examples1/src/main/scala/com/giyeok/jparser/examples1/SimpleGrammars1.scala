package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

object SimpleGrammar1 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A")),
        "A" -> ListSet(i("abc")))
    val start = "S"

    val validInputs = Set("abc")
    val invalidInputs = Set("a")
}

object SimpleGrammar1_1 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("B"))),
        "A" -> ListSet(chars("abc").repeat(2)),
        "B" -> ListSet(seq(chars("def").repeat(2), i("s"))))
    val start = "S"

    val validInputs = Set("abcabcddfefefes")
    val invalidInputs = Set("abds")
}

object SimpleGrammar1_2 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> ListSet(i("abc")),
        "B" -> ListSet(i("bc")))
    val start = "S"

    val validInputs = Set(
        "bcbcbc",
        "abcbcbcabc")
    val invalidInputs = Set("abc")
}

object SimpleGrammar1_3 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c'))))
    val start = "S"

    val validInputs = Set(
        "ac",
        "abc",
        "abbbbbbbc")
    val invalidInputs = Set()
}

object SimpleGrammar1_3_2 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_3_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c').opt)))
    val start = "S"

    val validInputs = Set(
        "a",
        "abc",
        "abb")
    val invalidInputs = Set()
}

object SimpleGrammar1_3_3 extends GrammarWithStringSamples {
    // ambiguous language
    val name = "Simple Grammar 1_3_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), c('b').star, c('c').opt, c('b').star)))
    val start = "S"

    val validInputs = Set(
        "a",
        "abc",
        "abbbcbbb")
    val invalidInputs = Set()
}

object SimpleGrammar1_4 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seqWS(chars(" \t\n").star, i("ab"), i("qt").opt, i("cd"))))
    val start = "S"

    val validInputs = Set(
        "ab   \tqt\t  cd",
        "abcd",
        "abqtcd")
    val invalidInputs = Set(
        "a  bcd",
        "abc  d")
    override val ambiguousInputs = Set(
        "ab cd",
        "ab  cd")
}

object SimpleGrammar1_5 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(oneof(n("A"), n("B")).repeat(2, 5)),
        "A" -> ListSet(i("abc")),
        "B" -> ListSet(i("ab")))
    val start = "S"

    val validInputs = Set(
        "abcabababc",
        "abcabababcabc")
    val invalidInputs = Set()
}

object SimpleGrammar1_6 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A"), empty),
        "A" -> ListSet(i("abc")))
    val start = "S"

    val validInputs = Set(
        "",
        "abc")
    val invalidInputs = Set()
}

object SimpleGrammar1_7 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_7"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").opt),
        "A" -> ListSet(i("abc")))
    val start = "S"

    val validInputs = Set(
        "",
        "abc")
    val invalidInputs = Set()
}

object SimpleGrammar1_8 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_8"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(c('a'), n("A").opt)),
        "A" -> ListSet(i("abc")))
    val start = "S"

    val validInputs = Set(
        "a",
        "aabc")
    val invalidInputs = Set()
}

object SimpleGrammar1_9 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_9"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(chars("abcefgijkxyz")).opt))
    val start = "S"

    val validInputs = Set(
        "a",
        "x")
    val invalidInputs = Set()
}

object SimpleGrammar1_10 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_10 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(chars('a' to 'z').plus, chars(" ")))
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("asdf")
}

object SimpleGrammar1_11 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_11 (ambiguous)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(chars(" "), n("A")).star),
        "A" -> ListSet(
            chars('a' to 'z'),
            seq(chars('a' to 'z'), n("A"))))
    val start = "S"

    val validInputs = Set()
    val invalidInputs = Set()
    override val ambiguousInputs = Set("abcd")
}

object SimpleGrammar1_12 extends GrammarWithStringSamples {
    val name = "Simple Grammar 1_12"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(c('a').opt, n("B").opt, c('c').opt, c('d').opt, c('e').opt, c('f').opt)),
        "B" -> ListSet(
            seq(n("B").opt, c('b'))))
    val start = "S"

    val validInputs = Set(
        "",
        "abcdef",
        "abbbbbbbbbbcdef")
    val invalidInputs = Set()
}

object SimpleGrammars1 extends ExampleGrammarSet {
    val examples = Set(
        SimpleGrammar1.toPair,
        SimpleGrammar1_1.toPair,
        SimpleGrammar1_2.toPair,
        SimpleGrammar1_3.toPair,
        SimpleGrammar1_3_2.toPair,
        SimpleGrammar1_3_3.toPair,
        SimpleGrammar1_4.toPair,
        SimpleGrammar1_5.toPair,
        SimpleGrammar1_6.toPair,
        SimpleGrammar1_7.toPair,
        SimpleGrammar1_8.toPair,
        SimpleGrammar1_9.toPair,
        SimpleGrammar1_10.toPair,
        SimpleGrammar1_11.toPair,
        SimpleGrammar1_12.toPair)
}
