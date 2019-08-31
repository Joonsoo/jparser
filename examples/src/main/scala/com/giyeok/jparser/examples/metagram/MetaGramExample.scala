package com.giyeok.jparser.examples.metagram

import com.giyeok.jparser.{Grammar, NGrammar}

sealed trait MetaGramExample {
    val name: String
    val grammar: String
    val correctExamples: List[String]
    val incorrectExamples: List[String]
    val ambiguousExamples: List[String]
}

case class MetaGram1Example(name: String, grammar: String,
                            correctExamples: List[String] = List(),
                            incorrectExamples: List[String] = List(),
                            ambiguousExamples: List[String] = List()) extends MetaGramExample {
    def toGrammar(translateForce: (String, String) => Grammar): Grammar = translateForce(name, grammar)

    def example(newExample: String): MetaGram1Example = copy(correctExamples = correctExamples :+ newExample)
}

case class MetaGram2Example(name: String, grammar: String,
                            correctExamples: List[String] = List(),
                            incorrectExamples: List[String] = List(),
                            ambiguousExamples: List[String] = List()) extends MetaGramExample {
    def example(newExample: String): MetaGram2Example = copy(correctExamples = correctExamples :+ newExample)
}

trait MetaGramExamples {
    val examples: List[MetaGramExample]
}

object MetaGramExamples extends MetaGramExamples {
    val examples: List[MetaGramExample] = List(
        SimpleGrammars.examples,
        ExpressionGrammars.examples,
        JsonGrammar.examples,
        LexicalGrammars.examples,
        MetaGramInMetaGram.examples).flatten
}
