package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar

object All {
    val allExampleSets: List[ExampleGrammarSet] = List(
        BackupGrammars,
        ExceptGrammars1,
        JavaScriptSamples1,
        JavaScriptVarDecSamples,
        JoinGrammars,
        PaperSamples,
        ParsingTechniquesTests,
        LongestMatchGrammars,
        LookaheadGrammars,
        LookaheadIsGrammars,
        RecursiveGrammars,
        SimpleGrammars1,
        SimpleGrammars2,
        SimpleGrammars3
    )
    val allExamples: List[(Grammar, StringSamples)] = allExampleSets flatMap {_.examples}
    val allGrammars: List[Grammar] = allExamples map {_._1}
}

trait ExampleGrammarSet {
    val examples: Set[(Grammar, StringSamples)]
}
