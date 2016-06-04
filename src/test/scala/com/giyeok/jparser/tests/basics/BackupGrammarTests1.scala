package com.giyeok.jparser.tests.basics

import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.tests.Samples
import com.giyeok.jparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.jparser.GrammarHelper._
import scala.collection.immutable.ListSet

object BackupGrammar1 extends Grammar with StringSamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(seq(c('a').plus), seq(wsChars.star, c(';')).backup(wsChars.plus)).star))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "aa;a ",
        "aaaa aaaa ")
    val incorrectSamples = Set[String]()
}

object BackupGrammar2 extends Grammar with StringSamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar2 (automatic semicolon insertion)"

    val lineend = c(';').backup(oneof(
        seq(n("WS").star, lookahead_is(chars("\n\r\t"))),
        seq(n("WS").star, lookahead_is(c('}')))))
    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            n("Stmt").star),
        "Stmt" -> ListSet(
            seq(c('a').plus, lineend),
            seq(c('{'), n("Stmt").star, c('}'))),
        "WS" -> ListSet(chars(" \n\r\t")))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "aa;a ",
        "aaaa aaaa ")
    val incorrectSamples = Set[String]()
}

object BackupGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        BackupGrammar1,
        BackupGrammar2)
}

class BackupGrammarTestSuite1 extends BasicParseTest(BackupGrammars.grammars)
