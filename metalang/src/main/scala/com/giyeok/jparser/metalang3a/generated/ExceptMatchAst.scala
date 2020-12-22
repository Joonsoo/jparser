package com.giyeok.jparser.metalang3a.generated

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

object ExceptMatchAst {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("S"), Set(3)),
  4 -> NGrammar.NRepeat(4, Symbols.Repeat(Symbols.Nonterminal("Token"), 0), 5, 6),
  7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("Token"), Set(8)),
  9 -> NGrammar.NLongest(9, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Kw")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))))))), 10),
  10 -> NGrammar.NOneOf(10, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Kw")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS")))))), Set(11,33,35,42)),
  11 -> NGrammar.NProxy(11, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))), 12),
  13 -> NGrammar.NNonterminal(13, Symbols.Nonterminal("Id"), Set(14)),
  15 -> NGrammar.NExcept(15, Symbols.Except(Symbols.Nonterminal("Name"), Symbols.Nonterminal("Kw")), 16, 21),
  16 -> NGrammar.NNonterminal(16, Symbols.Nonterminal("Name"), Set(17)),
  18 -> NGrammar.NRepeat(18, Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 19, 20),
  19 -> NGrammar.NTerminal(19, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
  21 -> NGrammar.NNonterminal(21, Symbols.Nonterminal("Kw"), Set(22,27)),
  23 -> NGrammar.NProxy(23, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'),Symbols.ExactChar('f')))), 24),
  25 -> NGrammar.NTerminal(25, Symbols.ExactChar('i')),
  26 -> NGrammar.NTerminal(26, Symbols.ExactChar('f')),
  28 -> NGrammar.NProxy(28, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))), 29),
  30 -> NGrammar.NTerminal(30, Symbols.ExactChar('e')),
  31 -> NGrammar.NTerminal(31, Symbols.ExactChar('l')),
  32 -> NGrammar.NTerminal(32, Symbols.ExactChar('s')),
  33 -> NGrammar.NProxy(33, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Kw")))), 34),
  35 -> NGrammar.NProxy(35, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))), 36),
  37 -> NGrammar.NNonterminal(37, Symbols.Nonterminal("Num"), Set(38)),
  39 -> NGrammar.NRepeat(39, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1), 40, 41),
  40 -> NGrammar.NTerminal(40, Symbols.Chars(('0' to '9').toSet)),
  42 -> NGrammar.NProxy(42, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS")))), 43),
  44 -> NGrammar.NNonterminal(44, Symbols.Nonterminal("WS"), Set(45)),
  46 -> NGrammar.NRepeat(46, Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet), 1), 47, 48),
  47 -> NGrammar.NTerminal(47, Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("Token"), 0))), Seq(4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq()), Seq()),
  6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("Token"), 0),Symbols.Nonterminal("Token"))), Seq(4,7)),
  8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Kw")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))))))))), Seq(9)),
  12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(13)),
  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Except(Symbols.Nonterminal("Name"), Symbols.Nonterminal("Kw")))), Seq(15)),
  17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))), Seq(18)),
  20 -> NGrammar.NSequence(20, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1),Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(18,19)),
  22 -> NGrammar.NSequence(22, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('i'),Symbols.ExactChar('f')))))), Seq(23)),
  24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.ExactChar('i'),Symbols.ExactChar('f'))), Seq(25,26)),
  27 -> NGrammar.NSequence(27, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e')))))), Seq(28)),
  29 -> NGrammar.NSequence(29, Symbols.Sequence(Seq(Symbols.ExactChar('e'),Symbols.ExactChar('l'),Symbols.ExactChar('s'),Symbols.ExactChar('e'))), Seq(30,31,32,30)),
  34 -> NGrammar.NSequence(34, Symbols.Sequence(Seq(Symbols.Nonterminal("Kw"))), Seq(21)),
  36 -> NGrammar.NSequence(36, Symbols.Sequence(Seq(Symbols.Nonterminal("Num"))), Seq(37)),
  38 -> NGrammar.NSequence(38, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1))), Seq(39)),
  41 -> NGrammar.NSequence(41, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1),Symbols.Chars(('0' to '9').toSet))), Seq(39,40)),
  43 -> NGrammar.NSequence(43, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))), Seq(44)),
  45 -> NGrammar.NSequence(45, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet), 1))), Seq(46)),
  48 -> NGrammar.NSequence(48, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet), 1),Symbols.Chars(Set('\r',' ') ++ ('\t' to '\n').toSet))), Seq(46,47))),
    1)

  sealed trait WithParseNode { val parseNode: Node }
  case class Id(name: String)(override val parseNode: Node) extends Token with WithParseNode
  case class Keyword(typ: Kw.Value)(override val parseNode: Node) extends Token with WithParseNode
  case class Number(value: String)(override val parseNode: Node) extends Token with WithParseNode
  sealed trait Token extends WithParseNode
  case class WS()(override val parseNode: Node) extends Token with WithParseNode
  object Kw extends Enumeration { val ELSE, IF = Value }

  def matchId(node: Node): Id = {
    val BindNode(v1, v2) = node
    val v8 = v1.id match {
      case 14 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 15)
        val BindNode(v6, v7) = v5
        assert(v6.id == 16)
        Id(matchName(v7).map(x => x.toString).mkString(""))(v2)
    }
    v8
  }

  def matchKw(node: Node): Keyword = {
    val BindNode(v9, v10) = node
    val v11 = v9.id match {
      case 22 =>
      Keyword(Kw.IF)(v10)
      case 27 =>
      Keyword(Kw.ELSE)(v10)
    }
    v11
  }

  def matchName(node: Node): List[Char] = {
    val BindNode(v12, v13) = node
    val v18 = v12.id match {
      case 17 =>
        val v14 = v13.asInstanceOf[SequenceNode].children.head
        val v15 = unrollRepeat1(v14).map { elem =>
        val BindNode(v16, v17) = elem
        assert(v16.id == 19)
        v17.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v15
    }
    v18
  }

  def matchNum(node: Node): Number = {
    val BindNode(v19, v20) = node
    val v25 = v19.id match {
      case 38 =>
        val v21 = v20.asInstanceOf[SequenceNode].children.head
        val v22 = unrollRepeat1(v21).map { elem =>
        val BindNode(v23, v24) = elem
        assert(v23.id == 40)
        v24.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        Number(v22.map(x => x.toString).mkString(""))(v20)
    }
    v25
  }

  def matchS(node: Node): List[Token] = {
    val BindNode(v26, v27) = node
    val v32 = v26.id match {
      case 3 =>
        val v28 = v27.asInstanceOf[SequenceNode].children.head
        val v29 = unrollRepeat0(v28).map { elem =>
        val BindNode(v30, v31) = elem
        assert(v30.id == 7)
        matchToken(v31)
        }
        v29
    }
    v32
  }

  def matchToken(node: Node): Token = {
    val BindNode(v33, v34) = node
    val v63 = v33.id match {
      case 8 =>
        val v35 = v34.asInstanceOf[SequenceNode].children.head
        val BindNode(v36, v37) = v35
        assert(v36.id == 9)
        val BindNode(v38, v39) = v37
        assert(v38.id == 10)
        val BindNode(v40, v41) = v39
        val v62 = v40.id match {
        case 11 =>
          val BindNode(v42, v43) = v41
          assert(v42.id == 12)
          val v44 = v43.asInstanceOf[SequenceNode].children.head
          val BindNode(v45, v46) = v44
          assert(v45.id == 13)
          matchId(v46)
        case 33 =>
          val BindNode(v47, v48) = v41
          assert(v47.id == 34)
          val v49 = v48.asInstanceOf[SequenceNode].children.head
          val BindNode(v50, v51) = v49
          assert(v50.id == 21)
          matchKw(v51)
        case 35 =>
          val BindNode(v52, v53) = v41
          assert(v52.id == 36)
          val v54 = v53.asInstanceOf[SequenceNode].children.head
          val BindNode(v55, v56) = v54
          assert(v55.id == 37)
          matchNum(v56)
        case 42 =>
          val BindNode(v57, v58) = v41
          assert(v57.id == 43)
          val v59 = v58.asInstanceOf[SequenceNode].children.head
          val BindNode(v60, v61) = v59
          assert(v60.id == 44)
          matchWS(v61)
      }
        v62
    }
    v63
  }

  def matchWS(node: Node): WS = {
    val BindNode(v64, v65) = node
    val v66 = v64.id match {
      case 45 =>
      WS()(v65)
    }
    v66
  }

  def matchStart(node: Node): List[Token] = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchS(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[List[Token], ParsingErrors.ParsingError] =
    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
}
