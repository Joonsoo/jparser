package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SampleGrammar2Tests extends FunSuite {
    import scala.collection.immutable.ListMap
    import com.giyeok.moonparser.Grammar
    import com.giyeok.moonparser.GrElems._
    import com.giyeok.moonparser.ParsedSymbols._
    import com.giyeok.moonparser.InputPieces._
    import com.giyeok.moonparser.dynamic.Parser
    import com.giyeok.moonparser.dynamic.Parser.Result._
    import com.giyeok.moonparser.dynamic.BasicBlackboxParser

    test("Gramamr with OneOf") {
        val grammar = new Grammar {
            val name = "Gramamr 2"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(oneof(n("A"), n("B"))),
                "A" -> Set(i("a")),
                "B" -> Set(i("b")))
        }
        val parser = new BasicBlackboxParser(grammar)
        assert(parser.parse("a") textEq "a")
        assert(parser.parse("b") textEq "b")
    }

    test("Gramamr with Repeat") {
        val grammar = new Grammar {
            val name = "Gramamr 4"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(n("A").repeat(1, 3), n("B").repeat(2))),
                "A" -> Set(i("a")),
                "B" -> Set(i("b")))
        }
        val parser = new BasicBlackboxParser(grammar)
        assert(parser.parse("aabbbbb") textEq "aabbbbb")
        assert(parser.parse("abbb") textEq "abbb")
    }

    test("Gramamr with various character inputs") {
        val grammar = new Grammar {
            val name = "Gramamr 5"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(c, c("abc"), unicode("Lu"), c('0', '9'))))
        }
        val parser = new BasicBlackboxParser(grammar)
        assert(parser.parse("-bQ5") textEq "-bQ5")
    }

    test("Gramamr X") {
        val grammar = new Grammar {
            val name = "Grammar X"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(n("A").opt, n("B").star, n("C").plus, n("D").plus, n("E"))),
                "A" -> Set(oneof(i("a"), i("aa"))),
                "B" -> Set(i("b")),
                "C" -> Set(seq(), i("c")),
                "D" -> Set(i("d")),
                "E" -> Set(i("e")))
        }
        val parser = new BasicBlackboxParser(grammar)
        assert(parser.parse("aaddde") textEq "aaddde")
    }

    /*
    test("Gramamr with except combined with repeat") {
        val grammar = new Grammar {
            val name = "Gramamr 6"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(i("a").plus.except(i("aaa")), i("bb"))))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("-bQ5")
    }

    test("Gramamr with lookahead_except") {
        val grammar = new Grammar {
            val name = "Gramamr 7"
            val startSymbol = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(i("a"), lookahead_except(i("b")), i("c")), seq(i("a"), i("b"))))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("ab")
        parser.parse("ac")
    }
*/

}
