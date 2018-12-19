package com.giyeok.jparser.tests

import com.giyeok.jparser.examples1.StringSamples
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser}
import com.giyeok.jparser.npreparser.PreprocessedParser
import com.giyeok.jparser._

case class GrammarTestCases(grammar: Grammar, samples: StringSamples) {
    lazy val ngrammar: NGrammar = NGrammar.fromGrammar(grammar)
    lazy val naiveParser = new NaiveParser(ngrammar)
    lazy val preprocessedParser = new PreprocessedParser(ngrammar)
}
