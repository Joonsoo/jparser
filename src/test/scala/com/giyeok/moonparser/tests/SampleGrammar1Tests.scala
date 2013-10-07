package com.giyeok.moonparser.tests

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SampleGrammar1Tests extends FunSuite {
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
            "S" -> Set(seq(n("A"), n("B"))),
            "A" -> Set(i("a"), seq(i("a"), n("A"))),
            "B" -> Set(seq(i("b"), n("B")), e))
    })

    test("Basic Grammar - test 1: aaa") {
        val t1 = grammar1.parse("aaa")
        println(t1.parsedOpt)
        assert(t1 textEq "aaa")
    }

    test("Basic Grammar - test 2: abbb") {
        val t2 = grammar1.parse("abbb")
        println(t2.parsedOpt)
        assert(t2 textEq "abbb")
    }

    test("Basic Grammar - test 3: aaaaabbbbb") {
        val t3 = grammar1.parse("aaaaabbbbb")
        println(t3.parsedOpt)
        assert(t3 textEq "aaaaabbbbb")
    }
    test("Basic Grammar - test 4: long text") {
        val longText = "a" * 500
        val t4 = grammar1.parse(longText)
        assert(t4 textEq longText)
    }
    // t5 = parser.parse("c")  // test it after implement backup items support
}
