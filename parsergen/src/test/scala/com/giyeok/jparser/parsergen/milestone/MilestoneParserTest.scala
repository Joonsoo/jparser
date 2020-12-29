package com.giyeok.jparser.parsergen.milestone

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.{Inputs, NGrammar}
import com.giyeok.jparser.metalang3a.generated.{ArrayExprAst, ExceptMatchAst, ExpressionGrammarAst, MetaLang3Ast}
import com.giyeok.jparser.parsergen.milestone.MilestoneParser.reconstructParseTree
import com.giyeok.jparser.parsergen.milestone.MilestoneParserGen.generateMilestoneParserData
import org.scalatest.flatspec.AnyFlatSpec

class MilestoneParserTest extends AnyFlatSpec {

  class Tester[T](val grammar: NGrammar, val matchStart: Node => T) {
    val parserData: MilestoneParserData = generateMilestoneParserData(grammar)

    def parse(input: String): List[T] = {
      val inputSeq = Inputs.fromString(input)
      val finalCtx = new MilestoneParser(parserData).parse(inputSeq)
      // TODO finalCtx.actionHistory 에서 accept condition 평가해서 unacceptable 한것들 날리기
      val parseForest = reconstructParseTree(parserData, finalCtx, inputSeq).get
      parseForest.trees.map(matchStart).toList
    }

    def parse1(input: String): T = {
      val trees = parse(input)
      assert(trees.size == 1)
      trees.head
    }
  }

  it should "correctly parses ExceptMatchAst" in {
    val tester = new Tester(ExceptMatchAst.ngrammar, ExceptMatchAst.matchStart)
    // val parsed = tester.parse("abcd if ifff hello else elseee else else")
    val parsed = tester.parse1("iiii if iff")
    assert(parsed.map(_.toString) == List("Id(iiii)", "WS()", "Keyword(IF)", "WS()", "Id(iff)"))
  }

  it should "correctly parses ExpressionGrammarAst" in {
    val tester = new Tester(ExpressionGrammarAst.ngrammar, ExpressionGrammarAst.matchStart)
    val parsed = tester.parse1("1*2+34")
    assert(parsed.toString == "BinOp(+,BinOp(*,Integer(1),Integer(2)),Integer(34))")
  }

  it should "correctly parses ArrayExprAst" in {
    val tester = new Tester(ArrayExprAst.ngrammar, ArrayExprAst.matchStart)
    val parsed = tester.parse1("[a,a,a]")
    assert(parsed.toString == "Arr(List(Literal(a), Literal(a), Literal(a)))")
  }

  it should "correctly parses MetaLang3Ast" in {
    val tester = new Tester(MetaLang3Ast.ngrammar, MetaLang3Ast.matchStart)
    val parsed = tester.parse1("LhsName = RhsNonterminal 'abc'")
    assert(parsed.toString == "Grammar(List(Rule(LHS(Nonterminal(NonterminalName(LhsName)),None),List(Sequence(List(Nonterminal(NonterminalName(RhsNonterminal)), TerminalChoice(List(CharAsIs(a), CharAsIs(b), CharAsIs(c)))))))))")
  }
}
