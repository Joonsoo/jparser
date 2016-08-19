package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListSet
import com.giyeok.jparser.tests.GrammarTestCases

object BackupGrammar1 extends Grammar with GrammarTestCases with StringSamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar1"

    val lineend = {
        val semicolon = seq(wsChars.star, c(';'))
        val alternative = wsChars.plus
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(seq(c('a').plus), lineend).star))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "aa;a ",
        "aaaa aaaa ")
    val incorrectSamples = Set[String]()
}

object BackupGrammar2 extends Grammar with GrammarTestCases with StringSamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar2 (automatic semicolon insertion)"

    def expr(s: Symbol*) = seqWS(n("WS").star, s: _*)

    val lineend = {
        val semicolon = c(';')
        val alternative = oneof(
            seq((n("WS").except(n("LT"))).star, n("LT")),
            seq(n("WS").star, lookahead_is(c('}'))))
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            seqWS(n("WS").star, n("Stmt"), n("S")),
            empty),
        "Stmt" -> ListSet(
            seq(longest(n("Expr")), lineend),
            expr(c('{'), n("S"), c('}'))),
        "Expr" -> ListSet(
            n("Term"),
            expr(chars("+-"), n("Term")),
            expr(n("Term"), chars("+-*/"), n("Expr"))),
        "Term" -> ListSet(
            c('a').plus,
            expr(c('('), n("Expr"), c(')'))),
        "WS" -> ListSet(chars(" \n\r\t")),
        "LT" -> ListSet(chars("\n\r")))
    val startSymbol = n("S")

    val grammar = this
    val correctSamples = Set[String](
        "{aaa\naaa}",
        "aaa\naaa\n",
        "{aaa;\naaa}",
        "{{}}",
        "{aaa\naaa\naaa;\naaa\naaa}",
        "{aaa\n+aaa\n+aaa}",
        "{aaa;\n+aaa}")
    val incorrectSamples = Set[String]()
}

object BackupGrammars {
    val tests: Set[GrammarTestCases] = Set(
        BackupGrammar1,
        BackupGrammar2)
}

class BackupGrammarTestSuite1 extends BasicParseTest(BackupGrammars.tests)
