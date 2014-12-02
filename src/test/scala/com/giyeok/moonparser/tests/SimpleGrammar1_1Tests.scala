package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._

object SimpleGrammar1_1 extends Grammar {
    val name = "Simple Grammar 1_1"
    val rules: RuleMap = ListMap(
        "S" -> Set(seq(n("A"), n("B"))),
        "A" -> Set(chars("abc").repeat(2)),
        "B" -> Set(seq(chars("def").repeat(2), i("s"))))
    val startSymbol = n("S")
}

class SimpleGrammar1_1Suite extends AssertionsForJUnit {
    private def test(source: String) = {
        val parser = new Parser(SimpleGrammar1_1)
        println(parser.checkFromStart)
        println(parser.startingContext)
        parser.parse(source)
    }

    @Test def correctSource() = {
        val result = test("abcab")
        println(result)
        result match {
            case Left(ctx) =>
                val result = ctx.toResult
                println(result)
                result.get.parseNode.printTree()
            case Right(_) =>
                // must not happen
                assertTrue(false)
        }
    }

    @Test def incorrectSource1() = {
    }
}
