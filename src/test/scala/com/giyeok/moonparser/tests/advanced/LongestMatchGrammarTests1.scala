package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.tests.AmbiguousSamples

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

    val correctSamples = Set[String](
        "abc a123123 def",
        "    abcdedr     afsdf   j1jdf1j35j")
    val incorrectSamples = Set[String]("12")
}

object LongestMatchGrammar2_2 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar2_2"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            elongest(n("IdName")),
            n("Number"),
            n("Punc"),
            n("Whitespace")),
        "IdName" -> ListSet(
            oneof(n("IdStart"), seq(n("IdName"), n("IdPart")))),
        "IdStart" -> ListSet(chars('a' to 'z', 'A' to 'Z')),
        "IdPart" -> ListSet(chars('a' to 'z', 'A' to 'Z', '0' to '9')),
        "Number" -> ListSet(elongest(seq(
            i("-").opt,
            seq(chars('1' to '9'), chars('0' to '9').star),
            seq(i("."), seq(chars('0' to '9').plus)).opt,
            seq(chars("eE"), seq(chars('1' to '9'), chars('0' to '9').star)).opt))),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Whitespace" -> ListSet(elongest(seq(wsChars.plus))))
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
        "aaaaa -11111.22222e33333   bbbbb",
        "12")
    val incorrectSamples = Set[String](
        "1111e",
        "1.a")
}

object LongestMatchGrammar2_3 extends Grammar with StringSamples with AmbiguousSamples {
    val name = "LongestMatchGrammar2_3"
    val wsChars = chars("\n\r\t ")
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            longest(n("IdName")),
            n("Number"),
            n("Punc"),
            n("Whitespace")),
        "IdName" -> ListSet(
            n("IdStart"),
            seq(n("IdName"), n("IdPart"))),
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
        "abc a123123 def",
        "    abcdedr     afsdf   j1jdf1j35j",
        "aaaaa 11111    bbbbb",
        "aaaaa -11111   bbbbb",
        "12",
        "1.a",
        "1111e",
        "111.222e333",
        "aaaaa 11111.222222   bbbbb",
        "aaaaa 11111e33333   bbbbb",
        "aaaaa 11111.222222e33333   bbbbb",
        "aaaaa -11111.22222e33333   bbbbb")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
}

object LongestMatchGrammar2_4 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar2_4"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Token").star),
        "Token" -> ListSet(
            elongest(n("Number")),
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
        "-1111.2222E123123;")
    val incorrectSamples = Set[String](
        "0.1")
}

object LongestMatchGrammar3_1 extends Grammar with StringSamples with AmbiguousSamples {
    val name = "LongestMatchGrammar3_1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(n("Number"), n("Punc"), n("Id")).star),
        "Number" -> ListSet(
            // eager longest로 바꿔서도 해보기
            longest(seq(chars('1' to '9'), chars('0' to '9').star, seq(i("."), chars('0' to '9').plus).opt))),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            longest(chars('a' to 'z', 'A' to 'Z').plus)))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "12",
        "1.a",
        "1.2")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String]()
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
            elongest(seq(chars('1' to '9'), chars('0' to '9').star, i("."), chars('0' to '9').plus))),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            elongest(chars('a' to 'z', 'A' to 'Z').plus)))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "1.2")
    val incorrectSamples = Set[String]("1.a")
}

object LongestMatchGrammar3_3 extends Grammar with StringSamples {
    val name = "LongestMatchGrammar3_3"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            oneof(elongest(n("Number")), n("Punc"), n("Id")).star),
        "Number" -> ListSet(
            // eager longest로 바꿔서도 해보기
            n("Float"),
            n("Int")),
        "Float" -> ListSet(
            seq(oneof(i("0"), seq(chars('1' to '9'), chars('0' to '9').star)), i("."), chars('0' to '9').plus)),
        "Int" -> ListSet(
            i("0"),
            seq(chars('1' to '9'), chars('0' to '9').star)),
        "Punc" -> ListSet(
            chars(".,;[](){}")),
        "Id" -> ListSet(
            chars('a' to 'z', 'A' to 'Z').plus))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "0.11233221",
        "1000.123123",
        "0",
        "123123")
    val incorrectSamples = Set[String]()
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
        "123", "123abc123")
    val incorrectSamples = Set[String](
        ".")
}

object LongestMatchGrammar5 extends Grammar with StringSamples {
    def expr(syms: Symbol*) = seq(syms.toSeq, Set[Symbol](n("WS")))
    val name = "LongestMatchGrammar5 (dangling else solution)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").star),
        "WS" -> ListSet(
            chars(" \r\n\t")),
        "Stmt" -> ListSet(
            n("IfStmt"),
            seq(i("{"), n("Stmt").star, i("}"))),
        "IfStmt" -> ListSet(
            longest(expr(i("if"), i("("), n("Cond"), i(")"), n("Stmt"), expr(i("else"), n("Stmt")).opt))),
        "Cond" -> ListSet(
            i("true"),
            i("false")))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "if(true)if(false){}else{}",
        "if(true)if(false){}else{}else{}")
    val incorrectSamples = Set[String]()
}

object LongestMatchGrammar5_1 extends Grammar with StringSamples with AmbiguousSamples {
    def expr(syms: Symbol*) = seq(syms.toSeq, Set[Symbol](n("WS")))
    val name = "LongestMatchGrammar5-1 (ambiguous, dangling else)"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").star),
        "WS" -> ListSet(
            chars(" \r\n\t")),
        "Stmt" -> ListSet(
            n("IfStmt"),
            seq(i("{"), n("Stmt").star, i("}"))),
        "IfStmt" -> ListSet(
            expr(i("if"), i("("), n("Cond"), i(")"), n("Stmt"), expr(i("else"), n("Stmt")).opt)),
        "Cond" -> ListSet(
            i("true"),
            i("false")))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "if(true)if(false){}else{}else{}")
    val incorrectSamples = Set[String]()
    val ambiguousSamples = Set[String](
        "if(true)if(false){}else{}")
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
        LongestMatchGrammar2_4,
        LongestMatchGrammar3_1,
        LongestMatchGrammar3_2,
        LongestMatchGrammar3_3,
        LongestMatchGrammar4,
        LongestMatchGrammar5,
        LongestMatchGrammar5_1)
}

class LongestMatchGrammarTestSuite1 extends BasicParseTest(LongestMatchGrammars.grammars)
