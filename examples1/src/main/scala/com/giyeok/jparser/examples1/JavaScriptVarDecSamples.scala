package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._

import scala.collection.immutable.{ListMap, ListSet}

trait Grammar0 extends Grammar {
    def expr(s: Symbol*): Symbol = seqWS(oneof(n("WhiteSpace"), n("LineTerminator"), n("Comment")).star, s: _*)
    def token(s: Symbol): Symbol = s.join(n("Token"))
}

object VarDecGrammar1 extends GrammarWithStringSamples with Grammar0 {
    val name = "JS VarDec Test 1"

    val start = "Start"
    val rules: RuleMap = ListMap(
        "Start" -> ListSet(
            expr(n("Stmt"), n("Start")),
            n("Stmt")),
        "Stmt" -> ListSet(
            n("VarStmt")),
        "VarStmt" -> ListSet(
            expr(token(i("var")), n("VarDecList"), i(";"))),
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
            seq(i("/*"), chars(" abcdefghijklmnopqrstuvwxyz\n\r\t").plus, i("*/"))))

    val validInputs = Set(
        "var abc = 123;\n\nvar xyz = 321; var if = 154;")
    val invalidInputs = Set(
        "")
}

object VarDecGrammar1_1 extends GrammarWithStringSamples with Grammar0 {
    val name = "JS VarDec Test 1_1"
    val start = "Start"
    val rules: RuleMap = VarDecGrammar1.rules.merge(ListMap(
        "Token" -> ListSet(
            seq(n("_IdName"), lookahead_except(n("_IdName"))),
            n("Number"))))

    val validInputs = Set(
        "var abc = 123;\n\nvar xyz = 321; var if = 154;")
    val invalidInputs = Set(
        "varx=1;")
}

object VarDecGrammar1_2 extends GrammarWithStringSamples with Grammar0 {
    val name = "JS VarDec Test 1_2"
    val start = "Start"
    val rules: RuleMap = VarDecGrammar1.rules.merge(ListMap(
        "TestExpr" -> ListSet(
            n("Number"),
            expr(n("TestExpr"), i("+"), n("TestExpr")),
            expr(n("TestExpr"), i("-"), n("TestExpr")),
            expr(n("TestExpr"), i("*"), n("TestExpr")),
            expr(n("TestExpr"), i("/"), n("TestExpr")),
            expr(i("("), n("TestExpr"), i(")")))))

    val validInputs = Set(
        "var abc = 123 + 321;\n\nvar xyz = 321 * (423-1); var if = 154;")
    val invalidInputs = Set(
        "")
}

object VarDecGrammar2 extends GrammarWithStringSamples with Grammar0 {
    val name = "VarDecGrammar2"
    val start = "Start"
    val rules: RuleMap = VarDecGrammar1.rules.merge(ListMap(
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

    val validInputs = Set()
    val invalidInputs = Set()
}

object VarDecGrammar3 extends GrammarWithStringSamples with Grammar0 {
    val name = "JS VarDec with Semicolon Backup Test 1"
    val lineend = {
        val semicolon = i(";")
        val alternative = oneof(
            seq((n("WhiteSpace").except(n("LineTerminator"))).star, n("LineTerminator")),
            seq(n("WhiteSpace").star, lookahead_is(i("}"))))
        oneof(semicolon, seq(lookahead_except(semicolon), alternative))
    }

    val start = "Start"
    val rules: RuleMap = VarDecGrammar1.rules.merge(ListMap(
        "VarStmt" -> ListSet(
            seq(expr(token(i("var")), n("VarDecList")), lineend))))

    val validInputs = Set(
        "var abc = 123\n\nvar xyz = 321; var if = 154;")
    val invalidInputs = Set(
        "")
}

object JavaScriptVarDecSamples extends ExampleGrammarSet {
    val examples = Set(
        VarDecGrammar1.toPair,
        VarDecGrammar1_1.toPair,
        VarDecGrammar1_2.toPair,
        VarDecGrammar2.toPair,
        VarDecGrammar3.toPair)
}
