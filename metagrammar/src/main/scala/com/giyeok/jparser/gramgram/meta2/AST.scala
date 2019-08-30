package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.{Inputs, ParseResultTree}
import com.giyeok.jparser.ParseResultTree.{BindNode, CyclicBindNode, CyclicSequenceNode, JoinNode, Node, SequenceNode, TerminalNode}
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.nparser.NGrammar.{NNonterminal, NRepeat, NSequence, NStart}

sealed trait AST

object AST {

    class Node(val node: ParseResultTree.Node) {
        override def toString: String = {
            def rec(node: ParseResultTree.Node): String = node match {
                case TerminalNode(_, input) => input.toRawString
                case BindNode(_, body) => rec(body)
                case JoinNode(body, _) => rec(body)
                case seq: SequenceNode => seq.children map rec mkString ""
                case _: CyclicBindNode | _: CyclicSequenceNode =>
                    throw new Exception("Cyclic bind")
            }

            rec(node)
        }

        def toRepr: String = super.toString
    }

    sealed trait AstNode {
        val nodeId: Int
    }

    case class Grammar(nodeId: Int, defs: List[Def]) extends AstNode

    sealed trait Def extends AstNode

    sealed trait TypeDef extends Def

    case class ClassDef(nodeId: Int, typeName: TypeName, params: List[ClassParam]) extends TypeDef

    case class SuperDef(nodeId: Int, typeName: TypeName, subs: List[SubType]) extends TypeDef

    case class TypeName(nodeId: Int, name: Node) extends ValueTypeDesc with AstNode

    case class ClassParam(nodeId: Int, name: ParamName, typeDesc: Option[Node]) extends AstNode

    case class ParamName(nodeId: Int, name: Node) extends AstNode

    case class TypeDesc(nodeId: Int, typ: ValueTypeDesc, optional: Boolean) extends AstNode

    case class ArrayTypeDesc(nodeId: Int, elemType: TypeDesc) extends ValueTypeDesc with AstNode

    sealed trait ValueTypeDesc extends AstNode

    sealed trait SubType extends AstNode

    case class OnTheFlyTypeDef(nodeId: Int, name: TypeName, supers: List[TypeName]) extends ValueTypeDesc with AstNode

    case class Rule(nodeId: Int, lhs: LHS, rhs: List[RHS]) extends Def with AstNode

    case class LHS(nodeId: Int, name: Nonterminal, typeDesc: Option[TypeDesc]) extends AstNode

    case class RHS(nodeId: Int, elems: List[Elem]) extends AstNode

    sealed trait Elem extends AstNode

    sealed trait Processor extends Elem with AstNode

    sealed trait PExpr extends Processor with BoundedPExpr with AstNode

    case class BinOpExpr(nodeId: Int, op: Node, lhs: PExpr, rhs: PTerm) extends PExpr with AstNode

    sealed trait PTerm extends PExpr with AstNode

    case class Ref(nodeId: Int, idx: Node) extends Processor with PTerm with BoundedPExpr with AstNode

    case class BoundPExpr(nodeId: Int, ctx: Ref, expr: BoundedPExpr) extends PTerm with BoundedPExpr with AstNode

    sealed trait BoundedPExpr extends AstNode with Processor

    sealed trait AbstractConstructExpr extends PTerm with AstNode

    case class ConstructExpr(nodeId: Int, typ: TypeName, params: List[PExpr]) extends PTerm with AbstractConstructExpr with AstNode

    case class PTermParen(nodeId: Int, expr: PExpr) extends PTerm with AstNode

    case class PTermSeq(nodeId: Int, elems: List[PExpr]) extends PTerm with AstNode

    case class OnTheFlyTypeDefConstructExpr(nodeId: Int, typeDef: OnTheFlyTypeDef, params: List[NamedParam]) extends BoundedPExpr with AbstractConstructExpr with AstNode

    case class NamedParam(nodeId: Int, name: ParamName, typeDesc: Option[TypeDesc], expr: PExpr) extends AstNode

    sealed trait Symbol extends Elem with AstNode

    sealed trait BinSymbol extends Symbol with AstNode

