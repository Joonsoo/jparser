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

        val typeInferer = new TypeInferer(transformer.startNonterminalName(), transformer.nonterminalInfo.specifiedTypes)
        transformer.classInfo.classConstructCalls.foreach { calls =>
            calls._2.foreach { call =>
                println(call)
                call.foreach { param =>
                    println(typeInferer.typeOfValuefyExpr(param))
                }
            }
        }

        val ngrammar = NGrammar.fromGrammar(transformer.grammar(grammarName))
        ProcessedGrammar(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs)
    }

    def analyzeGrammar(grammarDefinition: String, grammarName: String = "GeneratedGrammar"): ProcessedGrammar =
        analyzeGrammar(parseGrammar(grammarDefinition), grammarName)

    def main(args: Array[String]): Unit = {
        def testExample(example: MetaLang3Example): Unit = {
            println(example.grammar)
            val analysis = analyzeGrammar(example.grammar, example.name)
            val valuefyExprSimulator = new ValuefyExprSimulator(analysis.ngrammar, analysis.startNonterminalName, analysis.nonterminalValuefyExprs)
            example.correctExamples.foreach { exampleSource =>
                val parsed = valuefyExprSimulator.parse(exampleSource).left.get
                valuefyExprSimulator.printNodeStructure(parsed)
                valuefyExprSimulator.printValuefyStructure()
                println(valuefyExprSimulator.valuefy(parsed).prettyPrint())
            }
        }

        val ex1 = MetaLang3Example("Simple test",
            """A = B&X {MyClass(value=$0, qs=[])} | B&X Q {MyClass(value=$0, qs=$1)}
              |B = C 'b'
              |C = 'c' D
              |D = 'd' | #
              |X = 'c' 'd'* 'b'
              |Q = 'q'+ {QValue(value="hello")}
              |""".stripMargin)
            .example("cb")
            .example("cdb")
            .example("cbqq")
        val ex2 = MetaLang3Example("Simple",
            """A = ('b' Y 'd') 'x' {A(val=$0$1, raw=$0\\$1, raw1=\\$0)}
              |Y = 'y' {Y(value=true)}
              |""".stripMargin)
            .example("bydx")
        //        testExample(ex1)
        //        testExample(ex2)
        val ex3 = MetaLang3Example("BinOp",
            """A = B ' ' C {ClassA(value=str($0) + str($2))}
              |B = 'a'
              |C = 'b'
              |""".stripMargin)
            .example("a b")
        testExample(ex3)
    }
}
