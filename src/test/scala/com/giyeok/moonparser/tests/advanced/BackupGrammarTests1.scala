package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrammarHelper._
import scala.collection.immutable.ListSet

object BackupGrammar1 extends Grammar with StringSamples {
    val name = "BackupGrammar1"
    val rules: RuleMap = ListMap(
        "S" -> ListSet( /* TODO */ ))
    val startSymbol = n("S")

    val correctSamples = Set[String]()
    val incorrectSamples = Set[String]()
}

object BackupGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        BackupGrammar1)
}

class BackupGrammarTestSuite1 extends BasicParseTest(BackupGrammars.grammars)
