package com.giyeok.jparser.examples1

import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.{Grammar, Symbols}

import scala.collection.immutable.{ListMap, ListSet}

object BackupGrammar1 extends GrammarWithStringSamples {
    val name = "BackupGrammar1"

    val lineend: Symbols.OneOf = {
        val wsChars = chars(" ")
        val semicolon = seq(wsChars.star, c(';'))
        val alternative = wsChars.plus
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(seq(c('a').plus), lineend).star))
    val start = "S"

    override val validInputs = Set(
        "aa;a ",
        "aaaa aaaa ")
    override val invalidInputs = Set()
}

object BackupGrammar2 extends GrammarWithStringSamples {
    val name = "BackupGrammar2 (automatic semicolon insertion)"

    private def expr(s: Symbol*) = seqWS(n("WS").star, s: _*)

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
    val start = "S"

    val validInputs = Set(
        "{aaa\naaa}",
        "aaa\naaa\n",
        "{aaa;\naaa}",
        "{{}}",
        "{aaa\naaa\naaa;\naaa\naaa}",
        "{aaa\n+aaa\n+aaa}",
        "{aaa;\n+aaa}")
    val invalidInputs = Set()
}

object BackupGrammars extends ExampleGrammarSet {
    val examples = Set(
        BackupGrammar1.toPair,
        BackupGrammar2.toPair)
}
