package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SampleGrammarTests extends FunSuite {
    import scala.collection.immutable.ListMap
    import com.giyeok.moonparser.Grammar
    import com.giyeok.moonparser.GrElems._
    import com.giyeok.moonparser.dynamic.Parser
    import com.giyeok.moonparser.dynamic.BasicBlackboxParser

    val grammar1 = new Grammar {
        val name = "Simple Grammar 1"
        val startSymbol: String = "S"
        val rules: RuleMap = ListMap(
            "S" -> Set(seq(n("A"), n("B"))),
            "A" -> Set(i("a"), seq(i("a"), n("A"))),
            "B" -> Set(seq(i("b"), n("B")), e))
    }

    test("Simple Grammar 1") {
        val parser = new BasicBlackboxParser(grammar1)

        println(parser.parse("aaa"))
        println(parser.parse("abbb"))
        println(parser.parse("aaaaaaaaaabbbbb"))
        // println(parser.parse("c"))  // after implement backup items support
        assert(true)
    }

    /*
    test("Gramamr 3") {
        val grammar = new Grammar {
            val name = "Gramamr 3"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(oneof(n("A"), n("B"))),
                "A" -> Set(i("a")),
                "B" -> Set(i("b")))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("a")
        parser.parse("b")
    }

    test("Gramamr 4") {
        val grammar = new Grammar {
            val name = "Gramamr 4"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(n("A").repeat(1, 3), n("B").repeat(2))),
                "A" -> Set(i("a")),
                "B" -> Set(i("b")))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("aabbbbb")
        parser.parse("abbb")
    }

    test("Gramamr 5") {
        val grammar = new Grammar {
            val name = "Gramamr 5"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(c, c("abc"), unicode("Lu"), c('0', '9'))))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("-bQ5")
    }

    test("Gramamr 6") {
        val grammar = new Grammar {
            val name = "Gramamr 6"
            val startSymbol: String = "S"
            val rules: RuleMap = ListMap(
                "S" -> Set(seq(i("a").plus.except(i("aaa")), i("bb"))))
        }
        val parser = new BasicBlackboxParser(grammar)
        parser.parse("-bQ5")
    }

    test("Gramamr 7") {
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

    test("Gramamr 2.1") {
        val grammar = new Grammar {
            val name = "Grammar 2.1"
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
        parser.parse("aaddde")
    }
*/
}
