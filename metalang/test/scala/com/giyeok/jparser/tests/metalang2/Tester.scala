package com.giyeok.jparser.tests.metalang2

import com.giyeok.jparser.examples.metalang2.{ExpressionGrammarsMetaLang2, MetaLang2Grammar, MetaLang3Grammar}
import com.giyeok.jparser.metalang2.MetaLanguage2.{analyze, grammarSpecToAST}
import com.giyeok.jparser.metalang2.ScalaDefGenerator

import java.io.{BufferedWriter, File, FileWriter}
import scala.io.Source

object Tester {
  case class TestGrammar(packageName: Option[String], name: String, grammar: String, parseUtils: Boolean = true, astPrettyPrinter: Boolean = true)

  def readFile(path: String): String = {
    val s = Source.fromFile(new File(path))
    try s.getLines().mkString("\n") finally s.close()
  }

  def main(args: Array[String]): Unit = {
    val pkgName = Some("com.giyeok.jparser.metalang2.generated")
    val expressionGrammar = TestGrammar(pkgName, "ExpressionGrammar", ExpressionGrammarsMetaLang2.basic.grammar)
    val metaGrammar2 = TestGrammar(pkgName, "MetaGrammar2Ast", MetaLang2Grammar.inMetaLang2)
    val metaGrammar3 = TestGrammar(pkgName, "MetaGrammar3Ast", MetaLang3Grammar.inMetaLang2)
    val pyobjGrammar = TestGrammar(pkgName, "PyObjGrammar", readFile("./examples/src/main/resources/pyobj.cdg"))
    // val protobufTextFormatGrammar = TestGrammar(pkgName, "ProtobufTextFormatGrammar", readFile("./protobuf_textformat.cdg"))
    val argsGrammar = TestGrammar(pkgName, "ArgsGrammar", readFile("./examples/src/main/resources/args.cdg"))

    List(metaGrammar3) foreach { grammar =>
      val analysis = analyze(grammarSpecToAST(grammar.grammar).get)

      analysis.astAnalysis.astifiers.foreach { case (lhs, rhs) =>
        println(s"$lhs =")
        rhs.foreach { r =>
          println(s"  ${r._1.toShortString}")
          println(s"  ${r._2}")
        }
      }
      println(analysis.typeAnalysis.typeDependenceGraph.toDotGraphModel.printDotGraph())

      val scalaCode = s"package ${grammar.packageName.get}\n\n" +
        new ScalaDefGenerator(analysis, parseUtils = grammar.parseUtils).toGrammarObject(grammar.name)
      val filepath = s"./metalang/src/main/scala/com/giyeok/jparser/metalang2/generated/${grammar.name}.scala"
      val writer = new BufferedWriter(new FileWriter(filepath))
      writer.write(scalaCode)
      writer.close()

      println(s"Written parser to $filepath")
    }
  }
}
