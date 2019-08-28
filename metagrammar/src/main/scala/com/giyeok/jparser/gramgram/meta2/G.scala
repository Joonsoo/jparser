import com.giyeok.jparser.Inputs.InputToShortString
import com.giyeok.jparser.ParseResultTree.{JoinNode, Node, BindNode, TerminalNode, SequenceNode}
import com.giyeok.jparser.nparser.{NGrammar, ParseTreeConstructor, NaiveParser, Parser}
import com.giyeok.jparser.{ParsingErrors, ParseForestFunc, Symbols}

object G {
    val ngrammar = new NGrammar(
        Map(1 -> NGrammar.NStart(1, 2),
            2 -> NGrammar.NNonterminal(2, Symbols.Nonterminal("expression"), Set(3, 29)),
            4 -> NGrammar.NNonterminal(4, Symbols.Nonterminal("term"), Set(5, 27)),
            6 -> NGrammar.NNonterminal(6, Symbols.Nonterminal("factor"), Set(7, 17, 24)),
            8 -> NGrammar.NNonterminal(8, Symbols.Nonterminal("number"), Set(9, 11)),
            10 -> NGrammar.NTerminal(10, Symbols.ExactChar('0')),
            12 -> NGrammar.NTerminal(12, Symbols.Chars(('1' to '9').toSet)),
            13 -> NGrammar.NRepeat(13, Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), 14, 15),
            16 -> NGrammar.NTerminal(16, Symbols.Chars(('0' to '9').toSet)),
            18 -> NGrammar.NNonterminal(18, Symbols.Nonterminal("variable"), Set(19)),
            20 -> NGrammar.NLongest(20, Symbols.Longest(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)), 21),
            21 -> NGrammar.NRepeat(21, Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), 22, 23),
            22 -> NGrammar.NTerminal(22, Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet)),
            25 -> NGrammar.NTerminal(25, Symbols.ExactChar('(')),
            26 -> NGrammar.NTerminal(26, Symbols.ExactChar(')')),
            28 -> NGrammar.NTerminal(28, Symbols.ExactChar('*')),
            30 -> NGrammar.NTerminal(30, Symbols.ExactChar('+'))),
        Map(3 -> NGrammar.NSequence(3, Symbols.Sequence(Seq(Symbols.Nonterminal("term"))), Seq(4)),
            5 -> NGrammar.NSequence(5, Symbols.Sequence(Seq(Symbols.Nonterminal("factor"))), Seq(6)),
            7 -> NGrammar.NSequence(7, Symbols.Sequence(Seq(Symbols.Nonterminal("number"))), Seq(8)),
            9 -> NGrammar.NSequence(9, Symbols.Sequence(Seq(Symbols.ExactChar('0'))), Seq(10)),
            11 -> NGrammar.NSequence(11, Symbols.Sequence(Seq(Symbols.Chars(('1' to '9').toSet), Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0))), Seq(12, 13)),
            14 -> NGrammar.NSequence(14, Symbols.Sequence(Seq()), Seq()),
            15 -> NGrammar.NSequence(15, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('0' to '9').toSet), 0), Symbols.Chars(('0' to '9').toSet))), Seq(13, 16)),
            17 -> NGrammar.NSequence(17, Symbols.Sequence(Seq(Symbols.Nonterminal("variable"))), Seq(18)),
            19 -> NGrammar.NSequence(19, Symbols.Sequence(Seq(Symbols.Longest(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1)))), Seq(20)),
            23 -> NGrammar.NSequence(23, Symbols.Sequence(Seq(Symbols.Repeat(Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet), 1), Symbols.Chars(('A' to 'Z').toSet ++ ('a' to 'z').toSet))), Seq(21, 22)),
            24 -> NGrammar.NSequence(24, Symbols.Sequence(Seq(Symbols.ExactChar('('), Symbols.Nonterminal("expression"), Symbols.ExactChar(')'))), Seq(25, 2, 26)),
            27 -> NGrammar.NSequence(27, Symbols.Sequence(Seq(Symbols.Nonterminal("term"), Symbols.ExactChar('*'), Symbols.Nonterminal("factor"))), Seq(4, 28, 6)),
            29 -> NGrammar.NSequence(29, Symbols.Sequence(Seq(Symbols.Nonterminal("expression"), Symbols.ExactChar('+'), Symbols.Nonterminal("term"))), Seq(2, 30, 4))),
        1)

    sealed trait Expression

    case class BinOp(op: Node, lhs: Expression, rhs: Term) extends Term {
        override def toString: String = s"{$lhs ${sourceTextOf(op)} $rhs}"
    }

    sealed trait Term extends Expression

    sealed trait Factor extends Term

    case class Paren(expr: Expression) extends Factor {
        override def toString: String = s"($expr)"
    }

    sealed trait Number extends Factor

    case class Integer(value: List[Node]) extends Number {
        override def toString: String = (value map sourceTextOf).mkString
    }

    case class Variable(name: Node) extends Factor {
        override def toString: String = sourceTextOf(name)
    }

    def sourceTextOf(node: Node): String = node match {
        case TerminalNode(input) => input.toRawString
        case BindNode(_, body) => sourceTextOf(body)
        case JoinNode(body, _) => sourceTextOf(body)
        case seq: SequenceNode => seq.children map sourceTextOf mkString ""
        case _ => throw new Exception("Cyclic bind")
    }

    def matchExpression(node: Node): Expression = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 3 =>
                val v1 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v2, v3) = v1
                assert(v2.id == 4)
                val v4 = matchTerm(v3)
                v4
            case 29 =>
                val v5 = body.asInstanceOf[SequenceNode].children(1)
                val v6 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v7, v8) = v6
                assert(v7.id == 2)
                val v9 = matchExpression(v8)
                val v10 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v11, v12) = v10
                assert(v11.id == 4)
                val v13 = matchTerm(v12)
                val v14 = BinOp(v5, v9, v13)
                v14
        }
    }

    def matchTerm(node: Node): Term = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 5 =>
                val v15 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v16, v17) = v15
                assert(v16.id == 6)
                val v18 = matchFactor(v17)
                v18
            case 27 =>
                val v19 = body.asInstanceOf[SequenceNode].children(1)
                val v20 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v21, v22) = v20
                assert(v21.id == 4)
                val v23 = matchTerm(v22)
                val v24 = body.asInstanceOf[SequenceNode].children(2)
                val BindNode(v25, v26) = v24
                assert(v25.id == 6)
                val v27 = matchFactor(v26)
                val v28 = BinOp(v19, v23, v27)
                v28
        }
    }

    def matchFactor(node: Node): Factor = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 7 =>
                val v29 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v30, v31) = v29
                assert(v30.id == 8)
                val v32 = matchNumber(v31)
                v32
            case 17 =>
                val v33 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v34, v35) = v33
                assert(v34.id == 18)
                val v36 = matchVariable(v35)
                v36
            case 24 =>
                val v37 = body.asInstanceOf[SequenceNode].children(1)
                val BindNode(v38, v39) = v37
                assert(v38.id == 2)
                val v40 = matchExpression(v39)
                val v41 = Paren(v40)
                v41
        }
    }

    def matchNumber(node: Node): Number = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 9 =>
                val v42 = body.asInstanceOf[SequenceNode].children(0)
                val v43 = List(v42)
                val v44 = Integer(v43)
                v44
            case 11 =>
                val v45 = body.asInstanceOf[SequenceNode].children(0)
                val v46 = body.asInstanceOf[SequenceNode].children(1)
                val v47 = List(v45, v46)
                val v48 = Integer(v47)
                v48
        }
    }

    def matchVariable(node: Node): Variable = {
        val BindNode(symbol, body) = node
        symbol.id match {
            case 19 =>
                val v49 = body.asInstanceOf[SequenceNode].children(0)
                val BindNode(v50, v51) = v49
                assert(v50.id == 20)
                val v52 = Variable(v51)
                v52
        }
    }

    def matchStart(node: Node): Expression = {
        val BindNode(start, BindNode(startNonterm, body)) = node
        assert(start.id == 1)
        assert(startNonterm.id == 2)
        matchExpression(body)
    }

    lazy val naiveParser = new NaiveParser(ngrammar)

    def parse(text: String): Either[Parser.NaiveContext, ParsingErrors.ParsingError] =
        naiveParser.parse(text)

    def parseAst(text: String): Either[Expression, ParsingErrors.ParsingError] =
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
        println(parseAst("12*(34+abc)").swap.getOrElse())
        println(parseAst("12*(34+abc)+0").swap.getOrElse())
    }
}
