package com.giyeok.jparser.examples2

import com.giyeok.jparser.examples1.ExampleGrammarSet

object TrickyGrammar1 extends MetaGrammarWithStringSamples("Tricky Grammar 1") {
    val grammarText ="""S = 'x' $$"abc" "abc""""
    val validInputs = Set("xabc")
    val invalidInputs = Set()
}

object TrickyGrammar2 extends MetaGrammarWithStringSamples("Tricky Grammar 2") {
    val grammarText ="""S = 'x' $!"abc" "abc""""
    val validInputs = Set()
    val invalidInputs = Set("xabc")
}

object TrickyGrammars extends ExampleGrammarSet {
    val examples = Set(
        TrickyGrammar1.toPair,
        TrickyGrammar2.toPair
    )
}
