package com.giyeok.jparser.examples.basics

import com.giyeok.jparser.examples.{MetaLang1Example, MetaLangExample, MetaLangExamples}

object TrickyLookaheadsTests1 extends MetaLangExamples {
    val tricky1: MetaLang1Example = MetaLang1Example("Tricky Lookaheads 1",
        """S = 'x' $$"abc" "abc"
          |""".stripMargin)
        .example("xabc")

    val tricky2: MetaLang1Example = MetaLang1Example("Tricky Lookaheads 2",
        """S = 'x' $!"abc" "abc"
          |""".stripMargin)
        .incorrect("xabc")

    val examples: List[MetaLangExample] = List(tricky1, tricky2)
}
