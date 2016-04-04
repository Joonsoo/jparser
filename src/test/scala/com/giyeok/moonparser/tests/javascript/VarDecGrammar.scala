package com.giyeok.moonparser.tests.javascript

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.Grammar
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.Symbols.Symbol

class VarDecGrammar1 extends Grammar {
    private val whitespace = ListSet[Symbol](n("WhiteSpace"), n("LineTerminator"), n("Comment"))

    def expr(s: Symbol*): Symbol = seq(s.toSeq, whitespace)
    def token(s: Symbol): Symbol = s.join(n("Token"))
    private val lineend = i(";")

    val name = "VarDecGrammar1"
    val startSymbol = n("Start")
    val _rules: RuleMap = ListMap(
        "Start" -> ListSet(
            expr(n("Stmt"), n("Start")),
            n("Stmt")),
        "Stmt" -> ListSet(
            n("VarStmt")),
        "VarStmt" -> ListSet(
            expr(token(i("var")), n("VarDecList"), lineend)),
        "VarDecList" -> ListSet(
            n("VarDec"),
            expr(n("VarDecList"), i(","), n("VarDec"))),
        "VarDec" -> ListSet(
            expr(token(n("_IdName")), n("Init").opt)),
        "Init" -> ListSet(
            expr(i("="), n("TestExpr"))),
        "TestExpr" -> ListSet(
            token(chars('0' to '9').plus)),

        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("_IdName"), n("IdPart"))),
        "IdStart" -> ListSet(
            unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl"),
            i("$"),
            i("_")),
        "IdPart" -> ListSet(
            n("IdStart"),
            unicode("Nd")),
        "Number" -> ListSet(
            chars('0' to '9').plus),
        "Token" -> ListSet(
            n("_IdName"),
            n("Number")),

        "WhiteSpace" -> ListSet(
            chars("\t\u000B\u000C\uFEFF"), unicode("Zs")), // \u0020\u00A0  ->  already in Zs
        "LineTerminator" -> ListSet(
            chars("\n\r")),
        "Comment" -> ListSet(
            expr(i("/*"), chars(" abcdefghijklmnopqrstuvwxyz\n\r\t").plus, i("*/"))))
    val rules = _rules
}

class VarDecGrammar1_1 extends VarDecGrammar1 {
    override val name = "VarDecGrammar1_1"
    override val rules: RuleMap = _rules.merge(ListMap(
        "Token" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("_IdName"))),
            n("Number"))))
}

class VarDecGrammar1_2 extends VarDecGrammar1 {
    override val name = "VarDecGrammar1_2"
    override val rules: RuleMap = _rules.merge(ListMap(
        "TestExpr" -> ListSet(
            n("Number"),
            expr(n("TestExpr"), i("+"), n("TestExpr")),
            expr(n("TestExpr"), i("-"), n("TestExpr")),
            expr(n("TestExpr"), i("*"), n("TestExpr")),
            expr(n("TestExpr"), i("/"), n("TestExpr")),
            expr(i("("), n("TestExpr"), i(")")))))
}

class VarDecGrammar2 extends VarDecGrammar1 {
    override val name = "VarDecGrammar2"
    override val rules: RuleMap = _rules.merge(ListMap(
        "VarDec" -> ListSet(
            expr(token(n("Id")), n("Init").opt)),

        "Id" -> ListSet(
            n("IdName").butnot(n("Kw"))),
        "IdName" -> ListSet(
            token(n("_IdName"))),
        "Kw" -> ListSet(
            token(i("if")),
            token(i("var"))),

        "_IdName" -> ListSet(
            n("IdStart"),
            seq(n("_IdName"), n("IdPart"), lookahead_except(n("IdPart")))),
        "Token" -> ListSet(
            n("Id"))))
}

class VarDecGrammar3 extends VarDecGrammar1 {
    override val name = "VarDecGrammar3"
    private val lineend = i(";").backup(oneof(seq(oneof(n("WhiteSpace"), n("Comment")).star, n("LineTerminator")), eof))

    override val rules: RuleMap = _rules.merge(ListMap(
        "VarStmt" -> ListSet(
            expr(token(i("var")), n("VarDecList"), lineend))))
}
