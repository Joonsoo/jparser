package com.giyeok.jparser.tests

import com.giyeok.jparser.examples2.All
import org.scalatest.FlatSpec

class RunExamples2Tests extends FlatSpec {
    new BasicParseTest(All.allExamples map { example =>
        GrammarTestCases(example._1, example._2)
    })
}
