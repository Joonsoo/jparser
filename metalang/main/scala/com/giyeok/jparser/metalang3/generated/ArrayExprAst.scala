//package com.giyeok.jparser.metalang3.generated
//
//import com.giyeok.jparser.Inputs
//import com.giyeok.jparser.NGrammar
//import com.giyeok.jparser.ParseResultTree.BindNode
//import com.giyeok.jparser.ParseResultTree.Node
//import com.giyeok.jparser.ParseResultTree.SequenceNode
//import com.giyeok.jparser.ParseResultTree.TerminalNode
//import com.giyeok.jparser.ParsingErrors
//import com.giyeok.jparser.Symbols
//import com.giyeok.jparser.nparser.NaiveParser
//import com.giyeok.jparser.nparser.ParseTreeUtil
//import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
//import com.giyeok.jparser.nparser.Parser
//import scala.collection.immutable.ListSet
//
//object ArrayExprAst {
//  val ngrammar =   new NGrammar(
//    Map(1 -> NGrammar.NStart(1, 2),
//  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("E"), Set(3,5)),
//  4 -> NGrammar.NTerminal(4, Symbols.ExactChar('a')),
//  6 -> NGrammar.NNonterminal(6, Symbols.Nonterminal("A"), Set(7)),
//  8 -> NGrammar.NTerminal(8, Symbols.ExactChar('[')),
//  9 -> NGrammar.NNonterminal(9, Symbols.Nonterminal("WS"), Set(10)),
//  11 -> NGrammar.NRepeat(11, Symbols.Repeat(Symbols.ExactChar(' '), 0), 12, 13),
//  14 -> NGrammar.NTerminal(14, Symbols.ExactChar(' ')),
//  15 -> NGrammar.NRepeat(15, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))))), 0), 12, 16),
//  17 -> NGrammar.NOneOf(17, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))))), Set(18)),
//  18 -> NGrammar.NProxy(18, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))), 19),
//  20 -> NGrammar.NTerminal(20, Symbols.ExactChar(',')),
//  21 -> NGrammar.NTerminal(21, Symbols.ExactChar(']'))),
//    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('a'))), Seq(4)),
//  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Nonterminal("A"))), Seq(6)),
//  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.ExactChar('['),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))))), 0),Symbols.Nonterminal("WS"),Symbols.ExactChar(']'))), Seq(8,9,2,15,9,21)),
//  10 -> NGrammar.NSequence(10, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0))), Seq(11)),
//  12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq()), Seq()),
//  13 -> NGrammar.NSequence(13, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0),Symbols.ExactChar(' '))), Seq(11,14)),
//  16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E")))))))), Seq(15,17)),
//  19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("E"))), Seq(9,20,9,2))),
//    1)
//
//  sealed trait WithParseNode { val parseNode: Node }
//  case class Arr(elems: List[Expr])(override val parseNode: Node) extends Expr with WithParseNode
//  sealed trait Expr extends WithParseNode
//  case class Literal(value: Char)(override val parseNode: Node) extends Expr with WithParseNode
//
//
//  def matchA(node: Node): Arr = {
//    val BindNode(v1, v2) = node
//    val v18 = v1.id match {
//      case 7 =>
//        val v3 = v2.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v4, v5) = v3
//        assert(v4.id == 2)
//        val v6 = v2.asInstanceOf[SequenceNode].children(3)
//        val v7 = unrollRepeat0(v6).map { elem =>
//        val BindNode(v8, v9) = elem
//        assert(v8.id == 17)
//        val BindNode(v10, v11) = v9
//        val v17 = v10.id match {
//        case 18 =>
//          val BindNode(v12, v13) = v11
//          assert(v12.id == 19)
//          val v14 = v13.asInstanceOf[SequenceNode].children(3)
//          val BindNode(v15, v16) = v14
//          assert(v15.id == 2)
//          matchE(v16)
//      }
//        v17
//        }
//        Arr(List(matchE(v5)) ++ v7)(v2)
//    }
//    v18
//  }
//
//  def matchE(node: Node): Expr = {
//    val BindNode(v19, v20) = node
//    val v27 = v19.id match {
//      case 3 =>
//        val v21 = v20.asInstanceOf[SequenceNode].children.head
//        val BindNode(v22, v23) = v21
//        assert(v22.id == 4)
//        Literal(v23.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v20)
//      case 5 =>
//        val v24 = v20.asInstanceOf[SequenceNode].children.head
//        val BindNode(v25, v26) = v24
//        assert(v25.id == 6)
//        matchA(v26)
//    }
//    v27
//  }
//
//  def matchStart(node: Node): Expr = {
//    val BindNode(start, BindNode(_, body)) = node
//    assert(start.id == 1)
//    matchE(body)
//  }
//
//  val naiveParser = new NaiveParser(ngrammar)
//
//  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
//    naiveParser.parse(text)
//
//  def parseAst(text: String): Either[Expr, ParsingErrors.ParsingError] =
//    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
//
//  def main(args: Array[String]): Unit = {
//    println(parseAst("[a]"))
//  }
//}
