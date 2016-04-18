package com.giyeok.moonparser.tests.basics

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.GrammarHelper._
import scala.collection.immutable.ListMap
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.Inputs._
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples

object SimpleGrammar5 extends Grammar with StringSamples {
    val name = "Simple Grammar 5"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("B"))),
        "A" -> ListSet(i("a"), e),
        "B" -> ListSet(i("b"), e))
    val startSymbol = n("S")

    val correctSamples = Set("", "a", "b", "ab")
    val incorrectSamples = Set("aa")
}

object SimpleGrammar6 extends Grammar with StringSamples {
    val name = "Simple Grammar 6"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("C"))),
        "A" -> ListSet(seq(n("B"), i("a").star)),
        "B" -> ListSet(i("b"), e),
        "C" -> ListSet(seq(n("B"), i("c").star)))
    val startSymbol = n("S")

    val correctSamples = Set("", "ab", "c", "ccc", "abc", "aa", "aaabccc")
    val incorrectSamples = Set("cb")
}

object SimpleGrammar7_1 extends Grammar with StringSamples {
    val name = "Simple Grammar 7-1 (right associative)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Exp")),
        "Exp0" -> ListSet(
            n("Id"),
            n("Num"),
            seq(i("("), n("Num"), i(")"))),
        "Exp1" -> ListSet(
            n("Exp0"),
            seq(n("Exp0"), i("*"), n("Exp1"))),
        "Exp2" -> ListSet(
            n("Exp1"),
            seq(n("Exp1"), i("+"), n("Exp2"))),
        "Exp" -> ListSet(
            n("Exp2")),
        "Id" -> ListSet(
            elongest(chars('a' to 'z').plus)),
        "Num" -> ListSet(
            elongest(chars('0' to '9').plus)))
    val startSymbol = n("S")

    val correctSamples = Set("1+2+3")
    val incorrectSamples = Set("")
}

object SimpleGrammar7_2 extends Grammar with StringSamples {
    val name = "Simple Grammar 7-2 (left associative)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Exp")),
        "Exp0" -> ListSet(
            n("Id"),
            n("Num"),
            seq(i("("), n("Num"), i(")"))),
        "Exp1" -> ListSet(
            n("Exp0"),
            seq(n("Exp1"), i("*"), n("Exp0"))),
        "Exp2" -> ListSet(
            n("Exp1"),
            seq(n("Exp2"), i("+"), n("Exp1"))),
        "Exp" -> ListSet(
            n("Exp2")),
        "Id" -> ListSet(
            elongest(chars('a' to 'z').plus)),
        "Num" -> ListSet(
            elongest(chars('0' to '9').plus)))
    val startSymbol = n("S")

    val correctSamples = Set("1+2+3")
    val incorrectSamples = Set("")
}

object AsteriskNullable extends Grammar with StringSamples {
    val name = "*-nullable"
    val rules: RuleMap = ListMap(
        "S" -> ListSet((chars('a' to 'z').opt).star))
    val startSymbol = n("S")

    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object SimpleGrammarSet3 {
    val grammars: Set[Grammar with Samples] = Set(
        SimpleGrammar5,
        SimpleGrammar6,
        SimpleGrammar7_1,
        SimpleGrammar7_2,
        AsteriskNullable)
}

class SimpleGrammarTestSuite3 extends BasicParseTest(SimpleGrammarSet3.grammars)
