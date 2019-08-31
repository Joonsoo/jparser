package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.Inputs.ConcreteSource
import com.giyeok.jparser.examples.metagram.{ExpressionGrammars, LexicalGrammars, MetaGram1Example, MetaGramInMetaGram}
import com.giyeok.jparser.examples.{GrammarWithExamples, StringExamples}
import com.giyeok.jparser.gramgram.{GrammarGrammar, MetaGrammar}
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.{Grammar, Inputs}

object GrammarGrammarTests1 extends GrammarWithExamples with StringExamples {
    val grammar: GrammarGrammar.type = GrammarGrammar

    private val paperGrammar =
        """S = `Stmt+
          |Stmt = LetStmt
          |     | ExprStmt
          |LetStmt = Let ' ' Id ' ' Expr ';'
          |Let = Let0&Name
          |Let0 = 'l' 'e' 't'
          |Name = L(`[a-z]+)
          |Id = Name-Let
          |ExprStmt = Expr ';' la(LetStmt)
          |Token = '+' | Id
          |Expr = `Token+
          |`Stmt+ = `Stmt+ Stmt | Stmt
          |`Token+ = `Token+ Token | Token
          |`[a-z]+ = `[a-z]+ `[a-z] | `[a-z]
          |`[a-z] = [a-z]
          |""".stripMargin('|')
    val correctExamples: Set[String] = Set(paperGrammar, paperGrammar * 10)
    val incorrectExamples: Set[String] = Set[String]()
}

case class GrammarTestCasesFromMetaGram1Example(example: MetaGram1Example) extends GrammarWithExamples {
    lazy val grammar: Grammar = MetaGrammar.translateForce(example.name, example.grammar)

    val correctExampleInputs: Set[ConcreteSource] = example.correctExamples.toSet map Inputs.fromString
    val incorrectExampleInputs: Set[ConcreteSource] = example.incorrectExamples.toSet map Inputs.fromString
}

object GrammarGrammarTests {
    val tests: Set[GrammarWithExamples] = Set(
        GrammarGrammarTests1,
        GrammarTestCasesFromMetaGram1Example(MetaGramInMetaGram.metaGrammar1),
        GrammarTestCasesFromMetaGram1Example(MetaGramInMetaGram.metaGrammar2),
        GrammarTestCasesFromMetaGram1Example(ExpressionGrammars.basic),
        GrammarTestCasesFromMetaGram1Example(ExpressionGrammars.basic1),
        GrammarTestCasesFromMetaGram1Example(LexicalGrammars.basic0),
        GrammarTestCasesFromMetaGram1Example(LexicalGrammars.basic1),
        GrammarTestCasesFromMetaGram1Example(LexicalGrammars.basic2)
    )
}

class GrammarGrammarTestSuite1 extends BasicParseTest(GrammarGrammarTests.tests)
