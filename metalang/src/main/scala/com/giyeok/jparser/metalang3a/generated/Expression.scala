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

object Expression {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Expression"), Set(3,37)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("Term"), Set(5,35)),
  6 -> NGrammar.NNonterminal(6, Symbols.Nonterminal("Factor"), Set(7,17,27)),
  8 -> NGrammar.NNonterminal(8, Symbols.Nonterminal("Number"), Set(9,11)),
  10 -> NGrammar.NTerminal(10, Symbols.ExactChar('0')),
  12 -> NGrammar.NTerminal(12, Symbols.Chars(('1' to '9').toSet)),
  13 -> NGrammar.NRepeat(13, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 14, 15),
  16 -> NGrammar.NTerminal(16, Symbols.Chars(('0' to '9').toSet)),
  18 -> NGrammar.NNonterminal(18, Symbols.Nonterminal("Variable"), Set(19)),
  20 -> NGrammar.NLongest(20, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))), 21),
  21 -> NGrammar.NOneOf(21, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))))), Set(22)),
  22 -> NGrammar.NProxy(22, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))), 23),
  24 -> NGrammar.NRepeat(24, Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 25, 26),
  25 -> NGrammar.NTerminal(25, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  28 -> NGrammar.NTerminal(28, Symbols.ExactChar('(')),
  29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("WS"), Set(30)),
  31 -> NGrammar.NRepeat(31, Symbols.Repeat(Symbols.ExactChar(' '), 0), 14, 32),
  33 -> NGrammar.NTerminal(33, Symbols.ExactChar(' ')),
  34 -> NGrammar.NTerminal(34, Symbols.ExactChar(')')),
  36 -> NGrammar.NTerminal(36, Symbols.ExactChar('*')),
  38 -> NGrammar.NTerminal(38, Symbols.ExactChar('+'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("Term"))), Seq(4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Nonterminal("Factor"))), Seq(6)),
  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.Nonterminal("Number"))), Seq(8)),
  9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(10)),
  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(12,13)),
  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq()), Seq()),
  15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(13,16)),
  17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Nonterminal("Variable"))), Seq(18)),
  19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))))), Seq(20)),
  23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))), Seq(24)),
  26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1),Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(24,25)),
  27 -> NGrammar.NSequence(27, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expression"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(28,29,2,29,34)),
  30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0))), Seq(31)),
  32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0),Symbols.ExactChar(' '))), Seq(31,33)),
  35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Nonterminal("Term"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Factor"))), Seq(4,29,36,29,6)),
  37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Nonterminal("Expression"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Term"))), Seq(2,29,38,29,4))),
    1)

  sealed trait Expression
  sealed trait Term extends Expression
  sealed trait Factor extends Term
  case class BinOp(op: Op.Value, lhs: Expression, rhs: Term) extends Term
  sealed trait Number extends Factor
  case class Paren(expr: Expression) extends Factor
  case class Integer(value: String) extends Number
  case class Variable(name: String) extends Factor
  object Op extends Enumeration { val Add, Mul = Value }

  def matchExpression(node: Node): Expression = {
    val BindNode(v22, v23) = node
    v22.id match {
      case 3 =>
        val v24 = v23.asInstanceOf[SequenceNode].children.head
        val BindNode(v25, v26) = v24
        assert(v25.id == 4)
        matchTerm(v26)
      case 37 =>
        val v27 = v23.asInstanceOf[SequenceNode].children.head
        val BindNode(v28, v29) = v27
        assert(v28.id == 2)
        val v30 = v23.asInstanceOf[SequenceNode].children(4)
        val BindNode(v31, v32) = v30
        assert(v31.id == 4)
        BinOp(Op.Add, matchExpression(v29), matchTerm(v32))
    }
  }

  def matchVariable(node: Node): Variable = {
    val BindNode(v12, v13) = node
    v12.id match {
      case 19 =>
        val v14 = v13.asInstanceOf[SequenceNode].children.head
        Variable(v14.sourceText)
    }
  }

  def matchNumber(node: Node): Number = {
    val BindNode(v15, v16) = node
    v15.id match {
      case 9 =>
        val v17 = v16.asInstanceOf[SequenceNode].children.head
        val BindNode(v18, v19) = v17
        assert(v18.id == 10)
        Integer(v19.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)
      case 11 =>
        val v20 = v16.asInstanceOf[SequenceNode].children.head
        val v21 = v16.asInstanceOf[SequenceNode].children(1)
        Integer(v20.sourceText + v21.sourceText)
    }
  }

  def matchWS(node: Node): List[Char] = {
    val BindNode(v44, v45) = node
    v44.id match {
      case 30 =>
        val v46 = v45.asInstanceOf[SequenceNode].children.head
        val v47 = unrollRepeat0(v46).map { elem =>
        val BindNode(v48, v49) = elem
        assert(v48.id == 33)
        v49.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v47
    }
  }

  def matchTerm(node: Node): Term = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 5 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 6)
        matchFactor(v5)
      case 35 =>
        val v6 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v7, v8) = v6
        assert(v7.id == 4)
        val v9 = v2.asInstanceOf[SequenceNode].children(4)
        val BindNode(v10, v11) = v9
        assert(v10.id == 6)
        BinOp(Op.Mul, matchTerm(v8), matchFactor(v11))
    }
  }

  def matchFactor(node: Node): Factor = {
    val BindNode(v33, v34) = node
    v33.id match {
      case 7 =>
        val v35 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v36, v37) = v35
        assert(v36.id == 8)
        matchNumber(v37)
      case 17 =>
        val v38 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v39, v40) = v38
        assert(v39.id == 18)
        matchVariable(v40)
      case 27 =>
        val v41 = v34.asInstanceOf[SequenceNode].children(2)
        val BindNode(v42, v43) = v41
        assert(v42.id == 2)
        Paren(matchExpression(v43))
    }
  }

  def matchStart(node: Node): Expression = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchExpression(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[Expression, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)

  def main(args: Array[String]): Unit = {
    println(parseAst("1234+2*3*(4+5)*0+abc"))
  }
}
