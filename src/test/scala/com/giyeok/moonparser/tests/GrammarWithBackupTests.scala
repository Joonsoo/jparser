package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GrammarWithBackupTests extends FunSuite {
    import scala.collection.immutable.ListMap
    import com.giyeok.moonparser.Grammar
    import com.giyeok.moonparser.GrElems._
    import com.giyeok.moonparser.ParsedSymbols._
    import com.giyeok.moonparser.InputPieces._
    import com.giyeok.moonparser.dynamic.Parser
    import com.giyeok.moonparser.dynamic.Parser.Result._
    import com.giyeok.moonparser.dynamic.BasicBlackboxParser

    val grammar1 = new BasicBlackboxParser(new Grammar {
        // grammar only with StringInput, Nonterminal, Sequence
        val name = "Basic Grammar"
        val startSymbol: String = "S"
        val rules: RuleMap = ListMap(
            "S" -> Set(n("A").star),
            "A" -> Set(seq(ws(i(" ")), i("a"), n("A")), seq(i("a"), i(";").backup(i(" "), eof))))
    })

    test("Grammar with backups - test 1") {
        println(grammar1.parse("a   a").parsedOpt)
        println(grammar1.parse("a   a;aaa").parsedOpt)
        println(grammar1.parse("a   a;aa").parsedOpt)
    }
}
