package com.giyeok.jparser.examples2

import com.giyeok.jparser.examples1.ExampleGrammarSet

object ExpressionGrammar1 extends MetaGrammarWithStringSamples("Expression Grammar 1") {
    val grammarText: String =
        """expression = term | expression '+' term
          |term = factor | term '*' factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | {1-9} {0-9}*
          |variable = {A-Za-z}+""".stripMargin('|')

    val validInputs = Set(
        "1+2",
        "a+b",
        "1234+1234*4321"
    )
    val invalidInputs = Set()
}

object ExpressionGrammar2 extends MetaGrammarWithStringSamples("Expression Grammar 2") {
    val grammarText: String =
        """expression = term | expression {+\-} term
          |term = factor | term {*/} factor
          |factor = number | variable | '(' expression ')'
          |number = '0' | [{+\-}? {1-9} {0-9}* [{eE} {+\-}? {0-9}+]?]
          |variable = {A-Za-z}+""".stripMargin('|')

    val validInputs = Set(
        "1e+1+1",
        "e+e",
        "1234e+1234++1234",
        "1234e+1234++1234*abcdef-e"
    )
    val invalidInputs = Set()
}

object ExpressionGrammars extends ExampleGrammarSet {
    val examples = Set(
        ExpressionGrammar1.toPair,
        ExpressionGrammar2.toPair
    )
}
