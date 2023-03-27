package com.giyeok.jparser.examples.naive.javascript

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.GrammarHelper._
import com.giyeok.jparser.examples.naive.{GrammarWithExamples, StringExamples}

import scala.collection.immutable.{ListMap, ListSet}

trait Grammar0 extends Grammar {
  def expr(s: Symbol*): Symbol = seqWS(oneof(n("WhiteSpace"), n("LineTerminator"), n("Comment")).star, s: _*)

  def token(s: Symbol): Symbol = s.join(n("Token"))
}

object VarDecGrammar1 extends Grammar0 with GrammarWithExamples with StringExamples {
  val name = "JS VarDec Test 1"

  val startSymbol = n("Start")
  val _rules: RuleMap = ListMap(
    "Start" -> List(
      expr(n("Stmt"), n("Start")),
      n("Stmt")),
    "Stmt" -> List(
      n("VarStmt")),
    "VarStmt" -> List(
      expr(token(i("var")), n("VarDecList"), i(";"))),
    "VarDecList" -> List(
      n("VarDec"),
      expr(n("VarDecList"), i(","), n("VarDec"))),
    "VarDec" -> List(
      expr(token(n("_IdName")), n("Init").opt)),
    "Init" -> List(
      expr(i("="), n("TestExpr"))),
    "TestExpr" -> List(
      token(chars('0' to '9').plus)),

    "_IdName" -> List(
      n("IdStart"),
      seq(n("_IdName"), n("IdPart"))),
    "IdStart" -> List(
      unicode("Lu", "Ll", "Lt", "Lm", "Lo", "Nl"),
      i("$"),
      i("_")),
    "IdPart" -> List(
      n("IdStart"),
      unicode("Nd")),
    "Number" -> List(
      chars('0' to '9').plus),
    "Token" -> List(
      n("_IdName"),
      n("Number")),

    "WhiteSpace" -> List(
      chars("\t\u000B\u000C\uFEFF"), unicode("Zs")), // \u0020\u00A0  ->  already in Zs
    "LineTerminator" -> List(
      chars("\n\r")),
    "Comment" -> List(
      seq(i("/*"), chars(" abcdefghijklmnopqrstuvwxyz\n\r\t").plus, i("*/"))))
  val rules = _rules

  val grammar = this
  val correctExamples = Set(
    "var abc = 123;\n\nvar xyz = 321; var if = 154;")
  val incorrectExamples = Set(
    "")
}

object VarDecGrammar1_1 extends Grammar0 with GrammarWithExamples with StringExamples {
  val name = "JS VarDec Test 1_1"
  val startSymbol = n("Start")
  val rules: RuleMap = VarDecGrammar1._rules.merge(ListMap(
    "Token" -> List(
      seq(n("_IdName"), lookahead_except(n("_IdName"))),
      n("Number"))))

  val grammar = this
  val correctExamples = Set(
    "var abc = 123;\n\nvar xyz = 321; var if = 154;")
  val incorrectExamples = Set(
    "varx=1;")
}

object VarDecGrammar1_2 extends Grammar0 with GrammarWithExamples with StringExamples {
  val name = "JS VarDec Test 1_2"
  val startSymbol = n("Start")
  val rules: RuleMap = VarDecGrammar1._rules.merge(ListMap(
    "TestExpr" -> List(
      n("Number"),
      expr(n("TestExpr"), i("+"), n("TestExpr")),
      expr(n("TestExpr"), i("-"), n("TestExpr")),
      expr(n("TestExpr"), i("*"), n("TestExpr")),
      expr(n("TestExpr"), i("/"), n("TestExpr")),
      expr(i("("), n("TestExpr"), i(")")))))

  val grammar = this
  val correctExamples = Set(
    "var abc = 123 + 321;\n\nvar xyz = 321 * (423-1); var if = 154;")
  val incorrectExamples = Set(
    "")
}

object VarDecGrammar2 extends Grammar0 with GrammarWithExamples with StringExamples {
  val name = "VarDecGrammar2"
  val startSymbol = n("Start")
  val rules: RuleMap = VarDecGrammar1._rules.merge(ListMap(
    "VarDec" -> List(
      expr(token(n("Id")), n("Init").opt)),

    "Id" -> List(
      n("IdName").butnot(n("Kw"))),
    "IdName" -> List(
      token(n("_IdName"))),
    "Kw" -> List(
      token(i("if")),
      token(i("var"))),

    "_IdName" -> List(
      n("IdStart"),
      seq(n("_IdName"), n("IdPart"), lookahead_except(n("IdPart")))),
    "Token" -> List(
      n("Id"))))

  val grammar = this
  val correctExamples = Set[String]()
  val incorrectExamples = Set[String]()
}

object VarDecGrammar3 extends Grammar0 with GrammarWithExamples with StringExamples {
  val name = "JS VarDec with Semicolon Backup Test 1"
  val lineend = {
    val semicolon = i(";")
    val alternative = oneof(
      seq((n("WhiteSpace").except(n("LineTerminator"))).star, n("LineTerminator")),
      seq(n("WhiteSpace").star, lookahead_is(i("}"))))
    oneof(semicolon, seq(lookahead_except(semicolon), alternative))
  }

  val startSymbol = n("Start")
  val rules: RuleMap = VarDecGrammar1._rules.merge(ListMap(
    "VarStmt" -> List(
      seq(expr(token(i("var")), n("VarDecList")), lineend))))

  val grammar = this
  val correctExamples = Set(
    "var abc = 123\n\nvar xyz = 321; var if = 154;")
  val incorrectExamples = Set(
    "")
}

object JavaScriptVarDecTestSuite1 {
  val tests: Set[GrammarWithExamples] = Set(
    VarDecGrammar1,
    VarDecGrammar1_1,
    VarDecGrammar1_2,
    VarDecGrammar2,
    VarDecGrammar3)
}
