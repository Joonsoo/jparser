package com.giyeok.jparser.test

import com.giyeok.jparser.{Inputs, NGrammar, Symbols}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import PyObjOptAst._
import com.giyeok.jparser.nparser.Kernel

import scala.collection.immutable.{ListSet, Seq}

object PyObjOptAst {
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
      84 -> NGrammar.NSequence(84, Symbols.Sequence(Seq(
        Symbols.Repeat(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))), 0),
        Symbols.OneOf(ListSet(
          Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField")))))
        )
      )), Seq(83, 85)),
      87 -> NGrammar.NSequence(87, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"), Symbols.ExactChar(','), Symbols.Nonterminal("WS"), Symbols.Nonterminal("ObjField"))), Seq(5, 68, 5, 15))),
    1)

  sealed trait WithIdAndRange {
    val id: Int
    val start: Int
    val end: Int
  }

  case class BoolValue(value: BoolEnum.Value)(override val id: Int, override val start: Int, override val end: Int) extends Value with WithIdAndRange

  case class IntLiteral(value: String)(override val id: Int, override val start: Int, override val end: Int) extends WithIdAndRange

  case class IntValue(value: IntLiteral)(override val id: Int, override val start: Int, override val end: Int) extends Value with WithIdAndRange

  case class ListValue(elems: Option[List[Value]])(override val id: Int, override val start: Int, override val end: Int) extends Value with WithIdAndRange

  case class ObjField(name: StrLiteral, value: Value)(override val id: Int, override val start: Int, override val end: Int) extends WithIdAndRange

  case class PyObj(fields: Option[List[ObjField]])(override val id: Int, override val start: Int, override val end: Int) extends WithIdAndRange

  case class StrLiteral(value: String)(override val id: Int, override val start: Int, override val end: Int) extends WithIdAndRange

  case class StrValue(value: StrLiteral)(override val id: Int, override val start: Int, override val end: Int) extends Value with WithIdAndRange

  case class TupleValue(elems: List[Value])(override val id: Int, override val start: Int, override val end: Int) extends Value with WithIdAndRange

  sealed trait Value extends WithIdAndRange

  object BoolEnum extends Enumeration {
    val False, True = Value
  }
}

class PyObjOptAst(val inputs: Seq[Inputs.Input], val history: Seq[Kernels]) {
  private var idCounter = 0

  assert(inputs.size + 1 == history.size)

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  def matchStart(): PyObj = {
    val lastGen = inputs.size
    val matchedKernels = history(lastGen).kernels.filter(
      k => k.symbolId == 1 && k.endGen == lastGen)
    assert(matchedKernels.size == 1)
    val matchedKernel = matchedKernels.head
    matchPyObj(matchedKernel)
  }

  implicit class SingleKernels(kernels: Set[Kernel]) {
    def checkSingle(): Kernel = {
      assert(kernels.size == 1)
      kernels.head
    }

    def checkSingleOrNone(): Option[Kernel] = {
      assert(kernels.size <= 1)
      if (kernels.isEmpty) None else Some(kernels.head)
    }
  }

