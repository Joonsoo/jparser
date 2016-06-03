package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.jparser.GrammarHelper._

object LookaheadIsGrammar1 extends Grammar with StringSamples {
    val name = "LookaheadIsGrammar1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(n("A").star),
        "A" -> ListSet(
            seq(longest(chars('a' to 'z').plus), lookahead_is(c(' '))),
            chars(" ")))
    val startSymbol = n("S")

    val correctSamples = Set[String]("abc ", "abc def ")
    val incorrectSamples = Set[String]("abc")
}

object GrammarWithLookaheadIs {
    // Grammar 1, 2, 7 are double-* ambiguous language
    val grammars: Set[Grammar with Samples] = Set(
        LookaheadIsGrammar1)
}

class LookaheadIsTestSuite1 extends BasicParseTest(GrammarWithLookaheadIs.grammars)
