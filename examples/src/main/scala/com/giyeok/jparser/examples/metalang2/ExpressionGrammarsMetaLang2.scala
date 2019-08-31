package com.giyeok.jparser.examples.metalang2

import com.giyeok.jparser.examples.{MetaLang2Example, MetaLangExample, MetaLangExamples}

object ExpressionGrammarsMetaLang2 extends MetaLangExamples {
    val basic = MetaLang2Example("MetaLang2 Expression Grammar 0",
        """expression: @Expression = term
          |    | expression '+' term {@BinOp(op=$1, lhs:Expression=$0, rhs=$2)}
          |term: @Term = factor
          |    | term '*' factor {BinOp($1, $0, $2)}
          |factor: @Factor = number
          |    | variable
          |    | '(' expression ')' {@Paren(expr=$1)}
          |number: @Number = '0' {@Integer(value=[$0])}
          |    | '1-9' '0-9'* {Integer([$0] + $1)}
          |variable = <'A-Za-z'+> {@Variable(name=$0)}
          |""".stripMargin)
        .example("123*(456+abc)")

    val examples: List[MetaLangExample] = List(basic)
}
