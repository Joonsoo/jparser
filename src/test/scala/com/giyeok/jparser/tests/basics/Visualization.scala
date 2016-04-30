package com.giyeok.moonparser.tests.basics

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.tests.javascript.JavaScriptGrammar
import com.giyeok.moonparser.tests.Samples
import com.giyeok.moonparser.tests.StringSamples
import com.giyeok.moonparser.tests.Viewer

object Visualization extends Viewer {
    val allTests: Set[Grammar with Samples] = Set(
        SimpleGrammarSet1.grammars,
        SimpleGrammarSet2.grammars,
        SimpleGrammarSet3.grammars,
        RecursiveGrammarSet1.grammars,
        GrammarWithExcept.grammars,
        GrammarWithLookaheadExcept.grammars).flatten

    def main(args: Array[String]): Unit = {
        start()
    }
}
