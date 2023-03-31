package com.giyeok.jparser.tests.metalang3a

import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.ast.MetaLang3Ast
import com.giyeok.jparser.metalang3.{ErrorCollector, GrammarTransformer, MetaLang3Parser, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.PrivateMethodTester
import org.scalatest.funspec.AnyFunSpec

class GrammarTransformerTest extends AnyFunSpec with PrivateMethodTester {
  describe("test") {
    val parser = MetaLang3Parser.parser
    val inputs = Inputs.fromString(
      """BuildScript = Def
        |
        |Def: Def = TargetDef
        |
        |TargetDef = SimpleName WS '=' WS Expr {TargetDef(name=$0)}
        |
        |Expr: Expr = "good"
        |
        |SimpleName = ('a-zA-z' {str($0)})&Tk {$0}
        |
        |Keyword = "true" | "false" | "none" | "this"
        |
        |Tk = <'a-z'+>
        |
        |WS = ' '*
        |""".stripMargin)
    val parseResult = parser.parseOrThrow(inputs)
    val history = parser.kernelsHistory(parseResult)
    val reconstructor = new ParseTreeConstructor2(ParseForestFunc)(parser.parserData.grammar)(inputs, history.map(Kernels))
    val ast = reconstructor.reconstruct() match {
      case Some(forest) if forest.trees.size == 1 =>
        new MetaLang3Ast().matchStart(forest.trees.head)
      case None =>
        throw IllegalGrammar("??")
    }
    val errors = new ErrorCollector()

    val transformer = new GrammarTransformer(ast, errors)
    val grammar = transformer.grammar("Grammar")
    val ngrammar = NGrammar.fromGrammar(grammar)

    val simul = new ValuefyExprSimulator(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs, Map())
    val value = simul.valuefy("a = good")
    println(value)
  }

  describe("test2") {
    val parser = MetaLang3Parser.parser
    val inputs = Inputs.fromString(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' WS E (WS ',' WS E)* WS ']' {Arr(elems=[$2]+$3)}
        |WS = ' '*""".stripMargin)
    val parseResult = parser.parseOrThrow(inputs)
    val history = parser.kernelsHistory(parseResult)
    val reconstructor = new ParseTreeConstructor2(ParseForestFunc)(parser.parserData.grammar)(inputs, history.map(Kernels))
    val ast = reconstructor.reconstruct() match {
      case Some(forest) if forest.trees.size == 1 =>
        new MetaLang3Ast().matchStart(forest.trees.head)
      case None =>
        throw IllegalGrammar("??")
    }
    val errors = new ErrorCollector()

    val transformer = new GrammarTransformer(ast, errors)
    val grammar = transformer.grammar("Grammar")
    val ngrammar = NGrammar.fromGrammar(grammar)

    val simul = new ValuefyExprSimulator(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs, Map())
    val value = simul.valuefy("[a,a,a]")
    println(value)
  }
}