    case class JoinSymbol(nodeId: Int, symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol with AstNode

    case class ExceptSymbol(nodeId: Int, symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol with AstNode

    sealed trait PreUnSymbol extends BinSymbol with AstNode

    case class FollowedBy(nodeId: Int, expr: PreUnSymbol) extends PreUnSymbol with AstNode

    case class NotFollowedBy(nodeId: Int, expr: PreUnSymbol) extends PreUnSymbol with AstNode

    sealed trait PostUnSymbol extends PreUnSymbol with AstNode

    case class Repeat(nodeId: Int, repeatingSymbol: PostUnSymbol, repeatSpec: Node) extends PostUnSymbol with AstNode

    sealed trait AtomSymbol extends PostUnSymbol with AstNode

    case class Paren(nodeId: Int, choices: InPlaceChoices) extends AtomSymbol with AstNode

    case class Longest(nodeId: Int, choices: InPlaceChoices) extends AtomSymbol with AstNode

    case class EmptySeq(nodeId: Int) extends AtomSymbol with AstNode

    case class InPlaceChoices(nodeId: Int, choices: List[InPlaceSequence]) extends AtomSymbol with AstNode

    case class InPlaceSequence(nodeId: Int, seq: List[Symbol]) extends AtomSymbol with AstNode

    case class Nonterminal(nodeId: Int, name: Node) extends AtomSymbol with AstNode

    sealed trait Terminal extends AtomSymbol with AstNode

    case class TerminalChar(nodeId: Int, char: Node) extends Terminal with AstNode

    case class AnyTerminal(nodeId: Int) extends Terminal with AstNode

    case class TerminalChoice(nodeId: Int, choices: List[TerminalChoiceElem]) extends AtomSymbol with AstNode

    sealed trait TerminalChoiceElem extends AstNode

    case class TerminalChoiceChar(nodeId: Int, char: Node) extends TerminalChoiceElem with AstNode

    case class TerminalChoiceRange(nodeId: Int, start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem with AstNode

    case class StringLiteral(nodeId: Int, value: Node) extends AtomSymbol with AstNode

}

object ASTifier {
    private var nextId = 0

    private def newId() = {
        nextId += 1
        nextId
    }

    def transformNode(node: Node): AST.Node = new AST.Node(node)

    def unwindRepeat(node: Node): List[Node] = {
        val BindNode(repeat: NRepeat, body) = node
        body match {
            case BindNode(symbol, repeating: SequenceNode) if symbol.id == repeat.repeatSeq =>
                unwindRepeat(repeating.children(0)) :+ repeating.children(1)
            case BindNode(symbol, _) if symbol.id == repeat.baseSeq =>
                List(body)
            case seq@SequenceNode(_, _, symbol, _) if symbol.id == repeat.baseSeq =>
                seq.children.take(repeat.symbol.lower).toList
        }
    }

    def unwindOptional(node: Node): Option[SequenceNode] = {
        val BindNode(_, body) = node
        body match {
            case BindNode(_, BindNode(_, seq: SequenceNode)) =>
                if (seq.children.isEmpty) None else Some(seq)
            case BindNode(_, seq: SequenceNode) =>
                if (seq.children.isEmpty) None else Some(seq)
        }
    }

    def unwindNonterm(name: String, node: Node): Node = {
        val BindNode(NNonterminal(_, Nonterminal(`name`), _), body) = node
        body
    }

    def matchNonterminal(node: Node): AST.Nonterminal = {
        val BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) = node
        AST.Nonterminal(newId(), transformNode(nonterminalBody))
    }

    def matchTypeName(node: Node): AST.TypeName = {
        val BindNode(NNonterminal(_, Nonterminal("Id"), _), idBody) = node
        AST.TypeName(newId(), transformNode(idBody))
    }

    def matchParamName(node: Node): AST.ParamName = {
        val BindNode(NNonterminal(_, Nonterminal("Id"), _), idBody) = node
        AST.ParamName(newId(), transformNode(idBody))
    }

    def matchOnTheFlySuperTypes(node: Node): List[AST.TypeName] = {
        val BindNode(_, seq: SequenceNode) = node
        val super1 = matchTypeName(unwindNonterm("TypeName", seq.children(2)))
        val super2_ = unwindRepeat(seq.children(3))
        val super2 = super2_ map { b =>
            val BindNode(_, BindNode(_, seq1: SequenceNode)) = b
            matchTypeName(unwindNonterm("TypeName", seq1.children(3)))
        }
        super1 +: super2
    }

    def matchOnTheFlyTypeDef(node: Node): AST.OnTheFlyTypeDef = {
        val BindNode(_, seq: SequenceNode) = node
        val name = matchTypeName(unwindNonterm("TypeName", seq.children(2)))
        val supers: List[AST.TypeName] = (unwindOptional(seq.children(3)) map { seq =>
            matchOnTheFlySuperTypes(unwindNonterm("OnTheFlySuperTypes", seq.children(1)))
        }).getOrElse(List())
        AST.OnTheFlyTypeDef(newId(), name, supers)
    }

    def matchValueTypeDesc(node: Node): AST.ValueTypeDesc = node match {
        case BindNode(NNonterminal(_, Nonterminal("TypeName"), _), typeNameBody) =>
            matchTypeName(typeNameBody)
        case BindNode(NNonterminal(_, Nonterminal("OnTheFlyTypeDef"), _), ontheflyBody) =>
            matchOnTheFlyTypeDef(ontheflyBody)
        case BindNode(_, seq: SequenceNode) =>
            AST.ArrayTypeDesc(newId(), matchTypeDesc(unwindNonterm("TypeDesc", seq.children(2))))
    }

    def matchTypeDesc(node: Node): AST.TypeDesc = {
        val BindNode(_, seq: SequenceNode) = node
        val valueTypeDesc = matchValueTypeDesc(unwindNonterm("ValueTypeDesc", seq.children(0)))
        val optional = seq.children(1) match {
            case BindNode(_, BindNode(_, seq: SequenceNode)) =>
                seq.children.nonEmpty
            case BindNode(_, BindNode(_, BindNode(_, seq: SequenceNode))) =>
                seq.children.nonEmpty
        }
        AST.TypeDesc(newId(), valueTypeDesc, optional)
    }

    def matchLHS(node: Node): AST.LHS = {
        val BindNode(NNonterminal(_, Nonterminal("LHS"), _), BindNode(_, lhsBody: SequenceNode)) = node

        val name = matchNonterminal(lhsBody.children(0))
        val typeDesc_ = unwindOptional(lhsBody.children(1))
        val typeDesc = typeDesc_ map { opt =>
            matchTypeDesc(unwindNonterm("TypeDesc", opt.children(3)))
        }
        AST.LHS(newId(), name, typeDesc)
    }

    def matchTerminal(node: Node): AST.Terminal = node match {
        case BindNode(_, SequenceNode(_, _, _, List(_, BindNode(NNonterminal(_, Nonterminal("TerminalChar"), _), terminalCharBody), _))) =>
            AST.TerminalChar(newId(), transformNode(terminalCharBody))
        case _ => AST.AnyTerminal(newId())
    }

    def matchTerminalChoiceChar(node: Node): AST.TerminalChoiceChar =
        AST.TerminalChoiceChar(newId(), transformNode(node))

    def matchTerminalChoiceRange(node: Node): AST.TerminalChoiceRange = node match {
        case BindNode(_, seq: SequenceNode) =>
            AST.TerminalChoiceRange(newId(), matchTerminalChoiceChar(seq.children(0)), matchTerminalChoiceChar(seq.children(2)))
    }

    def matchTerminalChoiceElem(node: Node): AST.TerminalChoiceElem = node match {
        case BindNode(NNonterminal(_, Nonterminal("TerminalChoiceChar"), _), charBody) =>
            matchTerminalChoiceChar(charBody)
        case BindNode(NNonterminal(_, Nonterminal("TerminalChoiceRange"), _), rangeBody) =>
            matchTerminalChoiceRange(rangeBody)
    }

    def matchTerminalChoice(node: Node): AST.TerminalChoice = node match {
        case BindNode(_, seq: SequenceNode) if seq.children.size == 4 =>
            val elem1 = matchTerminalChoiceElem(unwindNonterm("TerminalChoiceElem", seq.children(1)))
            val elem2_ = unwindRepeat(seq.children(2))
            val elem2 = elem2_ map { b => matchTerminalChoiceElem(unwindNonterm("TerminalChoiceElem", b)) }
            AST.TerminalChoice(newId(), elem1 +: elem2)
        case BindNode(_, seq: SequenceNode) if seq.children.size == 3 =>
            val elem = matchTerminalChoiceElem(seq.children(1))
            AST.TerminalChoice(newId(), List(elem))
    }

    def matchLongest(node: Node): AST.Longest = {
        val BindNode(_, SequenceNode(_, _, _, List(_, body, _))) = node
        AST.Longest(newId(), matchInPlaceChoices(body))
    }

    def matchInPlaceSequence(node: Node): AST.InPlaceSequence = {
        val BindNode(_, BindNode(_, seq: SequenceNode)) = node

        val choices1 = matchSymbol(unwindNonterm("Symbol", seq.children(0)))
        val choices2_ = unwindRepeat(seq.children(1))
        val choices2 = choices2_ map { b =>
            val BindNode(_, BindNode(_, seq: SequenceNode)) = b
            matchSymbol(unwindNonterm("Symbol", seq.children(1)))
        }
        AST.InPlaceSequence(newId(), choices1 +: choices2)
    }

    def matchInPlaceChoices(node: Node): AST.InPlaceChoices = {
        val BindNode(_, BindNode(_, seq: SequenceNode)) = node

        val choices1 = matchInPlaceSequence(seq.children(0))
        val choices2_ = unwindRepeat(seq.children(1))
        val choices2 = choices2_ map { b =>
            val BindNode(_, BindNode(_, seq: SequenceNode)) = b
            matchInPlaceSequence(seq.children(3))
        }
        AST.InPlaceChoices(newId(), choices1 +: choices2)
    }

    def matchAtomSymbol(node: Node): AST.AtomSymbol = node match {
        case BindNode(NNonterminal(_, Nonterminal("Terminal"), _), terminalBody) =>
            matchTerminal(terminalBody)
        case BindNode(NNonterminal(_, Nonterminal("TerminalChoice"), _), terminalChoiceBody) =>
            matchTerminalChoice(terminalChoiceBody)
        case BindNode(NNonterminal(_, Nonterminal("StringLiteral"), _), BindNode(_, stringBody: SequenceNode)) =>
            AST.StringLiteral(newId(), transformNode(stringBody.children(1)))
        case BindNode(NNonterminal(_, Nonterminal("Nonterminal"), _), nonterminalBody) =>
            AST.Nonterminal(newId(), transformNode(nonterminalBody))
        case BindNode(NNonterminal(_, Nonterminal("Longest"), _), longestBody) =>
            matchLongest(longestBody)
        case BindNode(NNonterminal(_, Nonterminal("EmptySequence"), _), emptySeqBody) =>
            AST.EmptySeq(newId())
        case BindNode(_, SequenceNode(_, _, _, List(_, parenElem, _))) =>
            matchInPlaceChoices(parenElem)
    }

    def matchPostUnSymbol(node: Node): AST.PostUnSymbol = node match {
        case BindNode(NNonterminal(_, Nonterminal("AtomSymbol"), _), atomSymbolBody) =>
            matchAtomSymbol(atomSymbolBody)
        case BindNode(_, seq: SequenceNode) =>
            val sym = matchPostUnSymbol(unwindNonterm("PostUnSymbol", seq.children(0)))
            val op = seq.children(2)
            AST.Repeat(newId(), sym, transformNode(op))
    }

    def matchPreUnSymbol(node: Node): AST.PreUnSymbol = node match {
        case BindNode(NNonterminal(_, Nonterminal("PostUnSymbol"), _), postUnSymbolBody) =>
            matchPostUnSymbol(postUnSymbolBody)
        case BindNode(_, seq: SequenceNode) =>
            val op = seq.children(0)
            val sym = matchPreUnSymbol(unwindNonterm("PreUnSymbol", seq.children(2)))
            op match {
                case BindNode(_, TerminalNode(_, Inputs.Character('^'))) => AST.FollowedBy(newId(), sym)
                case BindNode(_, TerminalNode(_, Inputs.Character('!'))) => AST.NotFollowedBy(newId(), sym)
            }
    }

    def matchBinSymbol(node: Node): AST.BinSymbol = node match {
        case BindNode(NNonterminal(_, Nonterminal("PreUnSymbol"), _), preUnSymbolBody) =>
            matchPreUnSymbol(preUnSymbolBody)
        case BindNode(_, seq: SequenceNode) =>
            val op = seq.children(2)
            val lhs = matchBinSymbol(unwindNonterm("BinSymbol", seq.children(0)))
            val rhs = matchPreUnSymbol(unwindNonterm("PreUnSymbol", seq.children(4)))
            op match {
                case BindNode(_, TerminalNode(_, Inputs.Character('&'))) => AST.JoinSymbol(newId(), lhs, rhs)
                case BindNode(_, TerminalNode(_, Inputs.Character('-'))) => AST.ExceptSymbol(newId(), lhs, rhs)
            }
    }

    def matchSymbol(node: Node): AST.Symbol = node match {
        case BindNode(NNonterminal(_, Nonterminal("BinSymbol"), _), binSymbolBody) =>
            matchBinSymbol(binSymbolBody)
    }

    def matchRef(node: Node): AST.Ref = {
        val BindNode(_, seq: SequenceNode) = node
        AST.Ref(newId(), transformNode(seq.children(1)))
    }

    def matchProcessor(node: Node): AST.Processor = node match {
        case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
            matchRef(refBody)
        case BindNode(_, seq: SequenceNode) =>
            matchPExpr(unwindNonterm("PExpr", seq.children(2)))
    }

    def matchPExpr(node: Node): AST.PExpr = node match {
        case BindNode(NNonterminal(_, Nonterminal("PTerm"), _), ptermBody) =>
            matchPTerm(ptermBody)
        case BindNode(_, seq: SequenceNode) =>
            val op = seq.children(2)
            val lhs = matchPExpr(unwindNonterm("PExpr", seq.children(0)))
            val rhs = matchPTerm(unwindNonterm("PTerm", seq.children(4)))
            AST.BinOpExpr(newId(), transformNode(op), lhs, rhs)
    }

    def matchPTerm(node: Node): AST.PTerm = node match {
        case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
            matchRef(refBody)
        case BindNode(NNonterminal(_, Nonterminal("BoundPExpr"), _), boundPExprBody) =>
            matchBoundPExpr(boundPExprBody)
        case BindNode(NNonterminal(_, Nonterminal("ConstructExpr"), _), constructBody) =>
            matchConstructExpr(constructBody)
        case BindNode(NNonterminal(_, Nonterminal("ArrayTerm"), _), arrayBody) =>
            matchArrayTerm(arrayBody)
        case BindNode(_, seq: SequenceNode) =>
            AST.PTermParen(newId(), matchPExpr(unwindNonterm("PExpr", seq.children(2))))
    }

    def matchBoundPExpr(node: Node): AST.BoundPExpr = node match {
        case BindNode(_, seq: SequenceNode) =>
            val ctx = matchRef(unwindNonterm("Ref", seq.children(0)))
            val expr = matchBoundedPExpr(unwindNonterm("BoundedPExpr", seq.children(1)))
            AST.BoundPExpr(newId(), ctx, expr)
    }

    def matchBoundedPExpr(node: Node): AST.BoundedPExpr = node match {
        case BindNode(NNonterminal(_, Nonterminal("Ref"), _), refBody) =>
            matchRef(refBody)
        case BindNode(NNonterminal(_, Nonterminal("BoundPExpr"), _), boundPExprBody) =>
            matchBoundPExpr(boundPExprBody)
        case BindNode(_, seq: SequenceNode) =>
            matchPExpr(unwindNonterm("PExpr", seq.children(2)))
    }

    def matchConstructExpr(node: Node): AST.AbstractConstructExpr = node match {
        case BindNode(NNonterminal(_, Nonterminal("OnTheFlyTypeDefConstructExpr"), _), ontheflyBody) =>
            matchOnTheFlyTypeDefConstructExpr(ontheflyBody)
        case BindNode(_, seq: SequenceNode) =>
            val typename = matchTypeName(unwindNonterm("TypeName", seq.children(0)))
            val params = matchConstructParams(unwindNonterm("ConstructParams", seq.children(2)))
            AST.ConstructExpr(newId(), typename, params)
    }

    def matchConstructParams(node: Node): List[AST.PExpr] = {
        val BindNode(_, seq: SequenceNode) = node
        val params_ = unwindOptional(seq.children(2))
        (params_ map { p =>
            val param1 = matchPExpr(unwindNonterm("PExpr", p.children(0)))
            val param2_ = unwindRepeat(p.children(1))
            val param2 = param2_ map { b =>
                val BindNode(_, BindNode(_, s: SequenceNode)) = b
                matchPExpr(unwindNonterm("PExpr", s.children(3)))
            }
            param1 +: param2
        }).getOrElse(List())
    }

    def matchOnTheFlyTypeDefConstructExpr(node: Node): AST.OnTheFlyTypeDefConstructExpr = {
        val BindNode(_, seq: SequenceNode) = node
        val onTheFlyTypeDef = matchOnTheFlyTypeDef(unwindNonterm("OnTheFlyTypeDef", seq.children(0)))
        val params = matchNamedParams(unwindNonterm("NamedParams", seq.children(2)))
        AST.OnTheFlyTypeDefConstructExpr(newId(), onTheFlyTypeDef, params)
    }

    def matchNamedParams(node: Node): List[AST.NamedParam] = {
        val BindNode(_, seq1: SequenceNode) = node

        (unwindOptional(seq1.children(2)) map { seq =>
            val param1 = matchNamedParam(unwindNonterm("NamedParam", seq.children(0)))
            val param2 = unwindRepeat(seq.children(1)) map { b =>
                val BindNode(_, BindNode(_, s: SequenceNode)) = b
                matchNamedParam(unwindNonterm("NamedParam", s.children(3)))
            }
            param1 +: param2
        }).getOrElse(List())
    }

    def matchNamedParam(node: Node): AST.NamedParam = {
        val BindNode(_, seq: SequenceNode) = node

        val name = matchParamName(unwindNonterm("ParamName", seq.children(0)))
        val typeDesc = unwindOptional(seq.children(1)) map { seq =>
            matchTypeDesc(unwindNonterm("TypeDesc", seq.children(3)))
        }
        val expr = matchPExpr(unwindNonterm("PExpr", seq.children(5)))
        AST.NamedParam(newId(), name, typeDesc, expr)
    }

    def matchArrayTerm(node: Node): AST.PTermSeq = {
        val BindNode(_, seq: SequenceNode) = node

        val elems_ = unwindOptional(seq.children(2))
        val elems = (elems_ map { elems =>
            val elem1 = matchPExpr(unwindNonterm("PExpr", elems.children(0)))
            val elem2_ = unwindRepeat(elems.children(1))
            val elem2 = elem2_ map { tails_ =>
                val BindNode(_, BindNode(_, tails: SequenceNode)) = tails_
                matchPExpr(unwindNonterm("PExpr", tails.children(3)))
            }
            elem1 +: elem2
        }).getOrElse(List())
        AST.PTermSeq(newId(), elems)
    }

    def matchElem(node: Node): AST.Elem = {
        val BindNode(_, body) = node
        body match {
            case BindNode(NNonterminal(_, Nonterminal("Symbol"), _), symbolBody) =>
                matchSymbol(symbolBody)
            case BindNode(NNonterminal(_, Nonterminal("Processor"), _), processorBody) =>
                matchProcessor(processorBody)
        }
    }

    def matchRHS(node: Node): AST.RHS = {
        val BindNode(_, BindNode(_, rhsBody: SequenceNode)) = node
        val elem1 = matchElem(rhsBody.children(0))
        val elem2_ = unwindRepeat(rhsBody.children(1))
        val elem2 = elem2_ map { repeat =>
            val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
            matchElem(repeatBody.children(1))
        }
        AST.RHS(newId(), elem1 +: elem2)
    }

    def matchRHSs(node: Node): List[AST.RHS] = {
        val BindNode(_, BindNode(_, rhsBody: SequenceNode)) = node
        val rhs1 = matchRHS(rhsBody.children(0))
        val rhs2_ = unwindRepeat(rhsBody.children(1))
        val rhs2 = rhs2_ map { repeat =>
            val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
            matchRHS(repeatBody.children(3))
        }
        rhs1 +: rhs2
    }

    def matchRule(node: Node): AST.Rule = {
        val BindNode(_, seqBody: SequenceNode) = node
        val lhs = matchLHS(seqBody.children(0))
        val rhss = matchRHSs(seqBody.children(4))
        AST.Rule(newId(), lhs, rhss)
    }

    def matchTypeDef(node: Node): AST.TypeDef = {
        ???
    }

    def matchDef(node: Node): AST.Def = {
        val BindNode(NNonterminal(_, Nonterminal("Def"), _), body) = node

        body match {
            case BindNode(NNonterminal(_, Nonterminal("Rule"), _), ruleBody) => matchRule(ruleBody)
            case BindNode(NNonterminal(_, Nonterminal("TypeDef"), _), typeDefBody) => matchTypeDef(typeDefBody)
        }
    }

    def matchGrammar(node: Node): AST.Grammar = {
        val BindNode(NStart(_, _), BindNode(NNonterminal(_, Nonterminal("Grammar"), _), BindNode(_: NSequence, grammarBody: SequenceNode))) = node
        val def1 = matchDef(grammarBody.children(1))
        val def2_ = unwindRepeat(grammarBody.children(2))
        val def2 = def2_ map { repeat =>
            val BindNode(_, BindNode(_, repeatBody: SequenceNode)) = repeat
            matchDef(repeatBody.children(1))
        }
        AST.Grammar(newId(), def1 +: def2)
    }
}
