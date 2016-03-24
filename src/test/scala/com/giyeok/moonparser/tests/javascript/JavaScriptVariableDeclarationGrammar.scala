package com.giyeok.moonparser.tests.javascript

import scala.collection.immutable.ListMap
import com.giyeok.moonparser.GrammarHelper._
import com.giyeok.moonparser.Grammar
import scala.collection.immutable.ListSet
import com.giyeok.moonparser.Symbols.Symbol

object JavaScriptVariableDeclarationGrammar extends Grammar {
    private val whitespace = ListSet[Symbol](n("WhiteSpace"), n("LineTerminator"), n("Comment"))

    def expr(s: Symbol*): Symbol = seq(s.toSeq, whitespace)
    def token(s: Symbol): Symbol = s.join(n("Token"))
    val lineend = i(";").backup(oneof(seq(oneof(n("WhiteSpace"), n("Comment")).star, n("LineTerminator")), eof))

    val name = "JavaScriptIdentifier"
    val startSymbol = n("Start")
    val rules: RuleMap = ListMap(
        "Start" -> ListSet(
            n("Statement").star),
        "Statement" -> ListSet(
            n("VariableStatement")),
        "VariableStatement" -> ListSet(
            expr(token(i("var")), n("VariableDeclarationList"), lineend)),
        "VariableDeclarationList" -> ListSet(
            n("VariableDeclaration"),
            expr(n("VariableDeclarationList"), i(","), n("VariableDeclaration"))),
        "VariableDeclaration" -> ListSet(
            expr(n("Id"), n("Initialiser").opt)),
        "Initialiser" -> ListSet(
            expr(i("="), n("TestingExpression"))),
        "TestingExpression" -> ListSet(
            token(chars('0' to '9').plus)),

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
        "IdStart" -> ListSet(
            unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl"),
            i("$"),
            i("_")),
        "IdPart" -> ListSet(
            n("IdStart"),
            unicode("Nd")),
        "Token" -> ListSet(
            n("_IdName")),

        "WhiteSpace" -> ListSet(
            chars("\u0009\u000B\u000C\uFEFF"), unicode("Zs")), // \u0020\u00A0  ->  already in Zs
        "LineTerminator" -> ListSet(
            chars("\n\r\u2028\u2029")),
        "Comment" -> ListSet())
}
