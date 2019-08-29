import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{NGrammar, ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object G2 {
    val ngrammar = new NGrammar(
        Map(1 -> NGrammar.NStart(1, 2),
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("TerminalChoice"), Set(3,25)),
            4 -> NGrammar.NTerminal(4, Symbols.ExactChar('\'')),
            5 -> NGrammar.NNonterminal(5, Symbols.Nonterminal("TerminalChoiceElem"), Set(6,15)),
            7 -> NGrammar.NNonterminal(7, Symbols.Nonterminal("TerminalChoiceChar"), Set(8,12)),
            9 -> NGrammar.NExcept(9, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))), 10, 11),
            10 -> NGrammar.NTerminal(10, Symbols.AnyChar),
            11 -> NGrammar.NTerminal(11, Symbols.Chars(Set('\'','-','\\'))),
            13 -> NGrammar.NTerminal(13, Symbols.ExactChar('\\')),
            14 -> NGrammar.NTerminal(14, Symbols.Chars(Set('\'','-','\\','b','n','r','t'))),
            16 -> NGrammar.NNonterminal(16, Symbols.Nonterminal("TerminalChoiceRange"), Set(17)),
            18 -> NGrammar.NTerminal(18, Symbols.ExactChar('-')),
            19 -> NGrammar.NRepeat(19, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq()))))), 1), 20, 24),
            20 -> NGrammar.NProxy(20, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq()))))), 21),
            22 -> NGrammar.NProxy(22, Symbols.Proxy(Symbols.Sequence(Seq())), 23)),
        Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq()))))), 1),Symbols.ExactChar('\''))), Seq(4,5,19,4)),
            6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"))), Seq(7)),
            8 -> NGrammar.NSequence(8, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('\'','-','\\'))))), Seq(9)),
            12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('\'','-','\\','b','n','r','t')))), Seq(13,14)),
            15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceRange"))), Seq(16)),
            17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceChar"),Symbols.ExactChar('-'),Symbols.Nonterminal("TerminalChoiceChar"))), Seq(7,18,7)),
            21 -> NGrammar.NSequence(21, Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq())))), Seq(5,22)),
            23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq()), Seq()),
            24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq()))))), 1),Symbols.Proxy(Symbols.Sequence(Seq(Symbols.Nonterminal("TerminalChoiceElem"),Symbols.Proxy(Symbols.Sequence(Seq()))))))), Seq(19,20)),
            25 -> NGrammar.NSequence(25, Symbols.Sequence(Seq(Symbols.ExactChar('\''),Symbols.Nonterminal("TerminalChoiceRange"),Symbols.ExactChar('\''))), Seq(4,16,4))),
        1)

    case class TerminalChoice(choices:List[TerminalChoiceElem])
    sealed trait TerminalChoiceElem
    case class TerminalChoiceRange(start:TerminalChoiceChar, end:TerminalChoiceChar) extends TerminalChoiceElem
    sealed trait TerminalChoiceChar extends TerminalChoiceElem
    case class CharAsIs(c:Node) extends TerminalChoiceChar
    case class CharEscaped(escapeCode:Node) extends TerminalChoiceChar
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
                val v11 = unrollRepeat1(v6) map { n =>
                    val BindNode(v7, v8) = n
                    assert(v7.id == 20)
                    val BindNode(v9, v10) = v8
                    assert(v9.id == 21)
                    v10
                }
                val v16 = v11 map { n =>
                    val v12 = n.asInstanceOf[SequenceNode].children(0)
                    val BindNode(v13, v14) = v12
                    assert(v13.id == 5)
                    val v15 = matchTerminalChoiceElem(v14)
                    v15
                }
                val v17 = v5 ++ v16
                val v18 = TerminalChoice(v17)
                v18
            case 25 =>
                val v19 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v20, v21) = v19
                assert(v20.id == 16)
                val v22 = matchTerminalChoiceRange(v21)
                val v23 = List(v22)
                val v24 = TerminalChoice(v23)
                v24
        }
    }
    def matchTerminalChoiceElem(node: Node): TerminalChoiceElem = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 6 =>
                val v25 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v26, v27) = v25
                assert(v26.id == 7)
                val v28 = matchTerminalChoiceChar(v27)
                v28
            case 15 =>
                val v29 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v30, v31) = v29
                assert(v30.id == 16)
                val v32 = matchTerminalChoiceRange(v31)
                v32
        }
    }
    def matchTerminalChoiceRange(node: Node): TerminalChoiceRange = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 17 =>
                val v33 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v34, v35) = v33
                assert(v34.id == 7)
                val v36 = matchTerminalChoiceChar(v35)
                val v37 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v38, v39) = v37
                assert(v38.id == 7)
                val v40 = matchTerminalChoiceChar(v39)
                val v41 = TerminalChoiceRange(v36,v40)
                v41
        }
    }
    def matchTerminalChoiceChar(node: Node): TerminalChoiceChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 8 =>
                val v42 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v43, v44) = v42
                assert(v43.id == 9)
                val v45 = CharAsIs(v44)
                v45
            case 12 =>
                val v46 = body.asInstanceOf[SequenceNode].children(1)
                val v47 = CharEscaped(v46)
                v47
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
        println(parseAst("'abcde-z'"))
    }
}
