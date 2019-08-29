import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{NGrammar, ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{ParsingErrors, ParseForestFunc, Symbols}
import scala.collection.immutable.ListSet

object G3 {
    val ngrammar = new NGrammar(
        Map(// <start>,
            1 -> NGrammar.NStart(1, 2),
            // StringLiteral,
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("StringLiteral"), Set(3)),
            // '"',
            4 -> NGrammar.NTerminal(4, Symbols.ExactChar('"')),
            // StringChar*,
            5 -> NGrammar.NRepeat(5, Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0), 6, 7),
            // StringChar,
            8 -> NGrammar.NNonterminal(8, Symbols.Nonterminal("StringChar"), Set(9,13)),
            // <any>-{"\},
            10 -> NGrammar.NExcept(10, Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))), 11, 12),
            // <any>,
            11 -> NGrammar.NTerminal(11, Symbols.AnyChar),
            // {"\},
            12 -> NGrammar.NTerminal(12, Symbols.Chars(Set('"','\\'))),
            // '\',
            14 -> NGrammar.NTerminal(14, Symbols.ExactChar('\\')),
            // {"\bnrt},
            15 -> NGrammar.NTerminal(15, Symbols.Chars(Set('"','\\','b','n','r','t')))),
        Map(// ['"' StringChar* '"'],
            3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('"'),Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.ExactChar('"'))), Seq(4,5,4)),
            // [],
            6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq()), Seq()),
            // [StringChar* StringChar],
            7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Nonterminal("StringChar"), 0),Symbols.Nonterminal("StringChar"))), Seq(5,8)),
            // [<any>-{"\}],
            9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.Except(Symbols.AnyChar, Symbols.Chars(Set('"','\\'))))), Seq(10)),
            // ['\' {"\bnrt}],
            13 -> NGrammar.NSequence(13, Symbols.Sequence(Seq(Symbols.ExactChar('\\'),Symbols.Chars(Set('"','\\','b','n','r','t')))), Seq(14,15))),
        1)

    case class StringLiteral(value:List[StringChar])
    sealed trait StringChar
    case class CharAsIs(c:Node) extends StringChar
    case class CharEscaped(c:Node) extends StringChar
    def sourceTextOf(node: Node): String = node match {
        case TerminalNode(input) => input.toRawString
        case BindNode(_, body) => sourceTextOf(body)
        case JoinNode(body, _) => sourceTextOf(body)
        case seq: SequenceNode => seq.children map sourceTextOf mkString ""
        case _ => throw new Exception("Cyclic bind")
    }

    def matchStringLiteral(node: Node): StringLiteral = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 3 =>
                val v1 = body.asInstanceOf[SequenceNode].children(1)
                val v5 = unrollRepeat0(v1) map { n =>
                    val BindNode(v2, v3) = n
                    assert(v2.id == 8)
                    val v4 = matchStringChar(v3)
                    v4
                }
                val v6 = v5 map { n =>
                    n
                }
                val v7 = StringLiteral(v6)
                v7
        }
    }
    def matchStringChar(node: Node): StringChar = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 9 =>
                val v8 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v9, v10) = v8
                assert(v9.id == 10)
                val v11 = CharAsIs(v10)
                v11
            case 13 =>
                val v12 = body.asInstanceOf[SequenceNode].children(1)
                val v13 = CharEscaped(v12)
                v13
        }
    }
    def matchStart(node: Node): StringLiteral = {
        val BindNode(start, BindNode(startNonterm, body)) = node
        assert(start.id == 1)
        assert(startNonterm.id == 2)
        matchStringLiteral(body)
    }
    private def unrollRepeat0(node: Node): List[Node] = {
        val BindNode(repeat: NGrammar.NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) =>
                assert(symbol.id == repeat.repeatSeq)
                val s = repeating.children(1)
                val r = unrollRepeat0(repeating.children(0))
                r :+ s
            case SequenceNode(symbol, emptySeq) =>
                assert(symbol.id == repeat.baseSeq)
                assert(emptySeq.isEmpty)
                List()
        }
    }
    lazy val naiveParser = new NaiveParser(ngrammar)

    def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
        naiveParser.parse(text)

    def parseAst(text: String): Either[StringLiteral, ParsingErrors.ParsingError] =
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
        println(parseAst("\"abcdefg\""))
    }
}