  def matchPyObj(parent: Kernel): PyObj = {
    val bodyKernel = history(parent.endGen).kernels.filter(k =>
      k.beginGen == parent.beginGen &&
        (k.symbolId == 3 && k.pointer == 4) // TODO 여기처럼 body가 하나인 경우 이런 체크는 생략 가능
    ).checkSingle()
    println(bodyKernel)

    val k3 = history(parent.endGen).kernels.filter(k =>
      k.endGen == parent.endGen && (k.symbolId == 88 && k.pointer == 1)).checkSingle()
    val k2 = history(k3.beginGen).kernels.filter(k =>
      k.endGen == k3.beginGen && (k.symbolId == 11 && k.pointer == 1)).checkSingle()
    val v1 = {
      assert(k2.symbolId == 11 && k2.pointer == 1)
      // 12, 13은 skip하고 14로 왔음
      val k14o = history(k2.endGen).kernels.filter(k => k.beginGen == k2.beginGen &&
        (k.symbolId == 14 && k.pointer == 4)).checkSingleOrNone()
      val k73 = history(k2.endGen).kernels.filter(k => k.beginGen == k2.beginGen &&
        (k.symbolId == 73 && k.pointer == 1)).checkSingleOrNone()
      if (k14o.isDefined) {
        assert(k73.isEmpty)
        val k14 = k14o.get
        assert(k14.symbolId == 14 && k14.pointer == 4)
        // Seq(15, 83, 69, 5)
        println(k14)
        val k3 = history(k14.endGen).kernels.filter(k =>
          k.endGen == k14.endGen && (k.symbolId == 5 && k.pointer == 1)).checkSingle()
        val k2 = history(k3.beginGen).kernels.filter(k =>
          k.endGen == k3.beginGen && (k.symbolId == 69 && k.pointer == 1)).checkSingle()
        val k1 = history(k2.beginGen).kernels.filter(k =>
          k.endGen == k2.beginGen && (k.symbolId == 83 && k.pointer == 1)).checkSingle()
        val k0 = history(k1.beginGen).kernels.filter(k =>
          k.endGen == k1.beginGen && (k.symbolId == 15 && k.pointer == 1)).checkSingle()
        println(k0)
        println(s"$k0 $k1 $k2 $k3")
        val unrolled = unrollRepeat0(k1, 8, 84, 85).map { elem =>
          matchCommaAndObjField(elem)
        }
        val elem = matchObjField(k0)
        Some(elem +: unrolled)
      } else {
        assert(k73.isDefined)
        None
      }
    }
    PyObj(v1)(nextId(), parent.beginGen, parent.endGen)
  }

  def unrollRepeat0(parent: Kernel, baseSymId: Int, repeatSymId: Int, itemSymId: Int): List[Kernel] = {
    val base = history(parent.endGen).kernels.filter(k =>
      k.beginGen == parent.beginGen && (k.symbolId == baseSymId && k.pointer == 0)).checkSingleOrNone()
    val repeat = history(parent.endGen).kernels.filter(k =>
      k.beginGen == parent.beginGen && (k.symbolId == repeatSymId && k.pointer == 2)).checkSingleOrNone()
    if (base.isDefined) {
      assert(repeat.isEmpty)
      List()
    } else {
      assert(repeat.isDefined)
      val k1 = history(parent.endGen).kernels.filter(k =>
        k.endGen == parent.endGen && (k.symbolId == itemSymId && k.pointer == 1)).checkSingle()
      val k0 = history(k1.beginGen).kernels.filter(k =>
        k.endGen == k1.beginGen && (k.symbolId == parent.symbolId && k.pointer == 1)).checkSingle()
      unrollRepeat0(k0, baseSymId, repeatSymId, itemSymId) :+ k1
    }
  }

  def matchCommaAndObjField(parent: Kernel): ObjField = {
    val k3 = history(parent.endGen).kernels.filter(k =>
      k.endGen == parent.endGen && (k.symbolId == 15 && k.pointer == 1)).checkSingle()
    matchObjField(k3)
  }

  def matchObjField(parent: Kernel): ObjField = {
    assert(parent.symbolId == 15)
    // Seq(17, 5, 27, 5, 28)
    val k4 = history(parent.endGen).kernels.filter(k =>
      k.endGen == parent.endGen && (k.symbolId == 28 && k.pointer == 1)).checkSingle()
    val k3 = history(k4.beginGen).kernels.filter(k =>
      k.endGen == k4.beginGen && (k.symbolId == 5 && k.pointer == 1)).checkSingle()
    val k2 = history(k3.beginGen).kernels.filter(k =>
      k.endGen == k3.beginGen && (k.symbolId == 27 && k.pointer == 1)).checkSingle()
    val k1 = history(k2.beginGen).kernels.filter(k =>
      k.endGen == k2.beginGen && (k.symbolId == 5 && k.pointer == 1)).checkSingle()
    val k0 = history(k1.beginGen).kernels.filter(k =>
      k.endGen == k1.beginGen && (k.symbolId == 17 && k.pointer == 1)).checkSingle()
    println(s"$k0 $k1 $k2 $k3 $k4")
    ObjField(matchStrLiteral(k0), matchValue(k4))(nextId(), parent.beginGen, parent.endGen)
  }

  def matchStrLiteral(parent: Kernel): StrLiteral = {
    StrLiteral("TODO")(nextId(), parent.beginGen, parent.endGen)
  }

  def matchValue(parent: Kernel): Value = {
    StrValue(StrLiteral("TODO")(nextId(), -1, -1))(nextId(), parent.beginGen, parent.endGen)
  }
}
