package com.giyeok.jparser.examples1

import com.giyeok.jparser.Grammar

trait StringSamples {
    val validInputs: Set[String]
    val invalidInputs: Set[String]
    val ambiguousInputs: Set[String] = Set()
}

abstract class GrammarWithStringSamples extends Grammar with StringSamples {
    def toPair: (Grammar, StringSamples) = (this, this)
}
