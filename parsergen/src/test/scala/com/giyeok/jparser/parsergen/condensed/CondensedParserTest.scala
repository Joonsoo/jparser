package com.giyeok.jparser.parsergen.condensed

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.metalang3a.generated.{ArrayExprAst, ExceptMatchAst, ExpressionGrammarAst, MetaLang3Ast}
import com.giyeok.jparser.parsergen.condensed.CondensedParser.reconstructParseTree
import com.giyeok.jparser.parsergen.condensed.CondensedParserGen.generatedCondensedParserData
import org.scalatest.flatspec.AnyFlatSpec

class CondensedParserTest extends AnyFlatSpec {
  it should "create parser data for ExceptMatchAst" in {
    val grammar = ExceptMatchAst.ngrammar
    val input = Inputs.fromString("abcd if ifff hello else elseee else else")
    val valuefier = ExceptMatchAst.matchStart _

    val parserData = generatedCondensedParserData(grammar)
    val finalCtx = new CondensedParser(parserData).parse(input)
    // TODO finalCtx.actionHistory 에서 accept condition 평가해서 unacceptable 한것들 날리기
    //    val parseTree = reconstructParseTree(grammar, finalCtx, input).get.trees.head
    //    parseTree.printTree()
    //    val ast = valuefier(parseTree)
    //    println(ast)
  }

  it should "correctly parses ExpressionGrammarAst" in {
    val grammar = ExpressionGrammarAst.ngrammar
    val input = Inputs.fromString("1*2+34")
    val parserData = generatedCondensedParserData(grammar)
    val valuefier = ExpressionGrammarAst.matchStart _

    val finalCtx = new CondensedParser(parserData).parse(input)
    val parseTree = reconstructParseTree(grammar, finalCtx, input).get.trees.head
    parseTree.printTree()
    val ast = valuefier(parseTree)
    println(ast)
  }

  it should "correctly parses ArrayExprAst" in {
    val grammar = ArrayExprAst.ngrammar
    val input = Inputs.fromString("[a,a,a]")
    val parserData = generatedCondensedParserData(grammar)
    val valuefier = ArrayExprAst.matchStart _

    val finalCtx = new CondensedParser(parserData).parse(input)
    val parseTree = reconstructParseTree(grammar, finalCtx, input).get.trees.head
    parseTree.printTree()
    val ast = valuefier(parseTree)
    println(ast)
  }

  it should "correctly parses MetaLang3Ast" in {
    val grammar = MetaLang3Ast.ngrammar
    val input = Inputs.fromString("LhsName = RhsNonterminal 'abc'")
    val parserData = generatedCondensedParserData(grammar)
    val valuefier = MetaLang3Ast.matchStart _

    val finalCtx = new CondensedParser(parserData).parse(input)
    val parseTree = reconstructParseTree(grammar, finalCtx, input).get.trees.head
    parseTree.printTree()
    val ast = valuefier(parseTree)
    println(ast)
  }
}
