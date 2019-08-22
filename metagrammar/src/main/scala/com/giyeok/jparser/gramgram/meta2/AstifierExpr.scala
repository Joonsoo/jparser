package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.nparser.NGrammar.{NNonterminal, NSequence, NSymbol, NTerminal}

sealed trait AstifierExpr

case object ThisNode extends AstifierExpr

case class Unbinder(expr: AstifierExpr, symbol: NSymbol) extends AstifierExpr

case class SeqRef(expr: AstifierExpr, symbol: NSequence, idx: Int) extends AstifierExpr

case class OptionalUnroller(expr: AstifierExpr) extends AstifierExpr

case class Repeat0Unroller(expr: AstifierExpr) extends AstifierExpr

case class Repeat1Unroller(expr: AstifierExpr) extends AstifierExpr

case class MatchTo(expr: AstifierExpr, targetType: TypeSpec) extends AstifierExpr

case class SymbolSeqBound(ctx: AstifierExpr, foreach: AstifierExpr) extends AstifierExpr

case class ProcessorBound(ctx: AstifierExpr, foreach: AstifierExpr) extends AstifierExpr

case class CreateObj(className: String, args: List[AstifierExpr]) extends AstifierExpr

case class CreateList(elems: List[AstifierExpr]) extends AstifierExpr

case class ConcatList(lhs: AstifierExpr, rhs: AstifierExpr) extends AstifierExpr

object ExpressionGrammar {
    def x(): Unit = {
        val exprRhs2 = NSequence(2, Symbols.Sequence(Seq()), Seq())
        // Code generator에서 이걸 보고 공통된 부분(e.g. Unbinder(ThisNode, exprRhs2))을 별도로 빼면 됨

        val matchExpression = Map[NSymbol, AstifierExpr](
            NNonterminal(1, Symbols.Nonterminal("term"), Set()) -> MatchTo(ThisNode, ClassType("Term")),
            exprRhs2 -> CreateObj("BinOp", List(
                SeqRef(Unbinder(ThisNode, exprRhs2), exprRhs2, 1),
                MatchTo(SeqRef(Unbinder(ThisNode, exprRhs2), exprRhs2, 0), ClassType("Expression")),
                MatchTo(SeqRef(Unbinder(ThisNode, exprRhs2), exprRhs2, 2), ClassType("Term"))
            ))
        )

        val matchTerm = Map[NSymbol, AstifierExpr](
            NNonterminal(10, Symbols.Nonterminal("factor"), Set()) -> MatchTo(ThisNode, ClassType("Factor"))
        )
    }
}
