package com.giyeok.jparser.examples.metalang2

import com.giyeok.jparser.examples.{MetaLang2Example, MetaLangExamples}

object MetaLang2GrammarExamples extends MetaLangExamples {
  val examples: List[MetaLang2Example] = List(
    MetaLang2Example("Meta Language 2 in MetaLang2", MetaLang2Grammar.inMetaLang2),
    MetaLang2Example("Meta Language 3 in MetaLang2", MetaLang3Grammar.inMetaLang2),
  )
}
