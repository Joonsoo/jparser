package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.visualize.ParseGraphVisualizer
import com.giyeok.moonparser.Inputs

object SampleVisualization {
    val allTests: Set[{ val correctSamples: Set[String] }] = Set()

    def main(args: Array[String]): Unit = {
        ParseGraphVisualizer.start(SimpleGrammar1_1, Inputs.fromString("abcabcdefedfs"))
    }
}
