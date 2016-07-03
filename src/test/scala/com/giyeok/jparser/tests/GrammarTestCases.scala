package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.Grammar
import com.giyeok.jparser.NewParser
import com.giyeok.jparser.DerivationSliceFunc
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.NaiveParser
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultGraph
import com.giyeok.jparser.ParseResultGraphFunc

trait Samples {
    val correctSampleInputs: Set[Inputs.ConcreteSource]
    val incorrectSampleInputs: Set[Inputs.ConcreteSource]
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]

    lazy val correctSampleInputs: Set[Inputs.ConcreteSource] = correctSamples map { Inputs.fromString _ }
    lazy val incorrectSampleInputs: Set[Inputs.ConcreteSource] = incorrectSamples map { Inputs.fromString _ }
}

trait AmbiguousSamples extends Samples {
    val ambiguousSamples: Set[String]
    lazy val ambiguousSampleInputs: Set[Inputs.ConcreteSource] = ambiguousSamples map { Inputs.fromString _ }
}

trait GrammarTestCases extends Samples {
    val grammar: Grammar

    lazy val parser: NewParser[ParseResultGraph] = {
        val dgraph = new DerivationSliceFunc(grammar, ParseResultGraphFunc)
        // val parser = new NewParser(grammar, ParseResultGraphFunc, dgraph)
        val parser = new NaiveParser(grammar, ParseResultGraphFunc, dgraph)
        parser
    }
}

trait PreprocessedParser extends GrammarTestCases {
    override lazy val parser = {
        val dfunc = new DerivationSliceFunc(grammar, ParseResultGraphFunc)
        val startTime = System.currentTimeMillis()
        println("Preprocess begins")
        dfunc.preprocess
        println(s"Preprocess done in ${System.currentTimeMillis() - startTime} ms")
        new NewParser(grammar, ParseResultGraphFunc, dfunc)
    }
}
