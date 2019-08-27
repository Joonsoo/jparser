package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Symbols

sealed trait AstifierExpr {
    def replaceThisNode(node: AstifierExpr): AstifierExpr
}

case object ThisNode extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = node
}

case class Unbinder(expr: AstifierExpr, symbol: Symbols.Symbol) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = Unbinder(expr.replaceThisNode(node), symbol)
}

case class SeqRef(expr: AstifierExpr, idx: Int) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = SeqRef(expr.replaceThisNode(node), idx)
}

case class UnrollMapper(boundType: BoundType.Value, target: AstifierExpr, mapFn: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = UnrollMapper(boundType, target.replaceThisNode(node), mapFn)
}

case class UnrollChoices(choiceSymbols: Map[Symbols.Symbol, AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr =
        UnrollChoices(choiceSymbols.view.mapValues(_.replaceThisNode(node)).toMap)
}

case class CreateObj(className: String, args: List[AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = CreateObj(className, args map (_.replaceThisNode(node)))
}

case class CreateList(elems: List[AstifierExpr]) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = CreateList(elems map (_.replaceThisNode(node)))
}

case class ConcatList(lhs: AstifierExpr, rhs: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = ConcatList(lhs.replaceThisNode(node), rhs.replaceThisNode(node))
}

object BoundType extends Enumeration {
    val Sequence, Choice, Repeat0, Repeat1, Optional, Paren, Longest = Value
}

case class BoundRefs(boundType: BoundType.Value, refs: List[(AstifierExpr, Option[BoundRefs])]) {
    def changeBoundType(newBoundType: BoundType.Value) = BoundRefs(newBoundType, refs)

    def appendRef(astifier: AstifierExpr, boundRef: Option[BoundRefs]): BoundRefs =
        BoundRefs(boundType, refs :+ (astifier, boundRef))
}

object ExpressionGrammar {
    def x(): Unit = {
        (Symbols.Nonterminal("term"), Unbinder(ThisNode, Symbols.Nonterminal("term")))

        0 -> (Symbols.Nonterminal("expression") -> Unbinder(SeqRef(ThisNode, 0), Symbols.Nonterminal("expression")))
        1 -> (Symbols.ExactChar('+') -> Unbinder(SeqRef(ThisNode, 1), Symbols.ExactChar('+')))
        2 -> (Symbols.Nonterminal("term") -> Unbinder(SeqRef(ThisNode, 2), Symbols.Nonterminal("term")))

        CreateObj("BinOp", List(Unbinder(SeqRef(ThisNode, 1), Symbols.ExactChar('+')),
            Unbinder(SeqRef(ThisNode, 0), Symbols.Nonterminal("expression")),
            Unbinder(SeqRef(ThisNode, 2), Symbols.Nonterminal("term"))
        ))

        // array
        0 -> (Symbols.ExactChar('['), SeqRef(ThisNode, 0))
        1 -> (Symbols.Nonterminal("expression"), Unbinder(SeqRef(ThisNode, 1), Symbols.Nonterminal("expression")))
        val seqSymbol = Symbols.Sequence(Seq(Symbols.ExactChar(','), Symbols.Nonterminal("expression")))
        2 -> (seqSymbol, Unbinder(SeqRef(ThisNode, 2), seqSymbol), Map(
            0 -> (Symbols.ExactChar(','), SeqRef(Unbinder(SeqRef(ThisNode, 2), seqSymbol), 0)),
            1 -> (Symbols.Nonterminal("expression"), Unbinder(SeqRef(Unbinder(SeqRef(ThisNode, 2), seqSymbol), 0), Symbols.Nonterminal("expression")))
        ))
    }
}
