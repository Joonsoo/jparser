package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExample, MetaLangExamples}

object SimpleExamples extends MetaLangExamples {
    val ex1: MetaLang3Example = MetaLang3Example("Simple test",
        """A = B&X {MyClass(value=$0, qs=[])} | B&X Q {MyClass(value=$0, qs=$1)}
          |B = C 'b'
          |C = 'c' D
          |D = 'd' | #
          |X = 'c' 'd'* 'b'
          |Q = 'q'+ {QValue(value="hello")}
          |""".stripMargin)
        .example("cb")
        .example("cdb")
        .example("cbqq")
    val ex2: MetaLang3Example = MetaLang3Example("Simple",
        """A = ('b' Y 'd') 'x' {A(val=$0$1, raw=$0\\$1, raw1=\\$0)}
          |Y = 'y' {Y(value=true)}
          |""".stripMargin)
        .example("bydx")
    val ex3: MetaLang3Example = MetaLang3Example("BinOp",
        """A = B ' ' C {ClassA(value=str($0) + str($2))}
          |B = 'a'
          |C = 'b'
          |""".stripMargin)
        .example("a b")
    val ex4: MetaLang3Example = MetaLang3Example("Nonterm type inference",
        """expression: Expression = term
          |    | expression WS '+' WS term {BinOp(op=$2, lhs:Expression=$0, rhs=$4)}
          |term: Term = factor
          |    | term WS '*' WS factor {BinOp($2, $0, $4)}
          |factor: Factor = number
          |    | variable
          |    | '(' WS expression WS ')' {Paren(expr=$2)}
          |number: Number = '0' {Integer(value=[$0])}
          |    | '1-9' '0-9'* {Integer([$0] + $1)}
          |variable = <'A-Za-z'+> {Variable(name=$0)}
          |WS = ' '*
          |""".stripMargin)
        .example("1+2")
    val ex5: MetaLang3Example = MetaLang3Example("Canonical enum",
        """A = "hello" {%MyEnum.Hello} | "xyz" {%MyEnum.Xyz}
          |""".stripMargin)
        .example("hello", "%MyEnum.Hello")
        .example("xyz", "%MyEnum.Xyz")
    val ex6a: MetaLang3Example = MetaLang3Example("Shortened enum",
        """A:%MyEnum = "hello" {%Hello} | "xyz" {%Xyz}
          |""".stripMargin)
        .example("hello", "%MyEnum.Hello")
        .example("xyz", "%MyEnum.Xyz")
    val ex6b: MetaLang3Example = MetaLang3Example("Shortened enum",
        """A:%MyEnum = B | "xyz" {%Xyz}
          |B = "hello" {%Hello}
          |""".stripMargin)
        .example("hello", "%MyEnum.Hello")
        .example("xyz", "%MyEnum.Xyz")
    val ex6c: MetaLang3Example = MetaLang3Example("Shortened enum",
        """A:%MyEnum = B
          |B = "hello" {%Hello} | "xyz" {%Xyz}
          |""".stripMargin)
        .example("hello", "%MyEnum.Hello")
        .example("xyz", "%MyEnum.Xyz")
    val ex7: MetaLang3Example = MetaLang3Example("BindExpr on repeat",
        """A = ('h' 'Ee' "ll" 'Oo')* {$0{str($1) + str($3)}}
          |""".stripMargin)
        .example("hello")

    override val examples: List[MetaLangExample] =
        List(ex1, ex2, ex3, ex4, ex5, ex6a, ex6b, ex6c)
}
