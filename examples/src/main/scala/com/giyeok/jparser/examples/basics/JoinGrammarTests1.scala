package com.giyeok.jparser.examples.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object JoinGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    def token(sym: Symbol): Symbol = sym.join(n("Token"))
    def expr(syms: Symbol*): Symbol = seqWS(token(n("Whitespace")).opt, syms: _*)

    val name = "JoinGrammar1"

    val rules: RuleMap = ListMap(
        "S" -> List(
            expr(token(i("var")), n("Id"), token(i("=")), n("Expr"))
        ),
        "Id" -> List(
            token(chars('a' to 'z', 'A' to 'Z').plus)
        ),
        "Expr" -> List(
            n("Number"),
            expr(n("Number"), token(chars("+-*/")), n("Expr")),
            expr(token(i("(")), n("Expr"), token(i(")")))
        ),
        "Number" -> List(
            token(seq(chars('1' to '9'), chars('0' to '9').plus))
        ),
        "Token" -> List(
            seq(n("NumChar").plus, lookahead_except(n("NumChar"))),
            seq(n("ChChar").plus, lookahead_except(n("ChChar"))),
            n("PuncChar"),
            seq(n("WsChar").plus, lookahead_except(n("WsChar")))
        ),
        "NumChar" -> List(chars('0' to '9')),
        "ChChar" -> List(chars('A' to 'Z', 'a' to 'z')),
        "PuncChar" -> List(chars("=+-*/()")),
        "WsChar" -> List(chars(" \t\n")),
        "Whitespace" -> List(chars(" \t\n").plus)
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "var xyz = (123 + 456)"
    )
    val incorrectExamples = Set[String]()
}

object JoinGrammar2 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "JoinGrammar2 (nullable case)"

    val rules: RuleMap = ListMap(
        "S" -> List(
            n("A").join(n("B"))
        ),
        "A" -> List(
            chars('a' to 'z').star
        ),
        "B" -> List(
            chars('b' to 'y').star
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "bcd"
    )
    val incorrectExamples = Set[String](
        "bca"
    )
}

object JoinGrammar3 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "JoinGrammar3 (a^n b^n c^n)"

    val rules: RuleMap = ListMap(
        "S" -> List(
            n("L1").join(n("L2"))
        ),
        "L1" -> List(
            seq(n("A"), n("P"))
        ),
        "A" -> List(
            seq(c('a'), n("A"), c('b')),
            empty
        ),
        "P" -> List(
            seq(c('c'), n("P")),
            empty
        ),
        "L2" -> List(
            seq(n("Q"), n("C"))
        ),
        "Q" -> List(
            seq(c('a'), n("Q")),
            empty
        ),
        "C" -> List(
            seq(c('b'), n("C"), c('c')),
            empty
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "", "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectExamples = Set[String](
        "aaabbb"
    )
}

object JoinGrammar3_1 extends Grammar with GrammarWithExamples with StringExamples {
    val name = "JoinGrammar3_1 (a^n b^n c^n)"

    val rules: RuleMap = ListMap(
        "S" -> List(
            n("P").join(n("Q"))
        ),
        "P" -> List(
            seq(n("A"), c('c').plus)
        ),
        "A" -> List(
            i("ab"),
            seq(c('a'), n("A"), c('b'))
        ),
        "Q" -> List(
            seq(c('a').plus, n("B"))
        ),
        "B" -> List(
            i("bc"),
            seq(c('b'), n("B"), c('c'))
        )
    )
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "abc", "aabbcc", "aaabbbccc"
    )
    val incorrectExamples = Set[String](
        "aaabbb",
        ("a" * 4) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 4) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 4),
        ("a" * 2) + ("b" * 3) + ("c" * 3),
        ("a" * 3) + ("b" * 2) + ("c" * 3),
        ("a" * 3) + ("b" * 3) + ("c" * 2)
    )
}

object JoinGrammars {
    val tests: Set[GrammarWithExamples] = Set(
        JoinGrammar1,
        JoinGrammar2,
        JoinGrammar3,
        JoinGrammar3_1
    )
}
