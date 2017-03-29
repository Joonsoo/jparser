package com.giyeok.jparser.tests

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.nparser
import com.giyeok.jparser.nparser.CompactNGrammar
import com.giyeok.jparser.nparser.NGrammar

trait Samples {
    val correctSampleInputs: Set[Inputs.ConcreteSource]
    val incorrectSampleInputs: Set[Inputs.ConcreteSource]
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]

    lazy val correctSampleInputs: Set[Inputs.ConcreteSource] = correctSamples map { Inputs.fromString }
    lazy val incorrectSampleInputs: Set[Inputs.ConcreteSource] = incorrectSamples map { Inputs.fromString }
}

trait AmbiguousSamples extends Samples {
    val ambiguousSamples: Set[String]
    lazy val ambiguousSampleInputs: Set[Inputs.ConcreteSource] = ambiguousSamples map { Inputs.fromString }
}

trait GrammarTestCases extends Samples {
    val grammar: Grammar

    type R = ParseResultGraph
    val resultFunc = ParseResultGraphFunc

    lazy val ngrammar = NGrammar.fromGrammar(grammar)
    lazy val compactGrammar = CompactNGrammar.fromNGrammar(ngrammar)

    //    lazy val derivationPreprocessor = new OnDemandDerivationPreprocessor(ngrammar)
    //    lazy val slicedDerivationPreprocessor = new OnDemandSlicedDerivationPreprocessor(ngrammar)
    //    lazy val compactDerivationPreprocessor = new OnDemandCompactDerivationPreprocessor(compactGrammar)
    //    lazy val compactSlicedDerivationPreprocessor = new OnDemandCompactSlicedDerivationPreprocessor(compactGrammar)

    lazy val nparserNaive = new nparser.NaiveParser(ngrammar)
    //    lazy val nparserPreprocessed = new nparser.PreprocessedParser(ngrammar, derivationPreprocessor)
    //    lazy val nparserSlicedPreprocessed = new nparser.SlicedPreprocessedParser(ngrammar, slicedDerivationPreprocessor)
    //    lazy val nparserCompactPreprocessed = new nparser.PreprocessedParser(ngrammar, compactDerivationPreprocessor)
    //    lazy val nparserCompactSlicedPreprocessed = new nparser.SlicedPreprocessedParser(ngrammar, compactSlicedDerivationPreprocessor)
}
