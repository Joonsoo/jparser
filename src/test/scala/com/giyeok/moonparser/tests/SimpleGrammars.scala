package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import org.junit.Test
import com.giyeok.moonparser.Parser
import org.scalatest.junit.AssertionsForJUnit
import com.giyeok.moonparser.Inputs._

object SimpleGrammar1 extends Grammar {
    val name = "Simple Grammar 1"
    val rules: RuleMap = ListMap(
        "S" -> Set(n("A")),
        "A" -> Set(i("abc")))
    val startSymbol = n("S")
}

class SimpleGrammar1Suite extends AssertionsForJUnit {
    @Test def proper() = {
        val parser = new Parser(SimpleGrammar1)
        println(parser.checkFromStart)
        println(parser.startingContext)
        val proceededCtx = parser.startingContext.proceedTerminal(Character('a', 0))
        println(proceededCtx)
    }
}
