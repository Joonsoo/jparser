package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs

trait Samples {
    val correctSampleInputs: Set[Inputs.Source]
    val incorrectSampleInputs: Set[Inputs.Source]
}

trait StringSamples extends Samples {
    val correctSamples: Set[String]
    val incorrectSamples: Set[String]

    lazy val correctSampleInputs: Set[Inputs.Source] = correctSamples map { Inputs.fromString _ }
    lazy val incorrectSampleInputs: Set[Inputs.Source] = incorrectSamples map { Inputs.fromString _ }
}

trait AmbiguousSamples extends Samples {
    val ambiguousSamples: Set[String]
    lazy val ambiguousSampleInputs: Set[Inputs.Source] = ambiguousSamples map { Inputs.fromString _ }
}
