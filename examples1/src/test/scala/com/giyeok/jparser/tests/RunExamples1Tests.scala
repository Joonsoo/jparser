package com.giyeok.jparser.tests

import com.giyeok.jparser.examples1.All
import org.scalatest.FlatSpec

class RunExamples1Tests extends FlatSpec {
    new BasicParseTest(All.allExamples map { example =>
        GrammarTestCases(example._1, example._2)
    })
}
