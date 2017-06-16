package com.giyeok.jparser.tests.gramgram

import com.giyeok.jparser.tests.GrammarTestCases
import com.giyeok.jparser.tests.BasicParseTest
import com.giyeok.jparser.gramgram.GrammarGrammar
import com.giyeok.jparser.tests.StringSamples

object GrammarGrammarTests1 extends GrammarTestCases with StringSamples {
    val grammar = GrammarGrammar

    private val paperGrammar = """S = `Stmt+
                               #Stmt = LetStmt
                               #     | ExprStmt
                               #LetStmt = Let ' ' Id ' ' Expr ';'
                               #Let = Let0&Name
                               #Let0 = 'l' 'e' 't'
                               #Name = L(`[a-z]+)
                               #Id = Name-Let
                               #ExprStmt = Expr ';' la(LetStmt)
                               #Token = '+' | Id
                               #Expr = `Token+
                               #`Stmt+ = `Stmt+ Stmt | Stmt
                               #`Token+ = `Token+ Token | Token
                               #`[a-z]+ = `[a-z]+ `[a-z] | `[a-z]
                               #`[a-z] = [a-z]
                               #""".stripMargin('#')
    val correctSamples: Set[String] = Set(paperGrammar, paperGrammar * 10)
    val incorrectSamples: Set[String] = Set[String]()
}

object GrammarGrammarTests {
    val tests: Set[GrammarTestCases] = Set(
        GrammarGrammarTests1,
        MetaGrammarTests,
        ExpressionGrammarTests,
        LexicalGrammar0Tests,
        LexicalGrammar1Tests
    )
}

class GrammarGrammarTestSuite1 extends BasicParseTest(GrammarGrammarTests.tests)
