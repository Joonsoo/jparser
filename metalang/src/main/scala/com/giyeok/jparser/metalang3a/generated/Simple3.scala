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
import scala.collection.immutable.ListSet

object Simple3 {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("A"), Set(3)),
  4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("B"), Set(5)),
  6 -> NGrammar.NTerminal(6, Symbols.ExactChar('a')),
  7 -> NGrammar.NTerminal(7, Symbols.ExactChar(' ')),
  8 -> NGrammar.NNonterminal(8, Symbols.Nonterminal("C"), Set(9)),
  10 -> NGrammar.NTerminal(10, Symbols.ExactChar('b'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("B"),Symbols.ExactChar(' '),Symbols.Nonterminal("C"))), Seq(4,7,8)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.ExactChar('a'))), Seq(6)),
  9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.ExactChar('b'))), Seq(10))),
    1)

  case class ClassA(value: String)

  def matchA(node: Node): ClassA = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val BindNode(v4, v5) = v3
        assert(v4.id == 4)
        val v6 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v7, v8) = v6
        assert(v7.id == 8)
        ClassA(matchB(v5).toString + matchC(v8).toString)
    }
  }

  def matchB(node: Node): Char = {
    val BindNode(v9, v10) = node
    v9.id match {
      case 5 =>
        val v11 = v10.asInstanceOf[SequenceNode].children.head
        val BindNode(v12, v13) = v11
        assert(v12.id == 6)
        v13.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
  }

  def matchC(node: Node): Char = {
    val BindNode(v14, v15) = node
    v14.id match {
      case 9 =>
        val v16 = v15.asInstanceOf[SequenceNode].children.head
        val BindNode(v17, v18) = v16
        assert(v17.id == 10)
        v18.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
  }

  def matchStart(node: Node): ClassA = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchA(body)
  }

  val naiveParser = new NaiveParser(ngrammar)

  def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
    naiveParser.parse(text)

  def parseAst(text: String): Either[ClassA, ParsingErrors.ParsingError] = parse(text) match {
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