package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExamples}

object ClassRelExamples extends MetaLangExamples {
  val concreteWithSuper: MetaLang3Example = MetaLang3Example("Concrete class with super class",
    """MyClass<SuperClass>(value: string)
      |A: AnotherClass = # {MyClass("123")}
      |""".stripMargin)
    .example("", "MyClass(\"123\")")

  val concreteWithTwoSupers: MetaLang3Example = MetaLang3Example("Concrete class with two super classes",
    """MyClass<SuperClass, AnohterClass>(value: string)
      |A: AnotherClass = # {MyClass("123")}
      |""".stripMargin)
    .example("", "MyClass(\"123\")")

  val concreteWithUnspecifiedSuper: MetaLang3Example = MetaLang3Example("Concrete class with unspecified supers",
    """MyClass(value: string)
      |A: AnotherClass = # {MyClass("123")}
      |""".stripMargin)
    .example("", "MyClass(\"123\")")

  val superDef: MetaLang3Example = MetaLang3Example("Super class def",
    """SuperClass<GrandSuper> {
      |  SomeClass,
      |  SubSuperClass<AnotherClass> {
      |    IndirectSubConcreteClass<>(param1:string)
      |  },
      |  DirectSubConcreteClass<>()
      |}
      |A = 'a'
      |""".stripMargin)
    .example("a")

  override val examples: List[MetaLang3Example] = List(
    concreteWithSuper,
    concreteWithTwoSupers,
    concreteWithUnspecifiedSuper,
    superDef,
  )
}
