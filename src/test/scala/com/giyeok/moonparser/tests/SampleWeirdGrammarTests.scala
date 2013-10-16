package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SampleWeirdGrammarTests extends FunSuite {
    import scala.collection.immutable.ListMap
    import com.giyeok.moonparser.Grammar
    import com.giyeok.moonparser.GrElems._
    import com.giyeok.moonparser.ParsedSymbols._
    import com.giyeok.moonparser.InputPieces._
    import com.giyeok.moonparser.dynamic.Parser
    import com.giyeok.moonparser.dynamic.Parser.Result._
    import com.giyeok.moonparser.dynamic.BasicBlackboxParser

    val grammar1 = new BasicBlackboxParser(new Grammar {
        val name = "Dual Grammar"
        val startSymbol: String = "S"
        val rules: RuleMap = ListMap(
            "S" -> Set(i("for"), seq(c('f'), c('o'), c('r'))))
    })

    val grammar2 = new BasicBlackboxParser(new Grammar {
        val name = "Nested Grammar 1"
        val startSymbol: String = "S"
        val rules: RuleMap = ListMap(
            "S" -> Set(n("U").plus, i("for")),
            "U" -> Set(seq(c)))
    })

    val grammar3 = new BasicBlackboxParser(new Grammar {
        val name = "Nested Grammar 2"
        val startSymbol: String = "S"
        val rules: RuleMap = ListMap(
            "S" -> Set(seq(seq(seq(i("abc")))), seq(seq(seq(i("def"))))))
    })

    test("grammar2 - fos") {
        grammar2.parse("fos")
    }
}
