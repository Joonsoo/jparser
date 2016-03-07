package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.GrammarHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._
import scala.collection.immutable.ListSet

object SimpleGrammar2 extends Grammar with StringSamples {
    val name = "Simple Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Decimal"),
            n("HexDecimal")),
        "Decimal" -> ListSet(
            seq(c('-').opt, oneof(c('0'), seq(n("D1"), n("D0").star)), seq(c('.'), n("D0").star).opt, seq(chars("eE"), c('-').opt, n("D0").plus).opt)),
        "D0" -> ListSet(chars('0' to '9')),
        "D1" -> ListSet(chars('1' to '9')),
        "HexDecimal" -> ListSet(
            seq(c('-').opt, c('0'), chars("xX"), n("HD1"), n("HD0").star)),
        "HD0" -> ListSet(chars('0' to '9', 'a' to 'f', 'A' to 'F')),
        "HD1" -> ListSet(chars('1' to '9', 'a' to 'f', 'A' to 'F')))
    val startSymbol = n("S")

    val correctSamples = Set("10.1e2", "10.1e-00002")
    val incorrectSamples = Set("--10.1e2")
}

object SimpleGrammar3 extends Grammar with StringSamples {
    val name = "Simple Grammar 3 - Donald Knuth 63"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(n("A"), n("D"))),
        "A" -> ListSet(seq(c('a'), n("C"))),
        "B" -> ListSet(i("bcd")),
        "C" -> ListSet(seq(n("B"), n("E"))),
        "D" -> ListSet(e),
        "E" -> ListSet(i("e")))
    val startSymbol = n("S")

    val correctSamples = Set("abcde")
    val incorrectSamples = Set("f")
}

object SimpleGrammar4 extends Grammar with StringSamples {
    val name = "Simple Grammar 4 - StackOverflow"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(chars("ab").star, c('a'), chars("ab").star)))
    val startSymbol = n("S")

    val correctSamples = Set("abb", "bbbabbb")
    val incorrectSamples = Set("c")
}

object SimpleGrammarSet2 {
    val grammars: Set[Grammar with Samples] = Set(
        SimpleGrammar2,
        SimpleGrammar3,
        SimpleGrammar4)
}

class SimpleGrammar2TestSuite extends BasicParseTest(SimpleGrammarSet2.grammars)
