package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExample, MetaLangExamples}

object PExprExamples extends MetaLangExamples {
  val ex1: MetaLang3Example = MetaLang3Example("Bind",
    """Expr: Expression = '0-9' {Number(value=str($0))}
      |    | 'A-Z' Params {FuncCall(name=str($0), params=$1)}
      |Params = '(' WS (Expr (WS ',' WS Expr)* WS)? ')' {$2{[$0] + $1} ?: []}
      |WS = ' '*
      |""".stripMargin)
    .example("A()")
    .example("A(1,2,3,4)")
    .example("A(1,B(2,3,4),3,4)")
  val ex1a: MetaLang3Example = MetaLang3Example("ex1 with no bind",
    """Expr: Expression = '0-9' {Number(value=str($0))}
      |    | 'A-Z' Params {FuncCall(name=str($0), params=$1)}
      |Params = '(' WS (Expr (WS ',' WS Expr)* WS {[$0] + $1})? ')' {$2 ?: []}
      |WS = ' '*
      |""".stripMargin)
    .example("A()")
    .example("A(1,2,3,4)")
  val ex2: MetaLang3Example = MetaLang3Example("char opt", "A: char? = 'a'?")
    .example("a", "'a'").example("", "null")
  val ex2a: MetaLang3Example = MetaLang3Example("char opt explicit", """A: char? = 'a' {$0} | "" {null}""")
    .example("a", "'a'").example("", "null")
  val ex2b: MetaLang3Example = MetaLang3Example("char opt semi-explicit", """A: char? = 'a' | "" {null}""")
    .example("a", "'a'").example("", "null")
  val ex3: MetaLang3Example = MetaLang3Example("char returning nonterm", "A: char = 'a' | 'z'")
    .example("a", "'a'").example("z", "'z'")
  val ex3a: MetaLang3Example = MetaLang3Example("char array returning nonterm", "A: [char] = 'a-z'*")
    .example("", "[]").example("xyz", "['x','y','z']")
  val ex3b: MetaLang3Example = MetaLang3Example("string returning nonterm", "A: string = \"abcdef\" | \"ghijkl\"")
    .example("abcdef", "\"abcdef\"").example("ghijkl", "\"ghijkl\"")

  override val examples: List[MetaLangExample] = List(ex1, ex1a, ex2, ex2a, ex2b, ex3, ex3a, ex3b)
}
