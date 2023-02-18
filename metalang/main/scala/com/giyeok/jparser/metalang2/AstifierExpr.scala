package com.giyeok.jparser.metalang2

import com.giyeok.jparser.Symbols

sealed trait AstifierExpr {
    def replaceThisNode(node: AstifierExpr): AstifierExpr
}

case object ThisNode extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): AstifierExpr = node
}

case class Unbind(expr: AstifierExpr, symbol: Symbols.Symbol) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): Unbind = Unbind(expr.replaceThisNode(node), symbol)
}

case class SeqRef(expr: AstifierExpr, idx: Int) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): SeqRef = SeqRef(expr.replaceThisNode(node), idx)
}

case class UnrollRepeat(lower: Int, source: AstifierExpr, eachAstifier: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollRepeat =
        UnrollRepeat(lower: Int, source.replaceThisNode(node), eachAstifier)
}

case class UnrollOptional(source: AstifierExpr, contentAstifier: AstifierExpr, emptySym: Symbols.Symbol, contentSym: Symbols.Symbol) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): UnrollOptional =
        UnrollOptional(source.replaceThisNode(node), contentAstifier, emptySym, contentSym)
}

case class EachMap(target: AstifierExpr, mapFn: AstifierExpr) extends AstifierExpr {
    override def replaceThisNode(node: AstifierExpr): EachMap =
        EachMap(target.replaceThisNode(node), mapFn)
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

    def replaceThisNode(node: AstifierExpr): AstifiedCtx = AstifiedCtx(refs map { r =>
        Astified(r.symbol, r.astifierExpr.replaceThisNode(node), r.insideCtx)
    })
}

// symbol이 ThisNode인 경우, 그 symbol을 astify하는 expression이 astifierExpr.
// BoundExpr에서 이 symbol을 지정하면 그 내부에서 사용 가능한 ref들이 insideCtx.
case class Astified(symbol: Symbols.Symbol, astifierExpr: AstifierExpr, insideCtx: Option[AstifiedCtx])
