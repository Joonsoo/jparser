package com.giyeok.jparser.examples.metalang3

import com.giyeok.jparser.examples.MetaLang3Example

object AllMetaLang3Examples {
    val examples: Seq[MetaLang3Example] =
        (SimpleExamples.examples ++ JoinExamples.examples) // :+ MetaLang3Grammar.inMetaLang3
}
