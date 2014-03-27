package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._
import org.junit.Assert._

object SimpleGrammar1 extends Grammar {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A")),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")
}

class SimpleGrammar1Suite extends AssertionsForJUnit {
    private def test(source: String) = {
        val parser = new Parser(SimpleGrammar1)
        println(parser.checkFromStart)
        println(parser.startingContext)
        source.toCharArray().zipWithIndex.foldLeft[Either[parser.ParsingContext, parser.ParsingError]](Left(parser.startingContext)) {
            (ctx, char) =>
                ctx match {
                    case Left(ctx) => ctx.proceedTerminal(Character(char._1, char._2))
                    case error @ Right(_) => error
                }
        }
    }

    @Test def correctSource() = {
        val result = test("abc")
        result match {
            case Left(ctx) =>
                val result = ctx.toResult
                println(result)
            case Right(_) =>
                // must not happen
                assertTrue(false)
        }
    }

    @Test def incorrectSource1() = {
    }
}
