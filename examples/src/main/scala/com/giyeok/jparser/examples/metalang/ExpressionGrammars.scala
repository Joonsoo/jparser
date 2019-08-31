package com.giyeok.jparser.examples.metalang

import com.giyeok.jparser.examples.{MetaLang1Example, MetaLangExamples}

object ExpressionGrammars extends MetaLangExamples {
    val simple: MetaLang1Example = MetaLang1Example(
        "Expression Grammar Simple",
        """E = T | E '+' T
          |T = F | T '*' F
          |F = N | '(' E ')'
          |N = '0' | {1-9} {0-9}*""".stripMargin)

    val basic: MetaLang1Example = MetaLang1Example(
        "Expression Grammar 0",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+""".stripMargin)
        .example("1+2")
        .example("a+b")
        .example("1234+1234*4321")

    val basic1: MetaLang1Example = MetaLang1Example(
        "Expression Grammar 1",
        """expression = term | expression {+\-} term
          |term = factor | term {*/} factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | [{+\-}? {1-9} {0-9}* [{eE} {+\-}? {0-9}+]?]
          |variable = {A-Za-z}+""".stripMargin('|'))
        .example("1e+1+1")
        .example("e+e")
        .example("1234e+1234++1234")
        .example("1234e+1234++1234*abcdef-e")

    val withStringInterpolation0: MetaLang1Example = MetaLang1Example(
        "Expression Grammar with String Interpolation",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')' | string
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+
          |string = '"' stringElem* '"'
          |stringElem = stringExpr | stringChar | stringEscape
          |stringExpr = '$' '{' expression '}'
          |stringChar = {a-zA-Z0-9 }
          |stringEscape = '\\' {$\\n}""".stripMargin)
        .example(""""${"${1+2}"}"""")
        .example(""""${"${"1"+"2"}"}"""")

    val withStringInterpolation: MetaLang1Example = MetaLang1Example(
        "Expression Grammar with String Interpolation",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')' | string
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+
          |string = '"' stringElem* '"'
          |stringElem = stringExpr | stringChar | stringEscape
          |stringExpr = '$' '{' expression '}'
          |stringChar = .-{"\\$}
          |stringEscape = '\\' .""".stripMargin)
        .example(""""${"${1+2}"}"""")
        .example(""""${"${"1"+"2"}"}"""")

    val examples: List[MetaLang1Example] = List(simple, basic, basic1, withStringInterpolation0, withStringInterpolation)
}
