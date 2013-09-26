package com.giyeok.moonparser.grammars

import scala.collection.immutable.ListMap

import com.giyeok.moonparser.CharacterRangeInput
import com.giyeok.moonparser.DefItem
import com.giyeok.moonparser.Grammar

abstract class SampleGrammar extends Grammar {
    val sampleInputs: List[String]
}

object SampleGrammar1 extends SampleGrammar {

    val name = "Sample1"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(seq(n("A"), n("B"))),
        "A" -> List(i("a"), seq(i("a"), n("A"))),
        "B" -> List(seq(i("b"), n("B")), seq()))
    val sampleInputs = List()
}

object SampleGrammar2 extends SampleGrammar {

    val name = "Sample2"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(sequence(List[DefItem](i(" ")), n("A"), n("B"))),
        "A" -> List(i("a"), seq(i("a"), n("A"))),
        "B" -> List(seq(i("b"), n("B")), seq()))
    val sampleInputs = List()
}

object SampleGrammar3 extends SampleGrammar {
    val name = "Sample2"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(oneof(n("A"), n("B"))),
        "A" -> List(i("a")),
        "B" -> List(i("b")))
    val sampleInputs = List()
}

object SampleGrammar4 extends SampleGrammar {
    val name = "Sample4"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(seq(n("A").repeat(1, 3), n("B").repeat(2))),
        "A" -> List(i("a")),
        "B" -> List(i("b")))
    val sampleInputs = List("aabbbbb", "abbb")
}

object SampleGrammar5 extends SampleGrammar {
    val name = "Sample5"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(seq(c(), c("abc"), unicode_categories("Lu"), CharacterRangeInput('0', '9'))))
    val sampleInputs = List("-bQ5")
}

object SampleGrammar6 extends SampleGrammar {
    val name = "Sample6"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(seq(i("a").plus.except(i("aaa")), i("bb"))))
    val sampleInputs = List("-bQ5")
}

object SampleGrammar7 extends SampleGrammar {
    val name = "Sample7"
    val startSymbol = "S"
    val rules = ListMap(
        "S" -> List(seq(i("a"), lookahead_except(i("b")), i("c")), seq(i("a"), i("b"))))
    val sampleInputs = List("ab", "ac")
}

object SampleGrammar2_1 extends SampleGrammar {

    val name = "Sample2_1"
    val startSymbol: String = "S"
    val rules = ListMap(
        "S" -> List(seq(n("A").opt, n("B").star, n("C").plus, n("D").plus, n("E"))),
        "A" -> List(oneof(i("a"), i("aa"))),
        "B" -> List(i("b")),
        "C" -> List(seq(), i("c")),
        "D" -> List(i("d")),
        "E" -> List(i("e")))
    val sampleInputs = List("aaddde")
}
