package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.examples.MetaLang3Example
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3a.ValuefyExpr.UnrollChoices

object MetaLanguage3 {

    case class IllegalGrammar(msg: String) extends Exception(msg)

    def check(cond: Boolean, errorMessage: String): Unit = {
        if (!cond) throw IllegalGrammar(errorMessage)
    }

    def parseGrammar(grammar: String): MetaGrammar3Ast.Grammar = MetaGrammar3Ast.parseAst(grammar) match {
        case Left(value) => value
        case Right(value) => throw IllegalGrammar(value.msg)
    }

    case class ProcessedGrammar(ngrammar: NGrammar, startNonterminalName: String, nonterminalValuefyExprs: Map[String, UnrollChoices])

    def analyzeGrammar(grammar: MetaGrammar3Ast.Grammar, grammarName: String): ProcessedGrammar = {
        val transformer = new GrammarTransformer(grammar)
        val ngrammar = NGrammar.fromGrammar(transformer.grammar(grammarName))
        ProcessedGrammar(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs)
    }

    def analyzeGrammar(grammarDefinition: String, grammarName: String = "GeneratedGrammar"): ProcessedGrammar =
        analyzeGrammar(parseGrammar(grammarDefinition), grammarName)

    def main(args: Array[String]): Unit = {
        def testExample(example: MetaLang3Example): Unit = {
            val analysis = analyzeGrammar(example.grammar, example.name)
            val valuefyExprSimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs)
            example.correctExamples.foreach { exampleSource =>
                val parsed = valuefyExprSimulator.parse(exampleSource).left.get
                // valuefyExprSimulator.printNodeStructure(parsed)
                println(valuefyExprSimulator.valuefy(parsed).prettyPrint())
            }
        }

        testExample(MetaLang3Example("Simple test",
            """A = B&X {MyClass(value=$0, qs=[])} | B&X Q {MyClass(value=$0, qs=$1)}
              |B = C 'b'
              |C = 'c' D
              |D = 'd' | #
              |X = 'c' 'd'* 'b'
              |Q = 'q'+ {QValue(value="hello")}
              |""".stripMargin)
            .example("cb")
            .example("cdb")
            .example("cbqq"))
    }
}
