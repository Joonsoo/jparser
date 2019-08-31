package com.giyeok.jparser.examples.metagram

object ExpressionGrammars extends MetaGramExamples {
    val simple: MetaGram1Example = MetaGram1Example(
        "Expression Grammar Simple",
        """E = T | E '+' T
          |T = F | T '*' F
          |F = N | '(' E ')'
          |N = '0' | {1-9} {0-9}*""".stripMargin)

    val basic: MetaGram1Example = MetaGram1Example(
        "Expression Grammar 0",
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+""".stripMargin)
        .example("1+2")
        .example("a+b")
        .example("1234+1234*4321")

    val basic1: MetaGram1Example = MetaGram1Example(
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

    val withStringInterpolation0: MetaGram1Example = MetaGram1Example(
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

    val withStringInterpolation: MetaGram1Example = MetaGram1Example(
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

    val examples: List[MetaGram1Example] = List(simple, basic, basic1, withStringInterpolation0, withStringInterpolation)
}
