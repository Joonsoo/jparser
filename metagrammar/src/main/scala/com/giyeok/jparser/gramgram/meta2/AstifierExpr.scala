package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Symbols

sealed trait AstifierExpr {
    def replaceThisNode(node: AstifierExpr): AstifierExpr
}

case object ThisNode extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = node
}

case class Matcher(expr: AstifierExpr, symbol: Symbols.Symbol) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): Matcher = Matcher(expr.replaceThisNode(node), symbol)
}

case class SeqRef(expr: AstifierExpr, idx: Int) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): SeqRef = SeqRef(expr.replaceThisNode(node), idx)
}

case class UnrollerRepeat(lower: Int, target: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollerRepeat =
        UnrollerRepeat(lower: Int, target.replaceThisNode(node))
}

case class UnrollerOptional(target: AstifierExpr, emptySym: Symbols.Symbol, contentSym: Symbols.Symbol) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollerOptional =
        UnrollerOptional(target.replaceThisNode(node), emptySym, contentSym)
}

case class UnrollMapper(referrer: AstifierExpr, target: AstifierExpr, mapFn: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollMapper =
        UnrollMapper(referrer, target.replaceThisNode(node), mapFn)
}

case class UnrollChoices(choiceSymbols: Map[Symbols.Symbol, AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollChoices =
        UnrollChoices(choiceSymbols.view.mapValues(_.replaceThisNode(node)).toMap)
}

case class CreateObj(className: String, args: List[AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): CreateObj = CreateObj(className, args map (_.replaceThisNode(node)))
}

case class CreateList(elems: List[AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): CreateList = CreateList(elems map (_.replaceThisNode(node)))
}

case class ConcatList(lhs: AstifierExpr, rhs: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): ConcatList = ConcatList(lhs.replaceThisNode(node), rhs.replaceThisNode(node))
}

case class AstifiedCtx(refs: List[Astified]) {
    def :+(ref: Astified): AstifiedCtx = AstifiedCtx(refs :+ ref)
}

// symbol이 ThisNode인 경우, 그 symbol을 astify하는 expression이 astifierExpr.
// BoundExpr에서 이 symbol을 지정하면 그 내부에서 사용 가능한 ref들이 insideCtx.
case class Astified(symbol: Symbols.Symbol, astifierExpr: AstifierExpr, insideCtx: Option[AstifiedCtx])

object ExpressionGrammar {
    def x(): Unit = {
        (Symbols.Nonterminal("term"), Matcher(ThisNode, Symbols.Nonterminal("term")))

        0 -> (Symbols.Nonterminal("expression") -> Matcher(SeqRef(ThisNode, 0), Symbols.Nonterminal("expression")))
        1 -> (Symbols.ExactChar('+') -> Matcher(SeqRef(ThisNode, 1), Symbols.ExactChar('+')))
        2 -> (Symbols.Nonterminal("term") -> Matcher(SeqRef(ThisNode, 2), Symbols.Nonterminal("term")))

        CreateObj("BinOp", List(Matcher(SeqRef(ThisNode, 1), Symbols.ExactChar('+')),
            Matcher(SeqRef(ThisNode, 0), Symbols.Nonterminal("expression")),
            Matcher(SeqRef(ThisNode, 2), Symbols.Nonterminal("term"))
        ))

        // array
        0 -> (Symbols.ExactChar('['), SeqRef(ThisNode, 0))
        1 -> (Symbols.Nonterminal("expression"), Matcher(SeqRef(ThisNode, 1), Symbols.Nonterminal("expression")))
        val seqSymbol = Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))
        2 -> (seqSymbol, Matcher(SeqRef(ThisNode, 2), seqSymbol), Map(
            0 -> (Symbols.ExactChar(','), SeqRef(Matcher(SeqRef(ThisNode, 2), seqSymbol), 0)),
            1 -> (Symbols.Nonterminal("expression"), Matcher(SeqRef(Matcher(SeqRef(ThisNode, 2), seqSymbol), 0), Symbols.Nonterminal("expression")))
        ))
    }
}
