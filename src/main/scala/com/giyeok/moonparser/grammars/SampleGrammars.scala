package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap

import com.giyeok.moonparser.CharacterRangeInput
import com.giyeok.moonparser.GrElem
import com.giyeok.moonparser.Grammar

abstract class SampleGrammar extends Grammar {
    val sampleInputs: List[String]
}

object SampleGrammar1 extends SampleGrammar {

    val name = "Sample1"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A"), n("B"))),
        "A" -> Set(i("a"), seq(i("a"), n("A"))),
        "B" -> Set(seq(i("b"), n("B")), seq()))
    val sampleInputs = List()
}

object SampleGrammar2 extends SampleGrammar {

    val name = "Sample2"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(Set[GrElem](i(" ")), n("A"), n("B"))),
        "A" -> Set(i("a"), seq(i("a"), n("A"))),
        "B" -> Set(seq(i("b"), n("B")), seq()))
    val sampleInputs = List()
}

object SampleGrammar3 extends SampleGrammar {
    val name = "Sample2"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(oneof(n("A"), n("B"))),
        "A" -> Set(i("a")),
        "B" -> Set(i("b")))
    val sampleInputs = List()
}

object SampleGrammar4 extends SampleGrammar {
    val name = "Sample4"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A").repeat(1, 3), n("B").repeat(2))),
        "A" -> Set(i("a")),
        "B" -> Set(i("b")))
    val sampleInputs = List("aabbbbb", "abbb")
}

object SampleGrammar5 extends SampleGrammar {
    val name = "Sample5"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(c, c("abc"), unicode("Lu"), CharacterRangeInput('0', '9'))))
    val sampleInputs = List("-bQ5")
}

object SampleGrammar6 extends SampleGrammar {
    val name = "Sample6"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(i("a").plus.except(i("aaa")), i("bb"))))
    val sampleInputs = List("-bQ5")
}

object SampleGrammar7 extends SampleGrammar {
    val name = "Sample7"
    val startSymbol = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(i("a"), lookahead_except(i("b")), i("c")), seq(i("a"), i("b"))))
    val sampleInputs = List("ab", "ac")
}

object SampleGrammar2_1 extends SampleGrammar {

    val name = "Sample2_1"
    val startSymbol: String = "S"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A").opt, n("B").star, n("C").plus, n("D").plus, n("E"))),
        "A" -> Set(oneof(i("a"), i("aa"))),
        "B" -> Set(i("b")),
        "C" -> Set(seq(), i("c")),
        "D" -> Set(i("d")),
        "E" -> Set(i("e")))
    val sampleInputs = List("aaddde")
}
