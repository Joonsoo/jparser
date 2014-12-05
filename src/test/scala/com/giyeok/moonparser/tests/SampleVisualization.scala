package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.visualize.ParseGraphVisualizer
import com.giyeok.moonparser.Inputs

trait Samples {
    val correctSampleInputs: Set[Inputs.Source]
    val incorrectSampleInputs: Set[Inputs.Source]
    val ambiguousSampleInputs: Set[Inputs.Source] = Set()
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]
    val ambiguousSamples: Set[String] = Set()

    val correctSampleInputs: Set[Inputs.Source] = correctSamples map { Inputs.fromString _ }
    val incorrectSampleInputs: Set[Inputs.Source] = incorrectSamples map { Inputs.fromString _ }
    val ambiguousSampleInputs: Set[Inputs.Source] = ambiguousSamples map { Inputs.fromString _ }
}

object SampleVisualization {
    val allTests: Set[Grammar with Samples] = Set(
        SimpleGrammarSet1.grammars).flatten

    def main(args: Array[String]): Unit = {
        ParseGraphVisualizer.start(SimpleGrammar1_1, Inputs.fromString("abcabcdefedfs"))
    }
}
