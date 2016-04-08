package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrammarHelper._
import scala.collection.immutable.ListSet

object BackupGrammar1 extends Grammar with StringSamples {
    val wsChars = chars(" ")
    val name = "BackupGrammar1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet(seq(seq(c('a').plus), seq(wsChars.star, c(';')).backup(wsChars.plus)).star))
    val startSymbol = n("S")

    val correctSamples = Set[String]("aa;a ")
    val incorrectSamples = Set[String]()
}

object BackupGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        BackupGrammar1)
}

class BackupGrammarTestSuite1 extends BasicParseTest(BackupGrammars.grammars)
