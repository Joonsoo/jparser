package com.giyeok.jparser.examples

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples.MetaLang3Example.CorrectExample
import com.giyeok.jparser.examples.metalang._
import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar

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

case class MetaLang3Example(name: String, grammar: String,
                            correctExamplesWithResults: List[CorrectExample] = List(),
                            incorrectExamples: List[String] = List(),
                            ambiguousExamples: List[String] = List()) extends MetaLangExample {
    def example(newExample: String): MetaLang3Example =
        copy(correctExamplesWithResults = correctExamplesWithResults :+ CorrectExample(newExample, None))

    def examples(newExamples: List[String]): MetaLang3Example =
        copy(correctExamplesWithResults = correctExamplesWithResults ++ newExamples.map(CorrectExample(_, None)))

    def example(newExample: String, valuefyResult: String): MetaLang3Example =
        copy(correctExamplesWithResults = correctExamplesWithResults :+ CorrectExample(newExample, Some(valuefyResult)))

    override val correctExamples: List[String] = correctExamplesWithResults.map(_.input)
}

object MetaLang3Example {

    case class CorrectExample(input: String, value: Option[String])

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
        MetaLangGrammar.examples,
        MetaLang3Grammar.examples).flatten
}
