package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.MetaLang3Example

object AllMetaLang3Examples {
  val examples: Seq[MetaLang3Example] = List(
    SimpleExamples.examples,
    JoinExamples.examples,
    ClassRelExamples.examples,
    OptionalExamples.examples,
    PExprExamples.examples,
    MetaLang3GrammarExamples.examples,
  ).flatten
}
