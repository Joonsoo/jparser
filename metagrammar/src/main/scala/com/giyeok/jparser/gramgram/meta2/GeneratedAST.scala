package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.Node

object GeneratedAST {

    case class Grammar(defs: List[Def])

    sealed trait Def

    sealed trait TypeDef extends Def

    case class ClassDef(typeName: Node, params: Option[List[ClassParam]]) extends SubType with TypeDef

    case class SuperDef(typeName: Node, subs: Option[List[SubType]]) extends SubType with TypeDef

    case class ClassParam(name: Node, typeDesc: Option[TypeDesc])

    case class TypeDesc(typ: ValueTypeDesc, optional: Node)

    sealed trait ValueTypeDesc

    case class ArrayTypeDesc(elemType: TypeDesc) extends ValueTypeDesc

    sealed trait SubType

    case class OnTheFlyTypeDef(name: Node, supers: Option[List[Node]]) extends ValueTypeDesc

    case class Rule(lhs: LHS, rhs: List[RHS]) extends Def

    case class LHS(name: Nonterminal, typeDesc: Option[TypeDesc])

    case class RHS(elems: List[Elem])

    sealed trait Elem

    sealed trait Processor extends Elem

    sealed trait PExpr extends BoundedPExpr with Processor

    case class BinOpExpr(op: Node, lhs: PExpr, rhs: PTerm) extends PExpr

    sealed trait PTerm extends PExpr

    case class PTermParen(expr: PExpr) extends PTerm

    case class Ref(idx: Node) extends PTerm

    case class PTermSeq(elems: Option[List[PExpr]]) extends PTerm

    case class BoundPExpr(ctx: Ref, expr: BoundedPExpr) extends PTerm

    sealed trait BoundedPExpr

    sealed trait AbstractConstructExpr extends PTerm

    case class ConstructExpr(typeName: Node, params: Option[List[PExpr]]) extends AbstractConstructExpr

    case class OnTheFlyTypeDefConstructExpr(typeDef: OnTheFlyTypeDef, params: Option[List[NamedParam]]) extends AbstractConstructExpr

    case class NamedParam(name: Node, typeDesc: Option[TypeDesc], expr: PExpr)

    sealed trait Symbol extends Elem

    sealed trait BinSymbol extends Symbol

    case class JoinSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

    case class ExceptSymbol(symbol1: BinSymbol, symbol2: PreUnSymbol) extends BinSymbol

    sealed trait PreUnSymbol extends BinSymbol

    case class FollowedBy(expr: PreUnSymbol) extends PreUnSymbol

    case class NotFollowedBy(expr: PreUnSymbol) extends PreUnSymbol

    sealed trait PostUnSymbol extends PreUnSymbol

    case class Repeat(expr: PostUnSymbol, repeat: Node) extends PostUnSymbol

    sealed trait AtomSymbol extends PostUnSymbol

    case class Paren(choices: InPlaceChoices) extends AtomSymbol

    case class EmptySeq() extends AtomSymbol

    case class InPlaceChoices(choices: List[InPlaceSequence])

    case class InPlaceSequence(seq: List[Symbol])

    case class Longest(choices: InPlaceChoices) extends AtomSymbol

    case class Nonterminal(name: Node) extends AtomSymbol

    sealed trait Terminal extends AtomSymbol

    case class AnyTerminal(c: Node) extends Terminal

    case class TerminalChoice(choices: List[TerminalChoiceElem]) extends AtomSymbol

    sealed trait TerminalChoiceElem

    case class TerminalChoiceRange(start: TerminalChoiceChar, end: TerminalChoiceChar) extends TerminalChoiceElem

    case class StringLiteral(value: List[StringChar]) extends AtomSymbol

    case class CharUnicode(code: List[Node]) extends StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChar extends Terminal

    case class CharAsIs(c: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    case class CharEscaped(escapeCode: Node) extends StringChar with TerminalChar with TerminalChoiceChar

    sealed trait TerminalChoiceChar extends TerminalChoiceElem

    sealed trait StringChar

}
