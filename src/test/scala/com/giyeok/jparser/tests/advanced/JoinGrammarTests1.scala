package com.giyeok.moonparser.tests.advanced

import com.giyeok.moonparser.tests.BasicParseTest
import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrammarHelper._
import scala.collection.immutable.ListSet

object JoinGrammar1 extends Grammar with StringSamples {
    def token(sym: Symbol): Symbol = sym.join(n("Token"))
    def expr(syms: Symbol*): Symbol = seq(syms.toSeq, Set[Symbol](token(n("Whitespace"))))

    val name = "JoinGrammar1"

    val rules: RuleMap = ListMap(
        "S" -> ListSet(
            expr(token(i("var")), n("Id"), token(i("=")), n("Expr"))),
        "Id" -> ListSet(
            token(chars('a' to 'z', 'A' to 'Z').plus)),
        "Expr" -> ListSet(
            n("Number"),
            expr(n("Number"), token(chars("+-*/")), n("Expr")),
            expr(token(i("(")), n("Expr"), token(i(")")))),
        "Number" -> ListSet(
            token(seq(chars('1' to '9'), chars('0' to '9').plus))),
        "Token" -> ListSet(
            seq(n("NumChar").plus, lookahead_except(n("NumChar"))),
            seq(n("ChChar").plus, lookahead_except(n("ChChar"))),
            n("PuncChar"),
            seq(n("WsChar").plus, lookahead_except(n("WsChar")))),
        "NumChar" -> ListSet(chars('0' to '9')),
        "ChChar" -> ListSet(chars('A' to 'Z', 'a' to 'z')),
        "PuncChar" -> ListSet(chars("=+-*/()")),
        "WsChar" -> ListSet(chars(" \t\n")),
        "Whitespace" -> ListSet(chars(" \t\n").plus))
    val startSymbol = n("S")

    val correctSamples = Set[String](
        "var xyz = (123 + 456)")
    val incorrectSamples = Set[String]()
}

object JoinGrammars {
    val grammars: Set[Grammar with Samples] = Set(
        JoinGrammar1)
}

class JoinGrammarTestSuite1 extends BasicParseTest(JoinGrammars.grammars)
