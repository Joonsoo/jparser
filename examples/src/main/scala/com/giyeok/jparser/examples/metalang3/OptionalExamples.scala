package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExample, MetaLangExamples}

object OptionalExamples extends MetaLangExamples {
    val simple: MetaLang3Example = MetaLang3Example("Simple Optional",
        """A = B {A(value:string?=str($0))}
          |B = "abc" | 'd'+ {str($0)}
          |""".stripMargin)
        .example("abc")
        .example("d")
        .example("dddd")
    val withShortEnum: MetaLang3Example = MetaLang3Example("Optional with Short Enum",
        """S = A '.' A {S(first=$0, second=$2)}
          |A = B?
          |B: %EnumB = 'a' {%A} | 'b' {%B}
          |""".stripMargin)
//        .example(".")
//        .example(".a")
//        .example(".b")
//        .example("a.")
//        .example("b.")
        .example("a.a")
//        .example("a.b")
//        .example("b.a")
//        .example("b.b")

    override val examples: List[MetaLangExample] = List(simple, withShortEnum)
}
