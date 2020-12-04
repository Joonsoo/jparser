package com.giyeok.jparser.metalang3a

import java.io.File

import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.metalang3a.MetaLanguage3.writeScalaParserCode

import scala.io.Source

object Grammars {
  private def generate(name: String)(func: => Unit): Unit = {
    println(s"Generating $name...")
    func
    println(s"$name generated!")
  }

  def generateMetaLang3Ast(): Unit = generate("MetaLang3Ast") {
    writeScalaParserCode(MetaLang3Grammar.inMetaLang3.grammar, "MetaLang3Ast",
      "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List("A = B C 'd' 'e'*")))
  }

  def generateMlProtoAst(): Unit = generate("MlProto") {
    val file = new File("./examples/src/main/resources/mlproto/mlproto.cdg")
    val source = Source.fromFile(file)
    writeScalaParserCode(try source.mkString finally source.close(),
      "MlProtoAst",
      "com.giyeok.jparser.metalang3a.generated", new File("./metalang/src/main/scala"),
      mainFuncExamples = Some(List("module Conv2d<>(inChannels: Int) {}")))
  }

  def main(args: Array[String]): Unit = {
    generateMetaLang3Ast()
    generateMlProtoAst()
  }
}
