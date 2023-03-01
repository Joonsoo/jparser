package com.giyeok.jparser.optim

import com.giyeok.jparser.{Inputs, Symbols}
import com.giyeok.jparser.ktlib.{IdIssuerImpl, KernelSet}
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.metalang3.codegen.KotlinOptCodeGen
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor2}
import com.giyeok.jparser.test.{PyObjKtOptAst}
import org.scalatest.flatspec.AnyFlatSpec

import scala.jdk.CollectionConverters.{SeqHasAsJava, SetHasAsJava}

class OptimizedReconstructorTest extends AnyFlatSpec {
  it should "work" in {
    val grammar = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/bibix2.cdg").readAllBytes()))
    //    val gen = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)

    val codegen = new KotlinOptCodeGen(grammar)
    println(codegen.classDefs(grammar.classRelations).code)
    println()

    var visitedNonterms = Set[String]()
    println(codegen.matchStartFunc().code)
    println()
    while ((codegen._requiredNonterms -- visitedNonterms).nonEmpty) {
      val next = (codegen._requiredNonterms -- visitedNonterms).head
      println(codegen.nonterminalMatchFunc(next).code)
      println()
      visitedNonterms += next
    }

    ////    val parser = new NaiveParser(grammar.ngrammar)
    ////    val inputs = Inputs.fromString("{\"hello\": 1, \"world\": (234,345,456,567), \"foo\": \"bar\"}")
    ////    val parsed = parser.parse(inputs).left.get
    ////
    ////    val history = parsed.history.map(g => new KernelSet(g.nodes.map(_.kernel).asJava))
    ////
    ////    val reconstructor = ParseTreeConstructor2.forestConstructor(grammar.ngrammar)(inputs, parsed.history, parsed.conditionFinal)
    ////    val parseTree = reconstructor.reconstruct()
    ////    println(parseTree.get.trees.size)
    //
    //    val ast = new PyObjKtOptAst(inputs.asJava, history.asJava, new IdIssuerImpl(0))
    //    println(ast.matchStart())

    //    grammar.nonterminalValuefyExprs.foreach { case (name, valuefyExpr) =>
    //      println(name)
    //      println(valuefyExpr)
    //    }
    //    println(new ScalaCodeGen(grammar, ScalaCodeGen.Options(emitNGrammar = true)).generateParser("PyObjAst"))

    //    val ast = new PyObjOptAst(inputs, history)
    //    println(ast.matchStart())
  }
}
