package com.giyeok.jparser.examples.naive

import com.giyeok.jparser.{Grammar, Inputs, NGrammar}

trait GrammarWithExamples extends Examples {
    val grammar: Grammar

    lazy val ngrammar: NGrammar = NGrammar.fromGrammar(grammar)
}

trait Examples {
    val correctExampleInputs: Set[Inputs.ConcreteSource]
    val incorrectExampleInputs: Set[Inputs.ConcreteSource]
}

trait StringExamples extends Examples {
    val correctExamples: Set[String]
    val incorrectExamples: Set[String]

    lazy val correctExampleInputs: Set[Inputs.ConcreteSource] = correctExamples map Inputs.fromString
    lazy val incorrectExampleInputs: Set[Inputs.ConcreteSource] = incorrectExamples map Inputs.fromString
}

trait AmbiguousExamples extends Examples {
    val ambiguousExamples: Set[String]
    lazy val ambiguousExampleInputs: Set[Inputs.ConcreteSource] = ambiguousExamples map Inputs.fromString
}
