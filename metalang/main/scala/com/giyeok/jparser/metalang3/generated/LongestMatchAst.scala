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
//object LongestMatchAst {
//  val ngrammar = new NGrammar(
//    Map(1 -> NGrammar.NStart(1, 2),
//      2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("S"), Set(3)),
//      4 -> NGrammar.NRepeat(4, Symbols.Repeat(Symbols.Nonterminal("Token"), 0), 5, 6),
//      7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("Token"), Set(8)),
//      9 -> NGrammar.NLongest(9, Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))))))), 10),
//      10 -> NGrammar.NOneOf(10, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS")))))), Set(11, 18, 25)),
//      11 -> NGrammar.NProxy(11, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))), 12),
//      13 -> NGrammar.NNonterminal(13, Symbols.Nonterminal("Id"), Set(14)),
//      15 -> NGrammar.NRepeat(15, Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 16, 17),
//      16 -> NGrammar.NTerminal(16, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
//      18 -> NGrammar.NProxy(18, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))), 19),
//      20 -> NGrammar.NNonterminal(20, Symbols.Nonterminal("Num"), Set(21)),
//      22 -> NGrammar.NRepeat(22, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1), 23, 24),
//      23 -> NGrammar.NTerminal(23, Symbols.Chars(('0' to '9').toSet)),
//      25 -> NGrammar.NProxy(25, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS")))), 26),
//      27 -> NGrammar.NNonterminal(27, Symbols.Nonterminal("WS"), Set(28)),
//      29 -> NGrammar.NRepeat(29, Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 1), 30, 31),
//      30 -> NGrammar.NTerminal(30, Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet))),
//    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("Token"), 0))), Seq(4)),
//      5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq()), Seq()),
//      6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("Token"), 0), Symbols.Nonterminal("Token"))), Seq(4, 7)),
//      8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Longest(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Id")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("Num")))), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))))))))), Seq(9)),
//      12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.Nonterminal("Id"))), Seq(13)),
//      14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1))), Seq(15)),
//      17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(15, 16)),
//      19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Nonterminal("Num"))), Seq(20)),
//      21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1))), Seq(22)),
//      24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 1), Symbols.Chars(('0' to '9').toSet))), Seq(22, 23)),
//      26 -> NGrammar.NSequence(26, Symbols.Sequence(Seq(Symbols.Nonterminal("WS"))), Seq(27)),
//      28 -> NGrammar.NSequence(28, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 1))), Seq(29)),
//      31 -> NGrammar.NSequence(31, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet), 1), Symbols.Chars(Set('\r', ' ') ++ ('\t' to '\n').toSet))), Seq(29, 30))),
//    1)
//
//  sealed trait WithParseNode {
//    val parseNode: Node
//  }
//
//  def matchId(node: Node): List[Char] = {
//    val BindNode(v1, v2) = node
//    val v7 = v1.id match {
//      case 14 =>
//        val v3 = v2.asInstanceOf[SequenceNode].children.head
//        val v4 = unrollRepeat1(v3).map { elem =>
//          val BindNode(v5, v6) = elem
//          assert(v5.id == 16)
//          v6.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//        }
//        v4
//    }
//    v7
//  }
//
//  def matchNum(node: Node): List[Char] = {
//    val BindNode(v8, v9) = node
//    val v14 = v8.id match {
//      case 21 =>
//        val v10 = v9.asInstanceOf[SequenceNode].children.head
//        val v11 = unrollRepeat1(v10).map { elem =>
//          val BindNode(v12, v13) = elem
//          assert(v12.id == 23)
//          v13.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//        }
//        v11
//    }
//    v14
//  }
//
//  def matchS(node: Node): List[List[Char]] = {
//    val BindNode(v15, v16) = node
//    val v21 = v15.id match {
//      case 3 =>
//        val v17 = v16.asInstanceOf[SequenceNode].children.head
//        val v18 = unrollRepeat0(v17).map { elem =>
//          val BindNode(v19, v20) = elem
//          assert(v19.id == 7)
//          matchToken(v20)
//        }
//        v18
//    }
//    v21
//  }
//
//  def matchToken(node: Node): List[Char] = {
//    val BindNode(v22, v23) = node
//    val v47 = v22.id match {
//      case 8 =>
//        val v24 = v23.asInstanceOf[SequenceNode].children.head
//        val BindNode(v25, v26) = v24
//        assert(v25.id == 9)
//        val BindNode(v27, v28) = v26
//        assert(v27.id == 10)
//        val BindNode(v29, v30) = v28
//        val v46 = v29.id match {
//          case 11 =>
//            val BindNode(v31, v32) = v30
//            assert(v31.id == 12)
//            val v33 = v32.asInstanceOf[SequenceNode].children.head
//            val BindNode(v34, v35) = v33
//            assert(v34.id == 13)
//            matchId(v35)
//          case 18 =>
//            val BindNode(v36, v37) = v30
//            assert(v36.id == 19)
//            val v38 = v37.asInstanceOf[SequenceNode].children.head
//            val BindNode(v39, v40) = v38
//            assert(v39.id == 20)
//            matchNum(v40)
//          case 25 =>
//            val BindNode(v41, v42) = v30
//            assert(v41.id == 26)
//            val v43 = v42.asInstanceOf[SequenceNode].children.head
//            val BindNode(v44, v45) = v43
//            assert(v44.id == 27)
//            matchWS(v45)
//        }
//        v46
//    }
//    v47
//  }
//
//  def matchWS(node: Node): List[Char] = {
//    val BindNode(v48, v49) = node
//    val v54 = v48.id match {
//      case 28 =>
//        val v50 = v49.asInstanceOf[SequenceNode].children.head
//        val v51 = unrollRepeat1(v50).map { elem =>
//          val BindNode(v52, v53) = elem
//          assert(v52.id == 30)
//          v53.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
//        }
//        v51
//    }
//    v54
//  }
//
//  def matchStart(node: Node): List[List[Char]] = {
//    val BindNode(start, BindNode(_, body)) = node
//    assert(start.id == 1)
//    matchS(body)
//  }
//
//  val naiveParser = new NaiveParser(ngrammar)
//
//  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
//    naiveParser.parse(text)
//
//  def parseAst(text: String): Either[List[List[Char]], ParsingErrors.ParsingError] =
//    ParseTreeUtil.parseAst(naiveParser, text, matchStart)
//}
