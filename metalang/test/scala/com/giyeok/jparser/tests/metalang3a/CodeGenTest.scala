package com.giyeok.jparser.tests.metalang3a

import com.giyeok.jparser.examples.metalang3.MetaLang3ExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.metalang3.codegen.{KotlinOptCodeGen, ScalaCodeGen}
import org.scalatest.flatspec.AnyFlatSpec

class CodeGenTest extends AnyFlatSpec {
  "metalang3 gramamr" should "work" in {
    val grammar = MetaLanguage3.analyzeGrammar(MetaLang3ExamplesCatalog.INSTANCE.getMetalang3.getGrammarText)

    val generatedScala = new ScalaCodeGen(grammar).generateParser("MetaLang3Ast")
    //    println(generatedScala)

    // refCtx로 넘어가는 어딘가에서 unbind가 누락되는 경우가 있는데..
    val generatedKt = new KotlinOptCodeGen(grammar).generate("MetaLang3AstKt")
    println(generatedKt)
  }

  "simple grammar" should "work" in {
    val grammar = MetaLanguage3.analyzeGrammar(
      """ArrayExpr = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {$2{[$0] + $1} ?: []}
        |PExpr = 'a-z'
        |WS = ' '*
        |""".stripMargin)

    val generatedScala = new ScalaCodeGen(grammar).generateParser("SimpleAst")
    println(generatedScala)
  }
}
