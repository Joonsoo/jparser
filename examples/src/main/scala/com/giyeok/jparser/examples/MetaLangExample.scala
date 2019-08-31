package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples.metalang._

sealed trait MetaLangExample {
    val name: String
    val grammar: String
    val correctExamples: List[String]
    val incorrectExamples: List[String]
    val ambiguousExamples: List[String]
}

case class MetaLang1Example(name: String, grammar: String,
                            correctExamples: List[String] = List(),
                            incorrectExamples: List[String] = List(),
                            ambiguousExamples: List[String] = List()) extends MetaLangExample {
    def toGrammar(translateForce: (String, String) => Grammar): Grammar = translateForce(name, grammar)

    def example(newExample: String): MetaLang1Example = copy(correctExamples = correctExamples :+ newExample)

    def incorrect(newExample: String): MetaLang1Example = copy(incorrectExamples = incorrectExamples :+ newExample)
}

case class MetaLang2Example(name: String, grammar: String,
                            correctExamples: List[String] = List(),
                            incorrectExamples: List[String] = List(),
                            ambiguousExamples: List[String] = List()) extends MetaLangExample {
    def example(newExample: String): MetaLang2Example = copy(correctExamples = correctExamples :+ newExample)
}

trait MetaLangExamples {
    val examples: List[MetaLangExample]
}

object MetaLangExamples extends MetaLangExamples {
    val examples: List[MetaLangExample] = List(
        SimpleGrammars.examples,
        ExpressionGrammars.examples,
        JsonGrammar.examples,
        LexicalGrammars.examples,
        MetaLangGrammar.examples).flatten
}
