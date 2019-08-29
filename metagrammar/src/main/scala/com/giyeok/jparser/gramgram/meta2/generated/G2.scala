package com.giyeok.jparser.gramgram.meta2.generated

import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode, TerminalNode}
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor, Parser}
import com.giyeok.jparser.{ParseForestFunc, ParsingErrors, Symbols}

object G2 {
    val ngrammar = new NGrammar(
        Map(1 -> NGrammar.NStart(1, 2),
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("TerminalChoice"), Set(3, 21)),
            4 -> NGrammar.NTerminal(4, Symbols.ExactChar('\'')),
            5 -> NGrammar.NNonterminal(5, Symbols.Nonterminal("TerminalChoiceElem"), Set(6, 15)),
            7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("TerminalChoiceChar"), Set(8, 12)),
            9 -> NGrammar.NExcept(9, Symbols.Except(Symbols.Any, Symbols.Chars(Set('\'', '-', '\\'))), 10, 11),
            10 -> NGrammar.NTerminal(10, Symbols.Any),
            11 -> NGrammar.NTerminal(11, Symbols.Chars(Set('\'', '-', '\\'))),
            13 -> NGrammar.NTerminal(13, Symbols.ExactChar('\\')),
            14 -> NGrammar.NTerminal(14, Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't'))),
            16 -> NGrammar.NNonterminal(16, Symbols.Nonterminal("TerminalChoiceRange"), Set(17)),
            18 -> NGrammar.NTerminal(18, Symbols.ExactChar('-')),
            19 -> NGrammar.NRepeat(19, Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), 5, 20)),
        Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceElem"), Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.ExactChar('\''))), Seq(4, 5, 19, 4)),
            6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(7)),
            8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Except(Symbols.Any, Symbols.Chars(Set('\'', '-', '\\'))))), Seq(9)),
            12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('\\'), Symbols.Chars(Set('\'', '-', '\\', 'b', 'n', 'r', 't')))), Seq(13, 14)),
            15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(16)),
            17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"), Symbols.ExactChar('-'), Symbols.Nonterminal("TerminalChoiceChar"))), Seq(7, 18, 7)),
            20 -> NGrammar.NSequence(20, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("TerminalChoiceElem"), 1), Symbols.Nonterminal("TerminalChoiceElem"))), Seq(19, 5)),
            21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.ExactChar('\''), Symbols.Nonterminal("TerminalChoiceRange"), Symbols.ExactChar('\''))), Seq(4, 16, 4))),
        1)

    case class TerminalChoice(choices: List[TerminalChoiceElem])

    sealed trait TerminalChoiceElem

    case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

    sealed trait TerminalChoiceChar extends TerminalChoiceElem

    case class CharAsIs(c: Node) extends TerminalChoiceChar

    case class CharEscaped(escapeCode: Node) extends TerminalChoiceChar

    def sourceTextOf(node: Node): String = node match {
        case TerminalNode(input) => input.toRawString
        case BindNode(_, body) => sourceTextOf(body)
        case JoinNode(body, _) => sourceTextOf(body)
        case seq: SequenceNode => seq.children map sourceTextOf mkString ""
        case _ => throw new Exception("Cyclic bind")
    }

    def matchTerminalChoice(node: Node): TerminalChoice = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 3 =>
                val v1 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2, v3) = v1
                assert(v2.id == 5)
                val v4 = matchTerminalChoiceElem(v3)
                val v5 = List(v4)
                val v6 = body.asInstanceOf[SequenceNode].children(2)
                val v14 = unrollRepeat1(v6) map { n =>
                    val BindNode(v7, v8) = n
                    assert(v7.id == 5)
                    val v9 = matchTerminalChoiceElem(v8)
                    v9
                }
                val v15 = v5 ++ v14
                val v16 = TerminalChoice(v15)
                v16
            case 21 =>
                val v17 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v18, v19) = v17
                assert(v18.id == 16)
                val v20 = matchTerminalChoiceRange(v19)
                val v21 = List(v20)
                val v22 = TerminalChoice(v21)
                v22
        }
    }

    def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 6 =>
                val v23 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v24, v25) = v23
                assert(v24.id == 7)
                val v26 = matchTerminalChoiceChar(v25)
                v26
            case 15 =>
                val v27 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v28, v29) = v27
                assert(v28.id == 16)
                val v30 = matchTerminalChoiceRange(v29)
                v30
        }
    }

    def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 17 =>
                val v31 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v32, v33) = v31
                assert(v32.id == 7)
                val v34 = matchTerminalChoiceChar(v33)
                val v35 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v36, v37) = v35
                assert(v36.id == 7)
                val v38 = matchTerminalChoiceChar(v37)
                val v39 = TerminalChoiceRange(v34, v38)
                v39
        }
    }

    def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 8 =>
                val v40 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v41, v42) = v40
                assert(v41.id == 9)
                val v43 = CharAsIs(v42)
                v43
            case 12 =>
                val v44 = body.asInstanceOf[SequenceNode].children(1)
                val v45 = CharEscaped(v44)
                v45
        }
    }

    def matchStart(node: Node): TerminalChoice = {
        val BindNode(start, BindNode(startNonterm, body)) = node
        assert(start.id == 1)
        assert(startNonterm.id == 2)
        matchTerminalChoice(body)
    }

    private def unrollRepeat1(node: Node): List[Node] = {
        val BindNode(repeat: NGrammar.NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
                assert(symbol.id == repeat.repeatSeq)
                val s = repeating.children(1)
                val r = unrollRepeat1(repeating.children(0))
                r :+ s
            case base =>
                List(base)
        }
    }

    lazy val naiveParser = new NaiveParser(ngrammar)

    def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
        naiveParser.parse(text)

    def parseAst(text: String): Either[TerminalChoice, ParsingErrors.ParsingError] =
        parse(text) match {
            case Left(ctx) =>
                val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                tree match {
                    case Some(forest) if forest.trees.size == 1 =>
                        Left(matchStart(forest.trees.head))
                    case Some(forest) =>
                        Right(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size))
                    case None => ???
                }
            case Right(error) => Right(error)
        }

    def main(args: Array[String]): Unit = {
        println(G2.parseAst("'abcdef-z'").swap.getOrElse())
    }
}
