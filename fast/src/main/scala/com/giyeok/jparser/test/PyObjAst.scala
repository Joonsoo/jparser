package com.giyeok.jparser.test

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
import com.giyeok.jparser.nparser.Parser

import scala.collection.immutable.ListSet
import PyObjAst._
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels

object PyObjAst {
  val ngrammar: NGrammar = new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
      2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("PyObj"), Set(3)),
      4 -> NGrammar.NTerminal(4, Symbols.ExactChar('{')),
      5 -> NGrammar.NNonterminal(5, Symbols.Nonterminal("WS"), Set(6)),
      7 -> NGrammar.NRepeat(7, Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0), 8, 9),
      10 -> NGrammar.NTerminal(10, Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet)),
      11 -> NGrammar.NOneOf(11, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(12, 73)),
      12 -> NGrammar.NOneOf(12, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Set(13)),
      13 -> NGrammar.NProxy(13, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))), 14),
      15 -> NGrammar.NNonterminal(15, Symbols.Nonterminal("ObjField"), Set(16)),
      17 -> NGrammar.NNonterminal(17, Symbols.Nonterminal("StrLiteral"), Set(18, 25)),
      19 -> NGrammar.NTerminal(19, Symbols.ExactChar('"')),
      20 -> NGrammar.NRepeat(20, Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), 8, 21),
      22 -> NGrammar.NNonterminal(22, Symbols.Nonterminal("StrChar"), Set(23)),
      24 -> NGrammar.NTerminal(24, Symbols.Chars(Set(' ', '_', '|') ++ ('0' to '9').toSet ++ ('<' to '>').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
      26 -> NGrammar.NTerminal(26, Symbols.ExactChar('\'')),
      27 -> NGrammar.NTerminal(27, Symbols.ExactChar(':')),
      28 -> NGrammar.NNonterminal(28, Symbols.Nonterminal("Value"), Set(29, 45, 54, 55, 75)),
      30 -> NGrammar.NNonterminal(30, Symbols.Nonterminal("BoolValue"), Set(31, 38)),
      32 -> NGrammar.NProxy(32, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('T'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))), 33),
      34 -> NGrammar.NTerminal(34, Symbols.ExactChar('T')),
      35 -> NGrammar.NTerminal(35, Symbols.ExactChar('r')),
      36 -> NGrammar.NTerminal(36, Symbols.ExactChar('u')),
      37 -> NGrammar.NTerminal(37, Symbols.ExactChar('e')),
      39 -> NGrammar.NProxy(39, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('F'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))), 40),
      41 -> NGrammar.NTerminal(41, Symbols.ExactChar('F')),
      42 -> NGrammar.NTerminal(42, Symbols.ExactChar('a')),
      43 -> NGrammar.NTerminal(43, Symbols.ExactChar('l')),
      44 -> NGrammar.NTerminal(44, Symbols.ExactChar('s')),
      46 -> NGrammar.NNonterminal(46, Symbols.Nonterminal("IntLiteral"), Set(47, 49)),
      48 -> NGrammar.NTerminal(48, Symbols.ExactChar('0')),
      50 -> NGrammar.NTerminal(50, Symbols.Chars(('1' to '9').toSet)),
      51 -> NGrammar.NRepeat(51, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 8, 52),
      53 -> NGrammar.NTerminal(53, Symbols.Chars(('0' to '9').toSet)),
      56 -> NGrammar.NNonterminal(56, Symbols.Nonterminal("ListValue"), Set(57)),
      58 -> NGrammar.NTerminal(58, Symbols.ExactChar('[')),
      59 -> NGrammar.NOneOf(59, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(60, 73)),
      60 -> NGrammar.NOneOf(60, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Set(61)),
      61 -> NGrammar.NProxy(61, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))), 62),
      63 -> NGrammar.NRepeat(63, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), 8, 64),
      65 -> NGrammar.NOneOf(65, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), Set(66)),
      66 -> NGrammar.NProxy(66, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))), 67),
      68 -> NGrammar.NTerminal(68, Symbols.ExactChar(',')),
      69 -> NGrammar.NOneOf(69, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Set(70, 73)),
      70 -> NGrammar.NOneOf(70, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Set(71)),
      71 -> NGrammar.NProxy(71, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))), 72),
      73 -> NGrammar.NProxy(73, Symbols.Proxy(Symbols.Sequence(Seq())), 8),
      74 -> NGrammar.NTerminal(74, Symbols.ExactChar(']')),
      76 -> NGrammar.NNonterminal(76, Symbols.Nonterminal("TupleValue"), Set(77, 80)),
      78 -> NGrammar.NTerminal(78, Symbols.ExactChar('(')),
      79 -> NGrammar.NTerminal(79, Symbols.ExactChar(')')),
      81 -> NGrammar.NRepeat(81, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 1), 65, 82),
      83 -> NGrammar.NRepeat(83, Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), 8, 84),
      85 -> NGrammar.NOneOf(85, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), Set(86)),
      86 -> NGrammar.NProxy(86, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))), 87),
      88 -> NGrammar.NTerminal(88, Symbols.ExactChar('}'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('{'), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar('}'))), Seq(4, 5, 11, 88)),
      6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0))), Seq(7)),
      8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq()), Seq()),
      9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet), 0), Symbols.Chars(Set(' ') ++ ('\t' to '\n').toSet))), Seq(7, 10)),
      14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Nonterminal("ObjField"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"))), Seq(15, 83, 69, 5)),
      16 -> NGrammar.NSequence(16, Symbols.Sequence(Seq(Symbols.Nonterminal("StrLiteral"), Symbols.Nonterminal("WS"), Symbols.ExactChar(':'), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value"))), Seq(17, 5, 27, 5, 28)),
      18 -> NGrammar.NSequence(18, Symbols.Sequence(Seq(Symbols.ExactChar('"'), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('"'))), Seq(19, 20, 19)),
      21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.Nonterminal("StrChar"))), Seq(20, 22)),
      23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq(Symbols.Chars(Set(' ', '_', '|') ++ ('0' to '9').toSet ++ ('<' to '>').toSet ++ ('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(24)),
      25 -> NGrammar.NSequence(25, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Repeat(Symbols.Nonterminal("StrChar"), 0), Symbols.ExactChar('\''))), Seq(26, 20, 26)),
      29 -> NGrammar.NSequence(29, Symbols.Sequence(Seq(Symbols.Nonterminal("BoolValue"))), Seq(30)),
      31 -> NGrammar.NSequence(31, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('T'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e')))))), Seq(32)),
      33 -> NGrammar.NSequence(33, Symbols.Sequence(Seq(Symbols.ExactChar('T'), Symbols.ExactChar('r'), Symbols.ExactChar('u'), Symbols.ExactChar('e'))), Seq(34, 35, 36, 37)),
      38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('F'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e')))))), Seq(39)),
      40 -> NGrammar.NSequence(40, Symbols.Sequence(Seq(Symbols.ExactChar('F'), Symbols.ExactChar('a'), Symbols.ExactChar('l'), Symbols.ExactChar('s'), Symbols.ExactChar('e'))), Seq(41, 42, 43, 44, 37)),
      45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Nonterminal("IntLiteral"))), Seq(46)),
      47 -> NGrammar.NSequence(47, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(48)),
      49 -> NGrammar.NSequence(49, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(50, 51)),
      52 -> NGrammar.NSequence(52, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(51, 53)),
      54 -> NGrammar.NSequence(54, Symbols.Sequence(Seq(Symbols.Nonterminal("StrLiteral"))), Seq(17)),
      55 -> NGrammar.NSequence(55, Symbols.Sequence(Seq(Symbols.Nonterminal("ListValue"))), Seq(56)),
      57 -> NGrammar.NSequence(57, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("WS"), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS")))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.ExactChar(']'))), Seq(58, 5, 59, 74)),
      62 -> NGrammar.NSequence(62, Symbols.Sequence(Seq(Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"))), Seq(28, 63, 69, 5)),
      64 -> NGrammar.NSequence(64, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))))), Seq(63, 65)),
      67 -> NGrammar.NSequence(67, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value"))), Seq(5, 68, 5, 28)),
      72 -> NGrammar.NSequence(72, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','))), Seq(5, 68)),
      75 -> NGrammar.NSequence(75, Symbols.Sequence(Seq(Symbols.Nonterminal("TupleValue"))), Seq(76)),
      77 -> NGrammar.NSequence(77, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value"), Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(78, 5, 28, 5, 68, 5, 79)),
      80 -> NGrammar.NSequence(80, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value"), Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 1), Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(',')))))), Symbols.Proxy(Symbols.Sequence(Seq())))), Symbols.Nonterminal("WS"), Symbols.ExactChar(')'))), Seq(78, 5, 28, 81, 69, 5, 79)),
      82 -> NGrammar.NSequence(82, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))), 1), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("Value")))))))), Seq(81, 65)),
      84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0), Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))))), Seq(83, 85)),
      87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField"))), Seq(5, 68, 5, 15))),
    1)

  sealed trait WithIdAndParseNode {
    val id: Int;
    val parseNode: Node
  }

  case class BoolValue(value: BoolEnum.Value)(override val id: Int, override val parseNode: Node) extends Value with WithIdAndParseNode

  case class IntLiteral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class IntValue(value: IntLiteral)(override val id: Int, override val parseNode: Node) extends Value with WithIdAndParseNode

  case class ListValue(elems: Option[List[Value]])(override val id: Int, override val parseNode: Node) extends Value with WithIdAndParseNode

  case class ObjField(name: StrLiteral, value: Value)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class PyObj(fields: Option[List[ObjField]])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class StrLiteral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class StrValue(value: StrLiteral)(override val id: Int, override val parseNode: Node) extends Value with WithIdAndParseNode

  case class TupleValue(elems: List[Value])(override val id: Int, override val parseNode: Node) extends Value with WithIdAndParseNode

  sealed trait Value extends WithIdAndParseNode

  object BoolEnum extends Enumeration {
    val False, True = Value
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[PyObj, ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, new PyObjAst().matchStart)
}

