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

object OptionalWithShortEnumExample {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("S"), Set(3)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("A"), Set(5)),
  6 -> NGrammar.NOneOf(6, Symbols.OneOf(ListSet(Symbols.Nonterminal("B"),Symbols.Proxy(Symbols.Sequence(Seq())))), Set(7,12)),
  7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("B"), Set(8,10)),
  9 -> NGrammar.NTerminal(9, Symbols.ExactChar('a')),
  11 -> NGrammar.NTerminal(11, Symbols.ExactChar('b')),
  12 -> NGrammar.NProxy(12, Symbols.Proxy(Symbols.Sequence(Seq())), 13),
  14 -> NGrammar.NTerminal(14, Symbols.ExactChar('.'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("A"),Symbols.ExactChar('.'),Symbols.Nonterminal("A"))), Seq(4,14,4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.OneOf(ListSet(Symbols.Nonterminal("B"),Symbols.Proxy(Symbols.Sequence(Seq())))))), Seq(6)),
  8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.ExactChar('a'))), Seq(9)),
  10 -> NGrammar.NSequence(10, Symbols.Sequence(Seq(Symbols.ExactChar('b'))), Seq(11)),
  13 -> NGrammar.NSequence(13, Symbols.Sequence(Seq()), Seq())),
    1)

  case class S(first: Option[EnumB.Value], second: Option[EnumB.Value])
  object EnumB extends Enumeration { val A, B = Value }

  def matchB(node: Node): EnumB.Value = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 8 =>
      EnumB.A
      case 10 =>
      EnumB.B
    }
  }

  def matchS(node: Node): S = {
    val BindNode(v3, v4) = node
    v3.id match {
      case 3 =>
        val v5 = v4.asInstanceOf[SequenceNode].children.head
        val BindNode(v6, v7) = v5
        assert(v6.id == 4)
        val v8 = v4.asInstanceOf[SequenceNode].children(2)
        val BindNode(v9, v10) = v8
        assert(v9.id == 4)
        S(matchA(v7), matchA(v10))
    }
  }

  def matchA(node: Node): Option[EnumB.Value] = {
    val BindNode(v11, v12) = node
    v11.id match {
      case 5 =>
        val v13 = v12.asInstanceOf[SequenceNode].children.head
        val BindNode(v14, v15) = v13
        assert(v14.id == 6)
        val BindNode(v16, v17) = v15
        v16.id match {
        case 12 =>
        None
        case 7 =>
        Some(matchB(v17))
      }
    }
  }

  def matchStart(node: Node): S = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchS(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[S, ParsingErrors.ParsingError] = parse(text) match {
    case Left(ctx) =>
      val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
      tree match {
        case Some(forest) if forest.trees.size == 1 => Left(matchStart(forest.trees.head))
        case Some(forest) => Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
        case None => ???
      }
    case Right(error) => Right(error)
  }

  def main(args: Array[String]): Unit = {
    println(parseAst("."))
    println(parseAst(".a"))
    println(parseAst(".b"))
    println(parseAst("a."))
    println(parseAst("b."))
    println(parseAst("a.a"))
    println(parseAst("a.b"))
    println(parseAst("b.a"))
    println(parseAst("b.b"))
  }
}
