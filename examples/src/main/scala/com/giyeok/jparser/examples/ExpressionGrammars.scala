package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.gramgram.MetaGrammar

object ExpressionGrammars {
    val simple: Grammar = MetaGrammar.translateForce(
        "Expression Grammar Simple",
        """E = T | E '+' T
          |T = F | T '*' F
          |F = N | '(' E ')'
          |N = '0' | {1-9} {0-9}*""".stripMargin)

    val basic: Grammar = MetaGrammar.translateForce(
        "Expression Grammar",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+""".stripMargin)

    val withStringInterpolation: GrammarWithExamples = GrammarWithExamples(MetaGrammar.translateForce(
        "Expression Grammar with String Interpolation",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')' | string
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+
          |string = <['"' stringElem* '"']>
          |stringElem = <(stringExpr | stringChar | stringEscape)>
          |stringExpr = '$' '{' expression '}'
          |stringChar = .-{"\\$}
          |stringEscape = '\\' .""".stripMargin))
        .example(""""${"${1+2}"}"""")
        .example(""""${"${"1"+"2"}"}"""")
}
