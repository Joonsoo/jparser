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
//import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
//import com.giyeok.jparser.nparser.Parser
//import scala.collection.immutable.ListSet
//
//object ExpressionGrammarAst {
//  val ngrammar =   new NGrammar(
//    Map(1 -> NGrammar.NStart(1, 2),
//  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("Expression"), Set(3,37)),
//  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("Term"), Set(5,35)),
//  6 -> NGrammar.NNonterminal(6, Symbols.Nonterminal("Factor"), Set(7,17,27)),
//  8 -> NGrammar.NNonterminal(8, Symbols.Nonterminal("Number"), Set(9,11)),
//  10 -> NGrammar.NTerminal(10, Symbols.ExactChar('0')),
//  12 -> NGrammar.NTerminal(12, Symbols.Chars(('1' to '9').toSet)),
//  13 -> NGrammar.NRepeat(13, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 14, 15),
//  16 -> NGrammar.NTerminal(16, Symbols.Chars(('0' to '9').toSet)),
//  18 -> NGrammar.NNonterminal(18, Symbols.Nonterminal("Variable"), Set(19)),
//  20 -> NGrammar.NLongest(20, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))), 21),
//  21 -> NGrammar.NOneOf(21, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))))), Set(22)),
//  22 -> NGrammar.NProxy(22, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))), 23),
//  24 -> NGrammar.NRepeat(24, Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 25, 26),
//  25 -> NGrammar.NTerminal(25, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
//  28 -> NGrammar.NTerminal(28, Symbols.ExactChar('(')),
//  29 -> NGrammar.NNonterminal(29, Symbols.Nonterminal("WS"), Set(30)),
//  31 -> NGrammar.NRepeat(31, Symbols.Repeat(Symbols.ExactChar(' '), 0), 14, 32),
//  33 -> NGrammar.NTerminal(33, Symbols.ExactChar(' ')),
//  34 -> NGrammar.NTerminal(34, Symbols.ExactChar(')')),
//  36 -> NGrammar.NTerminal(36, Symbols.ExactChar('*')),
//  38 -> NGrammar.NTerminal(38, Symbols.ExactChar('+'))),
//    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("Term"))), Seq(4)),
//  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Nonterminal("Factor"))), Seq(6)),
//  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.Nonterminal("Number"))), Seq(8)),
//  9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(10)),
//  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet),Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(12,13)),
//  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq()), Seq()),
//  15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0),Symbols.Chars(('0' to '9').toSet))), Seq(13,16)),
//  17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Nonterminal("Variable"))), Seq(18)),
//  19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))))))))), Seq(20)),
//  23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))), Seq(24)),
//  26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1),Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(24,25)),
//  27 -> NGrammar.NSequence(27, Symbols.Sequence(Seq(Symbols.ExactChar('('),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Expression"),Symbols.Nonterminal("WS"),Symbols.ExactChar(')'))), Seq(28,29,2,29,34)),
//  30 -> NGrammar.NSequence(30, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0))), Seq(31)),
//  32 -> NGrammar.NSequence(32, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar(' '), 0),Symbols.ExactChar(' '))), Seq(31,33)),
//  35 -> NGrammar.NSequence(35, Symbols.Sequence(Seq(Symbols.Nonterminal("Term"),Symbols.Nonterminal("WS"),Symbols.ExactChar('*'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Factor"))), Seq(4,29,36,29,6)),
//  37 -> NGrammar.NSequence(37, Symbols.Sequence(Seq(Symbols.Nonterminal("Expression"),Symbols.Nonterminal("WS"),Symbols.ExactChar('+'),Symbols.Nonterminal("WS"),Symbols.Nonterminal("Term"))), Seq(2,29,38,29,4))),
//    1)
//
//  sealed trait WithParseNode { val parseNode: Node }
//  case class BinOp(op: Char, lhs: Expression, rhs: Term)(override val parseNode: Node) extends Term with WithParseNode
//  sealed trait Expression extends WithParseNode
//  sealed trait Factor extends Term with WithParseNode
//  case class Integer(value: String)(override val parseNode: Node) extends Number with WithParseNode
//  sealed trait Number extends Factor with WithParseNode
//  case class Paren(expr: Expression)(override val parseNode: Node) extends Factor with WithParseNode
//  sealed trait Term extends Expression with WithParseNode
//  case class Variable(name: List[Char])(override val parseNode: Node) extends Factor with WithParseNode
//
//
//  def matchExpression(node: Node): Expression = {
//    val BindNode(v1, v2) = node
//    val v15 = v1.id match {
//      case 3 =>
//        val v3 = v2.asInstanceOf[SequenceNode].children.head
//        val BindNode(v4, v5) = v3
//        assert(v4.id == 4)
//        matchTerm(v5)
//      case 37 =>
//        val v6 = v2.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v7, v8) = v6
//        assert(v7.id == 38)
//        val v9 = v2.asInstanceOf[SequenceNode].children.head
//        val BindNode(v10, v11) = v9
//        assert(v10.id == 2)
//        val v12 = v2.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v13, v14) = v12
//        assert(v13.id == 4)
//        BinOp(v8.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, matchExpression(v11), matchTerm(v14))(v2)
//    }
//    v15
//  }
//
//  def matchFactor(node: Node): Factor = {
//    val BindNode(v16, v17) = node
//    val v27 = v16.id match {
//      case 7 =>
//        val v18 = v17.asInstanceOf[SequenceNode].children.head
//        val BindNode(v19, v20) = v18
//        assert(v19.id == 8)
//        matchNumber(v20)
//      case 17 =>
//        val v21 = v17.asInstanceOf[SequenceNode].children.head
//        val BindNode(v22, v23) = v21
//        assert(v22.id == 18)
//        matchVariable(v23)
//      case 27 =>
//        val v24 = v17.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v25, v26) = v24
//        assert(v25.id == 2)
//        Paren(matchExpression(v26))(v17)
//    }
//    v27
//  }
//
//  def matchNumber(node: Node): Number = {
//    val BindNode(v28, v29) = node
//    val v37 = v28.id match {
//      case 9 =>
//      Integer("0")(v29)
//      case 11 =>
//        val v30 = v29.asInstanceOf[SequenceNode].children.head
//        val BindNode(v31, v32) = v30
//        assert(v31.id == 12)
//        val v33 = v29.asInstanceOf[SequenceNode].children(1)
//        val v34 = unrollRepeat0(v33).map { elem =>
//        val BindNode(v35, v36) = elem
//        assert(v35.id == 16)
//        v36.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//        }
//        Integer(v32.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v34.map(x => x.toString).mkString(""))(v29)
//    }
//    v37
//  }
//
//  def matchTerm(node: Node): Term = {
//    val BindNode(v38, v39) = node
//    val v52 = v38.id match {
//      case 5 =>
//        val v40 = v39.asInstanceOf[SequenceNode].children.head
//        val BindNode(v41, v42) = v40
//        assert(v41.id == 6)
//        matchFactor(v42)
//      case 35 =>
//        val v43 = v39.asInstanceOf[SequenceNode].children(2)
//        val BindNode(v44, v45) = v43
//        assert(v44.id == 36)
//        val v46 = v39.asInstanceOf[SequenceNode].children.head
//        val BindNode(v47, v48) = v46
//        assert(v47.id == 4)
//        val v49 = v39.asInstanceOf[SequenceNode].children(4)
//        val BindNode(v50, v51) = v49
//        assert(v50.id == 6)
//        BinOp(v45.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char, matchTerm(v48), matchFactor(v51))(v39)
//    }
//    v52
//  }
//
//  def matchVariable(node: Node): Variable = {
//    val BindNode(v53, v54) = node
//    val v69 = v53.id match {
//      case 19 =>
//        val v55 = v54.asInstanceOf[SequenceNode].children.head
//        val BindNode(v56, v57) = v55
//        assert(v56.id == 20)
//        val BindNode(v58, v59) = v57
//        assert(v58.id == 21)
//        val BindNode(v60, v61) = v59
//        val v68 = v60.id match {
//        case 22 =>
//          val BindNode(v62, v63) = v61
//          assert(v62.id == 23)
//          val v64 = v63.asInstanceOf[SequenceNode].children.head
//          val v65 = unrollRepeat1(v64).map { elem =>
//          val BindNode(v66, v67) = elem
//          assert(v66.id == 25)
//          v67.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//          }
//          v65
//      }
//        Variable(v68)(v54)
//    }
//    v69
//  }
//
//  def matchStart(node: Node): Expression = {
//    val BindNode(start, BindNode(_, body)) = node
//    assert(start.id == 1)
//    matchExpression(body)
//  }
//
//  val naiveParser = new NaiveParser(ngrammar)
//
//  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
//    naiveParser.parse(text)
//
//  def parseAst(text: String): Either[Expression, ParsingErrors.ParsingError] =
//    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
//}
