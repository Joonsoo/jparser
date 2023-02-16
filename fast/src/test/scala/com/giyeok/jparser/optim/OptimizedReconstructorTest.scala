package com.giyeok.jparser.optim

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.metalang3.codegen.{KotlinOptCodeGen, ScalaCodeGen}
import com.giyeok.jparser.milestone.MilestoneParserGen
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor2}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.test.{IdIssuerImpl, KernelSet, PyObjAst, PyObjKtOptAst, PyObjOptAst}
import org.scalatest.flatspec.AnyFlatSpec

import scala.jdk.CollectionConverters.{SeqHasAsJava, SetHasAsJava}

class OptimizedReconstructorTest extends AnyFlatSpec {
  it should "work" in {
    val grammar = MetaLanguage3.analyzeGrammar(new String(getClass.getResourceAsStream("/pyobj.cdg").readAllBytes()))
    //    val gen = MilestoneParserGen.generateMilestoneParserData(grammar.ngrammar)

    val codegen = new KotlinOptCodeGen(grammar)
    println(codegen.nonterminalMatchFunc("PyObj")._1.code)
    println(codegen.nonterminalMatchFunc("ObjField")._1.code)
    println(codegen.nonterminalMatchFunc("StrLiteral")._1.code)
    println(codegen.nonterminalMatchFunc("Value")._1.code)
    println(codegen.nonterminalMatchFunc("IntLiteral")._1.code)
    println(codegen.nonterminalMatchFunc("ListValue")._1.code)
    println(codegen.nonterminalMatchFunc("TupleValue")._1.code)
    println(codegen.nonterminalMatchFunc("BoolValue")._1.code)

    val parser = new NaiveParser(grammar.ngrammar)
    val inputs = Inputs.fromString("{\"hello\": 1, \"world\": 2, \"foo\": \"bar\"}")
    val parsed = parser.parse(inputs).left.get

    val history = parsed.history.map(g => new KernelSet(g.nodes.map(_.kernel).asJava))

    val reconstructor = ParseTreeConstructor2.forestConstructor(grammar.ngrammar)(inputs, parsed.history, parsed.conditionFinal)
    val parseTree = reconstructor.reconstruct()
    println(parseTree.get.trees.size)

    val ast = new PyObjKtOptAst(inputs.asJava, history.asJava, new IdIssuerImpl(0))
    println(ast.matchStart())

    //    grammar.nonterminalValuefyExprs.foreach { case (name, valuefyExpr) =>
    //      println(name)
    //      println(valuefyExpr)
    //    }
    //    println(new ScalaCodeGen(grammar, ScalaCodeGen.Options(emitNGrammar = true)).generateParser("PyObjAst"))

    //    val ast = new PyObjOptAst(inputs, history)
    //    println(ast.matchStart())
  }
}
