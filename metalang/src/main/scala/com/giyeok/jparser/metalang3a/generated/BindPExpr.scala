package com.giyeok.jparser.metalang3a.generated

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.Parser
import scala.collection.immutable.ListSet

object BindPExpr {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Expr"), Set(3,5)),
  4 -> NGrammar.NTerminal(4, Symbols.Chars(('0' to '9').toSet)),
  6 -> NGrammar.NTerminal(6, Symbols.Chars(('A' to 'Z').toSet)),
  7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("Params"), Set(8)),
  9 -> NGrammar.NTerminal(9, Symbols.ExactChar('(')),
  10 -> NGrammar.NNonterminal(10, Symbols.Nonterminal("WS"), Set(11)),
  12 -> NGrammar.NRepeat(12, Symbols.Repeat(Symbols.ExactChar(' '), 0), 13, 14),
  15 -> NGrammar.NTerminal(15, Symbols.ExactChar(' ')),
  16 -> NGrammar.NOneOf(16, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Expr"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.Nonterminal("WS")))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(17,26)),
  17 -> NGrammar.NOneOf(17, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Expr"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.Nonterminal("WS")))))), Set(18)),
  18 -> NGrammar.NProxy(18, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Expr"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.Nonterminal("WS")))), 19),
  20 -> NGrammar.NRepeat(20, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0), 13, 21),
  22 -> NGrammar.NOneOf(22, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), Set(23)),
  23 -> NGrammar.NProxy(23, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))), 24),
  25 -> NGrammar.NTerminal(25, Symbols.ExactChar(',')),
  26 -> NGrammar.NProxy(26, Symbols.Proxy(Symbols.Sequence(Seq())), 13),
  27 -> NGrammar.NTerminal(27, Symbols.ExactChar(')'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Chars(('0' to '9').toSet))), Seq(4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Chars(('A' to 'Z').toSet),Symbols.Nonterminal("Params"))), Seq(6,7)),
  8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Expr"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.Nonterminal("WS")))))),Symbols.Proxy(Symbols.Sequence(Seq())))),Symbols.ExactChar(')'))), Seq(9,10,16,27)),
  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0))), Seq(12)),
  13 -> NGrammar.NSequence(13, Symbols.Sequence(Seq()), Seq()),
  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0),Symbols.ExactChar(' '))), Seq(12,15)),
  19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Nonterminal("Expr"),Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.Nonterminal("WS"))), Seq(2,20,10)),
  21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))), 0),Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr")))))))), Seq(20,22)),
  24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"),Symbols.ExactChar(','),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expr"))), Seq(10,25,10,2))),
    1)

  sealed trait Expression
  case class Number(value: String) extends Expression
  case class FuncCall(name: String, params: List[Expression]) extends Expression


  def matchExpr(node: Node): Expression = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 4)
        Number(v5.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)
      case 5 =>
        val v6 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v7, v8) = v6
        assert(v7.id == 6)
        val v9 = v2.asInstanceOf[SequenceNode].children(1)
        val BindNode(v10, v11) = v9
        assert(v10.id == 7)
        FuncCall(v8.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString, matchParams(v11))
    }
  }

  def matchParams(node: Node): List[Expression] = {
    val BindNode(v12, v13) = node
    v12.id match {
      case 8 =>
        val v15 = v13.asInstanceOf[SequenceNode].children(2)
        val BindNode(v16, v17) = v15
        assert(v16.id == 16)
        val BindNode(v18, v19) = v17
        val v14 = v18.id match {
        case 26 =>
        None
        case 17 =>
          val BindNode(v20, v21) = v19
          assert(v20.id == 18)
          val BindNode(v22, v23) = v21
          assert(v22.id == 19)
          val v24 = v23.asInstanceOf[SequenceNode].children.head
          val BindNode(v25, v26) = v24
          assert(v25.id == 2)
          val v27 = v23.asInstanceOf[SequenceNode].children(1)
          val v28 = unrollRepeat0(v27).map { elem =>
          val BindNode(v29, v30) = elem
          assert(v29.id == 22)
          val BindNode(v31, v32) = v30
          v31.id match {
          case 23 =>
            val BindNode(v33, v34) = v32
            assert(v33.id == 24)
            val v35 = v34.asInstanceOf[SequenceNode].children(3)
            val BindNode(v36, v37) = v35
            assert(v36.id == 2)
            matchExpr(v37)
        }
          }
          Some(List(matchExpr(v26)) ++ v28)
      }
        if (v14.isDefined) v14.get else List()
    }
  }

  def matchWS(node: Node): List[Char] = {
    val BindNode(v38, v39) = node
    v38.id match {
      case 11 =>
        val v40 = v39.asInstanceOf[SequenceNode].children.head
        val v41 = unrollRepeat0(v40).map { elem =>
        val BindNode(v42, v43) = elem
        assert(v42.id == 15)
        v43.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v41
    }
  }

  def matchStart(node: Node): Expression = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchExpr(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[Expression, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)

  def main(args: Array[String]): Unit = {
    println(parseAst("A()"))
    println(parseAst("A(1,2,3,4)"))
    println(parseAst("A(1,B(2,3,4),3,4)"))
  }
}
