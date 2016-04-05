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
            seq(n("IdStart"), n("IdPart").star, lookahead_except(n("IdPart")))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Whitespace" -> ListSet(seq(wsChars, lookahead_except(wsChars))))
    val startSymbol = n("S")

    val correctSamples = Set[String]("abc a123123 def")
    val incorrectSamples = Set[String]()
}

object LongestMatchGrammar2_0 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar2_0"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("IdName").star),
        "IdName" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("IdPart")))),
        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("_IdName"), n("IdPart"))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')))
    val startSymbol = n("S")

    val correctSamples = Set[String]("a123b", "abcd")
    val incorrectSamples = Set[String]("123")
}

object LongestMatchGrammar2_1 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar2_1"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("IdName"),
            n("Whitespace")),
        "IdName" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("IdPart")))),
        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("_IdName"), n("IdPart"))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Whitespace" -> ListSet(seq(wsChars.plus, lookahead_except(wsChars))))
    val startSymbol = n("S")

    val correctSamples = Set[String]("abc a123123 def", "    abcdedr     afsdf   j1jdf1j35j")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammar2_2 extends Grammar with StringSamples {
    val name = "LogestMatchGrammar2_2"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("IdName"),
            n("Number"),
            n("Punc"),
            n("Whitespace")),
        "IdName" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("IdPart")))),
        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("_IdName"), n("IdPart"))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Number" -> ListSet(seq(
            i("-").opt,
            seq(chars('1' to '9'), chars('0' to '9').star, lookahead_except(chars('0' to '9'))),
            seq(i("."), seq(chars('0' to '9').plus, lookahead_except(chars('0' to '9')))).opt,
            seq(chars("eE"), seq(chars('1' to '9'), chars('0' to '9').star, lookahead_except(chars('0' to '9')))).opt)),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Whitespace" -> ListSet(seq(wsChars.plus, lookahead_except(wsChars))))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "abc a123123 def",
        "    abcdedr     afsdf   j1jdf1j35j",
        "aaaaa 11111    bbbbb",
        "aaaaa -11111   bbbbb",
        "aaaaa 11111.222222   bbbbb",
        "aaaaa 11111e33333   bbbbb",
        "aaaaa 11111.222222e33333   bbbbb",
        "aaaaa -11111.22222e33333   bbbbb")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        LongestMatchGrammar1,
        LongestMatchGrammar1_1,
        LongestMatchGrammar2,
        LongestMatchGrammar2_0,
        LongestMatchGrammar2_1,
        LongestMatchGrammar2_2)
}

class LogestMatchGrammarTestSuite1 extends BasicParseTest(LongestMatchGrammars.grammars)