class PyObjAst {
  private var idCounter = 0

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  def matchStart(node: Node): PyObj = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchPyObj(body)
  }

  def matchBoolValue(node: Node): BoolEnum.Value = {
    val BindNode(v1, v2) = node
    val v3 = v1.id match {
      case 31 =>
        BoolEnum.True
      case 38 =>
        BoolEnum.False
    }
    v3
  }

  def matchIntLiteral(node: Node): IntLiteral = {
    val BindNode(v4, v5) = node
    val v13 = v4.id match {
      case 47 =>
        IntLiteral("0")(nextId(), v5)
      case 49 =>
        val v6 = v5.asInstanceOf[SequenceNode].children.head
        val BindNode(v7, v8) = v6
        assert(v7.id == 50)
        val v9 = v5.asInstanceOf[SequenceNode].children(1)
        val v10 = unrollRepeat0(v9).map { elem =>
          val BindNode(v11, v12) = elem
          assert(v11.id == 53)
          v12.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        IntLiteral(v8.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v10.map(x => x.toString).mkString(""))(nextId(), v5)
    }
    v13
  }

  def matchListValue(node: Node): ListValue = {
    val BindNode(v14, v15) = node
    val v41 = v14.id match {
      case 57 =>
        val v16 = v15.asInstanceOf[SequenceNode].children(2)
        val BindNode(v17, v18) = v16
        assert(v17.id == 59)
        val BindNode(v19, v20) = v18
        val v40 = v19.id match {
          case 73 =>
            None
          case 60 =>
            val BindNode(v21, v22) = v20
            assert(v21.id == 61)
            val BindNode(v23, v24) = v22
            assert(v23.id == 62)
            val v25 = v24.asInstanceOf[SequenceNode].children.head
            val BindNode(v26, v27) = v25
            assert(v26.id == 28)
            val v28 = v24.asInstanceOf[SequenceNode].children(1)
            val v29 = unrollRepeat0(v28).map { elem =>
              val BindNode(v30, v31) = elem
              assert(v30.id == 65)
              val BindNode(v32, v33) = v31
              val v39 = v32.id match {
                case 66 =>
                  val BindNode(v34, v35) = v33
                  assert(v34.id == 67)
                  val v36 = v35.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v37, v38) = v36
                  assert(v37.id == 28)
                  matchValue(v38)
              }
              v39
            }
            Some(List(matchValue(v27)) ++ v29)
        }
        ListValue(v40)(nextId(), v15)
    }
    v41
  }

  def matchObjField(node: Node): ObjField = {
    val BindNode(v42, v43) = node
    val v50 = v42.id match {
      case 16 =>
        val v44 = v43.asInstanceOf[SequenceNode].children.head
        val BindNode(v45, v46) = v44
        assert(v45.id == 17)
        val v47 = v43.asInstanceOf[SequenceNode].children(4)
        val BindNode(v48, v49) = v47
        assert(v48.id == 28)
        ObjField(matchStrLiteral(v46), matchValue(v49))(nextId(), v43)
    }
    v50
  }

  def matchPyObj(node: Node): PyObj = {
    val BindNode(v51, v52) = node
    val v78 = v51.id match {
      case 3 =>
        val v53 = v52.asInstanceOf[SequenceNode].children(2)
        val BindNode(v54, v55) = v53
        assert(v54.id == 11)
        val BindNode(v56, v57) = v55
        val v77 = v56.id match {
          case 73 =>
            None
          case 12 =>
            val BindNode(v58, v59) = v57
            assert(v58.id == 13)
            val BindNode(v60, v61) = v59
            assert(v60.id == 14)
            val v62 = v61.asInstanceOf[SequenceNode].children.head
            val BindNode(v63, v64) = v62
            assert(v63.id == 15)
            val v65 = v61.asInstanceOf[SequenceNode].children(1)
            val v66 = unrollRepeat0(v65).map { elem =>
              val BindNode(v67, v68) = elem
              assert(v67.id == 85)
              val BindNode(v69, v70) = v68
              val v76 = v69.id match {
                case 86 =>
                  val BindNode(v71, v72) = v70
                  assert(v71.id == 87)
                  val v73 = v72.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v74, v75) = v73
                  assert(v74.id == 15)
                  matchObjField(v75)
              }
              v76
            }
            Some(List(matchObjField(v64)) ++ v66)
        }
        PyObj(v77)(nextId(), v52)
    }
    v78
  }

  def matchStrChar(node: Node): Char = {
    val BindNode(v79, v80) = node
    val v84 = v79.id match {
      case 23 =>
        val v81 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v82, v83) = v81
        assert(v82.id == 24)
        v83.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v84
  }

  def matchStrLiteral(node: Node): StrLiteral = {
    val BindNode(v85, v86) = node
    val v95 = v85.id match {
      case 18 =>
        val v87 = v86.asInstanceOf[SequenceNode].children(1)
        val v88 = unrollRepeat0(v87).map { elem =>
          val BindNode(v89, v90) = elem
          assert(v89.id == 22)
          matchStrChar(v90)
        }
        StrLiteral(v88.map(x => x.toString).mkString(""))(nextId(), v86)
      case 25 =>
        val v91 = v86.asInstanceOf[SequenceNode].children(1)
        val v92 = unrollRepeat0(v91).map { elem =>
          val BindNode(v93, v94) = elem
          assert(v93.id == 22)
          matchStrChar(v94)
        }
        StrLiteral(v92.map(x => x.toString).mkString(""))(nextId(), v86)
    }
    v95
  }

  def matchTupleValue(node: Node): TupleValue = {
    val BindNode(v96, v97) = node
    val v116 = v96.id match {
      case 77 =>
        val v98 = v97.asInstanceOf[SequenceNode].children(2)
        val BindNode(v99, v100) = v98
        assert(v99.id == 28)
        TupleValue(List(matchValue(v100)))(nextId(), v97)
      case 80 =>
        val v101 = v97.asInstanceOf[SequenceNode].children(2)
        val BindNode(v102, v103) = v101
        assert(v102.id == 28)
        val v104 = v97.asInstanceOf[SequenceNode].children(3)
        val v105 = unrollRepeat1(v104).map { elem =>
          val BindNode(v106, v107) = elem
          assert(v106.id == 65)
          val BindNode(v108, v109) = v107
          val v115 = v108.id match {
            case 66 =>
              val BindNode(v110, v111) = v109
              assert(v110.id == 67)
              val v112 = v111.asInstanceOf[SequenceNode].children(3)
              val BindNode(v113, v114) = v112
              assert(v113.id == 28)
              matchValue(v114)
          }
          v115
        }
        TupleValue(List(matchValue(v103)) ++ v105)(nextId(), v97)
    }
    v116
  }

  def matchValue(node: Node): Value = {
    val BindNode(v117, v118) = node
    val v134 = v117.id match {
      case 75 =>
        val v119 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v120, v121) = v119
        assert(v120.id == 76)
        matchTupleValue(v121)
      case 29 =>
        val v122 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v123, v124) = v122
        assert(v123.id == 30)
        BoolValue(matchBoolValue(v124))(nextId(), v118)
      case 45 =>
        val v125 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v126, v127) = v125
        assert(v126.id == 46)
        IntValue(matchIntLiteral(v127))(nextId(), v118)
      case 55 =>
        val v128 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v129, v130) = v128
        assert(v129.id == 56)
        matchListValue(v130)
      case 54 =>
        val v131 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v132, v133) = v131
        assert(v132.id == 17)
        StrValue(matchStrLiteral(v133))(nextId(), v118)
    }
    v134
  }
}
