package com.giyeok.jparser.examples.basics

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

object BackupGrammar1 extends Grammar with GrammarWithExamples with StringExamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar1"

    val lineend = {
        val semicolon = seq(wsChars.star, c(';'))
        val alternative = wsChars.plus
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }
    val rules: RuleMap = ListMap(
        "S" -> List(seq(seq(c('a').plus), lineend).star))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "aa;a ",
        "aaaa aaaa ")
    val incorrectExamples = Set[String]()
}

object BackupGrammar2 extends Grammar with GrammarWithExamples with StringExamples {
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
        "S" -> List(
            seqWS(n("WS").star, n("Stmt"), n("S")),
            empty),
        "Stmt" -> List(
            seq(longest(n("Expr")), lineend),
            expr(c('{'), n("S"), c('}'))),
        "Expr" -> List(
            n("Term"),
            expr(chars("+-"), n("Term")),
            expr(n("Term"), chars("+-*/"), n("Expr"))),
        "Term" -> List(
            c('a').plus,
            expr(c('('), n("Expr"), c(')'))),
        "WS" -> List(chars(" \n\r\t")),
        "LT" -> List(chars("\n\r")))
    val startSymbol = n("S")

    val grammar = this
    val correctExamples = Set[String](
        "{aaa\naaa}",
        "aaa\naaa\n",
        "{aaa;\naaa}",
        "{{}}",
        "{aaa\naaa\naaa;\naaa\naaa}",
        "{aaa\n+aaa\n+aaa}",
        "{aaa;\n+aaa}")
    val incorrectExamples = Set[String]()
}

object BackupGrammars {
    val tests: Set[GrammarWithExamples] = Set(
        BackupGrammar1,
        BackupGrammar2)
}
