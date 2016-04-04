package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.BasicParseTest

object LongestMatchGrammar1 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar1"
    def longest(c: Symbol) = seq(c.plus, lookahead_except(c))
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("Id"),
            n("WS"),
            n("Num")),
        "Id" -> ListSet(longest(chars('a' to 'z', 'A' to 'Z'))),
        "WS" -> ListSet(longest(chars("\n\r\t "))),
        "Num" -> ListSet(longest(chars('0' to '9'))))

    val startSymbol = n("S")

    val correctSamples = Set[String]("abc def")
    val incorrectSamples = Set[String]()
}

object LongestMatchGrammar1_1 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar1_1"
    def longest(c: Symbol) = seq(c.plus, lookahead_except(c))
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("Id"),
            n("WS"),
            n("Num"),
            n("Punc")),
        "Id" -> ListSet(longest(chars('a' to 'z', 'A' to 'Z'))),
        "WS" -> ListSet(longest(chars("\n\r\t "))),
        "Num" -> ListSet(longest(chars('0' to '9'))),
        "Punc" -> ListSet(chars(";:[]().")))

    val startSymbol = n("S")

    val correctSamples = Set[String]("abc def")
    val incorrectSamples = Set[String]()
}

object LongestMatchGrammar2 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar2"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("IdName"),
            n("Whitespace")),
        "IdName" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("_IdName")))),
        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("IdName"), n("IdPart"))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Whitespace" -> ListSet(seq(wsChars, lookahead_except(wsChars))))
    val startSymbol = n("S")

    val correctSamples = Set[String]("abc def")
    val incorrectSamples = Set[String]()
}

object LongestMatchGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        LongestMatchGrammar1,
        LongestMatchGrammar1_1,
        LongestMatchGrammar2)
}

class LogestMatchGrammarTestSuite1 extends BasicParseTest(LongestMatchGrammars.grammars)
