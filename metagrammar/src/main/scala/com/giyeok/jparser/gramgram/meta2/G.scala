import com.giyeok.jparser.ParseResultTree.{BindNode, Node, SequenceNode}
import com.giyeok.jparser.nparser.NGrammar.NRepeat
import com.giyeok.jparser.nparser.{NGrammar, NaiveParser, ParseTreeConstructor, Parser}
import com.giyeok.jparser.{ParseForestFunc, ParseResultTree, ParsingErrors, Symbols}

object G {
    val ngrammar = new NGrammar(
        Map(5 -> NGrammar.NNonterminal(5, Symbols.Nonterminal("expression"), Set(6)),
            14 -> NGrammar.NTerminal(14, Symbols.ExactChar(']')),
            1 -> NGrammar.NStart(1, 2),
            13 -> NGrammar.NTerminal(13, Symbols.ExactChar(',')),
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("array"), Set(3)),
            7 -> NGrammar.NTerminal(7, Symbols.Chars(Set('0', 'a') ++ ('x' to 'z').toSet)),
            11 -> NGrammar.NProxy(11, Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))), 12),
            8 -> NGrammar.NRepeat(8, Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))), 0), 9, 10),
            4 -> NGrammar.NTerminal(4, Symbols.ExactChar('['))),
        Map(10 -> NGrammar.NSequence(10, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))), 0), Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))))), Seq(8, 11)),
            6 -> NGrammar.NSequence(6, Symbols.Sequence(Seq(Symbols.Chars(Set('0', 'a') ++ ('x' to 'z').toSet))), Seq(7)),
            9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq()), Seq()),
            12 -> NGrammar.NSequence(12, Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression"))), Seq(13, 5)),
            3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.ExactChar('['), Symbols.Nonterminal("expression"), Symbols.Repeat(Symbols.Proxy(Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))), 0), Symbols.ExactChar(']'))), Seq(4, 5, 8, 14))),
        1)

    case class Array(elems: List[Expression]) {
        override def toString: String = s"Array(${
            elems map { e => sourceTextOf(e.name) } mkString ","
        })"
    }

    case class Expression(name: Node)

    private def sourceTextOf(node: ParseResultTree.Node): String = node match {
        case ParseResultTree.TerminalNode(input) => input.toRawString
        case ParseResultTree.BindNode(_, body) => sourceTextOf(body)
        case ParseResultTree.JoinNode(body, _) => sourceTextOf(body)
        case seq: SequenceNode => seq.children map sourceTextOf mkString ""
        case _ => throw new Exception("Cyclic bind")
    }

    private def unrollRepeat0(node: Node): List[Node] = {
        val BindNode(repeat: NRepeat, body) = node
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

    def matchArray(node: Node): Array = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 3 =>
                val v1 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2, v3) = v1
                assert(v2.id == 5)
                val v4 = matchExpression(v3)
                val v5 = List(v4)
                val v6 = body.asInstanceOf[SequenceNode].children(2)
                val v19 = unrollRepeat0(v6) map { n =>
                    // val BindNode(v7, v8) = n
                    // assert(v7.id == 8)
                    val BindNode(v9, v10) = n
                    assert(v9.id == 11)
                    val BindNode(v11, v12) = v10
                    assert(v11.id == 12)
                    // val BindNode(v13, v14) = v12
                    // assert(v13.id == 12)
                    val v15 = v12.asInstanceOf[SequenceNode].children(1)
                    val BindNode(v16, v17) = v15
                    assert(v16.id == 5)
                    val v18 = matchExpression(v17)
                    v18
                }
                val v20 = v5 ++ v19
                val v21 = Array(v20)
                v21
        }
    }

    def matchExpression(node: Node): Expression = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 6 =>
                val v22 = body.asInstanceOf[SequenceNode].children(0)
                val v23 = Expression(v22)
                v23
        }
    }

    def matchStart(node: Node): Array = {
        val BindNode(start, BindNode(startNonterm, body)) = node
        assert(start.id == 1)
        assert(startNonterm.id == 2)
        matchArray(body)
    }

    lazy val naiveParser = new NaiveParser(ngrammar)

    def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
        naiveParser.parse(text)

    def parseAst(text: String): Either[Array, ParsingErrors.ParsingError] =
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

    def main(args: scala.Array[String]): Unit = {
        println(parseAst("[a,a,x,x,a]"))
    }
}
