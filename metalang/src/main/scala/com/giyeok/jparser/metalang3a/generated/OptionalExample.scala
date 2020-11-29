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
import com.giyeok.jparser.nparser.Parser
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
import scala.collection.immutable.ListSet

object OptionalExample {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("A"), Set(3)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("B"), Set(5,11)),
  6 -> NGrammar.NProxy(6, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('c')))), 7),
  8 -> NGrammar.NTerminal(8, Symbols.ExactChar('a')),
  9 -> NGrammar.NTerminal(9, Symbols.ExactChar('b')),
  10 -> NGrammar.NTerminal(10, Symbols.ExactChar('c')),
  12 -> NGrammar.NRepeat(12, Symbols.Repeat(Symbols.ExactChar('d'), 1), 13, 14),
  13 -> NGrammar.NTerminal(13, Symbols.ExactChar('d'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("B"))), Seq(4)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('c')))))), Seq(6)),
  7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.ExactChar('a'),Symbols.ExactChar('b'),Symbols.ExactChar('c'))), Seq(8,9,10)),
  11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('d'), 1))), Seq(12)),
  14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('d'), 1),Symbols.ExactChar('d'))), Seq(12,13))),
    1)

  case class A(value: Option[String])


  def matchA(node: Node): A = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 4)
        A(Some(matchB(v5).toString))
    }
  }

  def matchB(node: Node): String = {
    val BindNode(v6, v7) = node
    v6.id match {
      case 5 =>
        val v8 = v7.asInstanceOf[SequenceNode].children.head
        "abc"
      case 11 =>
        val v9 = v7.asInstanceOf[SequenceNode].children.head
        val v10 = unrollRepeat1(v9).map { elem =>
        val BindNode(v11, v12) = elem
        assert(v11.id == 13)
        v12.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        v10.toString
    }
  }

  def matchStart(node: Node): A = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchA(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[A, ParsingErrors.ParsingError] = parse(text) match {
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
    println(parseAst("abc"))
    println(parseAst("d"))
  }
}
