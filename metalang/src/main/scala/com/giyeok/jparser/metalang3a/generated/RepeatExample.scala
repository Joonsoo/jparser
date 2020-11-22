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
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGenUtil.unrollRepeat0
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGenUtil.unrollRepeat1
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.nparser.Parser
import scala.collection.immutable.ListSet

object RepeatExample {
  val ngrammar =   new NGrammar(
    Map(1 -> NGrammar.NStart(1, 2),
  2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("A"), Set(3)),
  4 -> NGrammar.NRepeat(4, Symbols.Repeat(Symbols.ExactChar('a'), 0), 5, 6),
  7 -> NGrammar.NTerminal(7, Symbols.ExactChar('a')),
  8 -> NGrammar.NRepeat(8, Symbols.Repeat(Symbols.ExactChar('b'), 1), 9, 10),
  9 -> NGrammar.NTerminal(9, Symbols.ExactChar('b'))),
    Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('a'), 0),Symbols.Repeat(Symbols.ExactChar('b'), 1))), Seq(4,8)),
  5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq()), Seq()),
  6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('a'), 0),Symbols.ExactChar('a'))), Seq(4,7)),
  10 -> NGrammar.NSequence(10, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.ExactChar('b'), 1),Symbols.ExactChar('b'))), Seq(8,9))),
    1)

  case class A(as: List[Char], bs: List[Char], cs: List[List[Char]], ds: List[Char])


  def matchA(node: Node): A = {
    val BindNode(v1, v2) = node
    v1.id match {
      case 3 =>
        val v3 = v2.asInstanceOf[SequenceNode].children.head
        val v4 = unrollRepeat0(v3).map { elem =>
        val BindNode(v5, v6) = elem
        assert(v5.id == 7)
        v6.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        val v7 = v2.asInstanceOf[SequenceNode].children(1)
        val v8 = unrollRepeat1(v7).map { elem =>
        val BindNode(v9, v10) = elem
        assert(v9.id == 9)
        v10.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        val v11 = v2.asInstanceOf[SequenceNode].children.head
        val v12 = unrollRepeat0(v11).map { elem =>
        val BindNode(v13, v14) = elem
        assert(v13.id == 7)
        v14.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        val v15 = v2.asInstanceOf[SequenceNode].children(1)
        val v16 = unrollRepeat1(v15).map { elem =>
        val BindNode(v17, v18) = elem
        assert(v17.id == 9)
        v18.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        val v19 = v2.asInstanceOf[SequenceNode].children.head
        val v20 = unrollRepeat0(v19).map { elem =>
        val BindNode(v21, v22) = elem
        assert(v21.id == 7)
        v22.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        val v23 = v2.asInstanceOf[SequenceNode].children(1)
        val v24 = unrollRepeat1(v23).map { elem =>
        val BindNode(v25, v26) = elem
        assert(v25.id == 9)
        v26.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
        A(v4, v8, List(v12, v16), v20 ++ v24)
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
    println(parseAst("b"))
    println(parseAst("aaabbbb"))
  }
}
