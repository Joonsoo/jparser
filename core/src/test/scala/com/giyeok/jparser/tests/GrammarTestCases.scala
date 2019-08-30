package com.giyeok.jparser.tests

import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.{Grammar, Inputs, NGrammar}

trait Samples {
    val correctSampleInputs: Set[Inputs.ConcreteSource]
    val incorrectSampleInputs: Set[Inputs.ConcreteSource]
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]

    lazy val correctSampleInputs: Set[Inputs.ConcreteSource] = correctSamples map Inputs.fromString
    lazy val incorrectSampleInputs: Set[Inputs.ConcreteSource] = incorrectSamples map Inputs.fromString
}

trait AmbiguousSamples extends Samples {
    val ambiguousSamples: Set[String]
    lazy val ambiguousSampleInputs: Set[Inputs.ConcreteSource] = ambiguousSamples map Inputs.fromString
}

trait GrammarTestCases extends Samples {
    val grammar: Grammar

    lazy val ngrammar: NGrammar = NGrammar.fromGrammar(grammar)
    lazy val naiveParser = new NaiveParser(ngrammar)
}
