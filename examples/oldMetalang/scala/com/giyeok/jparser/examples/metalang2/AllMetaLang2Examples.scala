package com.giyeok.jparser.examples.metalang2

import com.giyeok.jparser.examples.MetaLang2Example

object AllMetaLang2Examples {
  val examples: Seq[MetaLang2Example] = List(
    ExpressionGrammarsMetaLang2.examples,
    MetaLang2GrammarExamples.examples,
  ).flatten
}
