package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListSet
import com.giyeok.jparser.tests.GrammarTestCases

object JoinGrammar1 extends Grammar with GrammarTestCases with StringSamples {
    def token(sym: Symbol): Symbol = sym.join(n("Token"))
    def expr(syms: Symbol*): Symbol = seqWS(token(n("Whitespace")).opt, syms: _*)

    val name = "JoinGrammar1"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            expr(token(i("var")), n("Id"), token(i("=")), n("Expr"))
        ),
        "Id" -> ListSet(
            token(chars('a' to 'z', 'A' to 'Z').plus)
        ),
        "Expr" -> ListSet(
            n("Number"),
            expr(n("Number"), token(chars("+-*/")), n("Expr")),
            expr(token(i("(")), n("Expr"), token(i(")")))
        ),
        "Number" -> ListSet(
            token(seq(chars('1' to '9'), chars('0' to '9').plus))
        ),
        "Token" -> ListSet(
            seq(n("NumChar").plus, lookahead_except(n("NumChar"))),
            seq(n("ChChar").plus, lookahead_except(n("ChChar"))),
            n("PuncChar"),
            seq(n("WsChar").plus, lookahead_except(n("WsChar")))
        ),
        "NumChar" -> ListSet(chars('0' to '9')),
        "ChChar" -> ListSet(chars('A' to 'Z', 'a' to 'z')),
        "PuncChar" -> ListSet(chars("=+-*/()")),
        "WsChar" -> ListSet(chars(" \t\n")),
        "Whitespace" -> ListSet(chars(" \t\n").plus)
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "var xyz = (123 + 456)"
    )
    val incorrectSamples = Set[String]()
}

object JoinGrammar2 extends Grammar with GrammarTestCases with StringSamples {
    val name = "JoinGrammar2 (nullable case)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("A").join(n("B"))
        ),
        "A" -> ListSet(
            chars('a' to 'z').star
        ),
        "B" -> ListSet(
            chars('b' to 'y').star
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "bcd"
    )
    val incorrectSamples = Set[String](
        "bca"
    )
}

object JoinGrammar3 extends Grammar with GrammarTestCases with StringSamples {
    val name = "JoinGrammar3 (a^n b^n c^n)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("L1").join(n("L2"))
        ),
        "L1" -> ListSet(
            seq(n("A"), n("P"))
        ),
        "A" -> ListSet(
            seq(c('a'), n("A"), c('b')),
            empty
        ),
        "P" -> ListSet(
            seq(c('c'), n("P")),
            empty
        ),
        "L2" -> ListSet(
            seq(n("Q"), n("C"))
        ),
        "Q" -> ListSet(
            seq(c('a'), n("Q")),
            empty
        ),
        "C" -> ListSet(
            seq(c('b'), n("C"), c('c')),
            empty
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "", "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectSamples = Set[String](
        "aaabbb"
    )
}

object JoinGrammar3_1 extends Grammar with GrammarTestCases with StringSamples {
    val name = "JoinGrammar3_1 (a^n b^n c^n d^m)"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seq(n("L1").join(n("L2"))), n("D")
        ),
        "L1" -> ListSet(
            seq(n("A"), n("P"))
        ),
        "A" -> ListSet(
            seq(c('a'), n("A"), c('b')),
            empty
        ),
        "P" -> ListSet(
            seq(c('c'), n("P")),
            empty
        ),
        "L2" -> ListSet(
            seq(n("Q"), n("C"))
        ),
        "Q" -> ListSet(
            seq(c('a'), n("Q")),
            empty
        ),
        "C" -> ListSet(
            seq(c('b'), n("C"), c('c')),
            empty
        ),
        "D" -> ListSet(
            c('d').star
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "", "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectSamples = Set[String](
        "aaabbb"
    )
}

object JoinGrammars {
    val tests: Set[GrammarTestCases] = Set(
        JoinGrammar1,
        JoinGrammar2,
        JoinGrammar3
    )
}

class JoinGrammarTestSuite1 extends BasicParseTest(JoinGrammars.tests)
