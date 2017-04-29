package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.StringSamples

object MetaGrammarTests extends GrammarTestCases with StringSamples {
    val grammar = MetaGrammar

    val correctSamples = Set(
        "S = 'a'? 'b'+ <'c'*>",
        "S = (abc|def)",
        """S = "asdf"
          |  | A
          |A = ASDF
          |  | "qwer"
          |ASDF = 'c'?
        """.stripMargin
    )
    val incorrectSamples = Set[String]()

}
