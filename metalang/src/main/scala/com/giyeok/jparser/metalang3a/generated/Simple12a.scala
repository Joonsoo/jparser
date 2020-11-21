package com.giyeok.jparser.metalang3a.generated

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForestFunc
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.Parser
import scala.collection.immutable.ListSet

object Simple12a {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("A"), Set(3)),
  4 -> NGrammar.NOneOf(4, Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('c')))))),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(5,14)),
  5 -> NGrammar.NOneOf(5, Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('c')))))), Set(6)),
  6 -> NGrammar.NProxy(6, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('c')))), 7),
  8 -> NGrammar.NTerminal(8, Symbols.ExactChar('a')),
  9 -> NGrammar.NRepeat(9, Symbols.Repeat(Symbols.ExactChar('b'), 0), 10, 11),
  12 -> NGrammar.NTerminal(12, Symbols.ExactChar('b')),
  13 -> NGrammar.NTerminal(13, Symbols.ExactChar('c')),
  14 -> NGrammar.NProxy(14, Symbols.Proxy(Symbols.Sequence(Seq())), 10)),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.OneOf(ListSet(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('c')))))),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(4)),
  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('c'))), Seq(8,9,13)),
  10 -> NGrammar.NSequence(10, Symbols.Sequence(Seq()), Seq()),
  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('b'), 0),Symbols.ExactChar('b'))), Seq(9,12))),
    1)



  def matchA(node: Node): String = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 3 =>
        val v4 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v5, v6) = v4
        assert(v5.id == 4)
        val BindNode(v7, v8) = v6
        val v3 = v7.id match {
        case 14 =>
        None
        case 5 =>
          val BindNode(v9, v10) = v8
          assert(v9.id == 5)
          val BindNode(v11, v12) = v10
          assert(v11.id == 6)
          val BindNode(v13, v14) = v12
          assert(v13.id == 7)
          val v15 = v14.asInstanceOf[SequenceNode].children(1)
          Some(v15.toString)
      }
        if (v3.isDefined) v3.get else "none"
    }
  }

  def matchStart(node: Node): String = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchA(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[String, ParsingErrors.ParsingError] = parse(text) match {
    case Left(ctx) =>
      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
      tree match {
        case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
        case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
        case None => ???
      }
    case Right(error) => Right(error)
  }
}