package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.BasicParseTest

object LongestMatchGrammar1 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar1"
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
    val name = "LongestMatchGrammar1_1"
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
    val name = "LongestMatchGrammar2"
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
    val name = "LongestMatchGrammar2_0"
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
    val name = "LongestMatchGrammar2_1"
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
    val name = "LongestMatchGrammar2_2"
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
            longest(n("IdStart")),
            longest(seq(n("IdName"), n("IdPart")))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Number" -> ListSet(longest(seq(
            i("-").opt,
            seq(chars('1' to '9'), chars('0' to '9').star),
            seq(i("."), seq(chars('0' to '9').plus)).opt,
            seq(chars("eE"), seq(chars('1' to '9'), chars('0' to '9').star)).opt))),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Whitespace" -> ListSet(longest(seq(wsChars.plus))))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "111.222e333",
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

object LongestMatchGrammar2_3 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar2_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            n("Number"),
            n("Punc")),
        "Number" -> ListSet(seq(
            i("-").opt,
            seq(chars('1' to '9'), chars('0' to '9').star),
            seq(i("."), seq(chars('0' to '9').plus)).opt,
            seq(chars("eE"), seq(chars('1' to '9'), chars('0' to '9').star)).opt)),
        "Punc" -> ListSet(
            chars(".,;[](){}")))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "1.2;")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammar3_1 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar3_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("Number"), n("Punc"), n("Id")).star),
        "Number" -> ListSet(
            // eager longest로 바꿔서도 해보기
            longest(n("Float"))),
        "Float" -> ListSet(
            seq(chars('1' to '9'), seq(chars('0' to '9').star, i("."), chars('0' to '9').plus).opt)),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            chars('a' to 'z', 'A' to 'Z').plus))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "1.2",
        "1.a")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammar3_2 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar3_2"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("Number"), n("Punc"), n("Id")).star),
        "Number" -> ListSet(
            // eager longest로 바꿔서도 해보기
            n("Float")),
        "Float" -> ListSet(
            elongest(seq(chars('1' to '9'), chars('0' to '9').star, seq(i("."), chars('0' to '9').plus).opt))),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            elongest(chars('a' to 'z', 'A' to 'Z').plus)))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "1.2",
        "1.a")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammar3_3 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar3_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("Number"), n("Punc"), n("Id")).star),
        "Number" -> ListSet(
            // eager longest로 바꿔서도 해보기
            longest(n("Float")),
            longest(n("Int"))),
        "Float" -> ListSet(
            seq(chars('1' to '9'), chars('0' to '9').star, seq(i("."), chars('0' to '9').plus).opt)),
        "Int" -> ListSet(
            seq(chars('1' to '9'), chars('0' to '9').star)),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            chars('a' to 'z', 'A' to 'Z').plus))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "1.2")
    val incorrectSamples = Set[String]("12", "1.a")
}

object LongestMatchGrammar4 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").star),
        "A" -> ListSet(
            n("N"),
            n("M")),
        "N" -> ListSet(
            longest(chars('0' to '9').plus)),
        "M" -> ListSet(
            longest(chars('a' to 'z').plus)))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "123")
    val incorrectSamples = Set[String](
        "a")
}

object LongestMatchGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        LongestMatchGrammar1,
        LongestMatchGrammar1_1,
        LongestMatchGrammar2,
        LongestMatchGrammar2_0,
        LongestMatchGrammar2_1,
        LongestMatchGrammar2_2,
        LongestMatchGrammar2_3,
        LongestMatchGrammar3_1,
        LongestMatchGrammar3_2,
        LongestMatchGrammar3_3,
        LongestMatchGrammar4)
}

class LongestMatchGrammarTestSuite1 extends BasicParseTest(LongestMatchGrammars.grammars)
