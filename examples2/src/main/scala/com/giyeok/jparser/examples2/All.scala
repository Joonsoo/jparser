package com.giyeok.jparser.examples2

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples1.{ExampleGrammarSet, StringSamples}

object All {
    val allExampleSets: List[ExampleGrammarSet] = List(
        MetaGrammarSampleSet,
        ExpressionGrammars,
        LexicalGrammars,
        TrickyGrammars
    )
    val allExamples: List[(Grammar, StringSamples)] = allExampleSets flatMap {_.examples}
}
