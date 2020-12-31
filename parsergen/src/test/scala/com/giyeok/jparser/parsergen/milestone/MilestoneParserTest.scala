package com.giyeok.jparser.parsergen.milestone

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.examples.metalang3.{MetaLang3Grammar, SimpleExamples}
import com.giyeok.jparser.metalang3a.{MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.{Inputs, NGrammar}
import com.giyeok.jparser.metalang3a.generated.{ArrayExprAst, ExceptMatchAst, ExpressionGrammarAst, MetaLang3Ast}
import com.giyeok.jparser.parsergen.milestone.MilestoneParser.reconstructParseTree
import com.giyeok.jparser.parsergen.milestone.MilestoneParserGen.generateMilestoneParserData
import com.giyeok.jparser.parsergen.proto.MilestoneParserDataProto
import com.giyeok.jparser.parsergen.proto.MilestoneParserProtobufConverter.{convertMilestoneParserDataToProto, convertProtoToMilestoneParserData}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
import scala.util.Using

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

  private val metalang3Example = SimpleExamples.ex4.grammar
  private val expectedResult = "Grammar(List(Rule(LHS(Nonterminal(NonterminalName(Expression)),Some(TypeDesc(TypeName(Expression),false))),List(Sequence(List(Nonterminal(NonterminalName(Term)))), Sequence(List(Nonterminal(NonterminalName(Expression)), Nonterminal(NonterminalName(WS)), CharAsIs(+), Nonterminal(NonterminalName(WS)), Nonterminal(NonterminalName(Term)), ProcessorBlock(NamedConstructExpr(TypeName(BinOp),List(NamedParam(ParamName(op),None,FuncCallOrConstructExpr(TypeOrFuncName(str),List(ValRef(2,None)))), NamedParam(ParamName(lhs),Some(TypeDesc(TypeName(Expression),false)),ValRef(0,None)), NamedParam(ParamName(rhs),None,ValRef(4,None))),None)))))), Rule(LHS(Nonterminal(NonterminalName(Term)),Some(TypeDesc(TypeName(Term),false))),List(Sequence(List(Nonterminal(NonterminalName(Factor)))), Sequence(List(Nonterminal(NonterminalName(Term)), Nonterminal(NonterminalName(WS)), CharAsIs(*), Nonterminal(NonterminalName(WS)), Nonterminal(NonterminalName(Factor)), ProcessorBlock(FuncCallOrConstructExpr(TypeOrFuncName(BinOp),List(FuncCallOrConstructExpr(TypeOrFuncName(str),List(ValRef(2,None))), ValRef(0,None), ValRef(4,None)))))))), Rule(LHS(Nonterminal(NonterminalName(Factor)),Some(TypeDesc(TypeName(Factor),false))),List(Sequence(List(Nonterminal(NonterminalName(Number)))), Sequence(List(Nonterminal(NonterminalName(Variable)))), Sequence(List(CharAsIs((), Nonterminal(NonterminalName(WS)), Nonterminal(NonterminalName(Expression)), Nonterminal(NonterminalName(WS)), CharAsIs()), ProcessorBlock(NamedConstructExpr(TypeName(Paren),List(NamedParam(ParamName(expr),None,ValRef(2,None))),None)))))), Rule(LHS(Nonterminal(NonterminalName(Number)),Some(TypeDesc(TypeName(Number),false))),List(Sequence(List(CharAsIs(0), ProcessorBlock(NamedConstructExpr(TypeName(Integer),List(NamedParam(ParamName(value),None,FuncCallOrConstructExpr(TypeOrFuncName(str),List(ValRef(0,None))))),None)))), Sequence(List(TerminalChoice(List(TerminalChoiceRange(CharAsIs(1),CharAsIs(9)))), RepeatFromZero(TerminalChoice(List(TerminalChoiceRange(CharAsIs(0),CharAsIs(9))))), ProcessorBlock(FuncCallOrConstructExpr(TypeOrFuncName(Integer),List(FuncCallOrConstructExpr(TypeOrFuncName(str),List(RawRef(0,None), RawRef(1,None)))))))))), Rule(LHS(Nonterminal(NonterminalName(Variable)),None),List(Sequence(List(Longest(InPlaceChoices(List(Sequence(List(RepeatFromOne(TerminalChoice(List(TerminalChoiceRange(CharAsIs(A),CharAsIs(Z)), TerminalChoiceRange(CharAsIs(a),CharAsIs(z)))))))))), ProcessorBlock(NamedConstructExpr(TypeName(Variable),List(NamedParam(ParamName(name),None,ValRef(0,None))),None)))))), Rule(LHS(Nonterminal(NonterminalName(WS)),None),List(Sequence(List(RepeatFromZero(CharAsIs( ))))))))"

  it should "correctly parses MetaLang3Ast" in {
    val tester = new Tester(MetaLang3Ast.ngrammar, MetaLang3Ast.matchStart)
    val milestoneParserData = convertMilestoneParserDataToProto(tester.parserData)

    println(milestoneParserData.getSerializedSize)
    Using(new FileOutputStream("MetaLang3AstMilestoneParser.pb")) {
      milestoneParserData.writeTo(_)
    }

    val parsed = measureTime {
      tester.parse1(metalang3Example) //"LhsName = RhsNonterminal 'abc'")
    }
    println(parsed)
    assert(parsed.toString == expectedResult)
  }

  def measureTime[T](block: => T): T = {
    val startTime = System.nanoTime()
    val result = block
    val endTime = System.nanoTime()
    println(s"Elapsed: ${(endTime - startTime) / 1000000.0} ms")
    result
  }

  it should "correctly parses MetaLang3Ast by proto data" in {
    val parserData = measureTime {
      Using(new BufferedInputStream(new FileInputStream("MetaLang3AstMilestoneParser.pb"))) { input =>
        convertProtoToMilestoneParserData(MilestoneParserDataProto.MilestoneParserData.parseFrom(input))
      }.get
    }

    def parse(input: String): List[MetaLang3Ast.Grammar] = {
      val inputSeq = Inputs.fromString(input)
      val finalCtx = new MilestoneParser(parserData).parse(inputSeq)
      // TODO finalCtx.actionHistory 에서 accept condition 평가해서 unacceptable 한것들 날리기
      val parseForest = reconstructParseTree(parserData, finalCtx, inputSeq).get
      parseForest.trees.map(MetaLang3Ast.matchStart).toList
    }

    def parse1(input: String): MetaLang3Ast.Grammar = {
      val trees = parse(input)
      assert(trees.size == 1)
      trees.head
    }

    val parsed = measureTime {
      parse1(metalang3Example)
    }
    println(parsed)
    assert(parsed.toString == expectedResult)
  }

  it should "correctly parses MetaLang3Ast by naive parser" in {
    val parsed = measureTime {
      MetaLang3Ast.parseAst(metalang3Example).left.get
    }
    println(parsed)
    assert(parsed.toString == expectedResult)
  }

  it should "correctly parses line comment grammar" in {
    val grammar =
      """program = WS elem (WS elem)* WS {[$1]+$2}
        |elem = "elem"&Tk
        |
        |Tk = <'a-zA-Z0-9_'*>
        |WS = (' \n\r\t' | LineComment)*
        |LineComment = "//" (.-'\n')* (EOF | '\n')
        |EOF = !.""".stripMargin
    val gram = MetaLanguage3.analyzeGrammar(grammar)
    val data = MilestoneParserGen.generateMilestoneParserData(gram.ngrammar)
    val input = Inputs.fromString(
      """elem
        |// hello
        |elem elem
        |// bye~""".stripMargin)
    val finalCtx = new MilestoneParser(data, true).parse(input)
    val forest = MilestoneParser.reconstructParseTree(data, finalCtx, input).get
    assert(forest.trees.size == 1)
    ValuefyExprSimulator(gram).valuefyStart(forest.trees.head)
  }
}
