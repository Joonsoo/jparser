package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.metalang2.MetaLang3Grammar
import com.giyeok.jparser.examples.{MetaLang3Example, MetaLangExamples}

object MetaLang3GrammarExamples extends MetaLangExamples {
  val examples = List(
    MetaLang3Example("Meta Language 3", MetaLang3Grammar.inMetaLang3)
      .examples(SimpleExamples.examples.map(_.grammar)),
  )
}
