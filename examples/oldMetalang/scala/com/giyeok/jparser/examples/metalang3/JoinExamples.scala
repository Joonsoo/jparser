package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExamples}

object JoinExamples extends MetaLangExamples {
    val simple: MetaLang3Example = MetaLang3Example("Simple Join",
        """A = ('abc'+ & 'bcd'+)?
          |""".stripMargin)
        .example("bbcc")

    override val examples: List[MetaLang3Example] = List(simple)
}
