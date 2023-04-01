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

    val generatedKt = new KotlinOptCodeGen(grammar).generate("MetaLang3AstKt")
    println(generatedKt)
  }
}
