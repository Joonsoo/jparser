package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._

object ExceptGrammar extends Grammar {
    val name = "Simple Grammar 2"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("Token").star),
        "Token" -> Set(
            n("Name"),
            n("Keyword"),
            chars(" ()")),
        "Word" -> Set(
            seq(n("FirstChar"), n("SecondChar").star, lookahead_except(n("SecondChar")))),
        "Name" -> Set(
            n("Word").except(n("Keyword"))),
        "Keyword" -> Set(
            i("var"),
            i("if")),
        "FirstChar" -> Set(
            chars('a' to 'z', 'A' to 'Z')),
        "SecondChar" -> Set(
            chars('a' to 'z', 'A' to 'Z', '0' to '9')))
    val startSymbol = n("S")
}

class ExceptGrammarTestSuite extends AssertionsForJUnit {
    @Test def keyword() = {
    }
}
