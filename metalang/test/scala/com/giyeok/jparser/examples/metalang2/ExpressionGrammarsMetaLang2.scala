package com.giyeok.jparser.examples.metalang2

import com.giyeok.jparser.examples.{MetaLang2Example, MetaLangExamples}

object ExpressionGrammarsMetaLang2 extends MetaLangExamples {
  val basic = MetaLang2Example("MetaLang2 Expression Grammar 0",
    """expression: @Expression = term
      |    | expression WS '+' WS term {@BinOp(op=$2, lhs:Expression=$0, rhs=$4)}
      |term: @Term = factor
      |    | term WS '*' WS factor {BinOp($2, $0, $4)}
      |factor: @Factor = number
      |    | variable
      |    | '(' WS expression WS ')' {@Paren(expr=$2)}
      |number: @Number = '0' {@Integer(value=[$0])}
      |    | '1-9' '0-9'* {Integer([$0] + $1)}
      |variable = <'A-Za-z'+> {@Variable(name=$0)}
      |WS = ' '*
      |""".stripMargin)
    .example("123*(456+abc)")

  val examples: List[MetaLang2Example] = List(basic)
}
